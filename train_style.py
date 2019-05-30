# #BSD 3-Clause License
#
# Copyright (c) 2017, 
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
#
# * Redistributions in binary form must reproduce the above copyright notice,
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
#
# * Neither the name of the copyright holder nor the names of its
#   contributors may be used to endorse or promote products derived from
#   this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import torch
import argparse
import os
import sys
import time
import re
import random
import torch
from torch.autograd import Variable
from torch.optim import Adam
from torch.utils.data import DataLoader
from torchvision import datasets
from torchvision import transforms
from torchvision import models
import cv2
from server import utils
from server.transformer_net import TransformerNet
import numpy as np
from collections import namedtuple

class Vgg16(torch.nn.Module):
    def __init__(self, requires_grad=False):
        super(Vgg16, self).__init__()
        vgg_pretrained_features = models.vgg16(pretrained=True).features
        self.slice1 = torch.nn.Sequential()
        self.slice2 = torch.nn.Sequential()
        self.slice3 = torch.nn.Sequential()
        self.slice4 = torch.nn.Sequential()
        for x in range(4):
            self.slice1.add_module(str(x), vgg_pretrained_features[x])
        for x in range(4, 9):
            self.slice2.add_module(str(x), vgg_pretrained_features[x])
        for x in range(9, 16):
            self.slice3.add_module(str(x), vgg_pretrained_features[x])
        for x in range(16, 23):
            self.slice4.add_module(str(x), vgg_pretrained_features[x])
        if not requires_grad:
            for param in self.parameters():
                param.requires_grad = False

    def forward(self, X):
        h = self.slice1(X)
        h_relu1_2 = h
        h = self.slice2(h)
        h_relu2_2 = h
        h = self.slice3(h)
        h_relu3_3 = h
        h = self.slice4(h)
        h_relu4_3 = h
        vgg_outputs = namedtuple("VggOutputs", ['relu1_2', 'relu2_2', 'relu3_3', 'relu4_3'])
        out = vgg_outputs(h_relu1_2, h_relu2_2, h_relu3_3, h_relu4_3)
        return out

def check_paths(args):
    try:
        if not os.path.exists(args.save_model_dir):
            os.makedirs(args.save_model_dir)
        if args.checkpoint_model_dir is not None and not (os.path.exists(args.checkpoint_model_dir)):
            os.makedirs(args.checkpoint_model_dir)
    except OSError as e:
        print(e)
        sys.exit(1)


