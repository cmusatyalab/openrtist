#!/usr/bin/env python
# Copyright 2018 Carnegie Mellon University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
from PyQt5 import QtWidgets, QtGui, QtCore
from PyQt5.QtCore import Qt, QThread, pyqtSignal, QSize
from PyQt5.QtGui import *
#from PyQt5.QtGui import QPainter, QPixmap, QImage, QMessageBox, QVBoxLayout
import threading
import sys  # We need sys so that we can pass argv to QApplication
import design  # This file holds our MainWindow and all design related things
import os  # For listing directory methods
import client
import numpy as np
import re
import pdb

class UI(QtWidgets.QMainWindow, design.Ui_MainWindow):
    def __init__(self):
        super(self.__class__, self).__init__()
        self.setupUi(self)  # This is defined in design.py file automatically

    def set_image(self, frame,str_name):
        img = QImage(frame, frame.shape[1], frame.shape[0], QtGui.QImage.Format_RGB888)

        pix = QPixmap.fromImage(img)
        pix = pix.scaledToWidth(1200)

        #w = self.label_image.maximumWidth();
        #h = self.label_image.maximumHeight();
        #pix = pix.scaled(QSize(w, h), Qt.KeepAspectRatio, Qt.SmoothTransformation);
        pixmap = QPixmap()
        pixmap.load('style-image/'+str_name)
        print("UI STYLE {}".format('style-image/'+str_name))
        pixmap = pixmap.scaledToWidth(256)
        painter = QPainter()
        painter.begin(pix);
        painter.drawPixmap(0, 0, pixmap);
        painter.setPen(Qt.red)
        painter.drawRect(0,0,pixmap.width(),pixmap.height());
        painter.end()
        self.label_image.setPixmap(pix)
        self.label_image.setScaledContents(True);

class ClientThread(QThread):
    sig_frame_available = pyqtSignal(object,str)

    def __init__(self, *args, **kwargs):
        super(self.__class__, self).__init__()
        self._stop = threading.Event()
        self._gabriel_client = client.GabrielClient(*args, **kwargs)

    def run(self):
        self._gabriel_client.start(self.sig_frame_available)

    def stop(self):
        self._stop.set()
        self._gabriel_client.cleanup()

def main(inputs):
    app = QtWidgets.QApplication(sys.argv)
    ui = UI()
    ui.show()
    clientThread = ClientThread(inputs.server_ip)
    clientThread.sig_frame_available.connect(ui.set_image)
    clientThread.finished.connect(app.exit)
    clientThread.start()

    sys.exit(app.exec_())  # and execute the app


if __name__ == '__main__':  # if we're running file directly and not importing it
    parser = argparse.ArgumentParser()
    parser.add_argument("server_ip", action="store", help="IP address for Openrtist Server")
    inputs = parser.parse_args()
    main(inputs)
