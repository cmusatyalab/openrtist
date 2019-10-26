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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.Toast;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.content.Context;
import android.os.Environment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.media.MediaActionSound;

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
import edu.cmu.cs.gabriel.util.Screenshot;
import edu.cmu.cs.localtransfer.LocalTransfer;
import edu.cmu.cs.localtransfer.Utils;
import edu.cmu.cs.openrtist.R;

public class GabrielClientActivity extends Activity implements AdapterView.OnItemSelectedListener,TextureView.SurfaceTextureListener {

    private static final String LOG_TAG = "Main";
    private static final int REQUEST_CODE = 1000;
    private static int DISPLAY_WIDTH = 640;
    private static int DISPLAY_HEIGHT = 480;
    private static int BITRATE = 1*1024*1024;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

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
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private boolean capturingScreen = false;
    private boolean recordingInitiated = false;
    private String mOutputPath = null;

    private ReceivedPacketInfo receivedPacketInfo = null;

    private LogicalTime logicalTime = null;

    private boolean reset = false;

    private FileWriter controlLogWriter = null;

    // views
    private ImageView imgView = null;
    private ImageView stereoView1 = null;
    private ImageView stereoView2 = null;
    private ImageView camView2 = null;
    private ImageView iconView = null;
    private Handler iterationHandler = null;
    private Handler fpsHandler = null;
    private int cameraId = 0;
    private boolean imageRotate = false;
    private TextView fpsLabel = null;

    private int framesProcessed = 0;

    // local execution
    private boolean runLocally = false;
    private LocalTransfer localRunner = null;
    private HandlerThread localRunnerThread=null;
    private Handler localRunnerThreadHandler=null;
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
	        "starry-night",
            "cafe_gogh",
            "fall_icarus",
            "monet",
            "weeping_woman",
            "going_to_work",
            "david_vaughan"
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
            "The Starry Night (Vincent Van Gogh)",
            "Cafe Terrace at Night (Vincent Van Gogh)",
            "Landscape with the Fall of Icarus (Pieter Bruegel the Elder)",
            "Bain à la Grenouillère (Claude Monet)",
            "Weeping Woman (Pablo Picasso)",
            "Going to Work (L.S. Lowry)",
            "Painting 015 (David Vaughan)"
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
            R.drawable.the_scream,
            R.drawable.starry_night,
            R.drawable.cafe_gogh,
            R.drawable.fall_icarus,
            R.drawable.monet,
            R.drawable.weeping_woman,
            R.drawable.going_to_work,
            R.drawable.david_vaughan
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
        iconView = (ImageView) findViewById(R.id.style_image);
        fpsLabel = (TextView) findViewById(R.id.fpsLabel);



