#!/bin/bash
args=$*
# source /opt/intel/openvino/bin/setupvars.sh
source /opt/openvino_env/bin/activate
/usr/bin/nvidia-smi -a
python3 ./main.py $args
