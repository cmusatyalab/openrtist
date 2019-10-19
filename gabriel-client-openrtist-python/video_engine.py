from ocv_client import OcvClient
import time
import cv2
import client
import numpy as np
from gabriel_protocol import gabriel_pb2
from config import Config

STYLE_STRING = 'udnie'
ENGINE_NAME = 'openrtist'
RANDOM_SIZE = (240, 320, 3)


class VideoEngine(OcvClient):
    def __init__(self, server_ip, port):
        super().__init__(
            server_ip, port, cv2.VideoCapture('/home/roger/small.mp4'),
            STYLE_STRING, ENGINE_NAME)
        self.sent_count = 0
        self.recv_count = 0
        self.sent_all = False
        self.start_time = time.time()

    def input_processor(self, frame):
        if frame is None:
            self.sent_all = True
            frame = np.random.randint(0, 255, RANDOM_SIZE, np.uint8)
        else:
            self.sent_count += 1

        return frame

    def output_frame(self, frame, style):
        self.recv_count += 1
        if self.sent_all and (self.recv_count == self.sent_count):
            end_time = time.time()
            print(
                'AVG FPS',
                self.sent_count / (end_time - self.start_time))
            self.stop()
        else:
            cv2.imshow('frame', frame)
            cv2.waitKey(1)


def main():
    video_engine = VideoEngine('ip address', Config.PORT)
    video_engine.launch()


if __name__ == '__main__':
    main()
