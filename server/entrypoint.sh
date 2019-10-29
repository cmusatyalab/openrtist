#!/bin/bash
args=$*
source /opt/intel/openvino/bin/setupvars.sh
/usr/bin/python3 main.py $args
