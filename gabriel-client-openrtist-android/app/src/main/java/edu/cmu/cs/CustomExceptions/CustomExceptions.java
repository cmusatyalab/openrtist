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

package edu.cmu.cs.CustomExceptions;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/**
 * Created by junjuew on 3/3/16.
 */
public class CustomExceptions {
    public static void notifyError(String msg, final boolean terminate, final Activity activity){
        DialogInterface.OnClickListener error_listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (terminate){
                            activity.finish();
                        }
                    }
                };
        new AlertDialog.Builder(activity)
                .setTitle("Error").setMessage(msg)
                .setNegativeButton("close", error_listener).show();
    }

    public static void notifyError(String msg,
                                   DialogInterface.OnClickListener errorListener,
                                   final Activity activity){
        new AlertDialog.Builder(activity)
                .setTitle("Error").setMessage(msg)
                .setNegativeButton("close", errorListener).show();
    }

}
