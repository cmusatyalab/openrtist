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
from gabriel_client.server_comm import WebsocketClient
from abc import abstractmethod
from ocv_client import OcvClient


START_STYLE_STRING = 'udnie'
ENGINE_NAME = 'openrtist'


class OpenrtistClient(OcvClient):
    @abstractmethod
    def consume_update(self, rgb_frame, style):
        pass

    def __init__(self, server_ip):
        super().__init__(
            server_ip, Config.PORT, cv2.VideoCapture(-1),
            START_STYLE_STRING, ENGINE_NAME)

        self.style_array = os.listdir('./style-image')
        random.shuffle(self.style_array)
        self.SEC = Config.TIME_SEC
        self.FPS = Config.CAM_FPS
        self.INTERVAL = self.SEC*self.FPS
        self.video_capture.set(cv2.CAP_PROP_FPS, self.FPS)

        self.style_num = 0

    def input_processor(self, frame):
        if (self.get_frame_id() % self.INTERVAL) == 0:
            self.style_num = (self.style_num + 1) % len(self.style_array)
            self.style_string = self.style_array[self.style_num].split(".")[0]

        frame = cv2.flip(frame, 1)
        frame = cv2.resize(frame, (Config.IMG_WIDTH, Config.IMG_HEIGHT))

        return frame

    def output_frame(self, frame, style):
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        self.consume_update(rgb_frame, style)
