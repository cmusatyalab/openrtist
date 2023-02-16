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
from sinfonia_tier3 import sinfonia_tier3
import sys
import logging
import socket
import subprocess
from time import sleep, time

STAGING = "stage2"
DEFAULT_TIMEOUT = 10 # timeout in seconds
TIER1_URL = "https://cmu.findcloudlet.org"
CPU_UUID = "737b5001-d27a-413f-9806-abf9bfce6746"
GPU_UUID = "755e5883-0788-44da-8778-2113eddf4271"
OPENRTIST_DNS = "openrtist"
OPENRTIST_PORT = 9099


def launchServer(application_args, use_gpu=False):
    """
    Call sinfonia tier3 to deploy the backend,
    and call stage2 which will wait until the backend is ready.
    """
    # TODO: make tier1 url and uuid as optional customized input

    logging.info("Launching Backend Server using Sinfonia...")

    sinfonia_uuid = GPU_UUID if use_gpu else CPU_UUID
    tier1_url = TIER1_URL

    logging.info(f"Sending request to sinfonia-tier3 to launch backend.")

    print([
            sys.executable, 
            "-m", 
            "sinfonia_wrapper", # TODO: add relative path to sinfonia_wrapper
            "-s"
        ] + application_args)

    status = sinfonia_tier3(
        str(tier1_url),
        sinfonia_uuid,
        [
            sys.executable, 
            "-m", 
            "sinfonia_wrapper", # TODO: add relative path to sinfonia_wrapper
            "-s"
        ] + application_args
        ) 

    logging.info(f"Status: {status}")

# def parse_args(inputs):
#     """
#     return the bash commands for openrtist as a string

#     :param inputs: parse args namespace
#     :return: a string of flags and values after the ui.py command
#     """

#     # server_ip = inputs.server_ip
#     v_flag = "-v " + str(inputs.video) if inputs.video else ""
#     d_flag = "-d " + str(inputs.device) if inputs.device else ""
#     fs_flag = "--fullscreen" if inputs.fullscreen else ""
#     all_flags = " ".join([v_flag, d_flag, fs_flag])
#     return all_flags.strip()

def stage(application_args, timeout=DEFAULT_TIMEOUT):
    timeout = DEFAULT_TIMEOUT # TODO: add timeout as a customized param
    logging.info("Staging, waiting for backend server to start...")

    start_time = time()

    while True:

        try:
            with socket.create_connection((OPENRTIST_DNS, OPENRTIST_PORT), 1.0) as sockfd:
                sockfd.settimeout(1.0)
                break
        except (socket.gaierror, ConnectionRefusedError, socket.timeout):
            logging.info("Backend not ready yet. Retry in 1 second...")
            sleep(1)
        
        if time() - start_time > timeout:
            raise Exception(f"Connection to backend server timeout after {timeout} seconds.")

    try:

        print(application_args)

        cmd = [
            sys.executable, 
            "-m", 
            "ui", # TODO: getting UI path
            "openrtist",  # TODO: setting openrtist alias name for customized backend
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
        "-g", "--gpu", action="store_true", help="Use GPU backend instead of CPU"
    )
    parser.add_argument(
        "-s", "--stage", action="store_true", help="Calling the staging for sinfonia, not for external use."
    )

    inputs, application_args = parser.parse_known_args()

    if (inputs.stage):
        logging.warning("Calling internal staging function!")
        stage(application_args)
    else:
        use_gpu = inputs.gpu

        logging.info("Using Sinfonia to open openrtist...")
        launchServer(application_args, use_gpu)

    sys.exit(0)


if __name__ == "__main__":
    main()
