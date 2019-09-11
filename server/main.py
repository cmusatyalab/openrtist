from gabriel_server.local_engine import runner
from openrtist_engine import OpenrtistEngine
import logging

logging.basicConfig(level=logging.INFO)


def main():
    def engine_setup():
        return OpenrtistEngine(use_gpu=True)
    runner.run(engine_setup)


if __name__ == '__main__':
    main()
