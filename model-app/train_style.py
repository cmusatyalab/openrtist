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

import argparse
import os
import time
import random
import torch
from torch.autograd import Variable
from torch.optim import Adam
from torch.utils.data import DataLoader
from torchvision import datasets
from torchvision import transforms
from torchvision import models
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
    # This may raise an OSError exception
    if not os.path.exists(args.save_model_dir):
        os.makedirs(args.save_model_dir)
    if (args.checkpoint_model_dir is not None) and (not os.path.exists(args.checkpoint_model_dir)):
        os.makedirs(args.checkpoint_model_dir)

def get_args():
    parser = argparse.ArgumentParser(description="parser for fast-neural-style")
    parser.add_argument("--epochs", type=int, default=2,
                        help="number of training epochs, default is 2")
    parser.add_argument("--batch-size", type=int, default=2,
                        help="batch size for training, default is 2")
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
                        help="size of training images, default is 512 X 512")
    parser.add_argument("--style-size", type=int, default=None,
                        help="size of style-image, default is the original size of style image")
    parser.add_argument("--seed", type=int, default=42,
                        help="random seed for training")
    parser.add_argument("--content-weight", type=float, default=1e5,
                        help="weight for content-loss, default is 1e5")
    parser.add_argument("--style-weight", type=float, default=5e10,
                        help="weight for style-loss, default is 1e10")
    parser.add_argument("--noise-weight", type=float, default=1000*1e5,
                        help="weight for noise-weight, default is 1000*1e5")
    parser.add_argument("--noise-count", type=int, default=1000,
                        help="weight for noise-count, default is 1000")
    parser.add_argument("--noise-range", type=int, default=30,
                        help="weight for noise-range, default is 30")
    parser.add_argument("--lr", type=float, default=1e-3,
                        help="learning rate, default is 1e-3")
    parser.add_argument("--log-interval", type=int, default=500,
                        help="number of images after which the training loss is logged, default is 500")
    parser.add_argument("--checkpoint-interval", type=int, default=2000,
                                  help="number of batches after which a checkpoint of the trained model will be created")

    return parser.parse_args()


def train(args, progress_callback):
    device = torch.device("cuda")
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

    transformer = TransformerNet().to(device)
    optimizer = Adam(transformer.parameters(), args.lr)
    mse_loss = torch.nn.MSELoss()

    vgg = Vgg16(requires_grad=False).to(device)
    style_transform = transforms.Compose([
        transforms.ToTensor(),
        transforms.Lambda(lambda x: x.mul(255))
    ])
    style = utils.load_image(args.style_image, size=args.style_size)
    style = style_transform(style)
    style = style.repeat(args.batch_size, 1, 1, 1).to(device)
    style_v = Variable(style)
    style_v = utils.normalize_batch(style_v)
    features_style = vgg(style_v)
    gram_style = [utils.gram_matrix(y) for y in features_style]

    for e in range(args.epochs):
        transformer.train()
        agg_content_loss = 0.
        agg_style_loss = 0.
        agg_flicker_loss = 0.
        count = 0
        if args.noise_count:
            noiseimg = torch.zeros([3, args.image_size, args.image_size])

            # prepare a noise image
            for ii in range(args.noise_count):
                xx = random.randrange(args.image_size)
                yy = random.randrange(args.image_size)

                noiseimg[0][yy][xx] += random.randrange(-args.noise_range, args.noise_range)
                noiseimg[1][yy][xx] += random.randrange(-args.noise_range, args.noise_range)
                noiseimg[2][yy][xx] += random.randrange(-args.noise_range, args.noise_range)

        for batch_id, (x, _) in enumerate(train_loader):
            n_batch = len(x)
            count += n_batch

            optimizer.zero_grad()

            if args.noise_count:
                # add the noise image to the source image
                noisy_x = x.clone()
                noisy_x = noisy_x + noiseimg
                noisy_x_v = Variable(noisy_x)
                noisy_x_v = noisy_x_v.to(device)
                noisy_y = transformer(noisy_x_v)
                noisy_y = utils.normalize_batch(noisy_y)
            
            x = Variable(x)
            x = x.to(device)
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

            flicker_loss = 0.
            if args.noise_count:
              flicker_loss = args.noise_weight * mse_loss(y, noisy_y.detach())
              total_loss += flicker_loss
              agg_flicker_loss += flicker_loss.item()

            total_loss.backward()
            optimizer.step()

            agg_content_loss += content_loss.item()
            agg_style_loss += style_loss.item()

            if (batch_id + 1) % args.log_interval == 0:
                progress_callback(e, args.epochs, count, len(train_dataset),
                    agg_content_loss / (batch_id + 1),
                    agg_style_loss / (batch_id + 1),
                    agg_flicker_loss / (batch_id + 1),
                    (agg_content_loss + agg_style_loss + agg_flicker_loss) / (batch_id + 1)
                )

            if args.checkpoint_model_dir is not None and (batch_id + 1) % args.checkpoint_interval == 0:
                transformer.eval().cpu()
                ckpt_model_filename = "ckpt_epoch_" + str(e) + "_batch_id_" + str(batch_id + 1) + ".pth"
                ckpt_model_path = os.path.join(args.checkpoint_model_dir, ckpt_model_filename)
                torch.save(transformer.state_dict(), ckpt_model_path)
                transformer.to(device).train()

    # save model
    transformer.eval().cpu()
    save_model_filename = os.path.basename(args.style_image).split('.')[0]+'.model'
    save_model_path = os.path.join(args.save_model_dir, save_model_filename)
    torch.save(transformer.state_dict(), save_model_path)

    print("\nDone, trained model saved at", save_model_path)
    return save_model_filename

def log_progress(epoch, num_epochs, count, num_images, content, style, flicker, total):
    mesg = "{}\tEpoch {}:\t[{}/{}]\tcontent: {:.6f}\tstyle: {:.6f}\tflicker: {:.6f}\ttotal: {:.6f}".format(
        time.ctime(), epoch + 1, count, num_images, content, style, flicker, total
    )
    print(mesg)

def main():
    if not torch.cuda.is_available():
        raise Exception("Cuda is not available")

    args = get_args()
    check_paths(args)
    train(args, log_progress)


if __name__ == "__main__":
    main()
