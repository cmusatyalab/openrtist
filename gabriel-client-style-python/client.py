#! /usr/bin/env python

import socket
import struct
import threading
import Queue
import StringIO
import cv2
import json
import time
from time import sleep
import pdb
import sys
import select
import numpy as np
from config import Config
import base64
import os
import protocol
from socketLib import ClientCommand, ClientReply, SocketClientThread
import errno
import posix
import random

class GabrielSocketCommand(ClientCommand):
    STREAM=len(ClientCommand.ACTIONS)
    ACTIONS=ClientCommand.ACTIONS + [STREAM]
    LISTEN=len(ACTIONS)
    ACTIONS.append(LISTEN)
    
    def __init__(self, type, data=None):
        super(self.__class__.__name__, self).__init__()
        
        
class VideoStreamingThread(SocketClientThread):
    def __init__(self, cmd_q=None, reply_q=None):
        super(self.__class__, self).__init__(cmd_q, reply_q)
        self.handlers[GabrielSocketCommand.STREAM] = self._handle_STREAM
        self.is_streaming=False
        self.style_array = os.listdir('./style-image')
        random.shuffle(self.style_array)
        #self.style_array = self.style_array[1:]
        self.length = len(self.style_array)
        self.SEC = 30
        self.FPS = 10
        self.INTERVAL = self.SEC*self.FPS

        #print(self.style_array)

    def run(self):
        while self.alive.isSet():
            try:
                cmd = self.cmd_q.get(True, 0.1)
                self.handlers[cmd.type](cmd)
            except Queue.Empty as e:
                continue

    # tokenm: token manager
    def _handle_STREAM(self, cmd):
        tokenm = cmd.data
        self.is_streaming=True
        video_capture = cv2.VideoCapture(0)
        video_capture.set(cv2.CAP_PROP_FPS, self.FPS)
        b_time = time.time() 
        e_time = time.time()
        id=0
        style_num = 0
        style_string = 'udnie'
        while self.alive.isSet() and self.is_streaming:
            # will be put into sleep if token is not available
            if id%self.INTERVAL==0:
                style_string = self.style_array[style_num%self.length].split(".")[0]
                style_num+=1
            tokenm.getToken()
            ret, frame = video_capture.read()
            capture_time = time.time()
            # print("Capture Time: {} {}".format(capture_time,video_capture.get(cv2.CAP_PROP_FPS)))
            frame = cv2.flip(frame,1)
            frame = cv2.resize(frame,(640,360))
            frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            ret, jpeg_frame=cv2.imencode('.jpg', frame)
            header={protocol.Protocol_client.JSON_KEY_FRAME_ID : str(id),
                    protocol.Protocol_client.JSON_KEY_STYLE : style_string }
            #header={protocol.Protocol_client.JSON_KEY_STYLE : "udnie"}
            header_json=json.dumps(header)
            send_time = time.time()
            # print("Send Time: {}".format(send_time))
            self._handle_SEND(ClientCommand(ClientCommand.SEND, header_json))
            self._handle_SEND(ClientCommand(ClientCommand.SEND, jpeg_frame.tostring()))
            id+=1

        video_capture.release()        

class ResultReceivingThread(SocketClientThread):
    def __init__(self, cmd_q=None, reply_q=None):
        super(self.__class__, self).__init__(cmd_q, reply_q)
        self.handlers[GabrielSocketCommand.LISTEN] =  self._handle_LISTEN
        self.is_listening=False
        
    def run(self):
        while self.alive.isSet():
            try:
                cmd = self.cmd_q.get(True, 0.1)
                self.handlers[cmd.type](cmd)
            except Queue.Empty as e:
                continue

    def _handle_LISTEN(self, cmd):
        tokenm = cmd.data
        self.is_listening=True
        while self.alive.isSet() and self.is_listening:
            if self.socket:
                input=[self.socket]
                inputready,outputready,exceptready = select.select(input,[],[]) 
                for s in inputready: 
                    if s == self.socket: 
                        # handle the server socket
                        header, data = self._recv_gabriel_data()
                        self.reply_q.put(self._success_reply( (header, data) ))
                        recv_time = time.time()
                        # print("Recv Time: {}".format(recv_time))
                        tokenm.putToken()
        
    def _recv_gabriel_data(self):
        #import pdb
        #pdb.set_trace()
        header_size = struct.unpack("!I", self._recv_n_bytes(4))[0]
        header = self._recv_n_bytes(header_size)
        header_json = json.loads(header)
        data_size = header_json['data_size']
        data = self._recv_n_bytes(data_size)

        return (header, data)
        
