# OpenRTiST: Real-Time Style Transfer -- OpenVINO port
OpenRTiST utilizes Gabriel, a platform for wearable cognitive assistance applications, to transform the live video from a mobile client into the styles of various artworks. The frames are streamed to a server where the chosen style is applied and the transformed images are returned to the client.

Copyright &copy; 2017-2019
Carnegie Mellon University 

This is a developing project.

## License
All source code and documentation are under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
A copy of this license is reproduced in the [LICENSE](LICENSE) file.


## Prerequisites
This version of the OpenRTiST server uses the [Intel&reg; OpenVINO toolkit](https://software.intel.com/en-us/openvino-toolkit) for accelerated processing on CPU and processor graphics on many Intel processors.  We have tested OpenRTiST using __Ubuntu 18.04 LTS (Bionic)__ and OpenVINO release 2018.5 on an Intel&reg; Core&trade; i7-6770HQ processor.  This version is distributed as source only. 

To install the pyTorch version of OpenRTiST (for CPU and CUDA), please checkout / switch to the [master](https://github.com/cmusatyalab/openrtist/) branch of this repository and follow the installation directions there.

OpenRTiST supports Android and standalone Python clients.  We have tested the Android client on __Nexus 6__ and __Samsung Galaxy S7__.



## Android Client
<a href='https://play.google.com/store/apps/details?id=edu.cmu.cs.openrtist'><img height='125px' width='323px' alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>
Google Play and the Google Play logo are trademarks of Google LLC.
### Managing Servers
Servers can be added by entering a server name and address and pressing the + sign button. Once a server has been added, pressing the 'Play' button will connect to the OpenRTiST server at that address. Pressing the trash can button will remove the server from the server list.
### Switching Styles
Once the camera is active, the application will be set to 'Clear Display' by default. This will show the  frames of the camera without a particular style. You can use the drop-down menu at the top to select a style to apply. Once selected, the frames from the camera will be sent to the server and the style will be applied. The results will be shipped back to the client and displayed. If the 'Iterate Styles Periodically' option is enabled, the dropdown menu will not be shown, and styles will change automatically based upon the interval defined.
### Enabling Stereoscopic Effect
In the main activity, there is a switch where you can toggle whether or not to enable the stereoscopic effect. When enabled, the resultant stylized frames will be split into left- and right-eye channels. This effect is interesting to use with various HUDs, such as Google Cardboard.
### Iterate Styles Periodically
When this option is enabled, an interval can be set (5-60 seconds). The styles will automatically be iterated through at the occurrence of this interval.
### Recording Videos
If the 'Show Video Recording Button' switch is enabled, a camera icon will be shown in the lower left corner of the display. Pressing this button will initiate a screen capture. You will be prompted to allow the capture and after closing this dialog, recording will begin. The screen contents will be recorded until the button is pressed a second time. The icon will turn red to indicate that recording is taking place. Video clips will be stored in Movies/OpenRTiST on the devices SD card. NOTE: If the 'Iterate Styles Periodically' option is enabled, styles will not iterate until the screen capture dialog has been accepted and recording has started.
### Front-facing Camera
You can toggle whether or not to use the front-facing camera on the main screen. When this option is enabled, a small rotation icon will be present in the upper right hand corner of this display. This icon can be pressed to rotate the view on devices where the styled frames appear upside down.


## Installation from source
### 1. Install OpenVINO
Download the latest OpenVINO release from https://software.intel.com/en-us/openvino-toolkit.  Full installation instructions are available at https://software.intel.com/en-us/articles/OpenVINO-Install-Linux. 

Be sure to install the Intel&reg; Graphics Compute Runtime for OpenCL&trade; Driver components (under Optional Steps) to enable the use of the integrated GPU.  

We recommend Ubuntu 18.04 for a painless install.  We have had success with Ubuntu 18.10 and Ubuntu 16.04, but `setupvars.sh` may not set up the environment correctly, and on the older distro, a new kernel will be needed.  

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
Note:  It may take up to a minute to load all of the style models.  The server is ready for clients at this point.

If you do not have the correct drivers or do not have a supported integrated GPU, you can run the server in CPU-only mode by first editing config.py and setting `USE_GPU = False`.

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