        if(Const.SHOW_RECORDER) {
            ImageView recButton = (ImageView) findViewById(R.id.imgRecord);
            recButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(capturingScreen) {
                        ((ImageView) findViewById(R.id.imgRecord)).setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_videocam_24px));
                        stopRecording();
                        MediaActionSound m = new MediaActionSound();
                        m.play(MediaActionSound.STOP_VIDEO_RECORDING);
                    } else {
                        recordingInitiated = true;
                        MediaActionSound m = new MediaActionSound();
                        m.play(MediaActionSound.START_VIDEO_RECORDING);
                        ((ImageView) findViewById(R.id.imgRecord)).setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_videocam_off_24px));
                        initRecorder();
                        shareScreen();
                    }
                }
            });
            ImageView screenshotButton = (ImageView) findViewById(R.id.imgScreenshot);
            screenshotButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap b = Screenshot.takescreenshotOfRootView(imgView);
                    storeScreenshot(b,getOutputMediaFile(MEDIA_TYPE_IMAGE).getPath());

                    }

            });
        } else if(!Const.STEREO_ENABLED){
            //this view doesn't exist when stereo is enabled (activity_stereo.xml)
            findViewById(R.id.imgRecord).setVisibility(View.GONE);
            findViewById(R.id.imgScreenshot).setVisibility(View.GONE);
        }

        if(Const.ITERATE_STYLES) {
            findViewById(R.id.spinner).setVisibility(View.GONE);
            iterationHandler = new Handler();
            //start iterating immediately if recording is not enabled,
            //otherwise we should hold off on iterating until onActivityResult is called
            //and let the user know this is the case
            if(!Const.SHOW_RECORDER) {
                iterationHandler.postDelayed(styleIterator, 1000 * Const.ITERATE_INTERVAL);
            } else {
                Toast.makeText(this, R.string.iteration_delayed, Toast.LENGTH_LONG).show();
            }
        }

        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        final ImageView camButton = (ImageView) findViewById(R.id.imgSwitchCam);
        final ImageView rotateButton = (ImageView) findViewById(R.id.imgRotate);
        camButton.setVisibility(View.VISIBLE);
        camButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.setPreviewCallback(null);
                CameraClose();
                if (cameraId > 0) {
                    camButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_camera_front_24px));
                    Const.USING_FRONT_CAMERA = false;
                    cameraId = 0;
                    rotateButton.setVisibility(View.GONE);
                } else {
                    camButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_camera_rear_24px));
                    cameraId = findFrontFacingCamera();
                    Const.USING_FRONT_CAMERA = true;
                    rotateButton.setVisibility(View.VISIBLE);
                }
                mSurfaceTexture = preview.getSurfaceTexture();
                mCamera = checkCamera();
                CameraStart();
                mCamera.setPreviewCallbackWithBuffer(previewCallback);
                reusedBuffer = new byte[1920 * 1080 * 3 / 2]; // 1.5 bytes per pixel
                mCamera.addCallbackBuffer(reusedBuffer);
            }
        });

        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                imageRotate = !imageRotate;
                Const.FRONT_ROTATION = !Const.FRONT_ROTATION;
                if(style_type.equals("none"))
                    preview.setRotation(180 - preview.getRotation());
            }
        });


        if(Const.SHOW_FPS) {
            findViewById(R.id.fpsLabel).setVisibility(View.VISIBLE);
            fpsHandler = new Handler();
            fpsHandler.postDelayed(fpsCalculator, 1000);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        // if a mobile only run is specified
        if (Const.SERVER_IP.equals(getString(R.string.local_execution_dns_placeholder))) {
            runLocally = true;
            localRunner = new LocalTransfer(
                    Const.IMAGE_WIDTH,
                    Const.IMAGE_HEIGHT
            );
            localRunnerThread = new HandlerThread("LocalTransfer Thread");
            localRunnerThread.start();
            localRunnerThreadHandler = new Handler(localRunnerThread.getLooper());
        }
    }

    private void storeScreenshot(Bitmap bitmap, String path) {
        OutputStream out = null;
        File imageFile = new File(path);

        try {
            MediaActionSound m = new MediaActionSound();
            m.play(MediaActionSound.SHUTTER_CLICK);
            out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(imageFile)));
            Toast.makeText(this, getString(R.string.screenshot_taken, path), Toast.LENGTH_LONG).show();
            out.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException when attempting to store screenshot: " + e.getMessage());
        }
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Log.d(LOG_TAG, "Front facing camera found");
                    cameraId = i;
                    break;
                }
        }
        return cameraId;
    }

    private Runnable styleIterator = new Runnable() {
        private int position = 1;

        @Override
        public void run() {
            Spinner spinner = (Spinner) findViewById(R.id.spinner);
            if(++position == itemname.length)
                position = 1;
            style_type = itemname[position];
            Toast.makeText(getApplicationContext(), display_names[position],
                    Toast.LENGTH_SHORT).show();
            if(Const.STEREO_ENABLED) {
                if (stereoView1.getVisibility() == View.INVISIBLE) {
                    stereoView1.setVisibility(View.VISIBLE);
                    stereoView2.setVisibility(View.VISIBLE);
                }
            } else {
                if(Const.DISPLAY_REFERENCE) {
                    iconView.setVisibility(View.VISIBLE);
                    iconView.setImageResource(imgid[position]);
                }
                if (imgView.getVisibility() == View.INVISIBLE) {
                    imgView.setVisibility(View.VISIBLE);
                }
            }
            iterationHandler.postDelayed(this, 1000 * Const.ITERATE_INTERVAL);
        }
    };

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
        if(iterationHandler != null)
            iterationHandler.removeCallbacks(styleIterator);
        if(capturingScreen)
            stopRecording();
        this.terminate();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "++onDestroy");
        if(iterationHandler != null)
            iterationHandler.removeCallbacks(styleIterator);
        if(capturingScreen)
            stopRecording();
        super.onDestroy();
    }

    /**
     * Creates a media file in the {@code Environment.DIRECTORY_PICTURES} directory. The directory
     * is persistent and available to other applications like gallery.
     *
     * @param type Media type. Can be video or image.
     * @return A file object pointing to the newly created file.
     */
    public  static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return  null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "OpenRTiST");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(LOG_TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();

            return;
        }

        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        capturingScreen = true;
        if(Const.ITERATE_STYLES)
            iterationHandler.postDelayed(styleIterator, 1000 * Const.ITERATE_INTERVAL);
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
    }

    private VirtualDisplay createVirtualDisplay() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        try {
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mOutputPath = getOutputMediaFile(MEDIA_TYPE_VIDEO).getPath();
            mMediaRecorder.setOutputFile(mOutputPath);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoEncodingBitRate(BITRATE);
            mMediaRecorder.setVideoFrameRate(24);
            mMediaRecorder.prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(LOG_TAG, "Recording Stopped");
        Toast.makeText(this,
                getString(R.string.recording_complete, mOutputPath), Toast.LENGTH_LONG).show();
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(new File(mOutputPath))));
        mMediaProjection = null;
        stopScreenSharing();
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
        // be reused again
        destroyMediaProjection();
        capturingScreen = false;
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {

            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(LOG_TAG, "MediaProjection Stopped");
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

        // don't connect to cloudlet if running locally
        if (runLocally) return;

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
        controlThread.setPriority(Thread.MIN_PRIORITY);
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


    private byte[] yuvToRGBBytes(byte[] yuvFrameBytes, Camera.Parameters parameters){
        Size cameraImageSize = parameters.getPreviewSize();
        YuvImage image = new YuvImage(yuvFrameBytes,
                parameters.getPreviewFormat(), cameraImageSize.width,
                cameraImageSize.height, null);

        ByteArrayOutputStream tmpBuffer = new ByteArrayOutputStream();
        // chooses quality 67 and it roughly matches quality 5 in avconv
        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()),
                67, tmpBuffer);
        return tmpBuffer.toByteArray();
    }

    private PreviewCallback previewCallback = new PreviewCallback() {
        // called whenever a new frame is captured
        public void onPreviewFrame(final byte[] frame, Camera mCamera) {
            if (isRunning) {
                final Camera.Parameters parameters = mCamera.getParameters();

                if(!style_type.equals("none")) {
                    if (runLocally) {
                        //local execution
                        localRunnerThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                float[] rgbImage = Utils.convertYuvToRgb(
                                        RenderScript.create(getApplicationContext()),
                                        frame,
                                        parameters.getPreviewSize()
                                );
                                localRunner.infer(rgbImage);
                            }
                        });
                    } else if (videoStreamingThread != null) { // cloudlet execution
                        videoStreamingThread.push(frame, parameters, style_type);
                    }
                } else{
                    Log.v(LOG_TAG, "Display Cleared");
                    if(Const.STEREO_ENABLED) {
                        byte[] bytes = yuvToRGBBytes(frame, parameters);
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

    private Runnable fpsCalculator = new Runnable() {

        @Override
        public void run() {
            if(true){ //if(Const.SHOW_FPS) {
                if (fpsLabel.getVisibility() == View.INVISIBLE) {
                    fpsLabel.setVisibility(View.VISIBLE);

                }
                String msg= "FPS: " + framesProcessed;
                if(tokenController != null)
                    msg += " Avg RTT: " + tokenController.getAvgRTT();
                fpsLabel.setText( msg );
            }
            framesProcessed=0;
            fpsHandler.postDelayed(this, 1000);
        }
    };
    /**
     * Handles messages passed from streaming threads and result receiving threads.
     */
    private Handler returnMsgHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == NetworkProtocol.NETWORK_RET_FAILED) {
                //terminate();
                if(!recordingInitiated) {  //suppress this error when screen recording as we have to temporarily leave this activity causing a network disruption
                    AlertDialog.Builder builder = new AlertDialog.Builder(GabrielClientActivity.this, AlertDialog.THEME_HOLO_DARK);
                    builder.setMessage(msg.getData().getString("message"))
                            .setTitle(R.string.connection_error)
                            .setNegativeButton(R.string.back_button, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            GabrielClientActivity.this.finish();
                                        }
                                    }
                            )
                            .setCancelable(false);

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

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
                framesProcessed++;

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
            Log.v(LOG_TAG , "!!!!!CAMERA "+cameraId+ " START!!!!");
            mCamera = Camera.open(cameraId);
        }
        return mCamera;
    }

    public void CameraStart() {
        Log.v(LOG_TAG , "++start");
        if (mCamera == null) {
            mCamera = Camera.open(cameraId);
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
            mCamera = Camera.open(cameraId);
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
                if(Const.DISPLAY_REFERENCE)
                    iconView.setVisibility(View.INVISIBLE);
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
                if(Const.DISPLAY_REFERENCE) {
                    iconView.setVisibility(View.VISIBLE);
                    iconView.setImageResource(imgid[position]);
                }
                if (imgView.getVisibility() == View.INVISIBLE) {
                    imgView.setVisibility(View.VISIBLE);
                }
            }

            if (runLocally) {
                localRunnerThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            localRunner.load(getApplicationContext(),
                                    String.format("%s.pt", style_type));
                        } catch (FileNotFoundException e) {
                            style_type = "none";
                            AlertDialog.Builder builder = new AlertDialog.Builder(
                                    GabrielClientActivity.this,
                                    AlertDialog.THEME_HOLO_DARK);
                            builder.setMessage("Style Not Found Locally")
                                    .setTitle("Failed to Load Style");
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                });
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
        mCamera = Camera.open(cameraId);

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
            mCamera = Camera.open(cameraId);
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
