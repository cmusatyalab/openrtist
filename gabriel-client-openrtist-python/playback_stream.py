from gabriel_client.timing_client import TimingClient
from gabriel_client.server_comm import WebsocketClient
from adapter import Adapter
import config
import cv2
import argparse


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "server_ip", action="store", help="IP address for Openrtist Server"
    )
    parser.add_argument("video", action="store", help="Path to video file")
    parser.add_argument(
        "--display", action="store_true", help="Show frames received from server"
    )
    parser.add_argument(
        "--timing", action="store_true", help="Print timing information"
    )
    args = parser.parse_args()

    def preprocess(frame):
        return frame

    if args.display:

        def consume_frame_style(frame, style):
            cv2.imshow("Result from server", frame)
            cv2.waitKey(1)

    else:

        def consume_frame_style(frame, style, style_image):
            pass

    video_capture = cv2.VideoCapture(args.video)

    adapter = Adapter(preprocess, consume_frame_style, video_capture)

    if args.timing:
        timing_client = TimingClient(
            args.server_ip, config.PORT, adapter.producer, adapter.consumer
        )
        try:
            timing_client.launch()
        except KeyboardInterrupt:
            timing_client.compute_avg_rtt()
    else:
        client = WebsocketClient(
            args.server_ip, config.PORT, adapter.producer, adapter.consumer
        )
        client.launch()


if __name__ == "__main__":
    main()
