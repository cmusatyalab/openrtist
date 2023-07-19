#!/bin/bash
if [[ $# -ne 1 ]]; then
   printf "Usage: stream_for_screensaver.sh openrtist_server_ip\n"
   exit 1;
fi
server_ip="$1"
printf "Trying to connect to specified openrtist server: ${server_ip}"

stream_pipe="/tmp/rgbpipe"
while true; do
	  echo "killing screensaver.py if there is any"
	  pkill -f -9 screensaver.py
	  echo "removing rgbpipe to make sure no image is half written/read"
	  rm /tmp/rgbpipe;
	  echo "create rgbpipe fifo"
	  mkfifo /tmp/rgbpipe;
	  echo "launching screensaver in the bg"
	  trap 'kill -KILL ${bg_pid}; wait ${bg_pid}; exit' TERM INT
	  ./screensaver.py "${server_ip}" "${stream_pipe}" &
	  bg_pid=$!
	  wait ${bg_pid}
	  trap - TERM INT
done
