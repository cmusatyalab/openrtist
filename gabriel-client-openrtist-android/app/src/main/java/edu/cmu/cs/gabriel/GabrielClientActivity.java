package edu.cmu.cs.gabriel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.ToggleButton;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.cs.gabriel.network.ControlThread;
import edu.cmu.cs.gabriel.network.LogicalTime;
import edu.cmu.cs.gabriel.network.NetworkProtocol;
import edu.cmu.cs.gabriel.util.PingThread;
import edu.cmu.cs.gabriel.network.ResultReceivingThread;
import edu.cmu.cs.gabriel.network.VideoStreamingThread;
import edu.cmu.cs.gabriel.token.ReceivedPacketInfo;
import edu.cmu.cs.gabriel.token.TokenController;
import edu.cmu.cs.gabriel.util.ResourceMonitoringService;
import edu.cmu.cs.openrtist.R;

public class GabrielClientActivity extends Activity implements AdapterView.OnItemSelectedListener,TextureView.SurfaceTextureListener {

    private static final String LOG_TAG = "Main";

    // major components for streaming sensor data and receiving information
    private String serverIP = null;
    private String style_type = "udnie";
    private String prev_style_type = "udnie";
    private VideoStreamingThread videoStreamingThread = null;
    private ResultReceivingThread resultThread = null;
    private ControlThread controlThread = null;
    private TokenController tokenController = null;
    private PingThread pingThread = null;

    private boolean isRunning = false;
    private boolean isFirstExperiment = true;

    //private CameraPreview preview = null;
    private Camera mCamera = null;
    private List<int[]> supportingFPS = null;
    private List<Camera.Size> supportingSize = null;
    private boolean isSurfaceReady = false;
    private boolean waitingToStart = false;
    private boolean isPreviewing = false;
    public byte[] reusedBuffer = null;

    private TextureView preview;
    private SurfaceTexture mSurfaceTexture;

    private MediaController mediaController = null;

    private ReceivedPacketInfo receivedPacketInfo = null;

    private LogicalTime logicalTime = null;

    private boolean reset = false;

    private FileWriter controlLogWriter = null;

    // views
    private ImageView imgView = null;
    private ImageView stereoView1 = null;
    private ImageView stereoView2 = null;
    private ImageView camView2 = null;

    //List of Styles

//    String[] itemname ={
//            "udnie",
//            "candy",
//            "mosaic",
//            "femmes_d_alger",
//            "sunday_afternoon",
//            "dido_carthage",
//            "the_scream",
//            "impression_sunrise",
//            "starry_night"
//    };
//
//    int[] imgid={
//            R.drawable.udnie,
//            R.drawable.candy,
//            R.drawable.mosaic,
//            R.drawable.femmes_d_alger,
//            R.drawable.sunday_afternoon,
//            R.drawable.dido_carthage,
//            R.drawable.the_scream,
//            R.drawable.impression_sunrise,
//            R.drawable.starry_night
//    };

    String[] itemname ={
            "Clear Display",
            "udnie",
            "candy",
            "mosaic",
            "rain_princess",
            "femmes_d_alger",
            "sunday_afternoon",
            "dido_carthage",
            "the_scream",
	        "starry-night"
    };


    String[] display_names ={
            "Clear Display",
            "Udnie (Francis Picabia)",
            "Candy (Unknown)",
            "Mosaic painting (Unknown)",
            "Rain Princess (Leonid Afremov)",
            "Les Femmes d'Alger (Pablo Picasso)",
            "A Sunday Afternoon on the Island of La Grande Jatte (Georges Seurat)",
            "The Rise of the Carthaginian Empire (J.M.W. Turner)",
            "The Scream (Edvard Munch)",
            "The Starry Night (Vincent Van Gogh)"
    };

