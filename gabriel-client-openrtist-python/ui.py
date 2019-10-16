#!/usr/bin/env python3

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
from PyQt5 import QtWidgets
from PyQt5 import QtGui
from PyQt5.QtCore import Qt
from PyQt5.QtCore import QThread
from PyQt5.QtCore import pyqtSignal
from PyQt5.QtCore import QSize
from PyQt5.QtGui import QPainter
from PyQt5.QtGui import QPixmap
from PyQt5.QtGui import QImage
import sys  # We need sys so that we can pass argv to QApplication
import design  # This file holds our MainWindow and all design related things
import client
import logging


class UiClient(client.OpenrtistClient):
    def __init__(self, server_ip, pyqt_signal):
        super().__init__(server_ip)
        self.pyqt_signal = pyqt_signal

    def consume_update(self, rgb_frame, style):
        self.pyqt_signal.emit(rgb_frame, style)


class UI(QtWidgets.QMainWindow, design.Ui_MainWindow):
    def __init__(self):
        super().__init__()
        self.setupUi(self)  # This is defined in design.py file automatically

    def set_image(self, frame,str_name):
        img = QImage(frame, frame.shape[1], frame.shape[0], QtGui.QImage.Format_RGB888)

        pix = QPixmap.fromImage(img)
        pix = pix.scaledToWidth(1200)

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
    pyqt_signal = pyqtSignal(object, str)

    def __init__(self, server_ip):
        super().__init__()
        self._openrtist_client = UiClient(server_ip, self.pyqt_signal)
    def run(self):
        self._openrtist_client.launch()

    def stop(self):
        self._openrtist_client.stop()


def main():
    logging.basicConfig(level=logging.INFO)

    parser = argparse.ArgumentParser()
    parser.add_argument("server_ip", action="store", help="IP address for Openrtist Server")
    inputs = parser.parse_args()

    app = QtWidgets.QApplication(sys.argv)
    ui = UI()
    ui.show()
    clientThread = ClientThread(inputs.server_ip)
    clientThread.pyqt_signal.connect(ui.set_image)
    clientThread.finished.connect(app.exit)
    clientThread.start()

    sys.exit(app.exec())  # return Dialog Code


if __name__ == '__main__':
    main()
