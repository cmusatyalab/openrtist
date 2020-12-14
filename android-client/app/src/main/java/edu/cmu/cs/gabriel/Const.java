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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Context;

public class Const {
    public static boolean USING_FRONT_CAMERA = false;
    public static boolean FRONT_ROTATION = false;
    public static boolean STEREO_ENABLED = false;
    public static boolean DISPLAY_REFERENCE = false;
    public static boolean ITERATE_STYLES = false;
    public static boolean ITERATION_STARTED = false;
    public static int ITERATE_INTERVAL = 2;
    public static boolean SHOW_RECORDER = true;
    public static boolean SHOW_FPS = true;

    // high level sensor control (on/off)
    public static boolean SENSOR_VIDEO = true;

    public static String SYNC_BASE = "video";
    public static boolean STYLES_RETRIEVED = false;
    public static final int REFERENCE_IMAGE = 99;

    // image size and frame rate
    public static int CAPTURE_FPS = 30;

    // options: 320x180, 640x360, 1280x720, 1920x1080
    public static int IMAGE_WIDTH = 320;
    public static int IMAGE_HEIGHT = 240;

    public static final int PORT = 9099;

    // server IP
    public static String SERVER_IP = "";  // Cloudlet

    // token size
    public static String TOKEN_LIMIT = "None";

    public static final String SOURCE_NAME = "openrtist";

    public static void loadPref(SharedPreferences sharedPreferences, String key) {
        Boolean b = null;
        Integer i = null;
        //update Const values so that new settings take effect
        switch(key) {
            case "general_recording":
                Const.SHOW_RECORDER = sharedPreferences.getBoolean(key, false);
                break;
            case "general_show_fps":
                b = sharedPreferences.getBoolean(key, false);
                Const.SHOW_FPS = b;
                break;
            case "experimental_resolution":
                i = new Integer(sharedPreferences.getString(key, "1"));
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
                Const.TOKEN_LIMIT = sharedPreferences.getString(key, "2");
                break;
            case "general_stereoscopic":
                b = sharedPreferences.getBoolean(key, false);
                Const.STEREO_ENABLED = b;
                if(b) {
                    Const.SHOW_FPS = false;
                    Const.SHOW_RECORDER = false;
                    Const.DISPLAY_REFERENCE = false;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("general_show_reference", false);
                    editor.putBoolean("general_front_camera", false);
                    editor.putBoolean("general_recording", false);
                    editor.putBoolean("general_show_fps", false);
                    editor.putString("general_iterate_delay", "2");
                    editor.commit();

                }
                break;
            case "general_show_reference":
                b = sharedPreferences.getBoolean(key, true);
                Const.DISPLAY_REFERENCE = b;
                break;

            case "general_iterate_delay":
                i = new Integer(sharedPreferences.getString(key, "0"));
                Const.ITERATE_STYLES = (i != 0);
                Const.ITERATE_INTERVAL = i * 5;
                break;

        }
    }
}
