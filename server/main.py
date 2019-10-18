#!/usr/bin/env python3

from gabriel_server.local_engine import runner
import timing_engine
import logging
import cv2
import argparse

USE_GPU = True
DEFAULT_STYLE = 'the_scream'
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]
PORT = 9098
NUM_TOKENS = 2
INPUT_QUEUE_MAXSIZE = 60
ENGINE = None
TIMING = True

logging.basicConfig(level=logging.INFO)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-t", "--tokens", type=int, default=2, help="number of tokens")
    parser.add_argument("-o", "--openvino", type=int, default=0, help="set it to 1 to use OpenVINO")
    args = parser.parse_args()

    NUM_TOKENS = args.tokens
    print(args)

    if args.openvino:
        from openvino_engine import OpenvinoEngine
        ENGINE = OpenvinoEngine
    else:
        from torch_engine import TorchEngine
        ENGINE = TorchEngine

    print(ENGINE)

    engine = (timing_engine.factory(ENGINE)
              if TIMING else ENGINE)

    def engine_setup():
        return engine(USE_GPU, DEFAULT_STYLE, COMPRESSION_PARAMS)
    runner.run(engine_setup, engine.ENGINE_NAME, INPUT_QUEUE_MAXSIZE,
               PORT, NUM_TOKENS)


if __name__ == '__main__':
    main()
