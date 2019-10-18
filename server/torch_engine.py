# OpenRTiST
#   - Real-time Style Transfer
#
#   Author: Zhuo Chen <zhuoc@cs.cmu.edu>
#           Shilpa George <shilpag@andrew.cmu.edu>
#           Thomas Eiszler <teiszler@andrew.cmu.edu>
#           Padmanabhan Pillai <padmanabhan.s.pillai@intel.com>
#           Roger Iyengar <iyengar@cmu.edu>
#
#   Copyright (C) 2011-2019 Carnegie Mellon University
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
#
# Portions of this code borrow from sample code distributed as part of 
# Intel OpenVino, which is also distributed under the Apache License.
# 
# Portions of this code were modified from sampled code distributed as part of
# the fast_neural_style example that is part of the pytorch repository and is
# distributed under the BSD 3-Clause License.
# https://github.com/pytorch/examples/blob/master/LICENSE

from openrtist_engine import OpenrtistEngine
from torch.autograd import Variable
from transformer_net import TransformerNet
from torchvision import transforms
import torch


class TorchEngine(OpenrtistEngine):
    def __init__(self, use_gpu, default_style, compression_params):
        super().__init__(default_style, compression_params)

        self.use_gpu = use_gpu

        self.style_model = TransformerNet()
        self.style_model.load_state_dict(torch.load(self.model))

        if (self.use_gpu):
            self.style_model.cuda()

        self.content_transform = transforms.Compose([transforms.ToTensor()])

    def change_style(self, new_style):
        filename = self.path + new_style + ".model"
        try:
            self.style_model.load_state_dict(torch.load(filename))
            self.model = filename
        except:
            self.style_model.load_state_dict(torch.load(self.model))
            new_style = self.style
        if (self.use_gpu):
            self.style_model.cuda()
        return new_style

    def preprocessing(self, img):
        content_image = self.content_transform(img)
        if (self.use_gpu):
            content_image = content_image.cuda()
        content_image = content_image.unsqueeze(0)
        return Variable(content_image, volatile=True)


    def inference(self, preprocessed):
        output = self.style_model(preprocessed)
        return output.data[0].clamp(0, 255).cpu().numpy()

    def postprocessing(self, post_inference):
        return post_inference.transpose(1, 2, 0)
