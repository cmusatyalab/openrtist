// Copyright 2018 Carnegie Mellon University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.cmu.cs.gabriel.network;

public class NetworkProtocol {
    // TODO: give better names to these constants

    public static final int NETWORK_RET_FAILED = 1;
    public static final int NETWORK_RET_SPEECH = 2;
    public static final int NETWORK_RET_CONFIG = 3;
    public static final int NETWORK_RET_TOKEN = 4;
    public static final int NETWORK_RET_IMAGE = 5;
    public static final int NETWORK_RET_VIDEO = 6;
    public static final int NETWORK_RET_ANIMATION = 7;
    public static final int NETWORK_RET_MESSAGE = 8;
    public static final int NETWORK_RET_DONE = 9;
    public static final int NETWORK_RET_SYNC = 10;

    public static final String HEADER_MESSAGE_CONTROL = "control";
    public static final String HEADER_MESSAGE_RESULT = "result";
    public static final String HEADER_MESSAGE_INJECT_TOKEN = "token_inject";
    public static final String HEADER_MESSAGE_FRAME_ID = "frame_id";
    public static final String HEADER_MESSAGE_STYLE = "style";
    public static final String HEADER_MESSAGE_ENGINE_ID = "engine_id";

    public static final String SENSOR_TYPE_KEY = "sensor_type";
    public static final String SENSOR_JPEG = "mjpeg";
    public static final String SENSOR_ACC = "acc";
    public static final String SENSOR_GPS = "gps";
    public static final String SENSOR_AUDIO = "audio";

    public static final String CUSTOM_DATA_MESSAGE_VALUE= "value";

    public static final String SERVER_CONTROL_SENSOR_TYPE_IMAGE = SENSOR_JPEG;
    public static final String SERVER_CONTROL_SENSOR_TYPE_ACC = SENSOR_ACC;
    public static final String SERVER_CONTROL_SENSOR_TYPE_AUDIO = SENSOR_AUDIO;
    public static final String SERVER_CONTROL_FPS = "fps";
    public static final String SERVER_CONTROL_IMG_WIDTH = "img_width";
    public static final String SERVER_CONTROL_IMG_HEIGHT = "img_height";
}
