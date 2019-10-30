#!/usr/bin/env python3

from gabriel_server.local_engine import runner
from openrtist_engine import OpenrtistEngine
from timing_engine import TimingEngine
import timing_engine
import logging
import cv2
import argparse
import importlib

PORT = 9098
DEFAULT_NUM_TOKENS = 2
INPUT_QUEUE_MAXSIZE = 60
DEFAULT_STYLE = 'the_scream'
COMPRESSION_PARAMS = [cv2.IMWRITE_JPEG_QUALITY, 67]

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger(__name__)


def create_adapter(openvino, cpu_only, force_torch):
    '''Create the best adapter based on constraints passed as CLI arguments.'''

    if force_torch and openvino:
        raise Exception('Cannot run with both Torch and OpenVINO')

    if not openvino:
        if importlib.util.find_spec('torch') is None:
            logger.info('Could not find Torch')
            openvino = True
        elif not cpu_only:
            import torch
            if torch.cuda.is_available():
                logger.info('Detected GPU / CUDA support')
                from torch_adapter import TorchAdapter
                return TorchAdapter(False, DEFAULT_STYLE)
            else:
                logger.info('Failed to detect GPU / CUDA support')

    if not force_torch:
        if importlib.util.find_spec('openvino') is None:
            logger.info('Could not find Openvino')
            if openvino:
                raise Exception('No suitable engine')
        else:
            if not cpu_only:
                from openvino.inference_engine import IEPlugin
                try:
                    IEPlugin('GPU')
                    logger.info('Detected iGPU / clDNN support')
                except RuntimeError:
                    logger.info('Failed to detect iGPU / clDNN support')
                    cpu_only = True

            logger.info('Using OpenVINO')
            logger.info('CPU Only: %s', cpu_only)
            from openvino_adapter import OpenvinoAdapter
            adapter = OpenvinoAdapter(cpu_only, DEFAULT_STYLE)
            return adapter

    logger.info('Using Toch with CPU')
    from torch_adapter import TorchAdapter
    return TorchAdapter(True, DEFAULT_STYLE)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-t', '--tokens', type=int, default=DEFAULT_NUM_TOKENS,
                        help='number of tokens')
    parser.add_argument('-o', '--openvino', action='store_true',
                        help='Pass this flag to force the use of OpenVINO.'
                        'Otherwise Torch may be used')
    parser.add_argument('-c', '--cpu-only', action='store_true',
                        help='Pass this flag to prevent the GPU from being used.')
    parser.add_argument('--torch', action='store_true',
                        help='Set this flag to force the use of torch. Otherwise'
                        'OpenVINO may be used.')
    parser.add_argument('--timing', action='store_true',
                        help='Print timing information')
    args = parser.parse_args()

    def engine_setup():
        adapter = create_adapter(args.openvino, args.cpu_only, args.torch)

        if args.timing:
            engine = TimingEngine(COMPRESSION_PARAMS, adapter)
        else:
            engine = OpenrtistEngine(COMPRESSION_PARAMS, adapter)

        return engine

    runner.run(engine_setup, OpenrtistEngine.ENGINE_NAME, INPUT_QUEUE_MAXSIZE,
               PORT, args.tokens)


if __name__ == '__main__':
    main()
