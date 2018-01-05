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

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import edu.cmu.cs.gabriel.network.NetworkProtocol;

/**
 * Created by junjuew on 2/2/16.
 */
public class GabrielConfigurationAsyncTask extends AsyncTask<Object, Integer, Boolean> {
    private static final String LOG_TAG = "ConfigurationAsyncTask";
    private Activity callingActivity;

    private ProgressDialog dialog;
    private InetAddress remoteIP;
    private int sendToPort;
    private int recvFromPort;

    // TCP send socket
    private static Socket tcpSocket = null;
    private static DataOutputStream networkWriter = null;

    // TCP recv socket
    private static Socket recvTcpSocket = null;
    private static DataInputStream networkReader = null;
    public AsyncResponse delegate =null;
    private String action=null;
    private volatile String uiMsg=null;

    private static int id=0;

    private byte[] extra=null;

    // you may separate this or combined to caller class.
    public interface AsyncResponse {
        void onGabrielConfigurationAsyncTaskFinish(String action, boolean output, byte[] extra);
    }

    public GabrielConfigurationAsyncTask(Activity activity,
                                         String IPString,
                                         int sendToPort,
                                         int recvFromPort,
                                         String action) {
        closeConnection();
        this.callingActivity =activity;
        dialog = new ProgressDialog(callingActivity);
        try {
            remoteIP = InetAddress.getByName(IPString);
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, "unknown host: " + e.getMessage());
        }
        this.sendToPort = sendToPort;
        this.recvFromPort = recvFromPort;
        this.action=action;
        Log.d(LOG_TAG,"async task created");
    }

    public GabrielConfigurationAsyncTask(Activity activity,
                                         String IPString,
                                         int sendToPort,
                                         int recvFromPort,
                                         String action,
                                         AsyncResponse delegate) {
        this(activity, IPString, sendToPort, recvFromPort, action);
        this.delegate = delegate;
    }

    private void closeConnection(){
        try {
            if (networkReader != null) {
                networkReader.close();
            }
            if (networkWriter != null) {
                networkWriter.close();
            }
            if (tcpSocket != null) {
                tcpSocket.close();
            }
            if (recvTcpSocket != null) {
                recvTcpSocket.close();
            }
        } catch (IOException e) {
            Log.d(LOG_TAG, "failed to close connection");
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "connection closed");
    }

    private void setupConnection(InetAddress ip, int sendToPort, int recvFromPort)
            throws IOException, InterruptedException {
        closeConnection();
        tcpSocket = new Socket();
        tcpSocket.setTcpNoDelay(true);
        tcpSocket.connect(new InetSocketAddress(ip, sendToPort), 5 * 1000);
        networkWriter = new DataOutputStream(tcpSocket.getOutputStream());

        recvTcpSocket = new Socket();
        recvTcpSocket.setTcpNoDelay(true);
        //3 min read time out
        recvTcpSocket.setSoTimeout(30 * 1000 * 5);
        recvTcpSocket.connect(new InetSocketAddress(ip, recvFromPort), 5 * 1000);
        networkReader = new DataInputStream(recvTcpSocket.getInputStream());
        //TODO: bug. on gabriel server side, if no sleep is added,
        //the result receiving handler is connected after the response has been generated.
        //resulting the response not sent back from result receiving handler
        Thread.sleep(1000,0);
    }

    private void sendPacket(byte[] header, byte[] data) throws IOException {
        //send add person packet first
        // make it as a single packet
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos=new DataOutputStream(baos);

        dos.writeInt(header.length);
        dos.writeInt(data.length);
        dos.write(header);
        dos.write(data);
        networkWriter.write(baos.toByteArray());
        networkWriter.flush();
        Log.d(LOG_TAG, "header size: " + header.length+ " data size: " +data.length);
    }

    private String receiveMsg(DataInputStream reader) throws IOException {
        int retLength = reader.readInt();
        byte[] recvByte = new byte[retLength];
        int readSize = 0;
        while(readSize < retLength){
            int ret = reader.read(recvByte, readSize, retLength-readSize);
            if(ret <= 0){
                break;
            }
            readSize += ret;
        }
        String receivedString = new String(recvByte);
        return receivedString;
    }

    private String parseResponseData(String response) throws JSONException {
        JSONObject obj;
        obj = new JSONObject(response);
        String result = null;
        result = obj.getString(NetworkProtocol.CUSTOM_DATA_MESSAGE_VALUE);
        Log.d(LOG_TAG, "resp: " + result.substring(0, Math.min(result.length(), 10)));
        return result;
    }

    private String parseResponsePacket(String recvData){
        // convert the message to JSON
        JSONObject obj;
        String msgData = null;
        long frameID = -1;
        String ret=null;
        try{
            obj = new JSONObject(recvData);
            frameID = obj.getLong(NetworkProtocol.HEADER_MESSAGE_FRAME_ID);

            msgData = obj.getString(NetworkProtocol.HEADER_MESSAGE_RESULT);
            Log.d(LOG_TAG, "received response. frameID: "+frameID + " msg: " + msgData);
            ret = parseResponseData(msgData);
        } catch(JSONException e){
            e.printStackTrace();
        }
        return ret;
    }

