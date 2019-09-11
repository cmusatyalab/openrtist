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
    libsm6 \
    libxrender1 \
    && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN git clone -b new-gabriel https://github.com/cmusatyalab/openrtist.git
WORKDIR openrtist/server
RUN git submodule update --init --recursive
RUN python3 -m pip install -r requirements.txt

EXPOSE 5555 9098
CMD ["python3", "main.py"]
