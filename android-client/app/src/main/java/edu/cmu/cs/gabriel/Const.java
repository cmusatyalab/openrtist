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

package edu.cmu.cs.gabriel;

import java.io.File;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.content.Context;

public class Const {
    public enum DeviceModel {
        GoogleGlass,
        Nexus6,
    }

    public static final DeviceModel deviceModel = DeviceModel.Nexus6;

    // whether to do a demo or a set of experiments
    public static final boolean IS_EXPERIMENT = false;

    // whether to use real-time captured images or load images from files for testing
    public static final boolean LOAD_IMAGES = false;
    // whether to use real-time sensed ACC values or load data from files for testing
    public static final boolean LOAD_ACC = false;
    // whether to use real-time captured audio or load audio data from files for testing
    public static final boolean LOAD_AUDIO = false;

    public static boolean USING_FRONT_CAMERA = false;
    public static boolean FRONT_ROTATION = false;
    public static boolean STEREO_ENABLED = false;
    public static boolean DISPLAY_REFERENCE = false;
    public static boolean ITERATE_STYLES = false;
    public static int ITERATE_INTERVAL = 2;
    public static boolean SHOW_RECORDER = false;
    public static boolean SHOW_FPS = false;

    // high level sensor control (on/off)
    public static boolean SENSOR_VIDEO = true;
    public static boolean SENSOR_ACC = false;
    public static boolean SENSOR_AUDIO = false;

    public static String SYNC_BASE = "video";
    public static boolean STYLES_RETRIEVED = false;
    public static final int REFERENCE_IMAGE = 99;

    /************************ In both demo and experiment mode *******************/
    // directory for all application related files (input + output)
    public static final File ROOT_DIR = new File(Environment.getExternalStorageDirectory() +
            File.separator + "Gabriel" + File.separator);

    // image size and frame rate
    public static int CAPTURE_FPS = 30;
    // options: 320x180, 640x360, 1280x720, 1920x1080
    //public static int IMAGE_WIDTH = 640;
    //public static int IMAGE_HEIGHT = 480;
    public static int IMAGE_WIDTH = 320;
    public static int IMAGE_HEIGHT = 240;


    public static final String GABRIEL_CONFIGURATION_SYNC_STATE = "syncState";

    public static final String GABRIEL_CONFIGURATION_RESET_STATE = "reset";
    public static String CONNECTIVITY_NOT_AVAILABLE = "Not Connected to the Internet. Please enable " +
            "network connections first.";


    public static final int PORT = 9099;

    // the app name
    public static final String APP_NAME = "style_transfer";

    // load images (JPEG) from files and pretend they are just captured by the camera
    public static final File TEST_IMAGE_DIR = new File(ROOT_DIR.getAbsolutePath() +
            File.separator + "images-" + APP_NAME + File.separator);

    // load audio data from file
    public static final File TEST_AUDIO_FILE = new File(ROOT_DIR.getAbsolutePath() +
            File.separator + "audio-" + APP_NAME + ".raw");

    // may include background pinging to keep network active
    public static final boolean BACKGROUND_PING = false;
    public static final int PING_INTERVAL = 200;

    // audio configurations
    public static final int RECORDER_SAMPLERATE = 16000;
    public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    /************************ Demo mode only *************************************/
    // server IP
    public static String SERVER_IP = "128.2.208.111";  // Cloudlet

    // token size
    public static String TOKEN_LIMIT = "None";

    /************************ Experiment mode only *******************************/
    // server IP list
    public static final String[] SERVER_IP_LIST = {
            "128.2.211.75",
    };

    // maximum times to ping (for time synchronization
    public static final int MAX_PING_TIMES = 20;

    // a small number of images used for compression (bmp files), usually a subset of test images
    // these files are loaded into memory first so cannot have too many of them!
    public static final File COMPRESS_IMAGE_DIR = new File(ROOT_DIR.getAbsolutePath() +
            File.separator + "images-" + APP_NAME + "-compress" + File.separator);
    // the maximum allowed compress images to load
    public static final int MAX_COMPRESS_IMAGE = 3;

    // result file
    public static final File EXP_DIR = new File(ROOT_DIR.getAbsolutePath() + File.separator + "exp");

    // control log file
    public static final File CONTROL_LOG_FILE = new File(ROOT_DIR.getAbsolutePath() + File.separator + "exp" + File.separator + "control_log.txt");

    public static void loadPref(Context c, String key, Object value) {
        String stringValue = value.toString();
        Boolean b = null;
        Integer i = null;
        //update Const values so that new settings take effect
        switch(key) {
            case "general_recording":
                Const.SHOW_RECORDER = new Boolean(value.toString());
                break;
            case "general_show_fps":
                b = new Boolean(value.toString());
                Const.SHOW_FPS = b;
                if(b) {
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(c)
                            .edit();
                    editor.putBoolean("general_stereoscopic", false);
                    editor.commit();
                }
                break;
            case "experimental_resolution":
                i = new Integer(stringValue);
                if(i == 1) {
                    Const.IMAGE_HEIGHT = 240;
                    Const.IMAGE_WIDTH = 320;
                } else if(i == 2) {
                    Const.IMAGE_HEIGHT = 480;
                    Const.IMAGE_WIDTH = 640;
                } else if (i == 3) {
                    Const.IMAGE_HEIGHT = 720;
                    Const.IMAGE_WIDTH = 1280;
                } else {
                    Const.IMAGE_HEIGHT = 240;
                    Const.IMAGE_WIDTH = 320;
                }
                break;
            case "experimental_token_limit":
                Const.TOKEN_LIMIT = stringValue;
                break;
            case "general_stereoscopic":
                b = new Boolean(value.toString());
                Const.STEREO_ENABLED = b;
                if(b) {
                    Const.SHOW_FPS = false;
                    Const.SHOW_RECORDER = false;
                    Const.DISPLAY_REFERENCE = false;
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(c)
                            .edit();
                    editor.putBoolean("general_show_reference", false);
                    editor.putBoolean("general_front_camera", false);
                    editor.putBoolean("general_recording", false);
                    editor.putBoolean("general_show_fps", false);
                    editor.putString("general_iterate_delay", "2");
                    editor.commit();
                }
                break;
            case "general_show_reference":
                b = new Boolean(value.toString());
                Const.DISPLAY_REFERENCE = b;
                if(b) {
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(c)
                            .edit();
                    editor.putBoolean("general_stereoscopic", false);
                    editor.commit();
                }
                break;

            case "general_iterate_delay":
                i = new Integer(stringValue);
                Const.ITERATE_STYLES = (i != 0);
                Const.ITERATE_INTERVAL = i * 5;
                break;

        }

    }

}
