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

OpenRTiST using PyTorch (including the pre-built image) has been tested on __Ubuntu 18.04 LTS (Bionic)__ using several nVidia GPUs (GTX 960, GTX 1060, GTX 1080 Ti, Tesla K40).

Alternatively, OpenRTiST can also use the [Intel&reg; OpenVINO toolkit](https://software.intel.com/en-us/openvino-toolkit) for accelerated processing on CPU and processor graphics on many Intel processors.  We have tested OpenVINO support using __Ubuntu 18.04 LTS (Bionic)__ and OpenVINO releases 2018.5 and 2019.3 on an Intel&reg; Core&trade; i7-6770HQ processor.  

The OpenRTiST server can run on CPU alone.  See below on installing from source for details.

OpenRTiST supports Android and standalone Python clients.  We have tested the Android client on __Nexus 6__, __Samsung Galaxy S7__, and __Essential PH-1__.

## Server Installation using Docker

The quickest way to set up an OpenRTiST server is to download and run our pre-built Docker container.  This build supports execution on NVIDIA GPUs, Intel integrated GPUs, and execution on the CPU. All of the following steps must be executed as root. We tested these steps using Docker 19.03.

### Step 1. Install Docker

If you do not already have Docker installed, install it using the steps in [this Docker install guide](https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/) or use the following convenience script:

```sh
curl -fsSL get.docker.com -o get-docker.sh
sh get-docker.sh
```

### Step 2. Ensure an NVIDIA driver are installed (Optional -- only for NVIDIA GPU support)

[These notes](https://github.com/NVIDIA/nvidia-docker/wiki/Frequently-Asked-Questions#how-do-i-install-the-nvidia-driver) explain how to install the driver.

If you think you may already have an NVIDIA driver installed, run `nvidia-smi`. The Driver version will be listed at the top of the table that gets printed.

### Step 3. Install the [NVIDIA Container Toolkit](https://github.com/NVIDIA/nvidia-docker) (Optional -- only for NVIDIA GPU support)

Follow [these instructions](https://github.com/NVIDIA/nvidia-docker#ubuntu-16041804-debian-jessiestretchbuster).

### Step 4. Obtain OpenRTiST Docker image

```sh
docker pull cmusatyalab/openrtist
```

### Step 5A. Launch the Docker container

For NVIDIA GPU support, run:

```sh
docker run --gpus all --rm -it -p 9099:9099 cmusatyalab/openrtist
```

For Intel iGPU support, run:

```sh
docker run --device /dev/dri:/dev/dri --rm -it -p 9099:9099 cmusatyalab/openrtist
```

For CPU support only, run:

```sh
docker run --rm -it -p 9099:9099 cmusatyalab/openrtist
```

Note:  With OpenVINO using an integrated GPU, it may take up to a minute to preload all of the style models.

---

## Running server on Amazon AWS

If you wish to compare between running the server on a cloudlet versus a cloud instance, you can launch the following instance type/image from your Amazon EC2 Dashboard:

__Instance Type__ - p2.xlarge (can be found by filtering under GPU compute instance types)

__Image__ - Deep Learning Base AMI (Ubuntu) - ami-041db87c

__Ensure that port 9099 is open in your security group rules so that traffic to/from the mobile client will pass through to the server.__

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

#### Step 3. Launch the container with the appropriate arguments

```bash
xhost +local:docker
docker run --entrypoint /bin/bash --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" --device /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro --rm -it  cmusatyalab/openrtist
```

#### Step 4. Launch the Python client UI specifying the server's IP address

```bash
cd /openrtist/python-client
./ui.py <server ip address>
```

### Android Client

You can download the client from the [Google Play Store](https://play.google.com/store/apps/details?id=edu.cmu.cs.openrtist).

Alternatively, you can build the client yourself using [Android Studio](https://developer.android.com/studio). The source code for the client is located in the `android-client` directory. You should use the standardDebug [build variant](https://developer.android.com/studio/run#changing-variant).

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
* Gabriel Token Limit - Allows configuration of the token-based flow control mechanism in the Gabriel platform. This indicates how many frames can be in flight before a response frame is received back from the server. The minimum of the token limit specified in the app and the number of tokens specified on the server will be used.

#### Front-facing Camera

Once connected to a server, an icon is displayed in the upper right hand corner which allows one to toggle between front- and rear-facing cameras.

## Installation from source (PyTorch or OpenVINO, GPU or CPU)

### 1. Install CUDA or OpenVINO

The OpenRTiST server can use PyTorch or OpenVINO to execute style transfer networks. You may want to install CUDA or OpenVINO (or both). However, you can run OpenRTiST using pytorch on the CPU without CUDA or OpenVINO. 

OpenRTiST has been tested with pytorch versions 0.2.0, 0.3.1, 1.0, and 1.2 with CUDA support.  As the APIs and layer names have been changed in newer releases, model files for both pre-1.0 and post-1.0 versions have been provided. __NOTE: Even if you do not have a CUDA-capable GPU, you can install pytorch with CUDA support and run OpenRTiST on CPU only.__

#### Option A. Install CUDA

Follow [NVIDIA's instructions](https://docs.nvidia.com/cuda/cuda-installation-guide-linux/index.html#package-manager-installation).

#### Option B. Install OpenVINO

We recommend Ubuntu 18.04 for a painless install.  We have had success with Ubuntu 18.10 and Ubuntu 16.04, but `setupvars.sh` may not set up the environment correctly, and on the older distro, a new Linux kernel will be needed to use the integrated GPU.  

##### Install the latest OpenVINO

You can find and download the latest OpenVINO release from <https://software.intel.com/en-us/openvino-toolkit>. Full installation instructions are available at <https://software.intel.com/en-us/articles/OpenVINO-Install-Linux>.

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
source /opt/intel/openvino/bin/setupvars.sh
```

### 2. Install Openrtist Dependencies

OpenRTiST requires Python 3.5 or later. We recommend using a [virtual environment](https://packaging.python.org/guides/installing-using-pip-and-virtual-environments/) to better control the Python environment and keep your distribution defaults clean.  

To install dependencies for Openrtist, navigate to the server directory, activate a Python 3 virtual environment, then run:

```bash
pip install -r requirements.txt
```

### 3. Run the server

With your virtual environment activated, start the server like this:

```bash
cd <openrtist-repo>/server/
python main.py
```

__Note:  With OpenVINO using an integrated GPU, it may take up to a minute to preload all of the style models.  This is not the case for OpenVINO on CPU, or with PyTorch.  Once initialized, the server is ready for clients at this point.__

With either PyTorch or OpenVINO, you can run the server in CPU-only mode by passing the --cpu CLI flag. By default, OpenRTiST tries to detect and use OpenVINO, and fails over to PyTorch.  To force it to use one system, pass the --openvino or --torch CLI flags.

### 4.  Run a python or mobile client using source code at python-client or the Android client from the Google Play Store

To run the python client:

```bash
cd <openrtist-repo>/python-client
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

Install OpenVINO or set `CONVERT_TO_OPEN_VINO=false` in model-app/config.py

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
