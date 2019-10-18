#!/usr/bin/env python3

from gabriel_server.local_engine import runner
import timing_engine
import logging
import cv2
import argparse

PORT = 9098
DEFAULT_NUM_TOKENS = 2
INPUT_QUEUE_MAXSIZE = 60
DEFAULT_STYLE = 'the_scream'
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]

logging.basicConfig(level=logging.INFO)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-t', '--tokens', type=int, default=DEFAULT_NUM_TOKENS,
                        help='number of tokens')
    parser.add_argument('-o', '--openvino', action='store_true',
                        help='Pass this flag to use OpenVINO. Otherwise Torch will be'
                        'used')
    parser.add_argument('-g', '--gpu', action='store_true',
                        help='Pass this flag to use the GPU instead of the CPU')
    parser.add_argument('--timing', action='store_true', help='Print timing information')
    args = parser.parse_args()

    print(args)

    if args.openvino:
        from openvino_engine import OpenvinoEngine
        engine = OpenvinoEngine
    else:
        from torch_engine import TorchEngine
        engine = TorchEngine

    if args.timing:
        engine = timing_engine.factory(engine)

    def engine_setup():
        return engine(args.gpu, DEFAULT_STYLE, COMPRESSION_PARAMS)
    runner.run(engine_setup, engine.ENGINE_NAME, INPUT_QUEUE_MAXSIZE,
               PORT, args.tokens)


if __name__ == '__main__':
    main()
