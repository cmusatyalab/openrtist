import sys

def detect( args ):
    has_pytorch=False
    has_cuda=False
    has_ov=False
    has_ovgpu=False
    if args.openvino=='auto' or args.openvino=='no':
        try:
            import torch
            print("Autodetect: Loaded PyTorch")
            if args.cpu_only=='auto' or args.cpu_only=='yes':
                has_pytorch=True
            if args.cpu_only=='auto' or args.cpu_only=='no':
                torch.zeros(2,2).cuda()
                print("Autodetect: Detected GPU / CUDA support")
                has_cuda=True
        except ImportError:
            print("Autodetect: Failed to load PyTorch")
        except:
            print("Autodetect: Failed to detect GPU / CUDA support")
    if args.openvino=='auto' or args.openvino=='yes':
        try:
            from openvino.inference_engine import IENetwork, IEPlugin
            print("Autodetect: Loaded OpenVINO")
            if args.cpu_only=='auto' or args.cpu_only=='yes':
                has_ov=True
            if args.cpu_only=='auto' or args.cpu_only=='no':
                IEPlugin('GPU')
                print("Autodetect: Detected iGPU / clDNN support")
                has_ovgpu=True
        except ImportError:
            print("Autodetect: Failed to load OpenVINO")
        except:
            print("Autodetect: Failed to detect iGPU / clDNN support")
    if has_cuda:
        print("Autodetect:  Using PyTorch on GPU")
        args.openvino=False
        args.cpu_only=False
    elif has_ovgpu:
        print("Autodetect:  Using OpenVINO on GPU")
        args.openvino=True
        args.cpu_only=False
    elif has_ov:
        print("Autodetect:  Using OpenVINO on CPU")
        args.openvino=True
        args.cpu_only=True
    elif has_pytorch:
        print("Autodetect:  Using PyTorch on CPU")
        args.openvino=False
        args.cpu_only=True
    else:
        print("Autodetect:  No suitable configutation found!")
        print("Ensure PyTorch or OpenVINO are installed and configured")
        print("  and that -c and -o command-line options are set appropriately")
        sys.exit()
    return
