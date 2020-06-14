from gabriel_server import network_engine

def main():
    network_engine.server.run(DEFAULT_PORT,
        'tcp://*:5555',
        DEFAULT_NUM_TOKENS,
        INPUT_QUEUE_MAXSIZE
    )

if __name__ == '__main__':
    main()
