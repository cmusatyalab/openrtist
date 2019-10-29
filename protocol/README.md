# Updating Protocol

See notes about getting the protobuf compiler
[here](https://github.com/cmusatyalab/gabriel-protocol#updating-protocol).
Then run:
1. `/path/to/protoc --python_out=. openrtist.proto`

This compiles the proto for the server and the Python client.

The proto will be compiled for Android the next time the Android client is
built.
