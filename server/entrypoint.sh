#!/bin/bash
args=$*
source /opt/intel/openvino/bin/setupvars.sh
/usr/bin/nvidia-smi -a
python3.7 ./main.py $args
