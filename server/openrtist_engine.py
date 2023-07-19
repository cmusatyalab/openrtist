# OpenRTiST
#   - Real-time Style Transfer
#
#   Authors: Zhuo Chen <zhuoc@cs.cmu.edu>
#           Shilpa George <shilpag@andrew.cmu.edu>
#           Thomas Eiszler <teiszler@andrew.cmu.edu>
#           Padmanabhan Pillai <padmanabhan.s.pillai@intel.com>
#           Roger Iyengar <iyengar@cmu.edu>
#           Meng Cao <mcao@andrew.cmu.edu>
#
#   Copyright (C) 2011-2020 Carnegie Mellon University
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
# Portions of this code were modified from sampled code distributed as part of
# the fast_neural_style example that is part of the pytorch repository and is
# distributed under the BSD 3-Clause License.
# https://github.com/pytorch/examples/blob/master/LICENSE

import cv2
import numpy as np
import logging
from gabriel_server import cognitive_engine
from gabriel_protocol import gabriel_pb2
import openrtist_pb2
import os
from io import BytesIO

logger = logging.getLogger(__name__)

from azure.cognitiveservices.vision.face import FaceClient
from azure.cognitiveservices.vision.face.models import FaceAttributeType
from msrest.authentication import CognitiveServicesCredentials
import http.client, urllib.request, urllib.parse, urllib.error, base64
import json
from emotion_to_style import emotion_to_style_map


