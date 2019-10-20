from client import Client
from abc import abstractmethod
import config
import cv2
import os
import random


class CaptureClient(Client):
    @abstractmethod
    def consume_rgb_frame_style(rgb_frame, style):
        pass

    def preprocess(self, frame):
        if (self.get_frame_id() % self.style_interval) == 0:
            self.style_num = (self.style_num + 1) % len(self.style_array)
            self.style = self.style_array[self.style_num].split(".")[0]

        frame = cv2.flip(frame, 1)
        frame = cv2.resize(frame, (config.IMG_WIDTH, config.IMG_HEIGHT))

        return frame

    def consume_frame_style(self, frame, style):
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        self.consume_rgb_frame_style(rgb_frame, style)

    def __init__(self, server_ip):
        super().__init__(server_ip, cv2.VideoCapture(-1))

        self.style_array = os.listdir('./style-image')
        random.shuffle(self.style_array)
        self.style_interval = config.STYLE_DISPLAY_INTERVAL
        self.video_capture.set(cv2.CAP_PROP_FPS, config.CAM_FPS)

        self.style_num = 0
