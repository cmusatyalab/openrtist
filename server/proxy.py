#!/usr/bin/env python
#
# OpenRTiST
#   - Real-time Style Transfer
#
#   Author: Zhuo Chen <zhuoc@cs.cmu.edu>
#           Shilpa George <shilpag@andrew.cmu.edu>
#           Thomas Eiszler <teiszler@andrew.cmu.edu>
#           Padmanabhan Pillai <padmanabhan.s.pillai@intel.com>
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
import cv2
import config

if not hasattr(config, 'USE_OPENVINO'):
    try:
        from openvino.inference_engine import IENetwork, IEPlugin
        config.USE_OPENVINO = True
        print "Autodetect:  Loaded OpenVINO"
    except ImportError:
        config.USE_OPENVINO = False
        print "Autodetect:  failed to load OpenVINO; fallback to pyTorch"
elif config.USE_OPENVINO:
    from openvino.inference_engine import IENetwork, IEPlugin

if config.USE_OPENVINO==False:
    import torch
    from torch.autograd import Variable
    from torch.optim import Adam
    from torch.utils.data import DataLoader
    from torchvision import datasets
    from torchvision import transforms
    from PIL import Image
    import utils
    from transformer_net import TransformerNet
    config.USE_OPENVINO = False

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

        # initialize model
        if (config.USE_OPENVINO):
            # Plugin initialization for specified device and load extensions library if specified
            if config.USE_GPU==True:
                self.device = "GPU"
            else:
                self.device = "CPU"
            self.plugin = IEPlugin(device=self.device, plugin_dirs=None)
            if self.device == "CPU":
                #plugin.add_cpu_extension(args.cpu_extension)
                model_xml = self.path+"32.xml"
                model_bin_suff = "-32.bin"
                self.plugin.add_cpu_extension("libcpu_extension_sse4.so")  # also avx2, but probably not a significant difference for the MVN layer needed here
            else:
                model_xml = self.path+"16v2.xml"
                model_bin_suff = "-16.bin"
                self.plugin.set_config({'CONFIG_FILE' : self.dir_path+"/../clkernels/mvn_custom_layer.xml"})

            self.exec_nets = {}
            for name in [ n[:-7] for n in os.listdir(self.path) if n.endswith(model_bin_suff)]:
                model_bin = self.path+name+model_bin_suff;
                # Read IR
                print("Loading network files:\n\t{}\n\t{}".format(model_xml, model_bin))
                net = IENetwork(model=model_xml, weights=model_bin)
            
                if self.plugin.device == "CPU":
                    supported_layers = self.plugin.get_supported_layers(net)
                    not_supported_layers = [l for l in net.layers.keys() if l not in supported_layers]
                    if len(not_supported_layers) != 0:
                        print("Following layers are not supported by the plugin for specified device {}:\n {}".
                                format(self.plugin.device, ', '.join(not_supported_layers)))
                        #print("Please try to specify cpu extensions library path in sample's command line parameters using -l "
                        #          "or --cpu_extension command line argument")
                        sys.exit(1)
            
                #log.info("Preparing input blobs")
                self.input_blob = next(iter(net.inputs))
                self.out_blob = next(iter(net.outputs))
                net.batch_size = 1
            
                # Loading model to the plugin
                print("Loading model to the plugin")
                self.exec_nets[ name ] = self.plugin.load(network=net)
                self.n, self.c, self.h, self.w = net.inputs[self.input_blob].shape
                del net
        else:
            self.style_model = TransformerNet()
            self.style_model.load_state_dict(torch.load(self.model))
            if (config.USE_GPU):
                self.style_model.cuda()
            self.content_transform = transforms.Compose([
            transforms.ToTensor()])
        self.style_type = "the_scream"
        
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
                if (config.USE_OPENVINO == False):
                    self.model = self.path + header['style'] + ".model"
                    self.style_model.load_state_dict(torch.load(self.model))
                    if (config.USE_GPU):
                        self.style_model.cuda()
                self.style_type = header['style']
                print('NEW STYLE {}'.format(self.style_type))

        # Preprocessing of input image
        np_data=np.fromstring(data, dtype=np.uint8)
        img=cv2.imdecode(np_data,cv2.IMREAD_COLOR)
        img=cv2.cvtColor(img,cv2.COLOR_BGR2RGB)
        if (config.USE_OPENVINO):
            if img.shape[:-1] != (self.h, self.w):
                LOG.warning("Image is resized from {} to {}".format(img.shape[:-1], (self.h, self.w)))
                img = cv2.resize(img, (self.w, self.h))
            img = img.transpose((2, 0, 1))  # Change data layout from HWC to CHW
            imgs = [ img ]
        else:
            content_image = self.content_transform(img)
            if (config.USE_GPU):
                content_image = content_image.cuda()
            content_image = content_image.unsqueeze(0)
            content_image = Variable(content_image, volatile=True)

        header['status'] =  'success'
        t1 = time.time()
        if (config.USE_OPENVINO):
            data = self.exec_nets[ self.style_type ].infer(inputs={self.input_blob: imgs})
        else:
            output = self.style_model(content_image)
            img_out = output.data[0].clamp(0, 255).cpu().numpy()
        t2 = time.time()
        if (config.USE_OPENVINO):
            img_out = data[self.out_blob][0]
            img_out = np.swapaxes(img_out, 0, 2)
            img_out = np.swapaxes(img_out, 0, 1)
            img_out[img_out < 0] = 0
            img_out[img_out > 255] = 255
        else:
            img_out = img_out.transpose(1, 2, 0)

        #Applying WaterMark
        img_mrk = img_out[-30:,-120:] # The waterMark is of dimension 30x120
        img_mrk[:,:,0] = (1-self.alpha)*img_mrk[:,:,0] + self.alpha*self.mrk
        img_mrk[:,:,1] = (1-self.alpha)*img_mrk[:,:,1] + self.alpha*self.mrk
        img_mrk[:,:,2] = (1-self.alpha)*img_mrk[:,:,2] + self.alpha*self.mrk
        img_out[-30:,-120:] = img_mrk
        img_out = img_out.astype('uint8')
        img_out = cv2.cvtColor(img_out,cv2.COLOR_RGB2BGR)

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

