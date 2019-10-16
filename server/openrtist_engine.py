import os
import cv2
import numpy as np
import logging
import torch
from torch.autograd import Variable
from torch.optim import Adam
from torch.utils.data import DataLoader
from torchvision import datasets
from torchvision import transforms
from PIL import Image
from transformer_net import TransformerNet
from gabriel_server import cognitive_engine
from gabriel_protocol import gabriel_pb2
from google.protobuf.any_pb2 import Any
from openrtist_protocol import openrtist_pb2

logger = logging.getLogger(__name__)


# TODO: support openvino
class OpenrtistEngine(cognitive_engine.Engine):
    def __init__(self, use_gpu, engine_name, default_style, compression_params):
        self.dir_path = os.getcwd()
        self.model = self.dir_path+'/../models/the_scream.model'
        self.path = self.dir_path+'/../models/'

        self.style_model = TransformerNet()
        self.style_model.load_state_dict(torch.load(self.model))

        self.use_gpu = use_gpu
        if (use_gpu):
            self.style_model.cuda()

        self.content_transform = transforms.Compose([transforms.ToTensor()])
        self.style = default_style

        wtr_mrk4 = cv2.imread('../wtrMrk.png',-1) # The waterMark is of dimension 30x120
        self.mrk,_,_,mrk_alpha = cv2.split(wtr_mrk4) # The RGB channels are equivalent
        self.alpha = mrk_alpha.astype(float)/255

        self.engine_name = engine_name
        self.compression_params = compression_params

        # TODO support server display

        logger.info('FINISHED INITIALISATION')

    def handle(self, from_client):
        engine_fields = cognitive_engine.unpack_engine_fields(
            openrtist_pb2.EngineFields, from_client)

        if engine_fields.style != self.style:
            self.model = self.path + engine_fields.style + ".model"
            self.style_model.load_state_dict(torch.load(self.model))
            if (self.use_gpu):
                self.style_model.cuda()
            self.style = engine_fields.style
            logger.info('New Style: %s', self.style)

        if (from_client.payload_type != gabriel_pb2.PayloadType.IMAGE):
            return cognitive_engine.wrong_input_format_error(
                from_client.frame_id)

        image = self._process_image(from_client.payload)
        image = self._apply_watermark(image)

        _, jpeg_img=cv2.imencode('.jpg', image, self.compression_params)
        img_data = jpeg_img.tostring()

        result = gabriel_pb2.ResultWrapper.Result()
        result.payload_type = gabriel_pb2.PayloadType.IMAGE
        result.engine_name = self.engine_name
        result.payload = img_data

        result_wrapper = gabriel_pb2.ResultWrapper()
        result_wrapper.frame_id = from_client.frame_id
        result_wrapper.status = gabriel_pb2.ResultWrapper.Status.SUCCESS
        result_wrapper.results.append(result)
        result_wrapper.engine_fields.Pack(engine_fields)

        return result_wrapper

    def _process_image(self, image):
        np_data=np.fromstring(image, dtype=np.uint8)
        img=cv2.imdecode(np_data,cv2.IMREAD_COLOR)
        img=cv2.cvtColor(img,cv2.COLOR_BGR2RGB)
        content_image = self.content_transform(img)
        if (self.use_gpu):
            content_image = content_image.cuda()
        content_image = content_image.unsqueeze(0)
        content_image = Variable(content_image, volatile=True)

        output = self.style_model(content_image)
        img_out = output.data[0].clamp(0, 255).cpu().numpy()
        img_out = img_out.transpose(1, 2, 0)

        return img_out

    def _apply_watermark(self, image):
        img_mrk = image[-30:,-120:] # The waterMark is of dimension 30x120
        img_mrk[:,:,0] = (1-self.alpha)*img_mrk[:,:,0] + self.alpha*self.mrk
        img_mrk[:,:,1] = (1-self.alpha)*img_mrk[:,:,1] + self.alpha*self.mrk
        img_mrk[:,:,2] = (1-self.alpha)*img_mrk[:,:,2] + self.alpha*self.mrk
        image[-30:,-120:] = img_mrk
        img_out = image.astype('uint8')
        img_out = cv2.cvtColor(img_out,cv2.COLOR_RGB2BGR)

        return img_out
