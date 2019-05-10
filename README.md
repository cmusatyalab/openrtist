# OpenRTiST: Real-Time Style Transfer
OpenRTiST utilizes Gabriel, a platform for wearable cognitive assistance applications, to transform the live video from a mobile client into the styles of various artworks. The frames are streamed to a server where the chosen style is applied and the transformed images are returned to the client.

Copyright &copy; 2017-2019
Carnegie Mellon University 

This is a developing project.

## License
All source code and documentation are under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
A copy of this license is reproduced in the [LICENSE](LICENSE) file.


## Prerequisites
__The pre-built OpenRTiST server Docker image requires a GPU for image processing.__ 

OpenRTiST using PyTorch (including the pre-built image) has been tested on __Ubuntu 16.04 LTS (Xenial)__ using several nVidia GPUs (GTX 960, GTX 1060, GTX 1080 Ti, Tesla K40). 

Alternatively, OpenRTiST can also use the [Intel&reg; OpenVINO toolkit](https://software.intel.com/en-us/openvino-toolkit) for accelerated processing on CPU and processor graphics on many Intel processors.  We have tested OpenVINO support using __Ubuntu 18.04 LTS (Bionic)__ and OpenVINO release 2018.5 on an Intel&reg; Core&trade; i7-6770HQ processor.  OpenVINO is supported when installed from source only. 

The OpenRTiST server can run on CPU alone.  See below on installing from source for details.

OpenRTiST supports Android and standalone Python clients.  We have tested the Android client on __Nexus 6__, __Samsung Galaxy S7__, and __Essential PH-1__.


##  Server Installation using Docker (with PyTorch, requires GPU)
### Step 1. Become root
```sh
sudo -i
```

### Step 2. Prepare the environment
```sh
apt-get update
apt-get upgrade
```

### Step 3. Install [Docker](https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/)
Follow the steps in the above Docker documentation or use the following convenience script:

```sh
curl -fsSL get.docker.com -o get-docker.sh
sh get-docker.sh
```

### Step 4. Ensure nVidia drivers are installed
```sh
apt-get install nvidia-384
```

### Step 5. Install [nvidia-docker](https://github.com/NVIDIA/nvidia-docker)
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

### Step 6. Obtain OpenRTiST docker container.
```sh
docker pull cmusatyalab/openrtist
```

### Step 7A. Launch the Docker container with nvidia-docker
```sh
nvidia-docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" -v /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro -p 7070:7070 -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 cmusatyalab/openrtist 
```

### Step 7B. Launch the container and manually start the server (if you wish configure things)
```sh
nvidia-docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" -v /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro -p 7070:7070 -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 cmusatyalab/openrtist /bin/bash
```
Type ifconfig and note the interface name and ip address inside the docker container __(for the below examples, we assume eth0 and 172.17.0.2)__.
```
ifconfig
```
Open tmux and create three windows (CTRL-b c).
```
tmux
```
In the first tmux window (CTRL-b 0), navigate to /gabriel/server/bin.
```
cd /gabriel/server/bin
```
---

Execute gabriel-control, specifying the interface inside the docker container with the -n flag
```
./gabriel-control -n eth0
```
---
In the next tmux window(CTRL-b 1), execute gabriel-ucomm, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.
```
cd /gabriel/server/bin
./gabriel-ucomm -s 172.17.0.2:8021
```

In the next tmux window(CTRL-b 2), navigate to the OpenRTiST application directory.
Execute the proxy, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.
```
cd /openrtist/server
./proxy.py -s 172.17.0.2:8021
```
---

### Running backend in Amazon AWS
If you wish to compare between running the server on a cloudlet versus a cloud instance, you can launch the following instance type/image from your Amazon EC2 Dashboard:

__Instance Type__ - p2.xlarge (can be found by filtering under GPU compute instance types)

__Image__ - Deep Learning Base AMI (Ubuntu) - ami-041db87c

__Ensure that ports 9000-10000 are open in your security group rules so that traffic to/from the mobile client will pass through to the server.__

Once the server is running in AWS, you can follow the steps above to setup the server.

__Note__ : If using vanilla Ubuntu Server 16.04 Image, install the required Nvidia driver and reboot. 
```
wget http://us.download.nvidia.com/tesla/375.51/nvidia-driver-local-repo-ubuntu1604_375.51-1_amd64.deb
sudo dpkg -i nvidia-driver-local-repo-ubuntu1604_375.51-1_amd64.deb
sudo apt-get update
sudo apt-get -y install cuda-drivers
sudo reboot
```

## Client Installation
### Python Client
The docker image for the server component also includes the Python client which can be run on a non-Android machine. __The python client requires that a USB webcam is connected to the machine.__

#### Step 1. Install [Docker](https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/)
Follow the steps in the above Docker documentation or use the following convenience script:

```
curl -fsSL get.docker.com -o get-docker.sh
sh get-docker.sh
```

#### Step 2. Obtain OpenRTiST docker container.
```
docker pull cmusatyalab/openrtist
```

#### Step 3. Set the xhost display and launch the container.
```
xhost local:root
docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" -v /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 cmusatyalab/openrtist /bin/bash
```
#### Step 4. Edit the client configuration (optional).
```
cd /openrtist/gabriel-client-openrtist-python
vim.tiny config.py
#Here you can edit the image resolution captured by the camera and the frames per second.
```

#### Step 5. Launch the Python client UI specifying the server's IP address.
```
./ui.py <server ip address>
```


### Android Client
<a href='https://play.google.com/store/apps/details?id=edu.cmu.cs.openrtist'><img height='125px' width='323px' alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>
Google Play and the Google Play logo are trademarks of Google LLC.
#### Managing Servers
Servers can be added by entering a server name and address and pressing the + sign button. Once a server has been added, pressing the 'Play' button will connect to the OpenRTiST server at that address. Pressing the trash can button will remove the server from the server list.
#### Switching Styles
Once the camera is active, the application will be set to 'Clear Display' by default. This will show the  frames of the camera without a particular style. You can use the drop-down menu at the top to select a style to apply. Once selected, the frames from the camera will be sent to the server and the style will be applied. The results will be shipped back to the client and displayed. If the 'Iterate Styles Periodically' option is enabled, the dropdown menu will not be shown, and styles will change automatically based upon the interval defined.
#### Enabling Stereoscopic Effect
In the main activity, there is a switch where you can toggle whether or not to enable the stereoscopic effect. When enabled, the resultant stylized frames will be split into left- and right-eye channels. This effect is interesting to use with various HUDs, such as Google Cardboard.
#### Iterate Styles Periodically
When this option is enabled, an interval can be set (5-60 seconds). The styles will automatically be iterated through at the occurrence of this interval.
#### Recording Videos
If the 'Show Video Recording Button' switch is enabled, a camera icon will be shown in the lower left corner of the display. Pressing this button will initiate a screen capture. You will be prompted to allow the capture and after closing this dialog, recording will begin. The screen contents will be recorded until the button is pressed a second time. The icon will turn red to indicate that recording is taking place. Video clips will be stored in Movies/OpenRTiST on the devices SD card. NOTE: If the 'Iterate Styles Periodically' option is enabled, styles will not iterate until the screen capture dialog has been accepted and recording has started.
#### Front-facing Camera
You can toggle whether or not to use the front-facing camera on the main screen. When this option is enabled, a small rotation icon will be present in the upper right hand corner of this display. This icon can be pressed to rotate the view on devices where the styled frames appear upside down.


## Installation from source (PyTorch or OpenVINO, GPU or CPU)
### 1. Install PyTorch or OpenVINO
The OpenRTiST server can use PyTorch or OpenVINO to execute style transfer networks.  Please install PyTorch or OpenVINO (or both).

#### Option A. Install torchvision and pytorch
OpenRTiST has been tested with pytorch versions 0.2.0 and 0.3.1 with CUDA support.  As the APIs and layer names have been changed in newer releases, please select an appropriate version from [here](https://pytorch.org/get-started/previous-versions/). __NOTE: Even if you do not have a CUDA-capable GPU, you can install pytorch with CUDA support and run OpenRTiST on CPU only.__

First install torchvision:
```
pip install torchvision
```
This will usually install a new version of pytorch.  Uninstall and replace this with the older pytorch version (e.g.):
```
pip uninstall torch
pip install https://download.pytorch.org/whl/cu75/torch-0.2.0.post3-cp27-cp27m-manylinux1_x86_64.whl
```

#### Option B. Install OpenVINO
Download the latest OpenVINO release from https://software.intel.com/en-us/openvino-toolkit.  Full installation instructions are available at https://software.intel.com/en-us/articles/OpenVINO-Install-Linux. 

Be sure to install the Intel&reg; Graphics Compute Runtime for OpenCL&trade; Driver components (under Optional Steps) to enable the use of the integrated GPU.  

We recommend Ubuntu 18.04 for a painless install.  We have had success with Ubuntu 18.10 and Ubuntu 16.04, but `setupvars.sh` may not set up the environment correctly, and on the older distro, a new Linux kernel will be needed.  

Note: although OpenVINO lists Python 3.5 as a prerequisite, it supports Python 2.7 as well.  

Setup environment variables and paths to use OpenVINO with Python 2.7:
```
$ source /opt/intel/computer_vision_sdk/bin/setupvars.sh -pyver 2.7
```

### 2. Setup Gabriel 
Follow the instructions given in [Gabriel repo](https://github.com/cmusatyalab/gabriel) to install and run the `control server` and `ucomm server`.  E.g., after installing gabriel, run:
```
$ cd $HOME/gabriel/server/bin
$ ./gabriel-control -n eth0 &
$ ./gabriel-ucomm -n eth0 -s x.x.x.x &
```
replacing `eth0` with your network nic device (e.g., may be `eth1` or `eno1`), and `x.x.x.x` with the IP address of the machine.
Add the path to gabriel/server to your `PYTHONPATH` environment variable, e.g.:
```
$ export PYTHONPATH=$HOME/gabriel/server/:$PYTHONPATH
```
Note: Gabriel may require Python 2.7.  


### 3. Run the server
Start the server like this:
```
$ cd <openrtist-repo>/server/
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
Note:  With OpenVINO using an integrated GPU, it may take up to a minute to preload all of the style models.  This is not the case for OpenVINO on CPU, or with PyTorch.  Once initialized, the server is ready for clients at this point.

With either PyTorch or OpenVINO, you can run the server in CPU-only mode by first editing config.py and setting `USE_GPU = False`.  By default, OpenRTiST tries to detect and use OpenVINO, and fails over to PyTorch.  To force it to use one system, add a line to config.py, setting `USE_OPENVINO = <True / False>`.  Remove this line to re-enable the auto-detection behavior.


### 4.  Run a python or mobile client using source code at gabriel-client-style-(client_type), or the Android client from the Google Play Store. 
To run the python client:
```
cd <openrtist-repo>/gabriel-client-openrtist-python
./ui.py <server ip address>
```
You can edit the config.py file to change webcam capture parameters.

The prebuilt Android client from the Google Play Store provides an interface to add a server with a custom IP address.


## Credits
Please see the [CREDITS](CREDITS.md) file for a list of acknowledgments.
