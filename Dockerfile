FROM nvidia/cuda-arm64:11.1.1-devel-ubuntu18.04
MAINTAINER Satyalab, satya-group@lists.andrew.cmu.edu

ARG DEBIAN_FRONTEND=noninteractive

# Install build and runtime dependencies
RUN apt-get update && apt-get install -y curl
RUN apt-get install -y \
    build-essential \
    clinfo \
    libgtk-3-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    python3 \
    python3-pip \
    python3-pyqt5 \
    python3-opencv \
 && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install PyTorch and Gabriel's external dependencies
COPY python-client/requirements.txt client-requirements.txt
COPY server/requirements.txt server-requirements.txt
RUN python3 -m pip install --upgrade pip \
 && python3 -m pip install --no-cache-dir \
    -r client-requirements.txt \
    -r server-requirements.txt

# You can speed up build slightly by reducing build context with
#     git archive --format=tgz HEAD | docker build -t openrtist -
COPY . openrtist
WORKDIR openrtist/server

EXPOSE 5555 9099
ENTRYPOINT ["./entrypoint.sh"]
