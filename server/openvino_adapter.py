# OpenRTiST
#   - Real-time Style Transfer
#
#   Author: Zhuo Chen <zhuoc@cs.cmu.edu>
#           Shilpa George <shilpag@andrew.cmu.edu>
#           Thomas Eiszler <teiszler@andrew.cmu.edu>
#           Padmanabhan Pillai <padmanabhan.s.pillai@intel.com>
#           Roger Iyengar <iyengar@cmu.edu>
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

import openvino
from openvino.inference_engine import IENetwork
from openvino.inference_engine import IEPlugin
from openrtist_adapter import OpenrtistAdapter
from cpuinfo import get_cpu_info
import numpy as np
import logging
import os
import sys
import cv2
from distutils.version import LooseVersion

logger = logging.getLogger(__name__)


class OpenvinoAdapter(OpenrtistAdapter):
    def __init__(self, cpu_only, default_style):
        super().__init__(default_style)
        device = 'CPU' if cpu_only else 'GPU'
        self.plugin = IEPlugin(device=device, plugin_dirs=None)

        if LooseVersion(openvino.inference_engine.__version__) >= LooseVersion("2.0"):
            models_dir = 'models_1p0'
            model_xml_num = '16'
            model_bin_suff = '.bin'
        else:
            models_dir = 'models'
            model_xml_num = '32' if cpu_only else '16v2'
            model_bin_suff = '-32.bin' if cpu_only else '-16.bin'

        self.path = os.path.join(os.getcwd(), '..', models_dir)
        model_xml = os.path.join(self.path, '{}.xml'.format(model_xml_num))

        conf = {}
        if cpu_only:
            cpuinf = get_cpu_info()
            if 'avx512' in cpuinf['flags']:
                self.plugin.add_cpu_extension("libcpu_extension_avx512.so")
            elif 'avx2' in cpuinf['flags']:
                self.plugin.add_cpu_extension("libcpu_extension_avx2.so")
            else:
                self.plugin.add_cpu_extension("libcpu_extension_sse4.so")
            conf['CPU_THREADS_NUM'] = str(cpuinf['count'])
        elif LooseVersion(openvino.inference_engine.__version__) < LooseVersion("2.0"):
            config_file = os.path.join(
                os.getcwd(), '..', 'clkernels', 'mvn_custom_layer.xml')
            self.plugin.set_config({'CONFIG_FILE' : config_file})

        self.exec_nets = {}
        names = [
            n[:-len(model_bin_suff)] for n in os.listdir(self.path) if n.endswith(model_bin_suff)
        ]
        for name in names:
            model_bin = os.path.join(self.path, name + model_bin_suff)
            m_xml = self.path+name+".xml"
            if not os.path.isfile(m_xml):
                m_xml = model_xml

            # Read IR
            print('Loading network files:\n\t', m_xml, '\n\t', model_bin)
            net = IENetwork(model=m_xml, weights=model_bin)

            if cpu_only:
                supported_layers = self.plugin.get_supported_layers(net)
                not_supported_layers = [
                    l for l in net.layers.keys() if l not in supported_layers
                ]

                if len(not_supported_layers) != 0:
                    print('Following layers are not supported by the plugin for'
                          ' specified device', self.plugin.device, ':\n',
                          ', '.join(not_supported_layers))
                    sys.exit(1)

            self.input_blob = next(iter(net.inputs))
            self.out_blob = next(iter(net.outputs))
            net.batch_size = 1

            # Loading model to the plugin
            print("Loading model to the plugin")
            self.exec_nets[name] = self.plugin.load(network=net, config=conf)
            self.add_supported_style(name)
            self.n, self.c, self.h, self.w = net.inputs[self.input_blob].shape
            del net

    def preprocessing(self, img):
        if img.shape[:-1] != (self.h, self.w):
            logger.warning('Image is resized from '+str(img.shape[:-1])+' to '+
                           str((self.h, self.w)) )
            img = cv2.resize(img,(self.w, self.h))
        img = img.transpose((2, 0, 1))  # Change data layout from HWC to CHW
        return [ img ]

    def inference(self, preprocessed):
        return self.exec_nets[self.get_style()].infer(
            inputs={self.input_blob: preprocessed})

    def postprocessing(self, post_inference):
        img_out = post_inference[self.out_blob][0]
        img_out = np.swapaxes(img_out, 0, 2)
        img_out = np.swapaxes(img_out, 0, 1)
        img_out[img_out < 0] = 0
        img_out[img_out > 255] = 255

        return img_out

