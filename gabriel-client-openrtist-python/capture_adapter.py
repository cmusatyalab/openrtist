from adapter import Adapter
from gabriel_client.server_comm import WebsocketClient
import config
import cv2
import os
import random


class CaptureAdapter:
    @property
    def producer(self):
        return self.adapter.producer

    @property
    def consumer(self):
        return self.adapter.consumer

    def preprocess(self, frame):
        if self.current_style_frames == self.style_interval:
            self.style_num = (self.style_num + 1) % len(self.style_array)
            self.adapter.set_style(
                self.style_array[self.style_num].split(".")[0])

            self.current_style_frames = 0
        else:
            self.current_style_frames += 1

        frame = cv2.flip(frame, 1)
        frame = cv2.resize(frame, (config.IMG_WIDTH, config.IMG_HEIGHT))

        return frame

    def __init__(self, consume_rgb_frame_style):
        '''
        consume_rgb_frame_style should take one rgb_frame parameter and one
        style parameter.
        '''

        self.style_array = os.listdir('./style-image')
        random.shuffle(self.style_array)
        self.style_num = 0
        self.style_interval = config.STYLE_DISPLAY_INTERVAL
        self.current_style_frames = 0

        video_capture = cv2.VideoCapture(-1)
        video_capture.set(cv2.CAP_PROP_FPS, config.CAM_FPS)

        def consume_frame_style(frame, style):
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            consume_rgb_frame_style(rgb_frame, style)

        self.adapter = Adapter(
            self.preprocess, consume_frame_style, video_capture)


def create_client(server_ip, consume_rgb_frame_style):
    '''
    consume_rgb_frame_style should take one rgb_frame parameter and one
    style parameter.
    '''

    adapter = CaptureAdapter(consume_rgb_frame_style)
    return WebsocketClient(
        server_ip, config.PORT, adapter.producer, adapter.consumer)
