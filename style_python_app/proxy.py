#!/usr/bin/env python
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

#display_list = config.DISPLAY_LIST
ANDROID_CLIENT=False


class StyleVideoApp(gabriel.proxy.CognitiveProcessThread):

    def __init__(self, image_queue, output_queue, engine_id, log_flag = True):
        super(StyleVideoApp, self).__init__(image_queue, output_queue, engine_id)
        self.log_flag = log_flag
        self.is_first_image = True
        self.dir_path = os.getcwd()
        self.model = self.dir_path+'/models/the-scream.model'
        self.path = self.dir_path+'/models/'
        print('MODEL PATH {}'.format(self.path))  
    
        # initialize model
        self.style_model = TransformerNet()
        self.style_model.load_state_dict(torch.load(self.model))
        self.style_model.cuda()
        self.style_type = "the-scream"
        self.content_transform = transforms.Compose([
            transforms.ToTensor(),
            transforms.Lambda(lambda x: x.mul(255))
        ])
 
        print('FINISHED INITIALISATION')
        #sound_server_addr = ("128.2.209.111", 8021)
        #try:
        #    self.style_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        #    self.style_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        #    self.style_sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        #    self.style_sock.connect(sound_server_addr)
        #    LOG.info(LOG_TAG + "connected to sound playing server")
        #except socket.error as e:
        #    LOG.warning(LOG_TAG + "Failed to connect to sound server at %s" % str(sound_server_addr))

    def add_to_byte_array(self, byte_array, extra_bytes):
        return struct.pack("!{}s{}s".format(len(byte_array),len(extra_bytes)), byte_array, extra_bytes)

    def handle(self, header, data):
        # PERFORM Cognitive Assistance Processing
        LOG.info("processing: ")
        LOG.info("%s\n" % header)
        start_time = time.time()
        if header.get('style',None) is not None:
            if header['style'] != self.style_type:
                    self.model = self.path + header['style'] + ".model"
                    print('NEW STYLE {}'.format(self.model))
                    self.style_model.load_state_dict(torch.load(self.model))
                    self.style_model.cuda()
                    self.style_type = header['style']

        np_data=np.fromstring(data, dtype=np.uint8)
        #img_in = Image.fromarray(np_data)
        img_in=cv2.imdecode(np_data,cv2.IMREAD_COLOR)
        #img_in = np.array(img_in).transpose(2, 0, 1)
        #content_image = torch.from_numpy(img_in).float()
        content_image = self.content_transform(img_in)
        content_image = content_image.unsqueeze(0)
        content_image = content_image.cuda()
        content_image = Variable(content_image, volatile=True)
        
        #content_image = img_in.unsqueeze(0)
        #content_image = content_image.cuda()
        #
        #content_image = Variable(content_image, volatile=True)

        output = self.style_model(content_image)
        #img_out = output.data[0].cpu().numpy()
        #img_out = output.data[0].clone().cpu().clamp(0, 255).numpy()
        #print('Network time After frwd: {}'.format(time.time()-start_time))
        img_out = output.data[0].cpu().numpy()
        #print('Network time gpu-cpu: {}'.format(time.time()-start_time))
        #img_out = np.clip(img_out,0,255)
        np.clip(img_out, 0, 255, out=img_out)
        img_out = img_out.transpose(1, 2, 0).astype('uint8')
        #print('Network time clip transpose: {}'.format(time.time()-start_time))
        _, jpeg_img=cv2.imencode('.jpg', img_out)
        #print('Network time encoding: {}'.format(time.time()-start_time))
        img_data = jpeg_img.tostring()
        print('Compute Done time: {}'.format(time.time()-start_time))
        #header['status']='success'
        # numpy tostring is equal to tobytes
        #rtn_data=img_data
        # header has (offset, size) for each data type
        #header={'data_size' : len(img_data)}
        #header_json=json.dumps(header)
        #packet = struct.pack("!I",len(header_json))
        #self.style_sock.sendall(packet)
        #packet = struct.pack("!I%ds" % len(header_json), len(header_json), header_json)
        #self.style_sock.sendall(packet)
        #packet_2 = struct.pack("!I%ds" % len(img_data), len(img_data), img_data)
        ##packet = self.add_to_byte_array(packet,packet_2)
        #self.style_sock.sendall(packet_2)
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
            dummy_video_app.terminate()
        #if acc_client is not None:
        #    acc_client.terminate()
        #if acc_app is not None:
        #    acc_app.terminate()
        result_pub.terminate()

