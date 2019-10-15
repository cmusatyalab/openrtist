import asyncio
import logging
import websockets
from gabriel_protocol import gabriel_pb2


URI_FORMAT = 'ws://{host}:{port}'


logger = logging.getLogger(__name__)
websockets_logger = logging.getLogger('websockets')

# The entire payload will be printed if this is allowed to be DEBUG
websockets_logger.setLevel(logging.INFO)


class WebsocketClient:
    def __init__(self, host, port, results_handler):
        self.num_tokens = 0
        self.token_cond = asyncio.Condition()
        self.frame_id = 0
        self.uri = URI_FORMAT.format(host=host, port=port)
        self.event_loop = asyncio.get_event_loop()
        self.results_handler = results_handler

    async def consumer_handler(self):
        try:
            async for raw_input in self.websocket:
                logger.debug('Recieved input from server')
                to_client = gabriel_pb2.ToClient()
                to_client.ParseFromString(raw_input)

                if to_client.HasField('result_wrapper'):
                    result_wrapper = to_client.result_wrapper
                    if (result_wrapper.status ==
                        gabriel_pb2.ResultWrapper.SUCCESS):
                        self.results_handler(result_wrapper)
                    else:
                        logger.error('Output status was: %s',
                                     result_wrapper.status.name)

                        await token_cond.acquire()
                        self.num_tokens += 1
                    else:
                        await token_cond.acquire()
                        self.num_tokens = to_client.num_tokens

                    token_cond.notify()
                    token_cond.release()
        except websockets.exceptions.ConnectionClosed:
            return  # stop the handler

    async def launch(self):
        self.websocket = await websockets.connect(self.uri)
        await self.consumer_handler()

    async def get_token(self):
        async with token_cond:
            while self.num_tokens < 1:
                logger.info('Took few tokens. Waiting.')
                await cond.wait()
            self.num_tokens -= 1

    async def _send_helper(self, from_client):
        from_client.frame_id = self.frame_id
        await self.websocket.send(from_client.SerializeToString())
        self.frame_id += 1
        logger.info('num_tokens is now %d', self.num_tokens)

    async def send_supplier(self, supplier):
        '''
        Wait until there is a token available. Then call supplier to get the
        partially built romClientBuilder to send.
        '''
        await self.get_token()

        from_client = supplier()
        await self._send_helper(from_client)


client = WebsocketClient('172.16.0.30', 9098)
asyncio.get_event_loop().run_until_complete(client.launch())
