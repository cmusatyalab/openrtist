from client import Client
import cv2
import argparse


class PlaybackClient(Client):
    def preprocess(self, frame):
        return frame

    def consume_frame_style(self, frame, style):
        if self.display:
            cv2.imshow('Result from server', frame)
            cv2.waitKey(1)

    def __init__(self, server_ip, video, display):
        super().__init__(server_ip, cv2.VideoCapture(video))
        self.display = display


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('server_ip', action='store',
                        help='IP address for Openrtist Server')
    parser.add_argument('video', action='store',
                        help='Path to video file')
    parser.add_argument('--display', action='store_true',
                        help='Show frames received from server')
    args = parser.parse_args()

    video_engine = PlaybackClient(args.server_ip, args.video, args.display)
    video_engine.launch()


if __name__ == '__main__':
    main()