class OpenrtistEngine(cognitive_engine.Engine):
    SOURCE_NAME = "openrtist"

    def __init__(self, compression_params, adapter):
        self.compression_params = compression_params
        self.adapter = adapter
        self.face_supported = os.getenv("FaceEnabled", False)
        if self.face_supported:
            logger.info("Emotion-based styling enabled via MS Face Service.")
        else:
            logger.info("Emotion-based styling disabled.")
        # The waterMark is of dimension 30x120
        wtr_mrk4 = cv2.imread("../wtrMrk.png", -1)

        # The RGB channels are equivalent
        self.mrk, _, _, mrk_alpha = cv2.split(wtr_mrk4)

        self.alpha = mrk_alpha.astype(float) / 255

        # TODO support server display

        # check if Face api is supported
        if self.face_supported:
            self.face_client = FaceClient(
                "http://ms-face-service:5000",
                CognitiveServicesCredentials(os.getenv("ApiKey")),
            )

        logger.info("FINISHED INITIALISATION")

    def handle(self, input_frame):
        if input_frame.payload_type != gabriel_pb2.PayloadType.IMAGE:
            status = gabriel_pb2.ResultWrapper.Status.WRONG_INPUT_FORMAT
            return cognitive_engine.create_result_wrapper(status)

        extras = cognitive_engine.unpack_extras(openrtist_pb2.Extras, input_frame)

        new_style = False
        send_style_list = False
        emotion_enabled = False

        if extras.style == "?":
            new_style = True
            send_style_list = True
        elif self.face_supported and extras.style == "aaa_emotion_enabled":
            emotion_enabled = True
            style = self.emotion_detection(input_frame.payloads[0])
            if style:
                self.adapter.set_style(style)
                new_style = True
        elif extras.style != self.adapter.get_style():
            self.adapter.set_style(extras.style)
            logger.info("New Style: %s", extras.style)
            new_style = True

        if not emotion_enabled:
            style = self.adapter.get_style()

        # Preprocessing steps used by both engines
        np_data = np.frombuffer(input_frame.payloads[0], dtype=np.uint8)
        orig_img = cv2.imdecode(np_data, cv2.IMREAD_COLOR)
        orig_img = cv2.cvtColor(orig_img, cv2.COLOR_BGR2RGB)

        # It is possible that no face is detected and style is None, if so bypass processing
        if style:
            image = self.process_image(orig_img)
        else:
            image = orig_img

        image = image.astype("uint8")
        if extras.HasField("depth_map"):
            # protobuf contains depth_map
            depth_map = extras.depth_map.value
            # get depth map (bytes) and perform depth thresholding to create foreground mask with 3 channels
            depth_threshold = extras.depth_threshold

            # data type conversion from bytes to a scaled-out 2d numpy array (480*640)
            np_depth_1d = np.frombuffer(depth_map, dtype=np.uint16)
            np_depth_2d = np.reshape(np_depth_1d, (-1, 160))

            # threshold on the distance
            mask_fg = cv2.inRange(np_depth_2d, 0, depth_threshold)

            # resize to match the image
            orig_h, orig_w, _ = orig_img.shape
            mask_fg = cv2.resize(
                mask_fg, (orig_w, orig_h), interpolation=cv2.INTER_NEAREST
            )

            # Apply morphology to the thresholded image to remove extraneous white regions and save a mask
            kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
            mask_fg = cv2.morphologyEx(mask_fg, cv2.MORPH_OPEN, kernel)
            mask_fg = cv2.morphologyEx(mask_fg, cv2.MORPH_CLOSE, kernel)

            fg = cv2.bitwise_and(orig_img, orig_img, mask=mask_fg)

            # get background mask by inversion
            mask_bg = cv2.bitwise_not(mask_fg)

            # get background from transformed image
            bg = cv2.bitwise_and(image, image, mask=mask_bg)

            # stitch transformed background and original foreground
            image = cv2.bitwise_or(fg, bg)

        # scale image back to original size to get a better watermark
        if orig_img.shape != image.shape:
            orig_h, orig_w, _ = orig_img.shape
            image = cv2.resize(
                image, (orig_w, orig_h), interpolation=cv2.INTER_LINEAR
            )
        image = self._apply_watermark(image)

        _, jpeg_img = cv2.imencode(".jpg", image, self.compression_params)
        img_data = jpeg_img.tostring()

        result = gabriel_pb2.ResultWrapper.Result()
        result.payload_type = gabriel_pb2.PayloadType.IMAGE
        result.payload = img_data

        extras = openrtist_pb2.Extras()

        if style:
            extras.style = style

        if new_style:
            extras.style_image.value = self.adapter.get_style_image()
        if send_style_list:
            if self.face_supported:
                extras.style_list[
                    "aaa_emotion_enabled"
                ] = "* Emotion-based styling (contempt,disgust,fear,happiness,sadness,surprise)"
            for k, v in self.adapter.get_all_styles().items():
                extras.style_list[k] = v

        status = gabriel_pb2.ResultWrapper.Status.SUCCESS
        result_wrapper = cognitive_engine.create_result_wrapper(status)
        result_wrapper.results.append(result)
        result_wrapper.extras.Pack(extras)

        return result_wrapper

    # https://westus.dev.cognitive.microsoft.com/docs/services/563879b61984550e40cbbe8d/operations/563879b61984550f30395236
    def emotion_detection(self, img_bytes):
        style = None
        detected_faces = []

        try:
            detected_faces = self.face_client.face.detect_with_stream(
                image=BytesIO(img_bytes),
                return_face_id=False,
                return_face_landmarks=False,
                return_face_attributes=list([FaceAttributeType.emotion]),
            )
        except Exception as e:
            logger.error(e)

        if len(detected_faces) == 0:
            # no face detected
            style = None
        else:
            # get the largest face in the image
            largest_face = detected_faces[0]

            # get the strongest emotion of the face
            emotions = largest_face.face_attributes.emotion
            strongest_emotion = max(emotions.as_dict(), key=emotions.as_dict().get)

            if strongest_emotion in emotion_to_style_map:
                style = emotion_to_style_map[strongest_emotion]

        return style

    def process_image(self, image):
        preprocessed = self.adapter.preprocessing(image)
        post_inference = self.inference(preprocessed)
        img_out = self.adapter.postprocessing(post_inference)
        return img_out

    def inference(self, preprocessed):
        """Allow timing engine to override this"""
        return self.adapter.inference(preprocessed)

    def _apply_watermark(self, image):
        img_mrk = image[-30:, -120:]  # The waterMark is of dimension 30x120
        img_mrk[:, :, 0] = (1 - self.alpha) * img_mrk[:, :, 0] + self.alpha * self.mrk
        img_mrk[:, :, 1] = (1 - self.alpha) * img_mrk[:, :, 1] + self.alpha * self.mrk
        img_mrk[:, :, 2] = (1 - self.alpha) * img_mrk[:, :, 2] + self.alpha * self.mrk
        image[-30:, -120:] = img_mrk
        # img_out = image.astype("uint8")
        img_out = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)

        return img_out
