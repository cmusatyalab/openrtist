#!/usr/bin/env python3

from gabriel_server.local_engine import runner
from openrtist_engine import OpenrtistEngine
from timing_engine import TimingEngine
import timing_engine
import logging
import cv2
import argparse
import autodetect

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
    parser.add_argument('-o', '--openvino', nargs='?', 
                        choices=['yes','no','auto'], const='yes', default='auto',
                        help='Pass this flag to use OpenVINO. Otherwise Torch '
                        'will be used')
    parser.add_argument('-c', '--cpu-only', nargs='?',
                        choices=['yes','no','auto'], const='yes', default='auto',
                        help='Set this flag to use CPU. Otherwise, use GPU')
    parser.add_argument('--timing', action='store_true',
                        help='Print timing information')
    args = parser.parse_args()

    print (args.openvino,args.cpu_only)
    autodetect.detect( args )

    def engine_setup():
        if args.openvino:
            # Prevent ImportError when user wants to use torch and does not
            # have openvino
            from openvino_adapter import OpenvinoAdapter

            adapter = OpenvinoAdapter(args.cpu_only, DEFAULT_STYLE)
        else:
            # Prevent ImportError when user wants to use openvino and does not
            # have Torch
            from torch_adapter import TorchAdapter

            adapter = TorchAdapter(args.cpu_only, DEFAULT_STYLE)

        if args.timing:
            engine = TimingEngine(COMPRESSION_PARAMS, adapter)
        else:
            engine = OpenrtistEngine(COMPRESSION_PARAMS, adapter)

        return engine

    runner.run(engine_setup, OpenrtistEngine.ENGINE_NAME, INPUT_QUEUE_MAXSIZE,
               PORT, args.tokens)


if __name__ == '__main__':
    main()
