#!/usr/bin/env python3

# Copyright 2023 Carnegie Mellon University
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
import logging
import os  # to get the path of the current running file
import socket
import subprocess
import sys
from time import sleep, time

from sinfonia_tier3 import sinfonia_tier3

DEFAULT_TIMEOUT = 10  # timeout in seconds
TIER1_URL = "https://cmu.findcloudlet.org"
OPENRTIST_BACKENDS = {
    "cpu": {
        "tier1": TIER1_URL,
        "uuid": "737b5001-d27a-413f-9806-abf9bfce6746",
        "host": "openrtist:9099",
    },
    "gpu": {
        "tier1": TIER1_URL,
        "uuid": "737b5001-d27a-413f-9806-abf9bfce6746",
        "host": "openrtist:9099",
    },
}
OPENRTIST_DEFAULT_PORT = 9099


def launchServer(backend, application_args):
    """
    Call sinfonia tier3 to deploy the backend,
    and call stage2 which will wait until the backend is ready.
    """
    # TODO: make tier1 url and uuid as optional customized input

    logging.info("Launching Backend Server using Sinfonia...")

    tier1_url = backend["tier1"]
    sinfonia_uuid = backend["uuid"]

    logging.info("Sending request to sinfonia-tier3 to launch backend.")

    status = sinfonia_tier3(
        str(tier1_url),
        sinfonia_uuid,
        [
            sys.executable,
            "-m",
            "openrtist.sinfonia_wrapper",
            "--connect",
            backend["host"],
        ]
        + application_args,
    )

    logging.info(f"Status: {status}")


# this should probably be promoted to a sinfonia_tier3 helper function.
def sinfonia_wait_for_port(host, port, timeout=DEFAULT_TIMEOUT):
    wait_until = time() + timeout

    while time() < wait_until:
        try:
            with socket.create_connection((host, port), 1.0) as sockfd:
                sockfd.settimeout(1.0)
                return True
        except (socket.gaierror, ConnectionRefusedError, socket.timeout):
            logging.info("Backend not ready yet. retrying...")
            sleep(1)
    return False


def launchUI(server, application_args):
    # split "server" into host + port
    host, *_port = server.rsplit(":", 1)
    try:
        port = int(_port[0])
    except (IndexError, ValueError):
        port = OPENRTIST_DEFAULT_PORT

    logging.info("Staging, waiting for backend server to start...")
    if not sinfonia_wait_for_port(host, port):
        log.error("Connection to backend server failed")
        sys.exit(1)

    try:
        from . import ui

        ret = ui.main(application_args + [server])
        logging.info("Frontend terminated.")
    except Exception as e:
        logging.exception("Caught exception...")
        ret = 2
    sys.exit(ret)


def main():
    logging.basicConfig(level=logging.INFO)

    parser = argparse.ArgumentParser()
    parser.add_argument(
        "-b",
        "--backend",
        choices=["gpu", "cpu"],
        default="cpu",
        help="Choose the backend for sinfonia to use",
    )
    parser.add_argument(
        "-c",
        "--connect",
        metavar="HOST:PORT",
        help="Connect to existing backend.",
    )
    args, application_args = parser.parse_known_args()

    if args.connect is None:
        backend = OPENRTIST_BACKENDS[args.backend]
        logging.info("Using Sinfonia to open openrtist...")
        launchServer(backend, application_args)
    else:
        logging.warning(f"Connecting to backend {args.connect}!")
        launchUI(args.connect, application_args)

    sys.exit(0)


if __name__ == "__main__":
    main()
