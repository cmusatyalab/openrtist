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

import os
import cv2
import numpy as np
import logging
from gabriel_server import cognitive_engine
from gabriel_protocol import gabriel_pb2
from google.protobuf.any_pb2 import Any
from openrtist_protocol import openrtist_pb2
from abc import abstractmethod

logger = logging.getLogger(__name__)


class OpenrtistEngine(cognitive_engine.Engine):
    ENGINE_NAME = 'openrtist'

    def __init__(self, default_style, compression_params, use_new_models=False):
        self.style = default_style
        self.compression_params = compression_params

        self.dir_path = os.getcwd()
        self.path = self.dir_path+('/../models_1p0/' if use_new_models else '/../models/')

        # The waterMark is of dimension 30x120
        wtr_mrk4 = cv2.imread('../wtrMrk.png',-1)

         # The RGB channels are equivalent
        self.mrk, _, _, mrk_alpha = cv2.split(wtr_mrk4)

        self.alpha = mrk_alpha.astype(float) / 255

        # TODO support server display

        logger.info('FINISHED INITIALISATION')

    @abstractmethod
    def change_style(self, new_style):
        pass

    @abstractmethod
    def preprocessing(self, img):
        '''Model-specific preprocessing'''
        pass

    @abstractmethod
    def inference(self, preprocessed):
        pass

    @abstractmethod
    def postprocessing(self, post_inference):
        '''Model-specific postprocessing'''
        pass


    def handle(self, from_client):
        if (from_client.payload_type != gabriel_pb2.PayloadType.IMAGE):
            return cognitive_engine.wrong_input_format_error(
                from_client.frame_id)

        engine_fields = cognitive_engine.unpack_engine_fields(
            openrtist_pb2.EngineFields, from_client)

        if engine_fields.style != self.style:
            self.style = self.change_style(engine_fields.style)
            logger.info('New Style: %s', self.style)

        image = self.process_image(from_client.payload)
        image = self._apply_watermark(image)

        _, jpeg_img=cv2.imencode('.jpg', image, self.compression_params)
        img_data = jpeg_img.tostring()

        result = gabriel_pb2.ResultWrapper.Result()
        result.payload_type = gabriel_pb2.PayloadType.IMAGE
        result.engine_name = self.ENGINE_NAME
        result.payload = img_data

        result_wrapper = gabriel_pb2.ResultWrapper()
        result_wrapper.frame_id = from_client.frame_id
        result_wrapper.status = gabriel_pb2.ResultWrapper.Status.SUCCESS
        result_wrapper.results.append(result)
        result_wrapper.engine_fields.Pack(engine_fields)

        return result_wrapper

    def process_image(self, image):

        # Preprocessing steps used by both engines
        np_data=np.fromstring(image, dtype=np.uint8)
        img=cv2.imdecode(np_data, cv2.IMREAD_COLOR)
        img=cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

        preprocessed = self.preprocessing(img)
        post_inference = self.inference(preprocessed)
        img_out = self.postprocessing(post_inference)
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
