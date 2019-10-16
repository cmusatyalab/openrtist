import asyncio
import logging
import websockets
from gabriel_protocol import gabriel_pb2
from abc import ABC
from abc import abstractmethod


URI_FORMAT = 'ws://{host}:{port}'


logger = logging.getLogger(__name__)
websockets_logger = logging.getLogger('websockets')

# The entire payload will be printed if this is allowed to be DEBUG
websockets_logger.setLevel(logging.INFO)


class WebsocketClient(ABC):
    def __init__(self, host, port):
        self._num_tokens = 0
        self._frame_id = 0
        self._running = True
        self._token_cond = asyncio.Condition()
        self._uri = URI_FORMAT.format(host=host, port=port)
        self._event_loop = asyncio.get_event_loop()

    @abstractmethod
    def consumer(self, result_wrapper):
        pass

    @abstractmethod
    def producer(self):
        pass

    def launch(self):
        self._event_loop.run_until_complete(self._handler())

    def get_frame_id(self):
        return self._frame_id

    def stop(self):
        self._running = False
        logger.info('stopping server')

    async def _consumer_handler(self):
        try:
            while self._running:
                raw_input = await self._websocket.recv()
                logger.debug('Recieved input from server')

                to_client = gabriel_pb2.ToClient()
                to_client.ParseFromString(raw_input)

                if to_client.HasField('result_wrapper'):
                    result_wrapper = to_client.result_wrapper
                    if (result_wrapper.status ==
                        gabriel_pb2.ResultWrapper.SUCCESS):
                        self.consumer(result_wrapper)
                    else:
                        logger.error('Output status was: %d',
                                     result_wrapper.status)

                    await self._token_cond.acquire()
                    self._num_tokens += 1
                else:
                    await self._token_cond.acquire()
                    self._num_tokens = to_client.num_tokens

                self._token_cond.notify()
                self._token_cond.release()
        except websockets.exceptions.ConnectionClosed:
            return  # stop the handler

    async def _get_token(self):
        async with self._token_cond:
            while self._num_tokens < 1:
                logger.info('Too few tokens. Waiting.')
                await self._token_cond.wait()
            self._num_tokens -= 1

    async def _send_helper(self, from_client):
        from_client.frame_id = self._frame_id
        await self._websocket.send(from_client.SerializeToString())
        self._frame_id += 1
        logger.info('num_tokens is now %d', self._num_tokens)

    async def _producer_handler(self):
        '''
        Loop waiting until there is a token available. Then call supplier to get
        the partially built FromClient to send.
        '''
        try:
            while self._running:
                await asyncio.sleep(0)  # Allow consumer to be scheduled
                await self._get_token()

                from_client = self.producer()
                await self._send_helper(from_client)
        except websockets.exceptions.ConnectionClosed:
            return  # stop the handler

    async def _handler(self):
        try:
            self._websocket = await websockets.connect(self._uri)
            consumer_task = asyncio.ensure_future(self._consumer_handler())
            producer_task = asyncio.ensure_future(self._producer_handler())
            done, pending = await asyncio.wait(
                [consumer_task, producer_task],
                return_when=asyncio.FIRST_COMPLETED)
            for task in pending:
                task.cancel()
        finally:
            logger.info('Server disconnected')
