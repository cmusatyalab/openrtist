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

import cv2
import numpy as np
import config
from gabriel_client.opencv_client import OpencvClient
from abc import abstractmethod


class Client(OpencvClient):
    @abstractmethod
    def consume_frame_style(self, frame, style):
        pass

    def produce_engine_fields(self):
        engine_fields = openrtist_pb2.EngineFields()
        engine_fields.style = self.style
        return engine_fields

    def consume_frame(self, frame, packed_engine_fields):
        engine_fields = openrtist_pb2.EngineFields()
        packed_engine_fields.Unpack(engine_fields)

        self.consume_frame_style(frame, engine_fields.style)

    def __init__(self, server_ip, video_capture):
        super().__init__(
            server_ip, config.PORT, video_capture, config.ENGINE_NAME)
        self.style = config.START_STYLE_STRING
