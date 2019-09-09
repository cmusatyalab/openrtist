from gabriel_server import single_engine_runner
from openrtist_engine import OpenrtistEngine
import logging

logging.basicConfig(level=logging.INFO)


def main():
    def engine_setup():
        return OpenrtistEngine(use_gpu=True)
    single_engine_runner.run(engine_setup)


if __name__ == '__main__':
    main()
