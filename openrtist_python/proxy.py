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
import select
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

#sys.path.insert(0, "..")
#import zhuocv as zc

config.setup(is_streaming = True)

LOG_TAG = "Style Transfer Proxy: "

#display_list = config.DISPLAY_LIST
ANDROID_CLIENT=False


class StyleVideoApp(gabriel.proxy.CognitiveProcessThread):

    def __init__(self, image_queue, output_queue, engine_id, log_flag = True):
        super(StyleVideoApp, self).__init__(image_queue, output_queue, engine_id)
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
        self.style_type = "the-scream"
        self.content_transform = transforms.Compose([
            transforms.ToTensor(),
            transforms.Lambda(lambda x: x.mul(255))
        ])
        wtr_mrk4 = cv2.imread('../wtrMrk.png',-1) # The waterMark is of dimension 30x120
        self.mrk,_,_,mrk_alpha = cv2.split(wtr_mrk4) # The RGB channels are equivalent
        self.alpha = mrk_alpha.astype(float)/255
        self.stats = { "wait" : 0.0, "pre" : 0.0, "infer" : 0.0, "post" : 0.0, "count" : 0 }
        self.lasttime = time.time()
        self.lastcount = 0
        self.lastprint = self.lasttime
        print('FINISHED INITIALISATION')

    def add_to_byte_array(self, byte_array, extra_bytes):
        return struct.pack("!{}s{}s".format(len(byte_array),len(extra_bytes)), byte_array, extra_bytes)

    def handle(self, header, data):
        # PERFORM Cognitive Assistance Processing
        t0 = time.time()
        LOG.info("processing: ")
        LOG.info("%s\n" % header)
        start_time = time.time()
        if header.get('style',None) is not None:
            if header['style'] != self.style_type:
                    self.model = self.path + header['style'] + ".model"
                    print('NEW STYLE {}'.format(self.model))
                    self.style_model.load_state_dict(torch.load(self.model))
                    if (config.USE_GPU):
                        self.style_model.cuda()
                    self.style_type = header['style']

        np_data=np.fromstring(data, dtype=np.uint8)
        img_in=cv2.imdecode(np_data,cv2.IMREAD_COLOR)
        content_image = self.content_transform(img_in)
        content_image = content_image.unsqueeze(0)
        if (config.USE_GPU):
            content_image = content_image.cuda()
        content_image = Variable(content_image, volatile=True)
        t1 = time.time()
        output = self.style_model(content_image)
        t2 = time.time()
        img_out = output.data[0].cpu().numpy()
        np.clip(img_out, 0, 255, out=img_out)
        img_out = img_out.transpose(1, 2, 0)

        #Applying WaterMark
        img_mrk = img_out[-30:,-120:] # The waterMark is of dimension 30x120
        #img_mrk = (1-self.alpha)*img_mrk + self.alpha*self.mrk
        img_mrk[:,:,0] = (1-self.alpha)*img_mrk[:,:,0] + self.alpha*self.mrk
        img_mrk[:,:,1] = (1-self.alpha)*img_mrk[:,:,1] + self.alpha*self.mrk
        img_mrk[:,:,2] = (1-self.alpha)*img_mrk[:,:,2] + self.alpha*self.mrk
        img_out[-30:,-120:] = img_mrk
        img_out = img_out.astype('uint8')

        _, jpeg_img=cv2.imencode('.jpg', img_out)
        img_data = jpeg_img.tostring()
        #print('Compute Done time: {}'.format(time.time()-start_time))
        t3 = time.time();
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
    result_queue = multiprocessing.Queue()
    print result_queue._reader

    settings = gabriel.util.process_command_line(sys.argv[1:])

    ip_addr, port = gabriel.network.get_registry_server_address(settings.address)
    service_list = gabriel.network.get_service_list(ip_addr, port)
    LOG.info("Gabriel Server :")
    LOG.info(pprint.pformat(service_list))

    video_ip = service_list.get(gabriel.ServiceMeta.VIDEO_TCP_STREAMING_IP)
    video_port = service_list.get(gabriel.ServiceMeta.VIDEO_TCP_STREAMING_PORT)
    ucomm_ip = service_list.get(gabriel.ServiceMeta.UCOMM_SERVER_IP)
    ucomm_port = service_list.get(gabriel.ServiceMeta.UCOMM_SERVER_PORT)

    # image receiving and processing threads
    image_queue = Queue.Queue(gabriel.Const.APP_LEVEL_TOKEN_SIZE)
    print "TOKEN SIZE OF OFFLOADING ENGINE: %d" % gabriel.Const.APP_LEVEL_TOKEN_SIZE # TODO
    video_receive_client = gabriel.proxy.SensorReceiveClient((video_ip, video_port), image_queue)
    video_receive_client.start()
    video_receive_client.isDaemon = True
    dummy_video_app = StyleVideoApp(image_queue, result_queue, engine_id = 'style_python') # dummy app for image processing
    dummy_video_app.start()
    dummy_video_app.isDaemon = True

    # result publish
    result_pub = gabriel.proxy.ResultPublishClient((ucomm_ip, ucomm_port), result_queue)
    result_pub.start()
    result_pub.isDaemon = True

    try:
        while True:
            time.sleep(1)
    except Exception as e:
        pass
    except KeyboardInterrupt as e:
        sys.stdout.write("user exits\n")
    finally:
        if video_receive_client is not None:
            video_receive_client.terminate()
        if dummy_video_app is not None:
            print ( dummy_video_app.stats )
            dummy_video_app.terminate()
        #if acc_client is not None:
        #    acc_client.terminate()
        #if acc_app is not None:
        #    acc_app.terminate()
        result_pub.terminate()

