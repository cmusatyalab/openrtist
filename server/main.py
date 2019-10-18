#!/usr/bin/env python3

from gabriel_server.local_engine import runner
from torch_engine import TorchEngine
# from openvino_engine import OpenvinoEngine
import timing_engine
import logging
import cv2

USE_GPU = True
DEFAULT_STYLE = 'the_scream'
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]
PORT = 9098
NUM_TOKENS = 2
INPUT_QUEUE_MAXSIZE = 2
ENGINE = TorchEngine
# ENGINE = OpenvinoEngine
TIMING = True

logging.basicConfig(level=logging.INFO)


def main():
    engine = (timing_engine.factory(ENGINE)
              if TIMING else ENGINE)

    def engine_setup():
        return engine(USE_GPU, DEFAULT_STYLE, COMPRESSION_PARAMS)
    runner.run(engine_setup, engine.ENGINE_NAME, INPUT_QUEUE_MAXSIZE,
               PORT, NUM_TOKENS)


if __name__ == '__main__':
    main()
