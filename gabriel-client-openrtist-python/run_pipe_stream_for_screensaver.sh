#! /bin/bash
stream_pipe="/tmp/stylepipe"
rm $stream_pipe
while true; do
    if [[ -p $stream_pipe ]]; then
	rm /tmp/rgbpipe;
	mkfifo /tmp/rgbpipe;
	./client.py &
	bgid=$!
	cat /tmp/rgbpipe | ffmpeg -f rawvideo -pixel_format rgb24 -video_size 960x540 -re -hwaccel vaapi -i - -f rawvideo -video_size 960x540 -pix_fmt rgb24 - > $stream_pipe
	kill -9 $bgid
	rm -f  $stream_pipe
	sleep 5
    else
	echo "streaming pipe not found. wait for it to be created..."
	sleep 5
    fi
done
