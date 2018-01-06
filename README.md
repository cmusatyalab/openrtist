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

```
curl -fsSL get.docker.com -o get-docker.sh
sh get-docker.sh
```

### Step 4. Install [nvidia-docker](https://github.com/NVIDIA/nvidia-docker)
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

### Step 5. Obtain OpenRTiST docker container.
```
docker pull a4anna/openstyletransfer
```

### Step 6. Launch the container with nvidia-docker.
```
nvidia-docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" -v /dev/video0:/dev/video0 -v /tmp/.X11-unix:/tmp/.X11-unix:ro -p 9098:9098 -p 9111:9111 -p 22222:22222 -p 8021:8021 a4anna/openstyletransfer bash
```

### Step 7. Launch Gabriel control/user communication/proxy modules to start server.
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
__If executing server for Python clients...__
Execute gabriel-control, specifying the interface inside the docker container with the -n flag
```
./gabriel-control -n eth0
```
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
In the next tmux window(CTRL-b 2), navigate to the OpenStyleTransfer application directory.
__If executing server for Python clients...__
Execute the Python proxy, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.
```
cd /workspace/gabriel/server/style_app/style_python_app
./proxy.py -s 172.17.0.2:8021
```
__If executing server for Android clients...__
Execute the legacy Android proxy, specifying the ip address listed earlier with the -s flag. Be sure to include the port 8021.
```
cd /workspace/gabriel/server/style_app/style_legacy_android_app
./proxy.py -s 172.17.0.2:8021
```


### Running backend in Amazon AWS
If you wish to compare between running the server on a cloudlet versus a cloud instance, you can launch the following instance type/image from your Amazon EC2 Dashboard:

__Instance Type__ - p2.xlarge (can be found by filtering under GPU compute instance types)
__Image__ - Deep Learning Base AMI (Ubuntu) - ami-041db87c

__Ensure that ports 9000-10000 are open in your security group rules so that traffic to/from the mobile client will pass through to the server.__

Once the server is running in AWS, you can follow the steps above to setup the server.


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
#### Download from Google Playstore here
#### Managing Servers
You can add the IP address of the host where the OpenRTiST server is running by hitting the menu button and going to 'manage servers'.  From this screen you can add an IP address for a cloud/cloudlet and name the server.  Once you had added the server, you can then select it from the drop-down menu before hitting the button to 'Run Demo'.
#### Switching Styles
Once the camera is active, the application will be set to 'Clear Display' by default. This will show the resultant frames of the camera without a particular style. You can use the drop-down menu at the top to select a style to apply. Once selected the frames from the camera will be sent to the server and the style transfer will be performed. The results will be shipped back to the client and displayed.
#### Enabling Stereoscopic Effect
In the main activity, there is a switch where you can toggle whether or not to enable the stereoscopic effect. When enabled, the resultant stylized frames will be split into left- and right-eye channels. This effect is interesting to use with various HUDs, such as Google Cardboard.

## Installation from source
### Recommended Source Directory Structure
```
+gabriel
  +client
  +server
    +openrtist
      -wtrMrk.png
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


## Acknowledgements
This project utilizes work from the following sources:

__List of Featured Artworks__
Udnie (Francis Picabia)
Rain Princess (Leonid Afremov)
Les Femmes d'Alger (Pablo Picasso)
A Sunday Afternoon on the Island of La Grande Jatte (Georges Seurat)
The Rise of the Carthaginian Empire (J.M.W. Turner)
The Scream (Edvard Munch)
The Starry Night (Vincent Van Gogh)
candy painting (from https://github.com/pytorch/examples/tree/master/fast_neural_style)
mosaic painting (from https://github.com/pytorch/examples/tree/master/fast_neural_style)

__Stanford University__ 
[Perceptual Losses for Real-Time Style Transfer and Super-Resolution](https://arxiv.org/pdf/1603.08155.pdf)
[Pytorch Fast Neural Style](https://github.com/pytorch/examples/tree/master/fast_neural_style)

__University of T&uuml;bingen__
[A Neural Algorithm of Artistic Style](https://arxiv.org/abs/1508.06576)

__Carnegie Mellon University__
[Towards Wearable Cognitive Assistance](http://dl.acm.org/citation.cfm?id=2594383)
[Gabriel](http://github.com/cmusatyalab/gabriel)
