#!/usr/bin/env python3
#
# Copyright 2018-2023 Carnegie Mellon University
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
import asyncio
import importlib.resources
import logging
import os
import sys  # We need sys so that we can pass argv to QApplication
from pathlib import Path

from PyQt5 import QtGui, QtWidgets
from PyQt5.QtCore import QRectF, Qt, QThread, pyqtSignal
from PyQt5.QtGui import (
    QBrush,
    QCursor,
    QFont,
    QFontMetrics,
    QImage,
    QPainter,
    QPainterPath,
    QPen,
    QPixmap,
)

from . import design  # This file holds our MainWindow and all design related things
from . import capture_adapter


class UI(QtWidgets.QMainWindow, design.Ui_MainWindow):
    def __init__(self):
        super().__init__()
        self.setupUi(self)  # This is defined in design.py file automatically

    def addArtistInfo(self, painter, thumbnail, text):
        painter.setRenderHint(QPainter.Antialiasing)

        # Create the path
        path = QPainterPath()
        # Set painter colors to given values.
        font = QFont("Arial", 7, QFont.Bold)
        pen = QPen(Qt.red, 1)
        painter.setPen(pen)
        brush = QBrush(Qt.black)
        painter.setBrush(brush)
        fm = QFontMetrics(font)
        textWidthInPixels = fm.width(text)
        textHeightInPixels = fm.height()
        rect = QRectF(
            0, thumbnail.height() + 5, textWidthInPixels + 10, textHeightInPixels + 35
        )

        # Add the rect to path.
        path.addRect(rect)
        painter.setClipPath(path)

        # Fill shape, draw the border and center the text.
        painter.fillPath(path, painter.brush())
        painter.strokePath(path, painter.pen())
        painter.setPen(Qt.white)
        painter.setFont(font)
        painter.drawText(rect, Qt.AlignCenter, text)

    def set_image(self, frame, str_name, style_image):
        img = QImage(
            frame,
            frame.shape[1],
            frame.shape[0],
            frame.strides[0],
            QtGui.QImage.Format_RGB888,
        )

        pix = QPixmap.fromImage(img)
        pix = pix.scaledToWidth(1200)

        # print(f"UI STYLE {str_name}")
        # w = self.label_image.maximumWidth();
        # h = self.label_image.maximumHeight();
        # pix = pix.scaled(QSize(w, h), Qt.KeepAspectRatio, Qt.SmoothTransformation);
        if style_image is not None:
            img = QImage(
                style_image,
                style_image.shape[1],
                style_image.shape[0],
                style_image.strides[0],
                QtGui.QImage.Format_RGB888,
            )
            pixmap = QPixmap.fromImage(img)
        else:
            pixmap = QPixmap()
            image_data = importlib.resources.read_binary(
                "openrtist.style_image", str_name
            )
            pixmap.loadFromData(image_data)

        try:
            artist_info_path = Path(str_name).with_suffix(".txt")
            artist_info = importlib.resources.read_text(
                "openrtist.style_image", artist_info_path
            )
        except IOError:
            artist_info = str_name + " (Unknown)"

        artist_info = artist_info.replace("(", "\n -")
        artist_info = artist_info.replace(")", "")
        pixmap = pixmap.scaledToWidth(256)
        painter = QPainter()
        painter.begin(pix)
        painter.drawPixmap(0, 0, pixmap)
        painter.setPen(Qt.red)
        painter.drawRect(0, 0, pixmap.width(), pixmap.height())
        self.addArtistInfo(painter, pixmap, artist_info)
        painter.end()
        self.label_image.setPixmap(pix)
        self.label_image.setScaledContents(True)


class ClientThread(QThread):
    pyqt_signal = pyqtSignal(object, str, object)

    def __init__(self, server, video_source=None, capture_device=-1):
        super().__init__()
        self._server = server
        self._video_source = video_source
        self._capture_device = capture_device

    def run(self):
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

        def consume_rgb_frame_style(rgb_frame, style, style_image):
            self.pyqt_signal.emit(rgb_frame, style, style_image)

        client = capture_adapter.create_client(
            self._server,
            consume_rgb_frame_style,
            video_source=self._video_source,
            capture_device=self._capture_device,
        )
        client.launch()

    def stop(self):
        self._client.stop()


def main(argv=sys.argv):
    logging.basicConfig(level=logging.INFO)

    parser = argparse.ArgumentParser()
    parser.add_argument(
        "server", help="IP address (and optional :port) for OpenRTiST Server"
    )
    parser.add_argument(
        "-v",
        "--video",
        metavar="URL",
        type=str,
        help="video stream (default: try to use USB webcam)",
    )
    parser.add_argument(
        "-d", "--device", type=int, default=-1, help="Capture device (default: -1)"
    )
    parser.add_argument("--fullscreen", action="store_true")
    args = parser.parse_args(argv)

    app = QtWidgets.QApplication(argv)
    ui = UI()
    ui.show()
    if args.fullscreen:
        ui.showFullScreen()
        app.setOverrideCursor(QCursor(Qt.BlankCursor))

    clientThread = ClientThread(args.server, args.video, args.device)
    clientThread.pyqt_signal.connect(ui.set_image)
    clientThread.finished.connect(app.exit)
    clientThread.start()

    return app.exec()  # return Dialog Code


if __name__ == "__main__":
    sys.exit(main())
