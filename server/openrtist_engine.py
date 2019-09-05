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
            self.model = self.path + header['style'] + ".model"
            self.style_model.load_state_dict(torch.load(self.model))
            if (self.use_gpu):
                self.style_model.cuda()
            self.style_type = header['style']
            logger.info('New Style: %s', self.style_type)

        if (input.type != gabriel_pb2.Input.Type.IMAGE):
            return self.error_output(input.frame_id)

        self.process_image(input.payload)



    def process_image(self, image):
