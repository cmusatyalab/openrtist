from gabriel_client.server_comm import WebsocketClient
import cv2
import numpy as np
from gabriel_protocol import gabriel_pb2
from openrtist_protocol import openrtist_pb2
from abc import abstractmethod

def pack_frame(frame, style, engine_name):
    ret, jpeg_frame=cv2.imencode('.jpg', frame)

    from_client = gabriel_pb2.FromClient()
    from_client.payload_type = gabriel_pb2.PayloadType.IMAGE
    from_client.engine_name = engine_name
    from_client.payload = jpeg_frame.tostring()

    engine_fields = openrtist_pb2.EngineFields()
    engine_fields.style = style
    from_client.engine_fields.Pack(engine_fields)

    return from_client


class OcvClient(WebsocketClient):
    @abstractmethod
    def input_processor(self, frame):
        pass

    @abstractmethod
    def output_frame(self, frame, style):
        pass

    def __init__(self, server_ip, port, video_capture,
                 style_string, engine_name):
        super().__init__(server_ip, port)
        self.video_capture = video_capture
        self.style_string = style_string
        self.engine_name = engine_name

    def producer(self):
        ret, frame = self.video_capture.read()

        frame = self.input_processor(frame)
        return pack_frame(frame, self.style_string, self.engine_name)

    def consumer(self, result_wrapper):
        if len(result_wrapper.results) == 1:
            result = result_wrapper.results[0]
            if result.payload_type == gabriel_pb2.PayloadType.IMAGE:
                if result.engine_name == self.engine_name:
                    img = result.payload
                    np_data = np.fromstring(img, dtype=np.uint8)
                    frame = cv2.imdecode(np_data, cv2.IMREAD_COLOR)

                    engine_fields = openrtist_pb2.EngineFields()
                    result_wrapper.engine_fields.Unpack(engine_fields)

                    self.output_frame(frame, engine_fields.style)
                else:
                    logger.error('Got result from engine %s',
                                 result.engine_name)
            else:
                logger.error('Got result of type %s', result.payload_type.name)
        else:
            logger.error('Got %d results in output',
                         len(result_wrapper.results))
