# for screensaver setup, launch ffserver first, and then send stream from ffmpeg. The stream is displayed using vlc

## launcher ffserver
ffserver -f ffserver.conf

## send stream using ffmpeg
./client.py 2>&1 >/dev/null | ffmpeg -f rawvideo -pixel_format rgb24 -video_size 640x360 -framerate 15 -i - http://172.17.0.1:8090/camera.ffm

## add following line in xscreensaver 'program' section to display the stream


"Stream" 	cvlc --loop --fullscreen --drawable-xid	      \
         	  $XSCREENSAVER_WINDOW			      \
		  --no-video-title-show			      \
		  http://localhost:8090/camera.mjpeg	    \n\



# If one wants to do every using containers
docker run --rm --name ffserver -v `pwd`:/config --entrypoint 
docker run --rm -v /tmp/.X11-unix:/tmp/.X11-unix -e uid=$(id -u) -e gid=$(id -g) -e DISPLAY=$DISPLAY --privileged --entrypoint cvlc --name vlc quay.io/galexrt/vlc -q http://172.17.0.1:8090/camera.mjpeg