def train(args):
    noise_count = 1000 
    noise_range = 30
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)

    transform = transforms.Compose([
        transforms.Resize(args.image_size),
        transforms.CenterCrop(args.image_size),
        transforms.ToTensor(),
        transforms.Lambda(lambda x: x.mul(255))
    ])
    train_dataset = datasets.ImageFolder(args.dataset, transform)
    train_loader = DataLoader(train_dataset, batch_size=args.batch_size)

    transformer = TransformerNet().cuda()
    optimizer = Adam(transformer.parameters(), args.lr)
    mse_loss = torch.nn.MSELoss()

    vgg = Vgg16(requires_grad=False).cuda()
    style_transform = transforms.Compose([
        transforms.ToTensor(),
        transforms.Lambda(lambda x: x.mul(255))
    ])
    style = utils.load_image(args.style_image, size=args.style_size)
    style = style_transform(style)
    style = style.repeat(args.batch_size, 1, 1, 1).cuda()
    style_v = Variable(style)
    style_v = utils.normalize_batch(style_v)
    features_style = vgg(style_v)
    gram_style = [utils.gram_matrix(y) for y in features_style]

    for e in range(args.epochs):
        transformer.train()
        agg_content_loss = 0.
        agg_style_loss = 0.
        agg_pop_loss = 0.
        count = 0
        if noise_count:
            noiseimg = torch.zeros([3, args.image_size, args.image_size])

            # prepare a noise image
            for ii in range(noise_count):
                xx = random.randrange(args.image_size)
                yy = random.randrange(args.image_size)

                noiseimg[0][yy][xx] += random.randrange(-noise_range, noise_range)
                noiseimg[1][yy][xx] += random.randrange(-noise_range, noise_range)
                noiseimg[2][yy][xx] += random.randrange(-noise_range, noise_range)

        for batch_id, (x, _) in enumerate(train_loader):
            n_batch = len(x)
            count += n_batch

            optimizer.zero_grad()

            if noise_count:
                # add the noise image to the source image
                noisy_x = x.clone()
                noisy_x = noisy_x + noiseimg
                noisy_x_v = Variable(noisy_x)
                noisy_x_v = noisy_x_v.cuda()
                noisy_y = transformer(noisy_x_v)
                noisy_y = utils.normalize_batch(noisy_y)
            
            x = Variable(x)
            x = x.cuda()
            y = transformer(x)

            y = utils.normalize_batch(y)
            x = utils.normalize_batch(x)

            features_y = vgg(y)
            features_x = vgg(x)

            content_loss = args.content_weight * mse_loss(features_y.relu2_2, features_x.relu2_2)

            style_loss = 0.
            for ft_y, gm_s in zip(features_y, gram_style):
                gm_y = utils.gram_matrix(ft_y)
                style_loss += mse_loss(gm_y, gm_s[:n_batch, :, :])
            style_loss *= args.style_weight

            total_loss = content_loss + style_loss

            pop_loss = 0.
            if noise_count:
              pop_loss = args.noise_weight * mse_loss(y, noisy_y.detach())
              total_loss += pop_loss

            total_loss.backward()
            optimizer.step()

            agg_content_loss += content_loss.data[0]
            agg_style_loss += style_loss.data[0]
            agg_pop_loss += pop_loss.data[0]

            if (batch_id + 1) % args.log_interval == 0:
                mesg = "{}\tEpoch {}:\t[{}/{}]\tcontent: {:.6f}\tstyle: {:.6f}\tpop: {:.6f}\ttotal: {:.6f}".format(
                    time.ctime(), e + 1, count, len(train_dataset),
                                  agg_content_loss / (batch_id + 1),
                                  agg_style_loss / (batch_id + 1),
                                  agg_pop_loss / (batch_id + 1),
                                  (agg_content_loss + agg_style_loss + agg_pop_loss) / (batch_id + 1)
                )
                print(mesg)

            if args.checkpoint_model_dir is not None and (batch_id + 1) % args.checkpoint_interval == 0:
                transformer.eval().cpu()
                ckpt_model_filename = "ckpt_epoch_" + str(e) + "_batch_id_" + str(batch_id + 1) + ".pth"
                ckpt_model_path = os.path.join(args.checkpoint_model_dir, ckpt_model_filename)
                torch.save(transformer.state_dict(), ckpt_model_path)
                transformer.cuda().train()

    # save model
    transformer.eval().cpu()
    save_model_filename = os.path.basename(args.style_image).split('.')[0]+'.model'
    save_model_path = os.path.join(args.save_model_dir, save_model_filename)
    torch.save(transformer.state_dict(), save_model_path)

    print("\nDone, trained model saved at", save_model_path)


def main():
    parser = argparse.ArgumentParser(description="parser for fast-neural-style")
    parser.add_argument("--epochs", type=int, default=2,
                        help="number of training epochs, default is 2")
    parser.add_argument("--batch-size", type=int, default=2,
                        help="batch size for training, default is 4")
    parser.add_argument("--dataset", type=str, default='/home/ubuntu/COCO/data/',
                        help="path to training dataset, the path should point to a folder "
                             "containing another folder with all the training images")
    parser.add_argument("--style-image", type=str, default="images/style-images/david_vaughan.jpg",
                        help="path to style-image")
    parser.add_argument("--save-model-dir", type=str, default='./',
                        help="path to folder where trained model will be saved.")
    parser.add_argument("--checkpoint-model-dir", type=str, default=None,
                        help="path to folder where checkpoints of trained models will be saved")
    parser.add_argument("--image-size", type=int, default=512,
                        help="size of training images, default is 256 X 256")
    parser.add_argument("--style-size", type=int, default=None,
                        help="size of style-image, default is the original size of style image")
    parser.add_argument("--seed", type=int, default=42,
                        help="random seed for training")
    parser.add_argument("--content-weight", type=float, default=1e5,
                        help="weight for content-loss, default is 1e5")
    parser.add_argument("--style-weight", type=float, default=5e10,
                        help="weight for style-loss, default is 1e10")
    parser.add_argument("--noise-weight", type=float, default=1000*1e5,
                        help="weight for style-loss, default is 1e10")
    parser.add_argument("--lr", type=float, default=1e-3,
                        help="learning rate, default is 1e-3")
    parser.add_argument("--log-interval", type=int, default=500,
                        help="number of images after which the training loss is logged, default is 500")
    parser.add_argument("--checkpoint-interval", type=int, default=2000,
                                  help="number of batches after which a checkpoint of the trained model will be created")

    args = parser.parse_args()

    if not torch.cuda.is_available():
        print("ERROR: cuda is not available")
        sys.exit(1)

    check_paths(args)
    train(args)


if __name__ == "__main__":
    main()