    int[] imgid={
            R.drawable.ic_delete_black_24dp,
            R.drawable.udnie,
            R.drawable.candy,
            R.drawable.mosaic,
            R.drawable.rain_princess,
            R.drawable.femmes_d_alger,
            R.drawable.sunday_afternoon,
            R.drawable.dido_carthage,
            R.drawable.the_scream
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "++onCreate");
        super.onCreate(savedInstanceState);

        if(Const.STEREO_ENABLED) {
            setContentView(R.layout.activity_stereo);
        } else {
            setContentView(R.layout.activity_main);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED+
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON+
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Spinner element
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        // Spinner click listener
        spinner.setOnItemSelectedListener(this);
        CustomAdapter customAdapter=new CustomAdapter(getApplicationContext(),imgid,display_names);
        spinner.setAdapter(customAdapter);
        stereoView1 = (ImageView) findViewById(R.id.guidance_image1);
        //styleView1 = (ImageView) findViewById(R.id.style_image1);
        stereoView2 = (ImageView) findViewById(R.id.guidance_image2);
        camView2 = (ImageView) findViewById(R.id.camera_preview2);
        imgView = (ImageView) findViewById(R.id.guidance_image);


        Intent intent = getIntent();
        //reset = intent.getExtras().getBoolean("reset");
    }

    @Override
    protected void onResume() {
        Log.v(LOG_TAG, "++onResume");
        super.onResume();

        // dim the screen
//        WindowManager.LayoutParams lp = getWindow().getAttributes();
//        lp.dimAmount = 1.0f;
//        lp.screenBrightness = 0.01f;
//        getWindow().setAttributes(lp);

        initOnce();
        if (Const.IS_EXPERIMENT) { // experiment mode
            runExperiments();
        } else { // demo mode
            serverIP = Const.SERVER_IP;
            initPerRun(serverIP, Const.TOKEN_SIZE, null);
        }
    }

