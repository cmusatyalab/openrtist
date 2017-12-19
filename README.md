openStyleTransfer: Edge Computing for live-video Style Transfer 
========================================================
Copyright (C) 2017-2018 Carnegie Mellon University
This is a developing project and some features might not be stable yet.
By utilizing Gabriel, platform for wearable cognitive assistance applications, the live
video from the mobile is streamed to the cloudlet on which the style-transfer
application is running.

Requirements
-------------
The style-transfer application uses [Gabriel](https://github.com/cmusatyalab/gabriel-private) ,[pytorch](http://pytorch.org/) and requires GPU to run.

Recommended Directory Structure
-------------
```
+gabriel
  +client
  +server
  	+style_app
  		+style_python_app
  		+style_legacy_android_app
``` 
How to use
--------------
1. Setup Gabriel 
    Assuming the user has installed pytorch and Gabriel. Follow the instructions given in [Gabriel repo] (https://github.com/cmusatyalab/gabriel-private) to run the `control server` and `ucomm server`.
	Note: The android app uses legacy mode. Pass the `-l` parameter while running the Gabriel control.

2. Run the style app
	Use the following command to run the style app:
	```
    $ cd <gabriel-repo>/server/style_app/style_<mode>_app/
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
    MODEL PATH <gabriel-repo>/server/style_app/style_<mode>_app/models/
	FINISHED INITIALISATION
    ```
3.  Run a python or mobile client using source code at gabriel-client-style-<mode>. 
    Make sure to change IP address of GABRIEL_IP variable at src/edu/cmu/cs/gabriel/Const.java for the android client and config.py for the python client

Acknowledgement
-------------
	* [Towards Wearable Cognitive Assistance](http://dl.acm.org/citation.cfm?id=2594383)
	* [Pytorch Fast Neural Style] (https://github.com/pytorch/examples/tree/master/fast_neural_style)
