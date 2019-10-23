#!/usr/bin/env python
#
# OpenRTiST
#   - Real-time Style Transfer
#
#   Author: Zhuo Chen <zhuoc@cs.cmu.edu>
#           Shilpa George <shilpag@andrew.cmu.edu>
#           Thomas Eiszler <teiszler@andrew.cmu.edu>
#           Padmanabhan Pillai <padmanabhan.s.pillai@intel.com>
#
#   Copyright (C) 2011-2019 Carnegie Mellon University
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
#
# Portions of this code borrow from sample code distributed as part of
# Intel OpenVino, which is also distributed under the Apache License.
#
# Portions of this code were modified from sampled code distributed as part of
# the fast_neural_style example that is part of the pytorch repository and is
# distributed under the BSD 3-Clause License.
# https://github.com/pytorch/examples/blob/master/LICENSE

import os
import sys
import subprocess

import torch
from server.transformer_net import TransformerNet

def convert(model):
	model_out = model if not model.endswith('.model') else model[:-6]
	style_model = TransformerNet()
	style_model.load_state_dict(torch.load(model))
	dummy_input = torch.randn(1, 3, 240, 320)
	input_names = ['Input']
	output_names = ['Output']
	torch.onnx.export(style_model, dummy_input, "tmp.onnx",
					  verbose=False, input_names=input_names, output_names=output_names)
	subprocess.call(['/opt/intel/openvino/deployment_tools/model_optimizer/mo_onnx.py',
					 '--input_model', 'tmp.onnx', '--data_type', 'FP16',
					 '--model_name', model_out])
	os.remove("tmp.onnx")

if __name__ == "__main__":
	if len(sys.argv) == 1:
		print("Use to convert pytorch .model files to openvino .xml and .bin files")
		print("Usage:  convert file1.model [file2.model ...]")
		print("   Note: requires opentorch, openvino, and onnx prerequisites for openvino are installed")
	else:
		for model in sys.argv[1:]:
			convert(model)
