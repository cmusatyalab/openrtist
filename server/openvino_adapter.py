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

import openvino.inference_engine
from openvino.inference_engine import IENetwork
from openvino.inference_engine import IEPlugin
from openrtist_adapter import OpenrtistAdapter
from cpuinfo import get_cpu_info
import numpy as np
import logging
import os
import cv2
from distutils.version import LooseVersion

logger = logging.getLogger(__name__)


class OpenvinoAdapter(OpenrtistAdapter):
    def __init__(self, cpu_only, default_style, use_myriad=False, max_lru=4):
        super().__init__(default_style)
        device = "MYRIAD" if use_myriad else ("CPU" if cpu_only else "GPU")
        self.plugin = IEPlugin(device=device, plugin_dirs=None)

        models_dir = "models"
        model_xml_num = "16"
        model_bin_suff = ".bin"

        self.path = os.path.join(os.getcwd(), "..", models_dir)
        model_xml = os.path.join(self.path, "{}.xml".format(model_xml_num))
        self.use_reshape = False  # True  # cpu_only

        self.conf = {}
        if use_myriad:
            pass
        elif cpu_only:
            cpuinf = get_cpu_info()
            if "avx512" in cpuinf["flags"]:
                self.plugin.add_cpu_extension("libcpu_extension_avx512.so")
            elif "avx2" in cpuinf["flags"]:
                self.plugin.add_cpu_extension("libcpu_extension_avx2.so")
            else:
                self.plugin.add_cpu_extension("libcpu_extension_sse4.so")
            self.conf["CPU_THREADS_NUM"] = str(cpuinf["count"])
        elif LooseVersion(openvino.inference_engine.__version__) < LooseVersion("2.0"):
            config_file = os.path.join(
                os.getcwd(), "..", "clkernels", "mvn_custom_layer.xml"
            )
            self.plugin.set_config({"CONFIG_FILE": config_file})

        self.nets = {}
        names = [
            n[: -len(model_bin_suff)]
            for n in os.listdir(self.path)
            if n.endswith(model_bin_suff)
        ]

        for name in names:
            model_bin = os.path.join(self.path, name + model_bin_suff)
            m_xml = os.path.join(self.path, name + ".xml")
            if not os.path.isfile(m_xml):
                m_xml = model_xml

            # Read IR
            logger.info("Loading network files:\n\t%s\n\t%s", m_xml, model_bin)
            net = IENetwork(model=m_xml, weights=model_bin)

            if not use_myriad and cpu_only:
                supported_layers = self.plugin.get_supported_layers(net)
                not_supported_layers = [
                    l for l in net.layers.keys() if l not in supported_layers
                ]

                if len(not_supported_layers) != 0:
                    logger.error(
                        "Following layers are not supported by the plugin"
                        " for specified device %s:\n%s",
                        self.plugin.device,
                        ", ".join(not_supported_layers),
                    )
                    raise Exception()

            self.input_blob = next(iter(net.inputs))
            self.out_blob = next(iter(net.outputs))
            net.batch_size = 1

            if self.use_reshape or use_myriad:
                self.nets[name] = (net, None)
            else:
                # Loading model to the plugin
                logger.info("Loading model to the plugin")
                self.nets[name] = (net, self.plugin.load(network=net, config=self.conf))
            self.add_supported_style(name)
            self.lru_style = []
            self.max_lru = max_lru

    def preprocessing(self, img):
        style = self.get_style()
        net, exec_net = self.nets[style]
        h, w = net.inputs[self.input_blob].shape[2:]
        reshaped = False
        if img.shape[:-1] != (h, w):
            if self.use_reshape:
                logger.warning("Network reshaped to %s", str(img.shape[:-1]))
                net.reshape({self.input_blob: (1, 3, img.shape[0], img.shape[1])})
                reshaped = True
            else:
                logger.warning(
                    "Image is resized from %s to %s", str(img.shape[:-1]), str((h, w))
                )
                img = cv2.resize(img, (w, h))
        if exec_net is None or reshaped:
            if len(self.lru_style) == 0 or self.lru_style[0] != style:
                if style in self.lru_style:
                    self.lru_style.remove(style)
                self.lru_style.insert(0, style)
            if len(self.lru_style) > self.max_lru:
                oldstyle = self.lru_style.pop()
                self.nets[oldstyle] = (self.nets[oldstyle][0], None)
            logger.info("Loading model to the plugin")
            self.nets[self.get_style()] = (
                net,
                self.plugin.load(network=net, config=self.conf),
            )
            if exec_net is not None:
                del exec_net
        img = img.transpose((2, 0, 1))  # Change data layout from HWC to CHW
        img = np.float32(img) * (1.0 / 255.0)  # convert to float
        return [img]

    def inference(self, preprocessed):
        return self.nets[self.get_style()][1].infer(
            inputs={self.input_blob: preprocessed}
        )

    def postprocessing(self, post_inference):
        img_out = post_inference[self.out_blob][0]
        img_out = img_out.transpose(1, 2, 0)
        img_out = np.clip(img_out, 0, 255)

        return img_out
