from gabriel_server.local_engine import runner
from torch_engine import TorchEngine
import logging
import cv2

logging.basicConfig(level=logging.INFO)

USE_GPU = True
DEFAULT_STYLE = 'the_scream'
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]
PORT = 9098
NUM_TOKENS = 2
INPUT_QUEUE_MAXSIZE = 2


def main():
    def engine_setup():
        return TorchEngine(USE_GPU, DEFAULT_STYLE, COMPRESSION_PARAMS)
    runner.run(engine_setup, TorchEngine.ENGINE_NAME, INPUT_QUEUE_MAXSIZE,
               PORT, NUM_TOKENS)


if __name__ == '__main__':
    main()
