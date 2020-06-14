#!/usr/bin/env python3

from gabriel_server.network_engine import engine_runner
from openrtist_engine import OpenrtistEngine
import logging
import cv2
import importlib


DEFAULT_STYLE = "the_scream"
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger(__name__)


def create_adapter(openvino, cpu_only, force_torch, use_myriad):
    """Create the best adapter based on constraints passed as CLI arguments."""

    if use_myriad:
       openvino=True
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

            adapter = OpenvinoAdapter(cpu_only, DEFAULT_STYLE, use_myriad=use_myriad)
            return adapter

    logger.info("Using Torch with CPU")
    from torch_adapter import TorchAdapter

    return TorchAdapter(True, DEFAULT_STYLE)


def main():
    adapter = create_adapter(False, False, False, False)
    openrtist_engine = OpenrtistEngine(COMPRESSION_PARAMS, adapter)

    engine_runner.run(openrtist_engine, OpenrtistEngine.FILTER_NAME, 'tcp://localhost:5555')

if __name__ == "__main__":
    main()
