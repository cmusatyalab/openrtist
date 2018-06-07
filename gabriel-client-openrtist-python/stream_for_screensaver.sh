#! /bin/bash
if [[ $# -ne 1 ]]; then
   printf "Usage: stream_for_screensaver.sh openrtist_server_ip\n"
   exit 1;
fi
server_ip="$1"
printf "Trying to connect to specified openrtist server: ${server_ip}"

stream_pipe="/tmp/rgbpipe"
while true; do
	  echo "killing client.py if there is any"
	  pkill -f -9 client.py
	  echo "removing rgbpipe"
	  rm /tmp/rgbpipe;
	  echo "make rgbpipe fifo"
	  mkfifo /tmp/rgbpipe;
	  echo "launching client in the bg"
	  trap 'kill -KILL ${bg_pid}; wait ${bg_pid}; exit' TERM INT
	  ./client.py "${server_ip}" "${stream_pipe}" &
	  bg_pid=$!
	  wait ${bg_pid}
	  trap - TERM INT
done
