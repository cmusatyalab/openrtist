# OpenRTiST: Real-Time Style Transfer
OpenRTiST utilizes Gabriel, a platform for wearable cognitive assistance applications, to transform the live video from a mobile client into the styles of various artworks. The frames are streamed to a server where the chosen style is applied and the transformed images are returned to the client.

Copyright &copy; 2017-2018
Carnegie Mellon University 

This is a developing project.

## License
All source code and documentation are under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
A copy of this license is reproduced in the [LICENSE](LICENSE) file.


## Prerequisites
__The OpenRTiST server application requires a GPU for image processing.__ We have tested OpenRTiST on __Ubuntu 16.04 LTS (Xenial)__ using several nVidia GPUs (GTX 960, GTX 1060, GTX 1080 Ti, Tesla K40). OpenRTiST supports Android and standalone Python clients.  We have tested the Android client on __Nexus 6__ and __Samsung Galaxy S7__.


##  Server Installation using Docker
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
docker pull a4anna/openrtist
```
For older verion of openrtist pull a4anna/openrtist:v1

### Step 7. Launch the container with nvidia-docker.
```sh
nvidia-docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" -v /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 a4anna/openrtist bash
```

### Step 8. Launch Gabriel control/user communication/proxy modules to start server.
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
cd /workspace/gabriel/server/bin
```
---
__If executing server for Python clients...__

Execute gabriel-control, specifying the interface inside the docker container with the -n flag
```
./gabriel-control -n eth0
```
---
__If executing server for Android clients...__

__NOTE: You must also add the -l flag to put gabriel into legacy mode!__
```
./gabriel-control -n eth0 -l
```
In the next tmux window(CTRL-b 1), execute gabriel-ucomm, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.
```
cd /workspace/gabriel/server/bin
./gabriel-ucomm -s 172.17.0.2:8021
```
In the next tmux window(CTRL-b 2), navigate to the OpenRTiST application directory.
---
__If executing server for Python clients...__

Execute the Python proxy, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.
```
cd /workspace/gabriel/server/openrtist/openrtist_python
./proxy.py -s 172.17.0.2:8021
```
---
__If executing server for Android clients...__

Execute the legacy Android proxy, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.
```
cd /workspace/gabriel/server/openrtist/openrtist_android
./proxy.py -s 172.17.0.2:8021
```


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
docker pull a4anna/openrtist
```

For older verion of openrtist pull a4anna/openrtist:v1

#### Step 3. Set the xhost display and launch the container.
```
xhost local:root
docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" -v /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 a4anna/openrtist bash
```
#### Step 4. Configure the client to talk to the server.
```
cd /workspace/gabriel/client/gabriel-client-style-python
vim.tiny config.py
#Edit the IP address to point to your server.
```

#### Step 5. Launch the python client.
```
./ui.py
```


### Android Client
<a href='https://play.google.com/store/apps/details?id=edu.cmu.cs.openrtist'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>
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


## Installation from source
### Recommended Source Directory Structure
```
+gabriel
  +client
    +gabriel-client-openrtist-android
    +gabriel-client-openrtist-python
  +server
    +openrtist
      -wtrMrk.png
      +models
      +openrtist_python
      +openrtist_android
``` 
### Methodology
#### 1. Setup Gabriel 
Assuming the user has installed pytorch and Gabriel. Follow the instructions given in [Gabriel repo](https://github.com/cmusatyalab/gabriel-private) to run the `control server` and `ucomm server`.
__Note: The android app uses legacy mode. Pass the `-l` parameter while running the Gabriel control.__

#### 2. Run the  app
```
$ cd <gabriel-repo>/server/oprtist/openrtist_<client_type>/
$ ./proxy.py -s x.x.x.x:8021
Discovery Control VM
INFO     execute : java -jar /home/ubuntu/Workspace/gabriel/server/gabriel/lib/gabriel_upnp_client.jar
INFO     Gabriel Server :
INFO     {u'acc_tcp_streaming_ip': u'x.x.x.x',
 u'acc_tcp_streaming_port': 10102,
 u'audio_tcp_streaming_ip': u'x.x.x.x',
 u'audio_tcp_streaming_port': 10103,
 u'ucomm_relay_ip': u'x.x.x.x',
 u'ucomm_relay_port': 9090,
 u'ucomm_server_ip': u'x.x.x.x',
 u'ucomm_server_port': 10120,
 u'video_tcp_streaming_ip': u'x.x.x.x',
 u'video_tcp_streaming_port': 10101}
TOKEN SIZE OF OFFLOADING ENGINE: 1
MODEL PATH <gabriel-repo>/server/oprtist/openrtist_<client_type>/models/
FINISHED INITIALISATION
```
#### 3.  Run a python or mobile client using source code at gabriel-client-style-(client_type). 
Make sure to change IP address of GABRIEL_IP variable at src/edu/cmu/cs/gabriel/Const.java for the android client and config.py for the python client


## Credits
Please see the [CREDITS](CREDITS.md) file for a list of acknowledgments.
