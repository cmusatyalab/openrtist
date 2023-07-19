FROM nvidia/cuda:11.8.0-runtime-ubuntu18.04
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
    python3.7 \
    python3.7-dev \
    python3-pip \
    python3-pyqt5 \
 && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Prevent NVIDIA libOpenCL.so from being loaded
RUN mv /usr/local/cuda-11.8/targets/x86_64-linux/lib/libOpenCL.so.1 \
       /usr/local/cuda-11.8/targets/x86_64-linux/lib/libOpenCL.so.1.bak

# Install PyTorch and Gabriel's external dependencies
COPY python-client/requirements.txt client-requirements.txt
COPY server/requirements.txt server-requirements.txt
RUN python3.7 -m pip install --upgrade pip \
 && python3.7 -m pip install --no-cache-dir \
    -r client-requirements.txt \
    -r server-requirements.txt

# You can speed up build slightly by reducing build context with
#     git archive --format=tgz HEAD | docker build -t openrtist -
COPY . openrtist
WORKDIR openrtist/server

EXPOSE 5555 9099
ENTRYPOINT ["./entrypoint.sh"]
