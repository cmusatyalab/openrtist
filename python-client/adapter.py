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

from gabriel_client.opencv_adapter import OpencvAdapter
import cv2
import config
import numpy as np
import random
import logging

import openrtist_pb2


logger = logging.getLogger(__name__)


class Adapter:
    @property
    def producer_wrappers(self):
        return self._opencv_adapter.get_producer_wrappers()

    @property
    def consumer(self):
        return self._opencv_adapter.consumer

    def set_style(self, style):
        self._style = style

    def get_styles(self):
        return self.available_styles

    def __init__(
        self,
        preprocess,
        consume_frame_style,
        video_capture,
        start_style=config.START_STYLE_STRING,
    ):
        """
        consume_frame_style takes one frame parameter and one style parameter
        """

        self._style = start_style
        self.available_styles = []
        self.style_image = None

        def produce_extras():
            extras = openrtist_pb2.Extras()
            extras.style = self._style
            return extras

        def consume_frame(frame, packed_extras):
            extras = openrtist_pb2.Extras()
            packed_extras.Unpack(extras)
            if self._style == "?":
                self._style = extras.style
            if len(extras.style_list) > 0:
                self.available_styles = list(extras.style_list.keys())
                random.shuffle(self.available_styles)
            if extras.HasField("style_image"):
                if len(extras.style_image.value) == 0:
                    self.style_image = None
                    logger.debug("got empty style image")
                else:
                    self.style_image = cv2.imdecode(
                        np.frombuffer(
                            extras.style_image.value, dtype=np.uint8),
                        cv2.IMREAD_COLOR,
                    )
                    self.style_image = cv2.cvtColor(
                        self.style_image, cv2.COLOR_BGR2RGB)
                    logger.debug("got style image")

            consume_frame_style(frame, extras.style, self.style_image)

        self._opencv_adapter = OpencvAdapter(
            preprocess,
            produce_extras,
            consume_frame,
            video_capture,
            config.SOURCE_NAME,
        )
