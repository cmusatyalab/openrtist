FROM cmusatyalab/gabriel
MAINTAINER Satyalab, satya-group@lists.andrew.cmu.edu

WORKDIR /
RUN git clone https://github.com/cmusatyalab/openrtist.git

RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    curl \
    git \
    gcc \
    vim \
    emacs \
    python \
    python-dev \
    pkg-config \
    libjpeg8-dev \
    libtiff5-dev \
    libgtk2.0-dev \
    libavcodec-dev \
    libavformat-dev \
    libswscale-dev \
    libv4l-dev \
    libatlas-base-dev \
    gfortran \
    libhdf5-dev \
    python-qt4 \
    python-qt4-dev \
    pyqt4-dev-tools \
    qt4-designer \
    default-jre \
    python-pip \
    python-numpy \
    python-opencv \
    python-openssl \
    pssh \
    python-psutil \
    wget \
    zip \
    libjpeg-dev \
    libfreetype6-dev \
    zlib1g-dev \
    x11-apps \
    tmux \
    net-tools \
    python-opencv \
    && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*


#Install PyTorch

RUN pip install numpy torch torchvision py-cpuinfo

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

EXPOSE 7070 9098 9111 22222
CMD ["bash", "-c", "gabriel-control -n eth0 & sleep 5; gabriel-ucomm -s 127.0.0.1:8021 & sleep 5; cd /openrtist/server ; source /opt/intel/openvino/bin/setupvars.sh -pyver 2.7 && python proxy.py -s 127.0.0.1:8021"]
