#!/usr/bin/env python
#
# Cloudlet Infrastructure for Mobile Computing
#   - Task Assistance
#
#   Author: Zhuo Chen <zhuoc@cs.cmu.edu>
#           Shilpa George <shilpag@andrew.cmu.edu>
#
#   Copyright (C) 2011-2013 Carnegie Mellon University
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

import json
import multiprocessing
import numpy as np
import os
import pprint
import Queue
import random
import string
import struct
import socket
import sys
import time
import threading
import config

import torch
from torch.autograd import Variable
from torch.optim import Adam
from torch.utils.data import DataLoader
from torchvision import datasets
from torchvision import transforms
from PIL import Image
import utils
from transformer_net import TransformerNet
import cv2

if os.path.isdir("../.."):
    sys.path.insert(0, "../..")

import gabriel
import gabriel.proxy
LOG = gabriel.logging.getLogger(__name__)
config.setup(is_streaming = True)
LOG_TAG = "Style Transfer Proxy: "

class StyleServer(gabriel.proxy.CognitiveProcessThread):
    def __init__(self, image_queue, output_queue, engine_id, log_flag = True):
        super(StyleServer, self).__init__(image_queue, output_queue, engine_id)
        self.log_flag = log_flag
        self.is_first_image = True
        self.dir_path = os.getcwd()
        self.model = self.dir_path+'/../models/the_scream.model'
        self.path = self.dir_path+'/../models/'
        print('MODEL PATH {}'.format(self.path))  

        # initialize model
        self.style_model = TransformerNet()
        self.style_model.load_state_dict(torch.load(self.model))
        if (config.USE_GPU):
            self.style_model.cuda()
        self.style_type = "the_scream"
        self.content_transform = transforms.Compose([
            transforms.ToTensor()
        ])
        wtr_mrk4 = cv2.imread('../wtrMrk.png',-1) # The waterMark is of dimension 30x120
        self.mrk,_,_,mrk_alpha = cv2.split(wtr_mrk4) # The RGB channels are equivalent
        self.alpha = mrk_alpha.astype(float)/255
        self.stats = { "wait" : 0.0, "pre" : 0.0, "infer" : 0.0, "post" : 0.0, "count" : 0 }
        self.lasttime = time.time()
        self.lastcount = 0
        self.lastprint = self.lasttime
        print('FINISHED INITIALISATION')

    def handle(self, header, data):
        # Receive data from control VM
        t0 = time.time()
        LOG.info("processing: ")
        LOG.info("%s\n" % header)
        header['status'] = "nothing"
        result = {}
        if header.get('style',None) is not None:
            if header['style'] != self.style_type:
                self.model = self.path + header['style'] + ".model"
                print('NEW STYLE {}'.format(self.model))
                self.style_model.load_state_dict(torch.load(self.model))
                if (config.USE_GPU):
                    self.style_model.cuda()
                self.style_type = header['style']

        # Preprocessing of input image
        np_data=np.fromstring(data, dtype=np.uint8)
        img=cv2.imdecode(np_data,cv2.IMREAD_COLOR)
        content_image = self.content_transform(img)
        if (config.USE_GPU):
            content_image = content_image.cuda()
        content_image = content_image.unsqueeze(0)
        content_image = Variable(content_image, volatile=True)

        t1 = time.time()
        output = self.style_model(content_image)
        header['status'] =  'success'
        img_out = output.data[0].clamp(0, 255).cpu().numpy()
        t2 = time.time()
        img_out = img_out.transpose(1, 2, 0)

        #Applying WaterMark
        img_mrk = img_out[-30:,-120:] # The waterMark is of dimension 30x120
        img_mrk[:,:,0] = (1-self.alpha)*img_mrk[:,:,0] + self.alpha*self.mrk
        img_mrk[:,:,1] = (1-self.alpha)*img_mrk[:,:,1] + self.alpha*self.mrk
        img_mrk[:,:,2] = (1-self.alpha)*img_mrk[:,:,2] + self.alpha*self.mrk
        img_out[-30:,-120:] = img_mrk
        img_out = img_out.astype('uint8')

        compression_params = [cv2.IMWRITE_JPEG_QUALITY, 67]
        _, jpeg_img=cv2.imencode('.jpg', img_out, compression_params)
        img_data = jpeg_img.tostring()

        t3 = time.time()
        header[gabriel.Protocol_measurement.JSON_KEY_APP_SYMBOLIC_TIME] = t3
        self.stats["wait"] += t0 - self.lasttime
        self.stats["pre"] += t1 - t0
        self.stats["infer"] += t2 - t1
        self.stats["post"] += t3 - t2
        self.stats["count"] += 1
        if (t3 - self.lastprint > 5):
            print (" current:  pre {0:.1f} ms, infer {1:.1f} ms, post {2:.1f} ms, wait {3:.1f} ms, fps {4:.2f} "
                      .format( (t1-t0)*1000, (t2-t1)*1000, (t3-t2)*1000, (t0-self.lasttime)*1000, 1.0/(t3-self.lasttime) ) )
            print (" avg fps: {0:.2f}".format( (self.stats["count"]-self.lastcount)/(t3-self.lastprint)) )
            self.lastcount = self.stats["count"]
            self.lastprint = t3
        self.lasttime = t3
        return img_data

if __name__ == "__main__":
    settings = gabriel.util.process_command_line(sys.argv[1:])

    ip_addr, port = gabriel.network.get_registry_server_address(settings.address)
    service_list = gabriel.network.get_service_list(ip_addr, port)
    LOG.info("Gabriel Server :")
    LOG.info(pprint.pformat(service_list))

    video_ip = service_list.get(gabriel.ServiceMeta.VIDEO_TCP_STREAMING_IP)
    video_port = service_list.get(gabriel.ServiceMeta.VIDEO_TCP_STREAMING_PORT)
    ucomm_ip = service_list.get(gabriel.ServiceMeta.UCOMM_SERVER_IP)
    ucomm_port = service_list.get(gabriel.ServiceMeta.UCOMM_SERVER_PORT)

    # Image receiving thread
    image_queue = Queue.Queue(gabriel.Const.APP_LEVEL_TOKEN_SIZE)
    print "TOKEN SIZE OF OFFLOADING ENGINE: %d" % gabriel.Const.APP_LEVEL_TOKEN_SIZE
    video_streaming = gabriel.proxy.SensorReceiveClient((video_ip, video_port), image_queue)
    video_streaming.start()
    video_streaming.isDaemon = True

    # App proxy
    result_queue = multiprocessing.Queue()
    app = StyleServer(image_queue, result_queue, engine_id = "Style")
    app.start()
    app.isDaemon = True

    # Publish result
    result_pub = gabriel.proxy.ResultPublishClient((ucomm_ip, ucomm_port), result_queue)
    result_pub.start()
    result_pub.isDaemon = True

    try:
        while True:
            time.sleep(1)
    except Exception as e:
        pass
    except KeyboardInterrupt as e:
        sys.stdout.write("User exits\n")
    finally:
        if video_streaming is not None:
            video_streaming.terminate()
        if app is not None:
            print ( app.stats )
            app.terminate()
        result_pub.terminate()