# token manager implementing gabriel's token mechanism
class tokenManager(object):
    def __init__(self, token_num):
        super(self.__class__, self).__init__()        
        self.token_num=token_num
        # token val is [0..token_num)
        self.token_val=token_num -1
        self.lock = threading.Lock()
        self.has_token_cv = threading.Condition(self.lock)

    def _inc(self):
        self.token_val= (self.token_val + 1) if (self.token_val<self.token_num) else (self.token_val)

    def _dec(self):
        self.token_val= (self.token_val - 1) if (self.token_val>=0) else (self.token_val)

    def empty(self):
        return (self.token_val<0)

    def getToken(self):
        with self.has_token_cv:
            while self.token_val < 0:
                self.has_token_cv.wait()
            self._dec()

    def putToken(self):
        with self.has_token_cv:
            self._inc()                    
            if self.token_val >= 0:
                self.has_token_cv.notifyAll()

def run(sig_frame_available=None):
    tokenm = tokenManager(Config.TOKEN)
    stream_cmd_q = Queue.Queue()
    result_cmd_q = Queue.Queue()    
    result_reply_q = Queue.Queue()
    video_streaming_thread=VideoStreamingThread(cmd_q=stream_cmd_q)
    stream_cmd_q.put(ClientCommand(ClientCommand.CONNECT, (Config.GABRIEL_IP, Config.VIDEO_STREAM_PORT)) )
    stream_cmd_q.put(ClientCommand(GabrielSocketCommand.STREAM, tokenm))    
    result_receiving_thread = ResultReceivingThread(cmd_q=result_cmd_q, reply_q=result_reply_q)    
    result_cmd_q.put(ClientCommand(ClientCommand.CONNECT, (Config.GABRIEL_IP, Config.RESULT_RECEIVING_PORT)) )
    result_cmd_q.put(ClientCommand(GabrielSocketCommand.LISTEN, tokenm))
    result_receiving_thread.start()
    sleep(0.1)
    video_streaming_thread.start()

    if sig_frame_available is None:
        rgbpipe_path = '/tmp/rgbpipe'
        if not os.path.exists(rgbpipe_path):
            os.mkfifo(rgbpipe_path)
        rgbpipe = os.open(rgbpipe_path, os.O_WRONLY)
    
    try:
        while True:
            sys.stdout.flush()
            resp=result_reply_q.get()
            # connect and send also send reply to reply queue without any data attached
            if resp.type == ClientReply.SUCCESS and resp.data is not None:
                tkn_time = time.time()
                # print("Tocken Time: {}".format(tkn_time))
                (resp_header, resp_data) =resp.data
                resp_header=json.loads(resp_header)
                img=resp_data
                data=img
                np_data=np.fromstring(data, dtype=np.uint8)
                frame=cv2.imdecode(np_data,cv2.IMREAD_COLOR)
                if sig_frame_available == None:
                    #print 'resp:{}'.format(img[:100])
                    # when there is no qt window, just dump the image into stdout
                    st = time.time()
                    rgb_frame = frame
                    # style_image = cv2.imread(os.path.join('./style-image', resp_header['style'] + '.jpg'), -1)
                    # style_image = cv2.cvtColor(style_image, cv2.COLOR_BGR2RGB)
                    # im_h, im_w, _ = rgb_frame.shape
                    style_image = style_name_to_image[resp_header['style']]
                    style_im_h, style_im_w, _ = style_image.shape
                    # style_im_aspect_ratio = style_im_h / float(style_im_w)
                    # resize_to_h = im_h * 0.3
                    # resize_to_w = resize_to_h / style_im_aspect_ratio
                    # style_image_resized = cv2.resize(style_image, (int(resize_to_w), int(resize_to_h)))
                    rgb_frame[0:style_im_h, 0:style_im_w, :] = style_image
                    cv2.rectangle(rgb_frame, (0,0), (int(style_im_w), int(style_im_h)), (255,0,0), 3)
                    rgb_frame_enlarged = cv2.resize(rgb_frame, None, fx=2, fy=2)
                    print('resize tooks: {}'.format(time.time()-st))
                    st = time.time()
                    os.write(rgbpipe, rgb_frame_enlarged.tostring())
                    print('write to pipe tooks: {}'.format(time.time()-st))
                    # sys.stderr.write(rgb_frame_enlarged)
                else:
                    # display received image on the pyqt ui
                    #rgb_frame = cv2.cvtColor(frame,cv2.COLOR_BGR2RGB)    
                    #print("HEADER STYLE {}".format(resp_header['style']))                
                    sig_frame_available.emit(frame,resp_header['style'])
    except KeyboardInterrupt:
        os.close(rgbpipe)
        video_streaming_thread.join()
        result_receiving_thread.join()
        with tokenm.has_token_cv:
            tokenm.has_token_cv.notifyAll()

def _load_style_images(style_dir_path='style-image'):
    style_name_to_image = {}
    for image_name in os.listdir(style_dir_path):
        im = cv2.imread(os.path.join(style_dir_path, image_name))
        im = cv2.cvtColor(im, cv2.COLOR_BGR2RGB)
        style_name_to_image[os.path.splitext(image_name)[0]] = im
    return style_name_to_image


if __name__ == '__main__':
    style_name_to_image = _load_style_images('style-image')
    run()
