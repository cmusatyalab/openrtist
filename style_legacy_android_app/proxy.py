#!/usr/bin/env python
from base64 import b64encode, b64decode
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
from PIL import Image
import io


import torch
from torch.autograd import Variable
from torch.optim import Adam
from torch.utils.data import DataLoader
from torchvision import datasets
from torchvision import transforms
from PIL import Image
import utils
from transformer_net import TransformerNet
from vgg import Vgg16
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

display_list = config.DISPLAY_LIST

class StyleServer(gabriel.proxy.CognitiveProcessThread):
    def __init__(self, image_queue, output_queue, engine_id, log_flag = True):
        super(StyleServer, self).__init__(image_queue, output_queue, engine_id)
        self.log_flag = log_flag
        self.is_first_image = True
        self.dir_path = os.getcwd()
        self.model = self.dir_path+'/models/the_scream.model'
        self.path = self.dir_path+'/models/'
        print('MODEL PATH {}'.format(self.path))  

        # initialize model
        self.style_model = TransformerNet()
        self.style_model.load_state_dict(torch.load(self.model))
        self.style_model.cuda()
        self.style_type = "the_scream"
        self.content_transform = transforms.Compose([
            transforms.ToTensor(),
            transforms.Lambda(lambda x: x.mul(255))
        ])
 
        print('FINISHED INITIALISATION')


    def handle(self, header, data):
        # Receive data from control VM
        LOG.info("received new image")
        header['status'] = "nothing"
        result = {}
	if header.get('style',None) is not None:
	    if header['style'] != self.style_type:
                self.model = self.path + header['style'] + ".model"
                print('NEW STYLE {}'.format(self.model))
                self.style_model.load_state_dict(torch.load(self.model))
                self.style_model.cuda()
                self.style_type = header['style']  

        # Preprocessing of input image
        #img_array = np.asarray(bytearray(data), dtype=np.uint8)
        #print(data.shape)
        #print(data.shape)
        img = Image.open(io.BytesIO(data)) 
        #img = cv2.imdecode(img_array, -1)
        #print("CAMEHERE")
        #print img.shape
        #img = cv2.cvtColor(img,cv2.COLOR_BGR2RGB)
        #print("NOTHERE")
        if self.is_first_image:
            #print('DIMENSION {}'.format(img.shape))
            self.is_first_image = False
        content_image = self.content_transform(img)
        content_image = content_image.unsqueeze(0)
        content_image = content_image.cuda()
        content_image = Variable(content_image, volatile=True)

        output = self.style_model(content_image)
        header['status'] =  'success'
        img_out = output.data[0].clone().cpu().clamp(0, 255).numpy()
        img_out = img_out.transpose(1, 2, 0).astype('uint8')
        #img_out = cv2.cvtColor(img_out,cv2.COLOR_BGR2RGB)
        #print img_out.shape
        #img_out = cv2.resize(img_out, (320, 240))
        pil_img = Image.fromarray(img_out)
        #img_io = io.StringIO()
        img_io = io.BytesIO()
        pil_img.save(img_io, 'JPEG', quality=70)
        img_io.seek(0)
        result['image'] = b64encode(img_io.read())
        #pil_img = Image.fromarray(img)
        #buff = BytesIO()
        #pil_img.save(buff, format="JPEG")
        #new_image_string = base64.b64encode(buff.getvalue()).decode("utf-8")
        # Send an image to the gabriel client in order to view camera better
        # TODO: Create a better UI so this hack isn't necessary
        #if self.first_frame:
        #    image_path = '/home/ubuntu/gabriel-apps/aperture/test/Pictures/plant.jpg'
        #    result['image'] = b64encode(zc.cv_image2raw(cv2.imread(image_path, 1)))
        #    self.first_frame = False

        # TODO: There is a lot more to do here for future iterations

        header[gabriel.Protocol_measurement.JSON_KEY_APP_SYMBOLIC_TIME] = time.time()
        return json.dumps(result)


if __name__ == "__main__":
    # shared between two proxies
    
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
            app.terminate()
        result_pub.terminate()
