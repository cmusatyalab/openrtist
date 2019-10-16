from gabriel_server.local_engine import runner
from openrtist_engine import OpenrtistEngine
from openrtist_engine import ENGINE_NAME
import logging
import cv2

logging.basicConfig(level=logging.INFO)

USE_GPU = True
ENGINE_NAME = 'openrtist'
DEFAULT_STYLE = 'the_scream'
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]
PORT = 9098
NUM_TOKENS = 2
INPUT_QUEUE_MAXSIZE = 2


def main():
    def engine_setup():
        return OpenrtistEngine(USE_GPU, ENGINE_NAME, DEFAULT_STYLE,
                               COMPRESSION_PARAMS)
    runner.run(engine_setup, ENGINE_NAME, INPUT_QUEUE_MAXSIZE, PORT, NUM_TOKENS)


if __name__ == '__main__':
    main()
