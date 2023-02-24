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

STAGING = "stage2"
DEFAULT_TIMEOUT = 10  # timeout in seconds
TIER1_URL = "https://cmu.findcloudlet.org"
OPENRTIST_BACKENDS = {
    "cpu": {
        "uuid": "737b5001-d27a-413f-9806-abf9bfce6746",
        "dns": "openrtist",
        "port": 9099,
    },
    "gpu": {
        "uuid": "737b5001-d27a-413f-9806-abf9bfce6746",
        "dns": "openrtist",
        "port": 9099,
    },
}


def launchServer(application_args, backend):
    """
    Call sinfonia tier3 to deploy the backend,
    and call stage2 which will wait until the backend is ready.
    """
    # TODO: make tier1 url and uuid as optional customized input

    logging.info("Launching Backend Server using Sinfonia...")

    sinfonia_uuid = OPENRTIST_BACKENDS[backend]["uuid"]
    tier1_url = TIER1_URL

    logging.info(f"Sending request to sinfonia-tier3 to launch backend.")

    logging.debug(
        f"Request sent: "
        + str([sys.executable, "-m", "sinfonia_wrapper", "-s"] + application_args)
    )

    print(os.path.dirname(os.path.abspath(__file__)))

    status = sinfonia_tier3(
        str(tier1_url),
        sinfonia_uuid,
        [
            sys.executable,
            "-m",
            "openrtist.sinfonia_wrapper",  # TODO: add relative path to sinfonia_wrapper
            "-s",
            "-b",
            backend,
        ]
        + application_args,
    )

    logging.info(f"Status: {status}")


def stage(application_args, backend):
    timeout = DEFAULT_TIMEOUT
    logging.info("Staging, waiting for backend server to start...")

    start_time = time()

    while True:
        OPENRTIST_DNS = OPENRTIST_BACKENDS[backend]["dns"]
        OPENRTIST_PORT = OPENRTIST_BACKENDS[backend]["port"]

        try:
            with socket.create_connection(
                (OPENRTIST_DNS, OPENRTIST_PORT), 1.0
            ) as sockfd:
                sockfd.settimeout(1.0)
                break
        except (socket.gaierror, ConnectionRefusedError, socket.timeout):
            logging.info("Backend not ready yet. Retry in 1 second...")
            sleep(1)

        if time() - start_time > timeout:
            raise Exception(
                f"Connection to backend server timeout after {timeout} seconds."
            )

    try:
        cmd = [
            sys.executable,
            "-m",
            "openrtist.ui",  # TODO: getting UI path
            OPENRTIST_BACKENDS[backend]["dns"],
        ] + application_args
        subprocess.run(cmd)
        logging.info("Frontend terminated.")

    except Exception as e:
        logging.info("Getting Error...")
        print(e)


def main():
    logging.basicConfig(level=logging.INFO)

    parser = argparse.ArgumentParser()
    parser.add_argument(
        "-b",
        "--backend",
        action="store",
        help="Choose the backend for sinfonia to use",
        default="cpu",
    )
    parser.add_argument(
        "-s",
        "--stage",
        action="store_true",
        help="Calling the staging for sinfonia, not for external use.",
    )

    inputs, application_args = parser.parse_known_args()

    if inputs.stage:
        logging.warning("Calling internal staging function!")
        stage(application_args, inputs.backend)
    else:
        logging.info("Using Sinfonia to open openrtist...")
        launchServer(application_args, inputs.backend)

    sys.exit(0)


if __name__ == "__main__":
    main()
