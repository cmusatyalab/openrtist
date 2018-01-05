# Lowest Latency Setup

The lowest latency is achieved using named pipes and rawvideo codecs.

* Use run_pipe_stream_for_screensaver.sh to launch style transfer python client and pipe data stream
```
#! /bin/bash
stream_pipe="/tmp/stylepipe"
rm $stream_pipe
while true; do
    if [[ -p $stream_pipe ]]; then
	rm /tmp/rgbpipe;
	mkfifo /tmp/rgbpipe;
	./client.py &
	bgid=$!
	cat /tmp/rgbpipe | ffmpeg -f rawvideo -pixel_format rgb24 -video_size 1280x720 -re -hwaccel vaapi -i - -f rawvideo -video_size 1280x720 -pix_fmt rgb24 - > $stream_pipe
	kill -9 $bgid
	rm -f  $stream_pipe
	sleep 5
    else
	echo "streaming pipe not found. wait for it to be created..."
	sleep 5
    fi
done
```
* Use following for xscreensaver program. See .xscreensaver for example

```
"mpvstream" 	/bin/bash -c 'pkill -f ^mpv; sleep 2;	      \
	  mkfifo -m				      \
	  666					      \
	  /tmp/stylepipe; cat /tmp/stylepipe |	      \
	  mpv --no-audio --no-cache		      \
	  --no-cache-pause --untimed		      \
	  --no-stop-xscreensaver --no-correct-pts     \
	  -wid $XSCREENSAVER_WINDOW -demuxer	      \
	  rawvideo -demuxer-rawvideo-w 1280	      \
	  --demuxer-rawvideo-h 720		      \
	  --demuxer-rawvideo-mp-format rgb24 -'	    \n\
```

# Short Latency Setup

Use mjpeg encoding with udp.

* For python client side,
```
./client.py 2>&1 >/dev/null | ffmpeg -f rawvideo -pixel_format rgb24 -video_size 640x360 -re -i - -f mpjpeg rgb24 udp://172.17.0.1:8091
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
./client.py 2>&1 >/dev/null | ffmpeg -f rawvideo -pixel_format rgb24 -video_size 640x360 -framerate 15 -i - http://172.17.0.1:8090/camera.ffm
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
