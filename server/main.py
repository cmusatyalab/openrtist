#!/usr/bin/env python3

from gabriel_server import local_engine
from openrtist_engine import OpenrtistEngine
from timing_engine import TimingEngine
import logging
import cv2
import argparse
import importlib

DEFAULT_PORT = 9099
DEFAULT_NUM_TOKENS = 2
INPUT_QUEUE_MAXSIZE = 60
DEFAULT_STYLE = "the_scream"
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger(__name__)


def create_adapter(openvino, cpu_only, force_torch, use_myriad):
    """Create the best adapter based on constraints passed as CLI arguments."""

    if use_myriad:
        openvino = True
        if cpu_only:
            raise Exception("Cannot run with both cpu-only and Myriad options")

    if force_torch and openvino:
        raise Exception("Cannot run with both Torch and OpenVINO")

    if not openvino:
        if importlib.util.find_spec("torch") is None:
            logger.info("Could not find Torch")
            openvino = True
        elif not cpu_only:
            import torch

            if torch.cuda.is_available():
                logger.info("Detected GPU / CUDA support")
                from torch_adapter import TorchAdapter

                return TorchAdapter(False, DEFAULT_STYLE)
            else:
                logger.info("Failed to detect GPU / CUDA support")

    if not force_torch:
        if importlib.util.find_spec("openvino") is None:
            logger.info("Could not find Openvino")
            if openvino:
                raise Exception("No suitable engine")
        else:
            if not cpu_only and not use_myriad:
                from openvino.inference_engine import IEPlugin

                try:
                    IEPlugin("GPU")
                    logger.info("Detected iGPU / clDNN support")
                except RuntimeError:
                    logger.info("Failed to detect iGPU / clDNN support")
                    cpu_only = True

            logger.info("Using OpenVINO")
            logger.info("CPU Only: %s", cpu_only)
            logger.info("Use Myriad: %s", use_myriad)
            from openvino_adapter import OpenvinoAdapter

            adapter = OpenvinoAdapter(cpu_only, DEFAULT_STYLE,
                                      use_myriad=use_myriad)
            return adapter

    logger.info("Using Torch with CPU")
    from torch_adapter import TorchAdapter

    return TorchAdapter(True, DEFAULT_STYLE)


def main():
    parser = argparse.ArgumentParser(
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument(
        "-t", "--tokens", type=int, default=DEFAULT_NUM_TOKENS,
        help="number of tokens")
    parser.add_argument(
        "-o",
        "--openvino",
        action="store_true",
        help="Pass this flag to force the use of OpenVINO."
        "Otherwise Torch may be used")
    parser.add_argument(
        "-c",
        "--cpu-only",
        action="store_true",
        help="Pass this flag to prevent the GPU from being used.")
    parser.add_argument(
        "--torch",
        action="store_true",
        help="Set this flag to force the use of torch. Otherwise"
        "OpenVINO may be used.")
    parser.add_argument(
        "--myriad",
        action="store_true",
        help="Set this flag to use Myriad VPU (implies use OpenVino).")
    parser.add_argument(
        "--timing", action="store_true", help="Print timing information")
    parser.add_argument(
        "-p", "--port", type=int, default=DEFAULT_PORT, help="Set port number")
    args = parser.parse_args()

    def engine_setup():
        adapter = create_adapter(args.openvino, args.cpu_only, args.torch,
                                 args.myriad)
        if args.timing:
            engine = TimingEngine(COMPRESSION_PARAMS, adapter)
        else:
            engine = OpenrtistEngine(COMPRESSION_PARAMS, adapter)

        return engine

    local_engine.run(
        engine_setup,
        OpenrtistEngine.SOURCE_NAME,
        INPUT_QUEUE_MAXSIZE,
        args.port,
        args.tokens)


if __name__ == "__main__":
    main()
