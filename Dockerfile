FROM nvidia/cuda:10.1-devel-ubuntu18.04
MAINTAINER Satyalab, satya-group@lists.andrew.cmu.edu

ARG DEBIAN_FRONTEND=noninteractive

# Install build and runtime dependencies, Intel OpenVINO and OpenCL drivers
RUN apt-get update && apt-get install -y curl
RUN echo "deb http://ppa.launchpad.net/intel-opencl/intel-opencl/ubuntu bionic main" >> /etc/apt/sources.list \
 && echo "deb https://apt.repos.intel.com/openvino/2019/ all main" >> /etc/apt/sources.list \
 && curl https://apt.repos.intel.com/intel-gpg-keys/GPG-PUB-KEY-INTEL-SW-PRODUCTS-2019.PUB | apt-key add - \
 && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys B9732172C4830B8F \
 && apt-get update && apt-get install -y \
    build-essential \
    clinfo \
    intel-opencl-icd \
    intel-openvino-dev-ubuntu18-2019.3.344 \
    libgtk-3-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    ocl-icd-libopencl1 \
    python3 \
    python3-pip \
 && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install PyTorch and Gabriel's external dependencies
RUN python3 -m pip install --no-cache-dir \
    'opencv-python<5' \
    protobuf \
    py-cpuinfo \
    pyzmq \
    'torchvision<0.5' \
    websockets \
    zmq \
    'gabriel-server==0.0.9'

# Prevent NVIDIA libOpenCL.so from being loaded
RUN mv /usr/local/cuda-10.1/targets/x86_64-linux/lib/libOpenCL.so.1 \
       /usr/local/cuda-10.1/targets/x86_64-linux/lib/libOpenCL.so.1.bak

# You can speed up build slightly by reducing build context with
#     git archive --format=tgz HEAD | docker build -t openrtist -
COPY . openrtist
WORKDIR openrtist/server
RUN python3 -m pip install -r requirements.txt

EXPOSE 5555 9098
ENTRYPOINT ["./entrypoint.sh"]
