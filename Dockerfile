FROM nvidia/cuda:10.1-devel-ubuntu18.04
MAINTAINER Satyalab, satya-group@lists.andrew.cmu.edu

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y \
    --no-install-recommends \
    apt-utils

RUN apt-get install -y \
    build-essential \
    python3 \
    python3-pip \
    git \
    && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN git clone -b new-gabriel https://github.com/cmusatyalab/openrtist.git
WORKDIR openrtist/server
RUN git submodule update --init --recursive
RUN pip install -r requirements.txt

#Install PyTorch

RUN pip install --upgrade pip && pip install --upgrade numpy && pip install \
    http://download.pytorch.org/whl/cu80/torch-0.3.0.post4-cp27-cp27mu-linux_x86_64.whl \
    torchvision==0.2.2 

EXPOSE 5555 9098
CMD ["bash"]
