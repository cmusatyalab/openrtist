from gabriel_server.server import Server
from openrtist_engine import OpenrtistEngine
import logging

logging.basicConfig(level=logging.INFO)


def engine_setup():
    return OpenrtistEngine(use_gpu=True)


def main():
    server = Server()
    server.serve(engine_setup)


if __name__ == '__main__':
    main()
