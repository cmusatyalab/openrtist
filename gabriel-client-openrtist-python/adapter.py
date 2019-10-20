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

from abc import abstractmethod
from gabriel_client.opencv_adapter import OpencvAdapter
from openrtist_protocol import openrtist_pb2
import cv2
import config
import numpy as np


class Adapter:
    @property
    def producer(self):
        return self._opencv_adapter.producer

    @property
    def consumer(self):
        return self._opencv_adapter.consumer

    def set_style(self, style):
        self._style = style

    def __init__(self, preprocess, consume_frame_style, video_capture):
        '''
        consume_frame_style takes one frame parameter and one style parameter
        '''

        self._style = config.START_STYLE_STRING

        def produce_engine_fields():
            engine_fields = openrtist_pb2.EngineFields()
            engine_fields.style = self._style
            return engine_fields

        def consume_frame(frame, packed_engine_fields):
            engine_fields = openrtist_pb2.EngineFields()
            packed_engine_fields.Unpack(engine_fields)

            consume_frame_style(frame, engine_fields.style)

        self._opencv_adapter = OpencvAdapter(
            preprocess, produce_engine_fields, consume_frame, video_capture,
            config.ENGINE_NAME)
