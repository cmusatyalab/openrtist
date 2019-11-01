#!/usr/bin/env python
#
# OpenRTiST
#   - Real-time Style Transfer
#
#   Author: Zhuo Chen <zhuoc@cs.cmu.edu>
#           Shilpa George <shilpag@andrew.cmu.edu>
#           Thomas Eiszler <teiszler@andrew.cmu.edu>
#           Padmanabhan Pillai <padmanabhan.s.pillai@intel.com>
#           Junjue Wang <junjuew.cs.cmu.edu>
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

"""Script to freeze and quantize a model for inference on mobile platforms
and other languages. Pytorch 1.3 is required.

To use:

In the root directory

```python
python scripts/freeze_model.py freeze \
--weight-file-path='models_1p0/starry-night.model' \
--output-file-path='starry-night.pt'
```

Note: Quantize doesn't seem to have an effect as openrtist does not contain
fully connected layer.
"""

import cv2
import fire
import torch
from torchvision import transforms

import sys

sys.path.append(".")
from server.transformer_net import TransformerNet


class Tracer:
    def __init__(self):
        self._model = TransformerNet()

    def freeze(
        self,
        weight_file_path="models_1p0/starry-night.model",
        output_file_path="starry-night.pt",
    ):
        """Freeze a pytorch model weight file together with model definition.

        The output frozen model can be loaded and used as a standalone file, in
        different platforms and languages.
        """
        self._model.load_state_dict(torch.load(weight_file_path))
        model_input = torch.rand(1, 3, 224, 224)
        traced_model = torch.jit.trace(self._model, model_input)
        traced_model.save(output_file_path)

    def quantize_and_freeze(
        self,
        weight_file_path="models_1p0/starry-night.model",
        output_file_path="starry-night-quantized.pt",
    ):
        """Quantize and freeze a pytorch weight file for fast inference on mobile devices.

        Only fully-connected layers are quantized. See more in pytorch's
        torch.quantization.quantize_dynamic API.
        """
        self._model.load_state_dict(torch.load(weight_file_path))
        self._model = torch.quantization.quantize_dynamic(
            self._model, dtype=torch.qint8
        )
        model_input = torch.rand(1, 3, 224, 224)
        traced_model = torch.jit.trace(self._model, model_input)
        traced_model.save(output_file_path)

    def verify_on_livestream(self, weight_file_path="models_1p0/starry-night.model"):
        self._model.load_state_dict(torch.load(weight_file_path))
        preprocess = transforms.Compose([transforms.ToTensor()])
        _cam = cv2.VideoCapture(0)
        while True:
            _, img = _cam.read()
            if img is None:
                break
            else:
                model_input = preprocess(img)
                model_input = model_input.unsqueeze(0)
                output = self._model(model_input)
                img_out = output.data[0].clamp(0, 255).numpy()
                img_out = img_out.transpose(1, 2, 0)
                img_out = img_out.astype("uint8")
                img_out = cv2.cvtColor(img_out, cv2.COLOR_RGB2BGR)
                cv2.imshow("transfered", img_out)
                cv2.waitKey(1)


if __name__ == "__main__":
    fire.Fire(Tracer)