    @Override
    protected void onPause() {
        Log.v(LOG_TAG, "++onPause");
        this.terminate();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "++onDestroy");
        super.onDestroy();
    }

    /**
     * Does initialization for the entire application. Called only once even for multiple experiments.
     */
    private void initOnce() {
        Log.v(LOG_TAG, "++initOnce");

        if (Const.SENSOR_VIDEO) {
            //preview = (CameraPreview) findViewById(R.id.camera_preview);
            if(Const.STEREO_ENABLED)
                preview = (TextureView) findViewById(R.id.camera_preview1);
            else
                preview = (TextureView) findViewById(R.id.camera_preview);
            mSurfaceTexture = preview.getSurfaceTexture();
            preview.setSurfaceTextureListener(this);
            mCamera = checkCamera();
            CameraStart();
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
            reusedBuffer = new byte[1920 * 1080 * 3 / 2]; // 1.5 bytes per pixel
            mCamera.addCallbackBuffer(reusedBuffer);
        }

        Const.ROOT_DIR.mkdirs();
        Const.EXP_DIR.mkdirs();


        // Media controller
        if (mediaController == null) {
            mediaController = new MediaController(this);
        }

        startResourceMonitoring();

        isRunning = true;
    }

    /**
     * Does initialization before each run (connecting to a specific server).
     * Called once before each experiment.
     */
    private void initPerRun(String serverIP, int tokenSize, File latencyFile) {
        Log.v(LOG_TAG, "++initPerRun");

        if ((pingThread != null) && (pingThread.isAlive())) {
            pingThread.kill();
            pingThread.interrupt();
            pingThread = null;
        }
        if ((videoStreamingThread != null) && (videoStreamingThread.isAlive())) {
            videoStreamingThread.stopStreaming();
            videoStreamingThread = null;
        }
        if ((resultThread != null) && (resultThread.isAlive())) {
            resultThread.close();
            resultThread = null;
        }

        if (Const.IS_EXPERIMENT) {
            if (isFirstExperiment) {
                isFirstExperiment = false;
            } else {
                try {
                    Thread.sleep(20 * 1000);
                } catch (InterruptedException e) {}
                controlThread.sendControlMsg("ping");
                // wait a while for ping to finish...
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {}
            }
        }
        if (tokenController != null) {
            tokenController.close();
        }
        if ((controlThread != null) && (controlThread.isAlive())) {
            controlThread.close();
            controlThread = null;
        }

        if (serverIP == null) return;

        if (Const.BACKGROUND_PING) {
	        pingThread = new PingThread(serverIP, Const.PING_INTERVAL);
	        pingThread.start();
        }

        logicalTime = new LogicalTime();

        tokenController = new TokenController(tokenSize, latencyFile);

        if (Const.IS_EXPERIMENT) {
            try {
                controlLogWriter = new FileWriter(Const.CONTROL_LOG_FILE);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Control log file cannot be properly opened", e);
            }
        }

        controlThread = new ControlThread(serverIP, Const.CONTROL_PORT, returnMsgHandler, tokenController);
        controlThread.start();

        if (Const.IS_EXPERIMENT) {
            controlThread.sendControlMsg("ping");
            // wait a while for ping to finish...
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {}
        }

        resultThread = new ResultReceivingThread(serverIP, Const.RESULT_RECEIVING_PORT, returnMsgHandler);
        resultThread.start();

        if (Const.SENSOR_VIDEO) {
            videoStreamingThread = new VideoStreamingThread(serverIP, Const.VIDEO_STREAM_PORT, returnMsgHandler, tokenController, mCamera, logicalTime);
            videoStreamingThread.start();
        }

    }

    /**
     * Runs a set of experiments with different server IPs and token numbers.
     * IP list and token sizes are defined in the Const file.
     */
    private void runExperiments() {
        final Timer startTimer = new Timer();
        TimerTask autoStart = new TimerTask() {
            int ipIndex = 0;
            int tokenIndex = 0;
            @Override
            public void run() {
                GabrielClientActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // end condition
                        if ((ipIndex == Const.SERVER_IP_LIST.length) || (tokenIndex == Const.TOKEN_SIZE_LIST.length)) {
                            Log.d(LOG_TAG, "Finish all experiemets");

                            initPerRun(null, 0, null); // just to get another set of ping results

                            startTimer.cancel();
                            terminate();
                            return;
                        }

                        // make a new configuration
                        serverIP = Const.SERVER_IP_LIST[ipIndex];
                        int tokenSize = Const.TOKEN_SIZE_LIST[tokenIndex];
                        File latencyFile = new File (Const.EXP_DIR.getAbsolutePath() + File.separator +
                                "latency-" + serverIP + "-" + tokenSize + ".txt");
                        Log.i(LOG_TAG, "Start new experiment - IP: " + serverIP +"\tToken: " + tokenSize);

                        // run the experiment
                        initPerRun(serverIP, tokenSize, latencyFile);

                        // move to the next experiment
                        tokenIndex++;
                        if (tokenIndex == Const.TOKEN_SIZE_LIST.length){
                            tokenIndex = 0;
                            ipIndex++;
                        }
                    }
                });
            }
        };

        // run 5 minutes for each experiment
        startTimer.schedule(autoStart, 1000, 5*60*1000);
    }

    private PreviewCallback previewCallback = new PreviewCallback() {
        // called whenever a new frame is captured
        public void onPreviewFrame(byte[] frame, Camera mCamera) {
            if (isRunning) {
                Camera.Parameters parameters = mCamera.getParameters();
                if(!style_type.equals("none")) {
                    if (videoStreamingThread != null) {
                        videoStreamingThread.push(frame, parameters, style_type);
                    }
                } else{
                    Log.v(LOG_TAG, "Display Cleared");
                    if(Const.STEREO_ENABLED) {
                        Size cameraImageSize = parameters.getPreviewSize();
                        YuvImage image = new YuvImage(frame, parameters.getPreviewFormat(), cameraImageSize.width,
                                cameraImageSize.height, null);
                        ByteArrayOutputStream tmpBuffer = new ByteArrayOutputStream();
                        // chooses quality 67 and it roughly matches quality 5 in avconv
                        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 67, tmpBuffer);
                        byte[] bytes = tmpBuffer.toByteArray();
                        final Bitmap camView = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        stereoView1.setVisibility(View.INVISIBLE);
                        stereoView2.setVisibility(View.INVISIBLE);
                        camView2.setImageBitmap(camView);
                    } else {
                        imgView.setVisibility(View.INVISIBLE);
                    }

                }
            }
            mCamera.addCallbackBuffer(frame);
        }
    };

    /**
     * Notifies token controller that some response is back
     */
    private void notifyToken() {
        Message msg = Message.obtain();
        msg.what = NetworkProtocol.NETWORK_RET_TOKEN;
        receivedPacketInfo.setGuidanceDoneTime(System.currentTimeMillis());
        msg.obj = receivedPacketInfo;
        try {
            tokenController.tokenHandler.sendMessage(msg);
        } catch (NullPointerException e) {
            // might happen because token controller might have been terminated
        }
    }

    private void processServerControl(JSONObject msgJSON) {
        if (Const.IS_EXPERIMENT) {
            try {
                controlLogWriter.write("" + logicalTime.imageTime + "\n");
                String log = msgJSON.toString();
                controlLogWriter.write(log + "\n");
            } catch (IOException e) {}
        }

        try {
            // Switching on/off image sensor
            if (msgJSON.has(NetworkProtocol.SERVER_CONTROL_SENSOR_TYPE_IMAGE)) {
                boolean sw = msgJSON.getBoolean(NetworkProtocol.SERVER_CONTROL_SENSOR_TYPE_IMAGE);
                if (sw) { // turning on
                    Const.SENSOR_VIDEO = true;
                    tokenController.reset();
                    if (preview == null) {
                        preview = (TextureView) findViewById(R.id.camera_preview);
                        mSurfaceTexture = preview.getSurfaceTexture();
                        preview.setSurfaceTextureListener(this);
                        mCamera = checkCamera();
                        CameraStart();
                        mCamera.setPreviewCallbackWithBuffer(previewCallback);
                        reusedBuffer = new byte[1920 * 1080 * 3 / 2]; // 1.5 bytes per pixel
                        mCamera.addCallbackBuffer(reusedBuffer);
                    }
                    if (videoStreamingThread == null) {
                        videoStreamingThread = new VideoStreamingThread(serverIP, Const.VIDEO_STREAM_PORT, returnMsgHandler, tokenController, mCamera, logicalTime);
                        videoStreamingThread.start();
                    }
                } else { // turning off
                    Const.SENSOR_VIDEO = false;
                    if (preview != null) {
                        mCamera.setPreviewCallback(null);
                        CameraClose();
                        reusedBuffer = null;
                        preview = null;
                        mCamera = null;
                    }
                    if (videoStreamingThread != null) {
                        videoStreamingThread.stopStreaming();
                        videoStreamingThread = null;
                    }
                }
            }

            // Camera configs
            if (preview != null) {
                int targetFps = -1, imgWidth = -1, imgHeight = -1;
                if (msgJSON.has(NetworkProtocol.SERVER_CONTROL_FPS))
                    targetFps = msgJSON.getInt(NetworkProtocol.SERVER_CONTROL_FPS);
                if (msgJSON.has(NetworkProtocol.SERVER_CONTROL_IMG_WIDTH))
                    imgWidth = msgJSON.getInt(NetworkProtocol.SERVER_CONTROL_IMG_WIDTH);
                if (msgJSON.has(NetworkProtocol.SERVER_CONTROL_IMG_HEIGHT))
                    imgHeight = msgJSON.getInt(NetworkProtocol.SERVER_CONTROL_IMG_HEIGHT);
                if (targetFps != -1 || imgWidth != -1)
                    updateCameraConfigurations(targetFps, imgWidth, imgHeight);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "" + msgJSON);
            Log.e(LOG_TAG, "error in processing server control messages" + e);
            return;
        }
    }

    /**
     * Handles messages passed from streaming threads and result receiving threads.
     */
    private Handler returnMsgHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == NetworkProtocol.NETWORK_RET_FAILED) {
                //terminate();
            }
            if (msg.what == NetworkProtocol.NETWORK_RET_MESSAGE) {
                receivedPacketInfo = (ReceivedPacketInfo) msg.obj;
                receivedPacketInfo.setMsgRecvTime(System.currentTimeMillis());
            }
            if (msg.what == NetworkProtocol.NETWORK_RET_IMAGE || msg.what == NetworkProtocol.NETWORK_RET_ANIMATION) {
                Bitmap feedbackImg = (Bitmap) msg.obj;
                if(Const.STEREO_ENABLED) {
                    stereoView1 = (ImageView) findViewById(R.id.guidance_image1);
                    stereoView1.setVisibility(View.VISIBLE);
                    stereoView1.setImageBitmap(feedbackImg);
                    stereoView2 = (ImageView) findViewById(R.id.guidance_image2);
                    stereoView2.setVisibility(View.VISIBLE);
                    stereoView2.setImageBitmap(feedbackImg);
                } else {
                    imgView = (ImageView) findViewById(R.id.guidance_image);
                    imgView.setVisibility(View.VISIBLE);
                    imgView.setImageBitmap(feedbackImg);
                }
            }
            if (msg.what == NetworkProtocol.NETWORK_RET_DONE) {
                notifyToken();
            }
            if (msg.what == NetworkProtocol.NETWORK_RET_CONFIG) {
                String controlMsg = (String) msg.obj;
                try {
                    final JSONObject controlJSON = new JSONObject(controlMsg);
                    if (controlJSON.has("delay")) {
                        final long delay = controlJSON.getInt("delay");

                        final Timer controlTimer = new Timer();
                        TimerTask controlTask = new TimerTask() {
                            @Override
                            public void run() {
                                GabrielClientActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        logicalTime.increaseImageTime((int) (delay * 15 / 1000));
                                        processServerControl(controlJSON);
                                    }
                                });
                            }
                        };

                        // run 5 minutes for each experiment
                        controlTimer.schedule(controlTask, delay);
                    } else {
                        processServerControl(controlJSON);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "error in jsonizing server control messages" + e);
                }
            }
        }
    };

    /**
     * Terminates all services.
     */
    private void terminate() {
        Log.v(LOG_TAG, "++terminate");

        isRunning = false;

        if ((pingThread != null) && (pingThread.isAlive())) {
            pingThread.kill();
            pingThread.interrupt();
            pingThread = null;
        }
        if ((resultThread != null) && (resultThread.isAlive())) {
            resultThread.close();
            resultThread = null;
        }
        if ((videoStreamingThread != null) && (videoStreamingThread.isAlive())) {
            videoStreamingThread.stopStreaming();
            videoStreamingThread = null;
        }
        if ((controlThread != null) && (controlThread.isAlive())) {
            controlThread.close();
            controlThread = null;
        }
        if (tokenController != null){
            tokenController.close();
            tokenController = null;
        }
        if (preview != null) {
            mCamera.setPreviewCallback(null);
            CameraClose();
            reusedBuffer = null;
            preview = null;
            mCamera = null;
        }
        stopResourceMonitoring();
        if (Const.IS_EXPERIMENT) {
            try {
                controlLogWriter.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error in closing control log file");
            }
        }
    }

    /**************** Camera Preview ***********************/

    public Camera checkCamera() {
        Log.v(LOG_TAG , "++checkCamera");
        if (mCamera == null) {
            Log.v(LOG_TAG , "!!!!!CAMERA START!!!!");
            mCamera = Camera.open();
        }
        return mCamera;
    }

    public void CameraStart() {
        Log.v(LOG_TAG , "++start");
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        if (isSurfaceReady) {
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException exception) {
                Log.e(LOG_TAG, "Error in setting camera holder: " + exception.getMessage());
                CameraClose();
            }
            updateCameraConfigurations(Const.CAPTURE_FPS, Const.IMAGE_WIDTH, Const.IMAGE_HEIGHT);
        } else {
            waitingToStart = true;
        }
    }

    public void CameraClose() {
        Log.v(LOG_TAG , "++close");
        if (mCamera != null) {
            mCamera.stopPreview();
            isPreviewing = false;
            mCamera.release();
            mCamera = null;
        }
    }

    public void changeConfiguration(int[] range, Size imageSize) {
        Log.v(LOG_TAG , "++changeConfiguration");
        Camera.Parameters parameters = mCamera.getParameters();
        if (range != null){
            Log.i("Config", "frame rate configuration : " + range[0] + "," + range[1]);
            parameters.setPreviewFpsRange(range[0], range[1]);
        }
        if (imageSize != null){
            Log.i("Config", "image size configuration : " + imageSize.width + "," + imageSize.height);
            parameters.setPreviewSize(imageSize.width, imageSize.height);
        }
        //parameters.setPreviewFormat(ImageFormat.JPEG);

        mCamera.setParameters(parameters);
    }

    public void surfaceCreated(SurfaceTexture surface) {
        Log.d(LOG_TAG, "++surfaceCreated");
        isSurfaceReady = true;
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        if (mCamera != null) {
            // get fps to capture
            Camera.Parameters parameters = mCamera.getParameters();
            this.supportingFPS = parameters.getSupportedPreviewFpsRange();
            for (int[] range: this.supportingFPS) {
                Log.i(LOG_TAG, "available fps ranges:" + range[0] + ", " + range[1]);
            }

            // get resolution
            this.supportingSize = parameters.getSupportedPreviewSizes();
            for (Camera.Size size: this.supportingSize) {
                Log.i(LOG_TAG, "available sizes:" + size.width + ", " + size.height);
            }

            if (waitingToStart) {
                waitingToStart = false;
                try {
                    mCamera.setPreviewTexture(mSurfaceTexture);
                } catch (IOException exception) {
                    Log.e(LOG_TAG, "Error in setting camera holder: " + exception.getMessage());
                    CameraClose();
                }
                updateCameraConfigurations(Const.CAPTURE_FPS, Const.IMAGE_WIDTH, Const.IMAGE_HEIGHT);
            }
        } else {
            Log.w(LOG_TAG, "Camera is not open");
        }
    }


    public void updateCameraConfigurations(int targetFps, int imgWidth, int imgHeight) {
        Log.d(LOG_TAG, "updateCameraConfigurations");
        if (mCamera != null) {
            if (targetFps != -1) {
                Const.CAPTURE_FPS = targetFps;
            }
            if (imgWidth != -1) {
                Const.IMAGE_WIDTH = imgWidth;
                Const.IMAGE_HEIGHT = imgHeight;
            }

            if (isPreviewing)
                mCamera.stopPreview();

            // set fps to capture
            int index = 0, fpsDiff = Integer.MAX_VALUE;
            for (int i = 0; i < this.supportingFPS.size(); i++){
                int[] frameRate = this.supportingFPS.get(i);
                int diff = Math.abs(Const.CAPTURE_FPS * 1000 - frameRate[0]) + Math.abs(Const.CAPTURE_FPS * 1000 - frameRate[1]);
                if (diff < fpsDiff){
                    fpsDiff = diff;
                    index = i;
                }
            }
            int[] targetRange = this.supportingFPS.get(index);

            // set resolution
            index = 0;
            int sizeDiff = Integer.MAX_VALUE;
            for (int i = 0; i < this.supportingSize.size(); i++){
                Camera.Size size = this.supportingSize.get(i);
                int diff = Math.abs(size.width - Const.IMAGE_WIDTH) + Math.abs(size.height - Const.IMAGE_HEIGHT);
                if (diff < sizeDiff){
                    sizeDiff = diff;
                    index = i;
                }
            }
            Camera.Size target_size = this.supportingSize.get(index);

            changeConfiguration(targetRange, target_size);

            mCamera.startPreview();
            isPreviewing = true;
        }
    }

    /**************** End of Camera Preview ****************/

    /**************** onItemSelected ***********************/

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        //Toast.makeText(getApplicationContext(), itemname[position], Toast.LENGTH_LONG).show();
        if(itemname[position] == "Clear Display"){
            style_type = "none";
            if(Const.STEREO_ENABLED) {
                stereoView1.setVisibility(View.INVISIBLE);
                stereoView2.setVisibility(View.INVISIBLE);
            } else {
                imgView.setVisibility(View.INVISIBLE);
            }
        }
        else {
            style_type = itemname[position];
            if(Const.STEREO_ENABLED) {
                if (stereoView1.getVisibility() == View.INVISIBLE) {
                    stereoView1.setVisibility(View.VISIBLE);
                    stereoView2.setVisibility(View.VISIBLE);
                }
            } else {
                if (imgView.getVisibility() == View.INVISIBLE) {
                    imgView.setVisibility(View.VISIBLE);
                }
            }
        }
