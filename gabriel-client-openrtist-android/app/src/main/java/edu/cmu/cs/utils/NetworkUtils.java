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

package edu.cmu.cs.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import edu.cmu.cs.gabriel.Const;

import static edu.cmu.cs.CustomExceptions.CustomExceptions.notifyError;

/**
 * Created by junjuew on 3/3/16.
 */
public class NetworkUtils {
    protected static final String
            IPV4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    protected static final String IPV6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

    public static boolean isOnline(Context m) {
        ConnectivityManager connMgr = (ConnectivityManager)
                m.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static boolean isIpAddress(String ipAddress) {
        if (ipAddress.matches(IPV4Pattern) || ipAddress.matches(IPV6Pattern)) {
            return true;
        }
        return false;
    }

    public static boolean checkOnline(Activity a){
        if (isOnline(a)) {
            return true;
        }
        notifyError(Const.CONNECTIVITY_NOT_AVAILABLE, false, a);
        return false;
    }


}
