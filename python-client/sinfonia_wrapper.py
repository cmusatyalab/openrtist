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
import asyncio
from PyQt5 import QtWidgets
from PyQt5 import QtGui
from PyQt5.QtCore import Qt, QRectF
from PyQt5.QtCore import QThread
from PyQt5.QtCore import pyqtSignal
from PyQt5.QtGui import QCursor
from PyQt5.QtGui import QPainter, QFont, QPainterPath, QPen, QBrush
from PyQt5.QtGui import QPixmap, QFontMetrics
from PyQt5.QtGui import QImage
from sinfonia_tier3 import sinfonia_tier3
import capture_adapter
from ui import *
import os
import sys  # We need sys so that we can pass argv to QApplication
import design  # This file holds our MainWindow and all design related things
import logging
from time import sleep, time

SINFONIA = "sinfonia"
STAGING = "stage2"
DEFAULT_TIMEOUT = 10 # timeout in seconds
TIER1_URL = "https://cmu.findcloudlet.org"
CPU_UUID = "737b5001-d27a-413f-9806-abf9bfce6746"
GPU_UUID = "755e5883-0788-44da-8778-2113eddf4271"


def launchServer(mode="CPU"):
    logging.info("Launching Backend Server using Sinfonia...")

    sinfonia_uuid = GPU_UUID if (mode == "GPU") else CPU_UUID
    tier1_url = TIER1_URL

    cmd = " ".join(["sinfonia_tier3", 
           tier1_url, 
           sinfonia_uuid, 
           "python3", 
           "-m", 
           sys.argv[0],
           "stage2"])
    
    logging.info(f"Sending request to launch backend via: ${cmd}.")

    status = sinfonia_tier3(
        str(tier1_url),
        sinfonia_uuid,
        [sys.executable, "-m", "ui", "stage2"]),

    logging.info(f"Status: {status}")


def main():
    logging.basicConfig(level=logging.INFO)

    print(f"Main method entered! {sys.argv}")

    parser = argparse.ArgumentParser()
    parser.add_argument(
        "server_ip", action="store", help="IP address for Openrtist Server"
    )
    parser.add_argument(
        "-v", "--video", metavar="URL", type=str, help="video stream (default: try to use USB webcam)"
    )
    parser.add_argument(
        "-d", "--device", type=int, default=-1, help="Capture device (default: -1)"
    )
    parser.add_argument(
        "--fullscreen", action="store_true"
    )
    inputs = parser.parse_args()

    if str(inputs.server_ip) == SINFONIA:
        logging.info("Using Sinfonia to open openrtist...")
        launchServer()
    elif str(inputs.server_ip) == STAGING:
        stageServer()
    else:
        logging.info("Wrong argument. Please use ui.py instead.")


    sys.exit(0)

    # sys.exit(app.exec())  # return Dialog Code


if __name__ == "__main__":
    main()
