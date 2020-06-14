from gabriel_server.network_engine import server_runner
import logging


logging.basicConfig(level=logging.INFO)


DEFAULT_PORT = 9099
DEFAULT_NUM_TOKENS = 2
INPUT_QUEUE_MAXSIZE = 60


def main():
    server_runner.run(
        DEFAULT_PORT, 'tcp://*:5555', DEFAULT_NUM_TOKENS, INPUT_QUEUE_MAXSIZE)


if __name__ == '__main__':
    main()
