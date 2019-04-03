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
    libjasper-dev \
    libpng12-dev \
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
    && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*


#Install PyTorch

RUN pip install --upgrade pip && pip install --upgrade numpy && pip install \
    http://download.pytorch.org/whl/cu80/torch-0.3.0.post4-cp27-cp27mu-linux_x86_64.whl \
    torchvision 

RUN apt-get update && apt-get install -y \
    tmux \
    net-tools \
    python-opencv \
&& apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

EXPOSE 7070 9098 9111 22222
CMD ["bash", "-c", "gabriel-control -d -n eth0 & sleep 5; gabriel-ucomm -s 127.0.0.1:8021 & sleep 5; cd /openrtist/server && python proxy.py -s 127.0.0.1:8021"]
