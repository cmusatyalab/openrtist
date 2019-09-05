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
import utils
from transformer_net import TransformerNet
from gabriel_server.cognitive_engine import Engine
from gabriel_server import gabriel_pb2


DEFAULT_STYLE = 'the_scream'
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]


logger = logging.getLogger(__name__)


# TODO: support openvino
class OpenrtistEngine(Engine):
    def __init__(self, use_gpu):
        self.dir_path = os.getcwd()
        self.model = self.dir_path+'/../models/the_scream.model'
        self.path = self.dir_path+'/../models/'

        self.style_model = TransformerNet()
        self.style_model.load_state_dict(torch.load(self.model))

        self.use_gpu = use_gpu
        if (use_gpu):
            self.style_model.cuda()


        self.content_transform = transforms.Compose([transforms.ToTensor()])
        self.style = DEFAULT_STYLE

        wtr_mrk4 = cv2.imread('../wtrMrk.png',-1) # The waterMark is of dimension 30x120
        self.mrk,_,_,mrk_alpha = cv2.split(wtr_mrk4) # The RGB channels are equivalent
        self.alpha = mrk_alpha.astype(float)/255

        # TODO support server display

        logger.info('FINISHED INITIALISATION')

    def handle(self, input):
        if input.style != self.style:
            self.model = self.path + input.syle + ".model"
            self.style_model.load_state_dict(torch.load(self.model))
            if (self.use_gpu):
                self.style_model.cuda()
            self.style_type = input.style
            logger.info('New Style: %s', self.style_type)

        if (input.type != gabriel_pb2.Input.Type.IMAGE):
            return self.error_output(input.frame_id)

        image = self.process_image(input.payload)
        image = self.apply_watermark(image)

        _, jpeg_img=cv2.imencode('.jpg', image, COMPRESSION_PARAMS)
        img_data = jpeg_img.tostring()

        return img_data


    def process_image(self, image):
        np_data=np.fromstring(image, dtype=np.uint8)
        img=cv2.imdecode(np_data,cv2.IMREAD_COLOR)
        img=cv2.cvtColor(img,cv2.COLOR_BGR2RGB)
        content_image = self.content_transform(img)
        if (config.USE_GPU):
            content_image = content_image.cuda()
        content_image = content_image.unsqueeze(0)
        content_image = Variable(content_image, volatile=True)

        output = self.style_model(content_image)
        img_out = output.data[0].clamp(0, 255).cpu().numpy()

        return img_out

    def apply_watermark(self, image):
        img_mrk = image[-30:,-120:] # The waterMark is of dimension 30x120
        img_mrk[:,:,0] = (1-self.alpha)*img_mrk[:,:,0] + self.alpha*self.mrk
        img_mrk[:,:,1] = (1-self.alpha)*img_mrk[:,:,1] + self.alpha*self.mrk
        img_mrk[:,:,2] = (1-self.alpha)*img_mrk[:,:,2] + self.alpha*self.mrk
        image[-30:,-120:] = img_mrk
        img_out = image.astype('uint8')
        img_out = cv2.cvtColor(img_out,cv2.COLOR_RGB2BGR)

        return img_out
