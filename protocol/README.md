# Updating Protocol

See notes about getting the protobuf compiler
[here](https://github.com/cmusatyalab/gabriel-protocol#updating-protocol).
Then run:
1. `/path/to/protoc --python_out=. openrtist.proto`
2. `/path/to/protoc --java_out=../gabriel-client-openrtist-android/app/src/main/java/ openrtist.proto`
