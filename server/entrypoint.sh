#!/bin/bash
args=$*
source /opt/intel/openvino/bin/setupvars.sh
./main.py $args
