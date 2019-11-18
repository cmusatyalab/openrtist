#! /bin/bash
# Openrtist screensaver script for GHC 9th floor Demo. This script launches a media player which displays a stream from
# a linux pipe.
# This script should be used together with ../stream_for_screensaver.sh

_V=0
while [[ $# -gt 0 ]]
do
  key="$1"
  case $key in
    -v|--verbose)
       _V=1
       ;;
    -h|--help)
       printf "Usage: openrtist_screensaver.sh [-vh]\n"
       exit
  esac
  shift
done

function log() {
    if [[ $_V -eq 1 ]]; then
        printf "$@" >> /tmp/openrtist_screensaver.log
    fi
}

log "$(date): invoking openrist screensaver\n"
log "killing running mpv\n"
pkill -f -9 ^mpv
log "xscreensaver window id ${XSCREENSAVER_WINDOW} \n"
log "launching mpv \n"
# xscreensaver needs sometime to create a screensaver window. Hence, a short sleep here is needed to make sure the
# xscreensaver window is created before mpv is launched
sleep 5
exec mpv --no-audio --no-cache \
--no-cache-pause --untimed \
--no-correct-pts --really-quiet \
--wid ${XSCREENSAVER_WINDOW} \
--demuxer rawvideo \
--demuxer-rawvideo-w 960 \
--demuxer-rawvideo-h 540 \
--demuxer-rawvideo-mp-format rgb24 \
/tmp/rgbpipe