//    private void switchConnection() throws IOException {
//        InetAddress ip=null;
//        if (remoteIP.equals(Const.CLOUD_GABRIEL_IP)){
//            try {
//                ip = InetAddress.getByName(Const.CLOUDLET_GABRIEL_IP);
//            } catch (UnknownHostException e) {
//                Log.e(LOG_TAG, "unknown host: " + e.getMessage());
//            }
//        } else {
//            try {
//                ip = InetAddress.getByName(Const.CLOUD_GABRIEL_IP);
//            } catch (UnknownHostException e) {
//                Log.e(LOG_TAG, "unknown host: " + e.getMessage());
//            }
//        }
//        setupConnection(ip, sendToPort,recvFromPort);
//        Log.d(LOG_TAG, "switching host: " + remoteIP + " changed to " + ip);
//    }


    private byte[] generateGetStateHeader(){
        JSONObject headerJson = new JSONObject();
        try{

            headerJson.put("id", id);
            headerJson.put("get_state", "True");
            Log.d(LOG_TAG, "send get_state request");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return headerJson.toString().getBytes();
    }


    private byte[] generateHeader(String headerContent) {
        JSONObject headerJson = new JSONObject();
        try{

            headerJson.put("id", id);
            headerJson.put(headerContent, "True");
            Log.d(LOG_TAG, "send request: " + headerContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return headerJson.toString().getBytes();
    }

    private byte[] generateHeader(String header, String value) {
        JSONObject headerJson = new JSONObject();
        try{

            headerJson.put("id", id);
            headerJson.put(header, value);
            Log.d(LOG_TAG, "send request: " + header + " value: "+value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return headerJson.toString().getBytes();
    }

    @Override
    protected void onPreExecute() {
        Log.d(LOG_TAG, "async task: "+action);
        dialog.setMessage("Communicating to Backend Server ... Please wait");
        dialog.show();
    }

    @Override
    protected void onPostExecute(Boolean bgResult) {
        dialog.dismiss();
        if (null != this.delegate){
            delegate.onGabrielConfigurationAsyncTaskFinish(this.action, bgResult, extra);
        }
        if (null!=uiMsg){
            Log.i("configurationAsyncTask", "success: " + bgResult + ". " + uiMsg);
            Toast.makeText(callingActivity.getApplicationContext(),
                    "Connecting to Backend. success? "+bgResult + "\nmessage: " + uiMsg,
                Toast.LENGTH_SHORT).show();
        } else {
            Log.i("configurationAsyncTask", "success: " + bgResult);

//            Toast.makeText(callingActivity.getApplicationContext(),
//                    "success? "+bgResult,
//                    Toast.LENGTH_LONG).show();
        }

//        Toast.makeText(callingActivity.getApplicationContext(), "async task success? "+bgResult,
//                Toast.LENGTH_LONG).show();
    }

    @Override
    protected Boolean doInBackground(Object... inputData) {
        Boolean success =false;
        String task = action;
        // task is to sync state
        try{
            if (task.equals(Const.GABRIEL_CONFIGURATION_SYNC_STATE)) {
                String copyFromIp = (String) inputData[0];
                InetAddress copyFrom = InetAddress.getByName(copyFromIp);
                setupConnection(copyFrom, sendToPort, recvFromPort);
                //get state
                byte[] header = generateGetStateHeader();
                byte[] data = "dummpy_long_enough".getBytes();
                sendPacket(header, data);
                String resp = receiveMsg(networkReader);
                String openfaceState = parseResponsePacket(resp);
                //switch connection
                closeConnection();
                setupConnection(remoteIP,sendToPort,recvFromPort);
                //load state
                byte[] loadStateHeader = generateHeader("load_state");
                data=openfaceState.getBytes();
                sendPacket(loadStateHeader, data);
                resp = receiveMsg(networkReader);
                String content = parseResponsePacket(resp).toLowerCase();
                if (content.equals("true")) {
                    success = true;
                }
            } else if (task.equals(Const.GABRIEL_CONFIGURATION_RESET_STATE)) {
                setupConnection(remoteIP, sendToPort, recvFromPort);
                //get state
                byte[] header = generateHeader("reset");
                byte[] data = "dummpy_long_enough".getBytes();
                sendPacket(header, data);
                String resp = receiveMsg(networkReader);
                String content = parseResponsePacket(resp).toLowerCase();
                if (content.equals("true")) {
                    success = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "IO exception sync state failed");
            uiMsg = "Is the FaceSwap Server Running at " + remoteIP + "?";
        } catch (InterruptedException e){
            e.printStackTrace();
            Log.e(LOG_TAG, "async task interrupted");
        } finally {
            closeConnection();
        }
        return success;
    }


}
