from gabriel_server.local_engine import runner
from openrtist_engine import OpenrtistEngine
from openrtist_engine import ENGINE_NAME
import logging

logging.basicConfig(level=logging.INFO)


def main():
    def engine_setup():
        return OpenrtistEngine(use_gpu=True)
    runner.run(engine_setup, ENGINE_NAME)


if __name__ == '__main__':
    main()
