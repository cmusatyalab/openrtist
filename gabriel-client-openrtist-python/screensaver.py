#!/usr/bin/env python3

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
import os
import cv2
import capture_adapter


STYLE_DIR_PATH = 'style-image'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("server_ip", action="store",
                        help="IP address for Openrtist Server")
    parser.add_argument("output_pipe_path", action="store",
                        help="The linux pipe the style-transferred images will "
                        "be streamed to")
    inputs = parser.parse_args()

    style_name_to_image = {}
    for image_name in os.listdir(STYLE_DIR_PATH):
        im = cv2.imread(os.path.join(STYLE_DIR_PATH, image_name))
        im = cv2.cvtColor(im, cv2.COLOR_BGR2RGB)
        style_name_to_image[os.path.splitext(image_name)[0]] = im

    if not os.path.exists(inputs.output_pipe_path):
        os.mkfifo(inputs.output_pipe_path)

    with open(inputs.output_pipe_path, 'wb') as rgbpipe:
        def consume_rgb_frame_style(rgb_frame, style):
            style_image = style_name_to_image[style]
            style_im_h, style_im_w, _ = style_image.shape
            rgb_frame[0:style_im_h, 0:style_im_w, :] = style_image
            cv2.rectangle(
                rgb_frame, (0,0), (int(style_im_w), int(style_im_h)), (255,0,0),
                3)
            rgb_frame_enlarged = cv2.resize(rgb_frame, (960, 540))
            rgbpipe.write(rgb_frame_enlarged.tostring())

        client = capture_adapter.create_client(
            inputs.server_ip, consume_rgb_frame_style)
        client.launch()


if __name__ == '__main__':
    main()
