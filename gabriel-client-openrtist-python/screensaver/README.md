# Lowest Latency Setup

The lowest latency is achieved using named pipes and rawvideo codecs.

* Use [stream_for_screensaver.sh](../stream_for_screensaver.sh) to launch style transfer python client and pipe style transferred data stream into a linux pipe.
* Copy the [openrtist_screensaver.sh](openrtist_screensaver.sh) to /usr/local/bin. It launches a media player displaying a stream from the linux pipe.
* Refer to [dotxscreensaver](dotxscreensaver) to add Openrtist to xscreensaver.
* GHC demo setup uses a containerized Openrtist. To give the container access to the host xserver use the following command to start the container. The host also needs to allow the container to connect to its xserver if one wants to run [ui.py](../ui.py). One quick yet insecure way to do so is ```xhost +local:root```. See [here](http://wiki.ros.org/docker/Tutorials/GUI) for more information.
```
docker run --privileged --rm -it --env DISPLAY=$DISPLAY --env="QT_X11_NO_MITSHM=1" \
-v /dev/video0:/dev/video0 \
-v /tmp:/tmp \
-p 9098:9098 \
-p 9111:9111 \
-p 22222:22222 \
-p 8021:8021 \
--name styletransfer \
cmusatyalab/openrtist \
/bin/bash
```

# Short Latency Setup

Use mjpeg encoding with udp.

* For python client side,
```
./screensaver.py 2>&1 >/dev/null | ffmpeg -f rawvideo -pixel_format rgb24 -video_size 640x360 -re -i - -f mpjpeg rgb24 udp://172.17.0.1:8091
```
* For screensaver receiving side:
```
mplayer -benchmark -demuxer mpjepg udp://172.17.0.1:8091
```

# Most Scalable Setup

Use ffserver to setup a streaming server

* Launch ffserver
```
ffserver -f ffserver.conf
```
* send stream using ffmpeg
```
./screensaver.py 2>&1 >/dev/null | ffmpeg -f rawvideo -pixel_format rgb24 -video_size 640x360 -framerate 15 -i - http://172.17.0.1:8090/camera.ffm
```
* Receive streams with vlc
```
"Stream" 	cvlc --loop --fullscreen --drawable-xid	      \
         	  $XSCREENSAVER_WINDOW			      \
		  --no-video-title-show			      \
		  http://localhost:8090/camera.mjpeg	    \n\
```
* If one want to do everything using containers
```
docker run --rm --name ffserver -v `pwd`:/config --entrypoint 
docker run --rm -v /tmp/.X11-unix:/tmp/.X11-unix -e uid=$(id -u) -e gid=$(id -g) -e DISPLAY=$DISPLAY --privileged --entrypoint cvlc --name vlc quay.io/galexrt/vlc -q http://172.17.0.1:8090/camera.mjpeg
```