//        styleView.setImageResource(imgid[position]);
//        style_type = itemname[position];
//        Log.i(LOG_TAG, "NEW STYLE TYPE: " + style_type);
//        switch (position) {
//            case 0:
//                style_type = "udnie";
//                break;
//            case 1:
//                style_type = "candy";
//                break;
//            case 2:
//                style_type = "mosaic";
//                break;
//            case 3:
//                style_type = "starry-night";
//                break;
//            default:
//                style_type = "udnie";
//                break;
//        };
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    /**************** End of onItemSelected ****************/

    /**************** SurfaceTexture Listener ***********************/

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        /*
        mCamera = Camera.open();

        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(
                previewSize.width, previewSize.height, Gravity.CENTER));

        try {
            mCamera.setPreviewTexture(surface);
        } catch (IOException t) {
        }

        mCamera.startPreview();
        */
        isSurfaceReady = true;
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        if (mCamera != null) {
            // get fps to capture
            Camera.Parameters parameters = mCamera.getParameters();
            this.supportingFPS = parameters.getSupportedPreviewFpsRange();
            for (int[] range: this.supportingFPS) {
                Log.i(LOG_TAG, "available fps ranges:" + range[0] + ", " + range[1]);
            }

            // get resolution
            this.supportingSize = parameters.getSupportedPreviewSizes();
            for (Camera.Size size: this.supportingSize) {
                Log.i(LOG_TAG, "available sizes:" + size.width + ", " + size.height);
            }

            if (waitingToStart) {
                waitingToStart = false;
                try {
                    mCamera.setPreviewTexture(surface);
                } catch (IOException exception) {
                    Log.e(LOG_TAG, "Error in setting camera holder: " + exception.getMessage());
                    CameraClose();
                }
                updateCameraConfigurations(Const.CAPTURE_FPS, Const.IMAGE_WIDTH, Const.IMAGE_HEIGHT);
            }
        } else {
            Log.w(LOG_TAG, "Camera is not open");
        }



    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, the Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        isSurfaceReady = false;
        CameraClose();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Update your view here!
    }

    /****************End of SurfaceTexture Listener ***********************/

    /**************** Battery recording *************************/
    /*
	 * Resource monitoring of the mobile device
     * Checks battery and CPU usage, as well as device temperature
	 */
    Intent resourceMonitoringIntent = null;

    public void startResourceMonitoring() {
        Log.i(LOG_TAG, "Starting Battery Recording Service");
        resourceMonitoringIntent = new Intent(this, ResourceMonitoringService.class);
        startService(resourceMonitoringIntent);
    }

    public void stopResourceMonitoring() {
        Log.i(LOG_TAG, "Stopping Battery Recording Service");
        if (resourceMonitoringIntent != null) {
            stopService(resourceMonitoringIntent);
            resourceMonitoringIntent = null;
        }
    }
    /**************** End of battery recording ******************/
}
