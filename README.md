# OpenRTiST: Real-Time Style Transfer

OpenRTiST utilizes Gabriel, a platform for wearable cognitive assistance applications, to transform the live video from a mobile client into the styles of various artworks. The frames are streamed to a server where the chosen style is applied and the transformed images are returned to the client.

Copyright &copy; 2017-2019
Carnegie Mellon University

This is a developing project.

## License

Unless otherwise stated, all source code and documentation are under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
A copy of this license is reproduced in the [LICENSE](LICENSE) file.

Portions from the following third party sources have
been modified and are included in this repository.
These portions are noted in the source files and are
copyright their respective authors with
the licenses listed.

Project | Modified | License
---|---|---|
[pytorch/examples](https://github.com/pytorch/examples) | Yes | BSD

## Prerequisites

OpenRTiST using PyTorch (including the pre-built image) has been tested on __Ubuntu 16.04 LTS (Xenial)__ using several nVidia GPUs (GTX 960, GTX 1060, GTX 1080 Ti, Tesla K40).

Alternatively, OpenRTiST can also use the [Intel&reg; OpenVINO toolkit](https://software.intel.com/en-us/openvino-toolkit) for accelerated processing on CPU and processor graphics on many Intel processors.  We have tested OpenVINO support using __Ubuntu 18.04 LTS (Bionic)__ and OpenVINO releases 2018.5 and 2019.3 on an Intel&reg; Core&trade; i7-6770HQ processor.  

The OpenRTiST server can run on CPU alone.  See below on installing from source for details.

OpenRTiST supports Android and standalone Python clients.  We have tested the Android client on __Nexus 6__, __Samsung Galaxy S7__, and __Essential PH-1__.

## Server Installation using Docker

The quickest way to set up an OpenRTiST server is to download and run our pre-built Docker container.  This build supports execution on NVIDIA GPUs, Intel integrated GPUs, and execution on the CPU.  All of the following steps must be executed as root.  

### Step 1. Install Docker

If you do not already have Docker installed, install as follows:

```bash
apt-get update
apt-get upgrade
apt-get install docker.io
```

Alternatively, you can follow the steps in [this Docker install guide](https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/) or use the following convenience script:

```sh
curl -fsSL get.docker.com -o get-docker.sh
sh get-docker.sh
```

### Step 2. Ensure nVidia drivers are installed (Optional -- only for NVIDIA GPU support)

```sh
apt-get install nvidia-384
```

### Step 3. Install [nvidia-docker](https://github.com/NVIDIA/nvidia-docker) (Optional -- only for NVIDIA GPU support)

```sh
# If you have nvidia-docker 1.0 installed: we need to remove it and all existing GPU containers
docker volume ls -q -f driver=nvidia-docker | xargs -r -I{} -n1 docker ps -q -a -f volume={} | xargs -r docker rm -f
apt-get purge -y nvidia-docker

# Add the package repositories
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/ubuntu16.04/amd64/nvidia-docker.list | tee /etc/apt/sources.list.d/nvidia-docker.list
apt-get update

# Install nvidia-docker2 and reload the Docker daemon configuration
apt-get install -y nvidia-docker2
pkill -SIGHUP dockerd

```

### Step 4. Obtain OpenRTiST Docker image

```sh
docker pull cmusatyalab/openrtist
```

### Step 5A. Launch the Docker container

For CPU and Intel iGPU support only, run the container with `docker`:

```sh
docker run --rm -it --device /dev/dri:/dev/dri -p 7070:7070 -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 cmusatyalab/openrtist
```

To also support NVIDIA GPUs, run the container with `nvidia-docker`:

```sh
nvidia-docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" -v /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro -p 7070:7070 -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 cmusatyalab/openrtist
```

Note:  With OpenVINO using an integrated GPU, it may take up to a minute to preload all of the style models.

### Step 5B. Launch the container and manually start the server (if you wish configure things)

If you don't need NVIDIA GPU support:

```sh
docker run --rm -it --device /dev/dri:/dev/dri -p 7070:7070 -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 cmusatyalab/openrtist /bin/bash
```

If you need NVIDIA GPU support:

```sh
nvidia-docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" -v /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro -p 7070:7070 -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 cmusatyalab/openrtist /bin/bash
```

> NOTE: To use PyTorch 1.0 and above use container : a4anna/openrtist_cu100_torch1p1

Type ifconfig and note the interface name and ip address inside the docker container __(for the below examples, we assume eth0 and 172.17.0.2)__.

```bash
ifconfig
```

Open tmux and create three windows (CTRL-b c).

```bash
tmux
```

In the first tmux window (CTRL-b 0), navigate to /gabriel/server/bin.

```bash
cd /gabriel/server/bin
```

---

Execute gabriel-control, specifying the interface inside the docker container with the -n flag

```bash
./gabriel-control -n eth0
```

---
In the next tmux window(CTRL-b 1), execute gabriel-ucomm, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.

```bash
cd /gabriel/server/bin
./gabriel-ucomm -s 172.17.0.2:8021
```

In the next tmux window(CTRL-b 2), navigate to the OpenRTiST application directory.
Execute the proxy, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.

```bash
cd /openrtist/server
./proxy.py -s 172.17.0.2:8021
```

__Note:  With OpenVINO using an integrated GPU, it may take up to a minute to preload all of the style models.__

---

## Running server on Amazon AWS

If you wish to compare between running the server on a cloudlet versus a cloud instance, you can launch the following instance type/image from your Amazon EC2 Dashboard:

__Instance Type__ - p2.xlarge (can be found by filtering under GPU compute instance types)

__Image__ - Deep Learning Base AMI (Ubuntu) - ami-041db87c

__Ensure that ports 9000-10000 are open in your security group rules so that traffic to/from the mobile client will pass through to the server.__

Once the server is running in AWS, you can follow the steps above to setup the server.

__Note__ : If using vanilla Ubuntu Server 16.04 Image, install the required Nvidia driver and reboot.

```bash
wget http://us.download.nvidia.com/tesla/375.51/nvidia-driver-local-repo-ubuntu1604_375.51-1_amd64.deb
sudo dpkg -i nvidia-driver-local-repo-ubuntu1604_375.51-1_amd64.deb
sudo apt-get update
sudo apt-get -y install cuda-drivers
sudo reboot
```

## Client Installation

### Python Client in Docker

The docker image for the server component also includes the Python client which can be run on a non-Android machine. __The python client requires that a USB webcam is connected to the machine.__

#### Step 1. Install Docker

If you do not already have Docker installed, install as follows:

``` bash
apt-get update
apt-get upgrade
apt-get install docker.io
```

Alternatively, you can follow the steps in [this Docker install guide](https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/) or use the following convenience script:

```sh
curl -fsSL get.docker.com -o get-docker.sh
sh get-docker.sh
```

#### Step 2. Obtain OpenRTiST Docker image

```bash
docker pull cmusatyalab/openrtist
```

#### Step 3. Set the xhost display and launch the container

```bash
xhost local:root
docker run --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" --device /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro cmusatyalab/openrtist /bin/bash
```

#### Step 4. Edit the client configuration (optional)

```bash
cd /openrtist/gabriel-client-openrtist-python
vim.tiny config.py
#Here you can edit the image resolution captured by the camera and the frames per second.
```

#### Step 5. Launch the Python client UI specifying the server's IP address

```bash
./ui.py <server ip address>
```

### Android Client

<a href='https://play.google.com/store/apps/details?id=edu.cmu.cs.openrtist'><img height='125px' width='323px' alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>
Google Play and the Google Play logo are trademarks of Google LLC.

#### Managing Servers

Servers can be added by entering a server name and address and pressing the + sign button. Once a server has been added, pressing the 'Play' button will connect to the OpenRTiST server at that address. Pressing the trash can button will remove the server from the server list.

#### Switching Styles

Once the camera is active, the application will be set to 'Clear Display' by default. This will show the  frames of the camera without a particular style. You can use the drop-down menu at the top to select a style to apply. Once selected, the frames from the camera will be sent to the server and the style will be applied. The results will be shipped back to the client and displayed. If the 'Iterate Styles Periodically' option is enabled, the dropdown menu will not be shown, and styles will change automatically based upon the interval defined.

#### Settings

##### General

* Show Screenshot/Recording Buttons - This will enable icons to allow you to capture video or screenshots while running OpenRTiST. __NOTE: If the 'Iterate Styles Periodically' option is enabled, styles will not iterate until the screen capture dialog has been accepted and recording has started.__
* Display Metrics - Enabling this option will show the FPS and RTT as measured from the device.
* Stereoscopic Effect - This option can be toggled for use in HUDs (heads-up displays) where there are both left and right channels.
* Show Reference Style Image - If enabled, the original artwork will be inlaid on the display.
* Iterate Styles Periodically - This can be enabled to automatically iterate through the list of styles after the set delay.

##### Experimental

* Resolution - Configure the resoultion to capture at. This will have a moderate impact in the computation time on the server.
* Gabriel Token Size - Allows configuration of the token-based flow control mechanism in the Gabriel platform. This indicates how many frames can be in flight before a response frame is received back from the serve.r

#### Front-facing Camera

Once connected to a server, an icon is displayed in the upper right hand corner which allows one to toggle between front- and rear-facing cameras.

## Installation from source (PyTorch or OpenVINO, GPU or CPU)

### 1. Install PyTorch or OpenVINO

The OpenRTiST server can use PyTorch or OpenVINO to execute style transfer networks.  Please install PyTorch or OpenVINO (or both).

#### Option A. Install torchvision and pytorch

OpenRTiST has been tested with pytorch versions 0.2.0, 0.3.1, 1.0, and 1.2 with CUDA support.  As the APIs and layer names have been changed in newer releases, model files for both pre-1.0 and post-1.0 versions have been provided. __NOTE: Even if you do not have a CUDA-capable GPU, you can install pytorch with CUDA support and run OpenRTiST on CPU only.__

OpenRTiST should work with Python 2.7 as well as Python 3.x.  We recommend using virtualenv/venv to better control the Python environment and keep your distribution defaults clean.  

To install PyTorch, simply install torchvision:

```bash
pip install torchvision
```

This will usually install the latest version of PyTorch.  If you need a different / older version (e.g., to support a particular CUDA version), you can find alternative versions [here](https://pytorch.org/get-started/previous-versions/). Uninstall and replace the auto-installed one with the desired PyTorch version (e.g.):

```bash
pip uninstall torch
pip install https://download.pytorch.org/whl/cu75/torch-0.2.0.post3-cp27-cp27m-manylinux1_x86_64.whl
```

#### Option B. Install OpenVINO

We recommend Ubuntu 18.04 for a painless install.  We have had success with Ubuntu 18.10 and Ubuntu 16.04, but `setupvars.sh` may not set up the environment correctly, and on the older distro, a new Linux kernel will be needed to use the integrated GPU.  

##### Install the latest OpenVINO

You can find and download the latest OpenVINO release from <https://software.intel.com/en-us/openvino-toolkit>.  Full installation instructions are available at <https://software.intel.com/en-us/articles/OpenVINO-Install-Linux>.

For Ubuntu 16.04 / 18.04, you can install using `apt` by adding the custom repository as follows:

```bash
wget https://apt.repos.intel.com/intel-gpg-keys/GPG-PUB-KEY-INTEL-SW-PRODUCTS-2019.PUB
sudo apt-key add GPG-PUB-KEY-INTEL-SW-PRODUCTS-2019.PUB
echo "deb https://apt.repos.intel.com/openvino/2019/ all main" | sudo tee /etc/apt/sources.list.d/intel-openvino-2019.list
sudo apt-get update
```

and then search for available packages using:

```bash
apt-cache search openvino-dev
```

Finally, install a version matching your distro (ubuntu16 or ubuntu18, e.g.):

```bash
sudo apt-get install intel-openvino-dev-ubuntu18-2019.3.344
```

##### Setup OpenCL to use Processor Graphics (Optional)

To utilize integrated Processor Graphics on Intel processors, the following are required:

* An Intel processor with Gen 8 or later graphics (Broadwell and later)
* A recent kernel release (4.14 or later).  This should already be the case for Ubuntu 18.04, but a new kernel will need to be installed for 16.04.
* Generic OpenCL runtime support.  Can be installed by:

```bash
sudo apt install ocl-icd-libopencl1
```

* The Intel&reg; Graphics Compute Runtime for OpenCL&trade; Driver components. Installation instructions for various systems are found [here](https://github.com/intel/compute-runtime/blob/master/documentation/Neo_in_distributions.md).  For Ubuntu 16.04 and 18.04:

```bash
sudo add-apt-repository ppa:intel-opencl/intel-opencl
sudo apt-get update
sudo apt-get install intel-opencl-icd
```

##### Setup Environment

Setup environment variables and paths to use OpenVINO in the current shell:

```sh
$source /opt/intel/openvino/bin/setupvars.sh
```

Note: although OpenVINO lists Python 3.5 as a prerequisite, it supports Python 2.7 as well.  To setup environment variables and paths for Python 2.7:

```sh
$source /opt/intel/openvino/bin/setupvars.sh -pyver 2.7
```

### 2. Setup Gabriel

Follow the instructions given in [Gabriel repo](https://github.com/cmusatyalab/gabriel) to download and install the Gabriel framework.  Note: although the full Gabriel framework needs Python 2, the server components used for OpenRTiST should work with Python 3.  

Run the `control server` and `ucomm server`, e.g.:

```bash
$cd $HOME/gabriel/server/bin
$./gabriel-control -n eth0 &
$./gabriel-ucomm -n eth0 -s x.x.x.x &
```

replacing `eth0` with your network nic device (e.g., may be `eth1` or `eno1`), and `x.x.x.x` with the IP address of the machine.
Add the path to gabriel/server to your `PYTHONPATH` environment variable, e.g.:

```bash
$export PYTHONPATH=$HOME/gabriel/server/:$PYTHONPATH
```

### 3. Run the server

Start the server like this:

```bash
$cd <openrtist-repo>/server/
$ ./proxy.py -s x.x.x.x:8021
Autodetect:  Loaded OpenVINO
TOKEN SIZE OF OFFLOADING ENGINE: 1
Loading network files:
        <openrtist-repo>/server/../models/16v2.xml
        <openrtist-repo>/server/../models/cafe_gogh-16.bin
Loading model to the plugin
Loading network files:
        <openrtist-repo>/server/../models/16v2.xml
                     .
                     .
                     .
        <openrtist-repo>/server/../models/udnie-16.bin
Loading model to the plugin
Loading network files:
        <openrtist-repo>/server/../models/16v2.xml
        <openrtist-repo>/server/../models/weeping_woman-16.bin
Loading model to the plugin
FINISHED INITIALISATION
```

__Note:  With OpenVINO using an integrated GPU, it may take up to a minute to preload all of the style models.  This is not the case for OpenVINO on CPU, or with PyTorch.  Once initialized, the server is ready for clients at this point.__

With either PyTorch or OpenVINO, you can run the server in CPU-only mode by first editing config.py and setting `USE_GPU = False`.  By default, OpenRTiST tries to detect and use OpenVINO, and fails over to PyTorch.  To force it to use one system, add a line to config.py, setting `USE_OPENVINO = <True / False>`.  Remove this line to re-enable the auto-detection behavior.

### 4.  Run a python or mobile client using source code at gabriel-client-style-(client_type), or the Android client from the Google Play Store

To run the python client:

```bash
cd <openrtist-repo>/gabriel-client-openrtist-python
./ui.py <server ip address>
```

You can edit the config.py file to change webcam capture parameters.

The prebuilt Android client from the Google Play Store provides an interface to add a server with a custom IP address.

## Training New Styles (Pytorch 1.3.0)

We use COCO 2014 Train Images as our default training dataset.

### Option A. Launch on EC2

An Amazon Machine Image with the model training frontend and training dataset configured out of the box is publicly available to deploy on Amazon EC2.

* AMI ID: `ami-0d9512927124b3c3a`
* Instance type (recommended): `g4dn.xlarge` (or alternatively any instance with an Nvidia GPU)
* Public IP enabled
* Security group
* Inbound: TCP 22, TCP 5000

After startup, connect to port 5000 of your machine to access the model training frontend.

### Option B. Run model training web frontend

Prequisites: A machine with an installed Nvidia driver and an accessible redis instance

To start the model training web frontend, first install the requisite python dependencies:

```bash
cd <openrtist-repo>/model-app
pip install -r requirements.txt
```

Then download the training data and point model-app/config.py accordingly:

```bash
wget http://images.cocodataset.org/zips/train2014.zip
unzip train2014.zip -d coco-data/
```

Install OpenVINO or set CONVERT_TO_OPEN_VINO=false in model-app/config.py

And then launch the web frontend:

```bash
cd <openrtist-repo>/model-app
flask run --host=0.0.0.0
```

And finally launch a celery worker in a separate terminal:

```bash
cd <openrtist-repo>/model-app
celery worker -A model-app.app.celery --loglevel=info
```

### Option C. Directly run training script

```bash
wget http://images.cocodataset.org/zips/train2014.zip
unzip train2014.zip -d coco-data/
cd <openrtist-repo>
python model-app/train_style.py --dataset <coco-data> --style-image <style-image> --save-model-dir models/ --epochs 2
```

To disable flicker-loss which removes flicker for temporal consistency in real-time image stream, set --noise-count 0

An additional script can convert from the generated PyTorch model to OpenVINO:

```bash
python model-app/openvino_convert.py <path_to_pytorch_model>
```

## Credits

Please see the [CREDITS](CREDITS.md) file for a list of acknowledgments.
