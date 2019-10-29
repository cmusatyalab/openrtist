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

#Install PyTorch
RUN python3 -m pip install opencv-python<5 torchvision

#Install Intel OpenVINO and OpenCL drivers
RUN echo "deb http://ppa.launchpad.net/intel-opencl/intel-opencl/ubuntu bionic main" >> /etc/apt/sources.list; \
    echo "deb https://apt.repos.intel.com/openvino/2019/ all main" >> /etc/apt/sources.list; \
    wget https://apt.repos.intel.com/intel-gpg-keys/GPG-PUB-KEY-INTEL-SW-PRODUCTS-2019.PUB; \
    apt-key add GPG-PUB-KEY-INTEL-SW-PRODUCTS-2019.PUB; \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys B9732172C4830B8F; \
    apt-get -y update ; \
    apt-get install -y intel-opencl-icd clinfo ocl-icd-libopencl1 intel-openvino-dev-ubuntu18-2019.3.344; \
    rm GPG-PUB-KEY-INTEL-SW-PRODUCTS-2019.PUB

#Prevent NVIDIA libOpenCL.so from being loaded
RUN mv /usr/local/cuda-10.1/targets/x86_64-linux/lib/libOpenCL.so.1 /usr/local/cuda-10.1/targets/x86_64-linux/lib/libOpenCL.so.1.bak

RUN git clone -b New-Gabriel-Module https://github.com/cmusatyalab/openrtist.git
WORKDIR openrtist/server
RUN python3 -m pip install -r requirements.txt

EXPOSE 5555 9098
ENTRYPOINT ["./main.py"]
