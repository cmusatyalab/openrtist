#! /usr/bin/env python3

# Copyright 2018 Carnegie Mellon University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import argparse
import signal
import socket
import struct
import threading
import queue as Queue
from io import StringIO
import cv2
import json
import time
import sys
import numpy as np
from config import Config
import base64
import os
from socketLib import ClientCommand, ClientReply, SocketClientThread
import random
from gabriel_protocol import gabriel_pb2
import openrtist_pb2
from gabriel_client.server_comm import WebsocketClient


START_STYLE_STRING = 'udnie'
ENGINE_NAME = 'openrtist'


class OpenrtistClient(WebsocketClient):
    def __init__(self, server_ip, pyqt_signal):
        super().__init__(server_ip, Config.PORT)

        self.style_string = START_STYLE_STRING
        self.style_array = os.listdir('./style-image')
        random.shuffle(self.style_array)
        self.length = len(self.style_array)
        self.SEC = Config.TIME_SEC
        self.FPS = Config.CAM_FPS
        self.INTERVAL = self.SEC*self.FPS
        self.video_capture = cv2.VideoCapture(-1)
        self.video_capture.set(cv2.CAP_PROP_FPS, self.FPS)

        self.style_num = 0
        self.pyqt_signal = pyqt_signal

    def producer(self):
        if (self.get_frame_id() % self.INTERVAL) == 0:
            self.style_string = (
                self.style_array[style_num%self.length].split(".")[0])
            self.style_num += 1

        ret, frame = self.video_capture.read()
        frame = cv2.flip(frame, 1)
        frame = cv2.resize(frame, (Config.IMG_WIDTH,Config.IMG_HEIGHT))
        ret, jpeg_frame=cv2.imencode('.jpg', frame)

        from_client = gabriel_pb2.FromClient()
        from_client.payload_type = gabriel_pb2.PayloadType.IMAGE
        from_client.engine_name = ENGINE_NAME
        from_client.payload = jpeg_frame.tostring()

        engine_fields = openrtist_pb2.EngineFields()
        engine_fields.style = self.style_string
        from_client.engine_fields.Pack(engine_fields)

        return from_client

    def consumer(self, result_wrapper):
        if len(result_wrapper.results) == 1:
            result = result_wrapper[0]
            if result.payload_type == gabriel_pb2.PayloadType.IMAGE:
                if result.engine_name == ENGINE_NAME:
                    img = result.payload
                    np_data=np.fromstring(img, dtype=np.uint8)
                    frame=cv2.imdecode(np_data,cv2.IMREAD_COLOR)
                    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

                    self.pyqt_signal.emit(rgb_frame, resp_header['style'])
                else:
                    logger.error('Got result from engine %s',
                                 result.engine_name)
            else:
                logger.error('Got result of type %s', result.payload_type.name)
        else:
            logger.error('Got %d results in output',
                         len(result_wrapper.results))


    def start(self, sig_frame_available=None):
        self._result_receiving_thread.start()
        sleep(0.1)
        self._video_streaming_thread.start()

        if sig_frame_available is None:

        while True:
            resp = self._result_reply_q.get()
            # connect and send also send reply to reply queue without any data attached
            if resp.type == ClientReply.SUCCESS and resp.data is not None:
                tkn_time = time.time()
                #print(tkn_time)
                (resp_header, resp_data) =resp.data
                resp_header=json.loads(resp_header)

    def cleanup(self):
        if self._rgbpipe is not None:
            self._rgbpipe.close()

def _load_style_images(style_dir_path='style-image'):
    style_name_to_image = {}
    for image_name in os.listdir(style_dir_path):
        im = cv2.imread(os.path.join(style_dir_path, image_name))
        im = cv2.cvtColor(im, cv2.COLOR_BGR2RGB)
        style_name_to_image[os.path.splitext(image_name)[0]] = im
    return style_name_to_image
