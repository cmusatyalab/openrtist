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
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;

import android.media.Image;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;

import edu.cmu.cs.gabriel.network.EngineInput;
import edu.cmu.cs.gabriel.network.FrameSupplier;
import edu.cmu.cs.gabriel.network.NetworkProtocol;
import edu.cmu.cs.gabriel.network.OpenrtistComm;
import edu.cmu.cs.gabriel.protocol.Protos;
import edu.cmu.cs.gabriel.protocol.Protos.InputFrame;
import edu.cmu.cs.gabriel.util.Screenshot;
import edu.cmu.cs.localtransfer.LocalTransfer;
import edu.cmu.cs.localtransfer.Utils;
import edu.cmu.cs.openrtist.R;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.CameraConfig;
import android.opengl.GLSurfaceView;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import android.opengl.GLES20;

import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.AugmentedFaceNode;
import com.google.ar.sceneform.ux.BaseArFragment;

import androidx.appcompat.app.AppCompatActivity;

public class GabrielClientActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String LOG_TAG = "GabrielClientActivity";
    private static final int REQUEST_CODE = 1000;
    private static int DISPLAY_WIDTH = 640;
    private static int DISPLAY_HEIGHT = 480;
    private static int BITRATE = 1*1024*1024;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    // major components for streaming sensor data and receiving information
    String serverIP = null;
    private String styleType = "?";

    private OpenrtistComm openrtistComm;

    private boolean isRunning = false;
    private boolean isFirstExperiment = true;

    private List<int[]> supportingFPS = null;
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
    private boolean cleared = false;

    private int framesProcessed = 0;
    private EngineInput engineInput;
    final private Object engineInputLock = new Object();
    private FrameSupplier frameSupplier = new FrameSupplier(this);

    private Session session;
    private Config config;
    private ArFragment arFragment;
    private ArSceneView sceneView;
    private Scene scene;
    private Image image;
    private Image depth_map;
    private Image img;

    SeekBar simpleSeekBar;
    private int depth_threshold = 0;

    private ArrayAdapter<String> spinner_adapter = null;
    private List<String> styleDescriptions = new ArrayList<>(Arrays.asList(
            "Clear Display"
    ));

    public List<String> getStyleDescriptions() {
        return styleDescriptions;
    }

    private List<String> styleIds = new ArrayList<>(Arrays.asList(
            "none"
    ));

    public List<String> getStyleIds() {
        return styleIds;
    }

    // Background threads based on
    // https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java#L652
    /**
     * Thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler backgroundHandler;

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageUpload");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        backgroundHandler.post(imageUpload);
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        openrtistComm.stop();
        backgroundThread.quitSafely();

        // Will stop backgroundThread.join() from blocking if backgroundThread is currently blocked
        // on a call to wait()
        backgroundThread.interrupt();

        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Problem stopping background thread", e);
        }
    }

    private Runnable imageUpload = new Runnable() {
        @Override
        public void run() {
            openrtistComm.sendSupplier(GabrielClientActivity.this.frameSupplier);

            if (isRunning) {
                backgroundHandler.post(imageUpload);
            }
        }
    };

    // local execution
    private boolean runLocally = false;
    private LocalTransfer localRunner = null;
    private HandlerThread localRunnerThread = null;
    private Handler localRunnerThreadHandler = null;
    private volatile boolean localRunnerBusy = false;
    private RenderScript rs = null;
    private Bitmap localRunnerBitmapCache = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isRunning = true;

        Log.v(LOG_TAG, "++onCreate");
        super.onCreate(savedInstanceState);
        Const.STYLES_RETRIEVED = false;
        Const.ITERATION_STARTED = false;
        if(Const.STEREO_ENABLED) {
            setContentView(R.layout.activity_stereo);
        } else {
            setContentView(R.layout.activity_main);
            // Spinner element
            Spinner spinner = (Spinner) findViewById(R.id.spinner);
            spinner_adapter = new ArrayAdapter<String>(
                    this, R.layout.mylist, styleDescriptions);
            // Spinner click listener
            spinner.setOnItemSelectedListener(this);
            spinner.setAdapter(spinner_adapter);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED+
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON+
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        stereoView1 = (ImageView) findViewById(R.id.guidance_image1);
        //styleView1 = (ImageView) findViewById(R.id.style_image1);
        stereoView2 = (ImageView) findViewById(R.id.guidance_image2);
        camView2 = (ImageView) findViewById(R.id.camera_preview2);
        imgView = (ImageView) findViewById(R.id.guidance_image);
        iconView = (ImageView) findViewById(R.id.style_image);
        fpsLabel = (TextView) findViewById(R.id.fpsLabel);


        // initiate  views
        simpleSeekBar=(SeekBar)findViewById(R.id.seekBar);
        // perform seek bar change listener event used for getting the progress value
        simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                depth_threshold = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (Const.DEPTH_SUPPORTED) {
                    Toast.makeText(GabrielClientActivity.this, "Depth threshold is set to " + depth_threshold + " millimeters",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GabrielClientActivity.this, "Depth mode is not supported on this device. Viewing normal mode",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });


        if(Const.SHOW_RECORDER) {
            final ImageView recButton = (ImageView) findViewById(R.id.imgRecord);
            recButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(capturingScreen) {
                        ((ImageView) findViewById(R.id.imgRecord)).setImageDrawable(getResources().
                                getDrawable(R.drawable.ic_baseline_videocam_24px));
                        stopRecording();
                        MediaActionSound m = new MediaActionSound();
                        m.play(MediaActionSound.STOP_VIDEO_RECORDING);
                    } else {
                        recordingInitiated = true;
                        MediaActionSound m = new MediaActionSound();
                        m.play(MediaActionSound.START_VIDEO_RECORDING);
                        ((ImageView) findViewById(R.id.imgRecord)).setImageDrawable(getResources().
                                getDrawable(R.drawable.ic_baseline_videocam_off_24px));
                        initRecorder();
                        shareScreen();
                    }
                    recButton.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS);
                }
            });
            final ImageView screenshotButton = (ImageView) findViewById(R.id.imgScreenshot);
            screenshotButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap b = Screenshot.takescreenshotOfRootView(imgView);
                    storeScreenshot(b,getOutputMediaFile(MEDIA_TYPE_IMAGE).getPath());
                    screenshotButton.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS);

                }

            });

        } else if(!Const.STEREO_ENABLED){
            //this view doesn't exist when stereo is enabled (activity_stereo.xml)
            findViewById(R.id.imgRecord).setVisibility(View.GONE);
            findViewById(R.id.imgScreenshot).setVisibility(View.GONE);
        }

        if(Const.ITERATE_STYLES) {
            if (!Const.STEREO_ENABLED) {
                final ImageView playpauseButton = (ImageView) findViewById(R.id.imgPlayPause);
                playpauseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Const.ITERATION_STARTED == false) {
                            Const.ITERATION_STARTED = true;
                            playpauseButton.setImageResource(R.drawable.ic_pause);

                            Toast.makeText(playpauseButton.getContext(), getString(R.string.iteration_started),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Const.ITERATION_STARTED = false;
                            playpauseButton.setImageResource(R.drawable.ic_play);
                            Toast.makeText(playpauseButton.getContext(), getString(R.string.iteration_stopped),
                                    Toast.LENGTH_LONG).show();
                        }
                        playpauseButton.performHapticFeedback(
                                android.view.HapticFeedbackConstants.LONG_PRESS);

                    }
                });

                findViewById(R.id.spinner).setVisibility(View.GONE);
            } else {
                //artificially start iteration since we don't display
                //any buttons in stereo view
                Const.ITERATION_STARTED = true;
            }

            iterationHandler = new Handler();
            iterationHandler.postDelayed(styleIterator, 100);
        } else {
            ImageView playpauseButton = (ImageView) findViewById(R.id.imgPlayPause);
            playpauseButton.setVisibility(View.GONE);
        }

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

        // setup local execution if needed
        if (Const.SERVER_IP.equals(getString(R.string.local_execution_dns_placeholder))) {
            runLocally = true;
            styleDescriptions.add("Going to Work (L.S. Lowry)");
            styleDescriptions.add("Mosaic (Unknown)");
            styleDescriptions.add("The Scream (Edvard Munch)");
            styleDescriptions.add("Starry Night (Vincent Van Gogh)");
            styleDescriptions.add("Weeping Woman (Pablo Picasso)");
            styleIds.add("going_to_work");
            styleIds.add("mosaic");
            styleIds.add("the_scream");
            styleIds.add("starry-night");
            styleIds.add("weeping_woman");
            Const.STYLES_RETRIEVED = true;
            localRunner = new LocalTransfer(
                    Const.IMAGE_WIDTH,
                    Const.IMAGE_HEIGHT
            );
            rs = RenderScript.create(this);
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
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(imageFile)));
            Toast.makeText(this, getString(R.string.screenshot_taken, path), Toast.LENGTH_LONG).show();
            out.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException when attempting to store screenshot", e);
        }
    }

    private Runnable styleIterator = new Runnable() {
        private int position = 1;

        @Override
        public void run() {
            if(Const.STYLES_RETRIEVED && Const.ITERATION_STARTED) {
                //wait until styles are retrieved before iterating
                if (++position == styleIds.size())
                    position = 1;
                styleType = styleIds.get(position);
                if(runLocally) {
                    localRunnerThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                localRunner.load(getApplicationContext(),
                                        String.format("%s.pt", styleType));
                            } catch (FileNotFoundException e) {
                                styleType = "none";
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
                Toast.makeText(getApplicationContext(), styleDescriptions.get(position),
                        Toast.LENGTH_SHORT).show();
                if (Const.STEREO_ENABLED) {
                    if (stereoView1.getVisibility() == View.INVISIBLE) {
                        stereoView1.setVisibility(View.VISIBLE);
                        stereoView2.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (Const.DISPLAY_REFERENCE) {
                        iconView.setVisibility(View.VISIBLE);
                    }
                    if (imgView.getVisibility() == View.INVISIBLE) {
                        imgView.setVisibility(View.VISIBLE);
                    }
                }
                iterationHandler.postDelayed(this, 1000 * Const.ITERATE_INTERVAL);
            } else {
                iterationHandler.postDelayed(this, 100);
            }
        }
    };


    @Override
    protected void onResume() {
        Log.v(LOG_TAG, "++onResume");
        super.onResume();

        initOnce();
        serverIP = Const.SERVER_IP;
        initPerRun(serverIP);
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
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()) {
                Log.d(LOG_TAG, "failed to create media directory");
                return null;
            }
        }

        // Create a media file name
        String pattern = "yyyyMMdd_HHmmss";
        String timeStamp = new SimpleDateFormat(pattern, Locale.US).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath(), "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath(), "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
        if (Const.ITERATE_STYLES)
            iterationHandler.postDelayed(styleIterator, 100 * Const.ITERATE_INTERVAL);
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
            Log.e(LOG_TAG, "Problem with recorder", e);
        }
    }

    private void stopRecording() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(LOG_TAG, "Recording Stopped");
        Toast.makeText(this,
                getString(R.string.recording_complete, mOutputPath), Toast.LENGTH_LONG).show();
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(new File(mOutputPath))));
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
     * Does initialization for the entire application.
     */
    private void initOnce() {
        Log.v(LOG_TAG, "++initOnce");

        if (Const.SENSOR_VIDEO) {
            //preview = (CameraPreview) findViewById(R.id.camera_preview);
            if(Const.STEREO_ENABLED)
                preview = (TextureView) findViewById(R.id.camera_preview1);
            else
                preview = (TextureView) findViewById(R.id.camera_preview);
        }

        // get ARFragment instance to use SceneForm functions with ARCore
        try {
            arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
            arFragment.getPlaneDiscoveryController().hide();

            sceneView = arFragment.getArSceneView();
            scene = sceneView.getScene();
            session = new Session(this);
            sceneView.setupSession(session);

            while (session == null) {
                Log.v("CHECKPOINT FAILED: ", "null session");
            }

            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.setDepthMode(Config.DepthMode.AUTOMATIC);
            } else {
                Log.v("CHECKPOINT FAILED: ", "DepthMode NOT Supported");
            }
            session.configure(config);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ensures that the update listener is called whenever the camera frame updates
        scene.addOnUpdateListener(
                frameTime -> {
                    if (isRunning) {
                        // obtain the current frame from ARSession
                        Frame frame = sceneView.getArFrame();

                        if (styleType.equals("?") || !styleType.equals("none")) {
                            if (GabrielClientActivity.this.openrtistComm != null) {
                                // cloudlet execution
                                synchronized (GabrielClientActivity.this.engineInputLock) {
                                    try {
                                        // obtain the camera image
                                        image = frame.acquireCameraImage();

                                        if (image == null) {
                                            Log.v("CHECKPOINT FAILED: ", "cameraimage null");
                                            return;
                                        }
                                        if (image.getFormat() != ImageFormat.YUV_420_888) {
                                            Log.v("CHECKPOINT FAILED: ", "Expected cameraimage in YUV_420_888 format, got format:"+ image.getFormat());
                                            return;
                                        }
                                        byte[] imageBytes = imageToByte(image);
                                        int imageHeight = image.getHeight();
                                        int imageWidth = image.getWidth();

                                        image.close();

                                        // obtain the depth map if depth mode is supported
                                        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                            Const.DEPTH_SUPPORTED = true;

                                            depth_map = frame.acquireDepthImage();
                                            if (depth_map == null) {
                                                Log.v("CHECKPOINT FAILED: ", "depth_map null");
                                                return;
                                            }
                                            if (depth_map.getFormat() != ImageFormat.DEPTH16) {
                                                Log.v("CHECKPOINT FAILED: ", "Expected depth_map in DEPTH16 format, got format:"+ depth_map.getFormat());
                                                return;
                                            }

                                            byte[] depthBytes = DEPTH16toBYTEs(depth_map);
                                            depth_map.close();

                                            // create engineInput for generating protobuf later and send data to server
                                            GabrielClientActivity.this.engineInput = new EngineInput(
                                                    imageBytes,  depthBytes, imageHeight, imageWidth, styleType, depth_threshold);
                                            GabrielClientActivity.this.engineInputLock.notifyAll();

                                            // Log.v("CHECKPOINT SUCCESS", "GabrielClientActivity addOnUpdateListener");
                                        } else {
                                            // send depth as null and only send the image if depth mode is not supported
                                            // assign the depth threshold as -1 as an indicator that the protobuf will not contain depth_map
                                            GabrielClientActivity.this.engineInput = new EngineInput(
                                                    imageBytes,  null, imageHeight, imageWidth, styleType, -1);
                                            GabrielClientActivity.this.engineInputLock.notifyAll();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            GabrielClientActivity.this.engineInput = null;
                            Log.v(LOG_TAG, "Display Cleared");

                            try {
                                if (img != null) {
                                    img.close();
                                }
                                img = frame.acquireCameraImage();
                                byte[] bytes = imageToByte(img);
                                final Bitmap camView = BitmapFactory.decodeByteArray(
                                        bytes, 0, bytes.length);
                                this.imgView = (ImageView)this.findViewById(R.id.guidance_image);
                                this.imgView.setVisibility(View.VISIBLE);
                                this.imgView.setImageBitmap(camView);

                            } catch (NotYetAvailableException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        // Media controller
        if (mediaController == null) {
            mediaController = new MediaController(this);
        }
        isRunning = true;
    }

    public EngineInput getEngineInput() {
        synchronized (this.engineInputLock) {
            try {
                while (isRunning && this.engineInput == null) {
                    engineInputLock.wait();
                }
                // Log.v("CHECKPOINT SUCCESS", "GabrielClientActivity getEngineInput NOTIFIED");
                EngineInput inputToSend = this.engineInput;
                this.engineInput = null;  // Prevent sending the same frame again
                return inputToSend;
            } catch (/* InterruptedException */ Exception e) {
                Log.e(LOG_TAG, "Error waiting for engine input", e);
                return null;
            }
        }
    }

    /**
     * Does initialization before each run (connecting to a specific server).
     */
    private void initPerRun(String serverIP) {
        Log.v(LOG_TAG, "++initPerRun");

        // don't connect to cloudlet if running locally
        // if a mobile only run is specified
        if (runLocally) {
            if ((localRunnerThread != null) && (localRunnerThread.isAlive())) {
                localRunnerThread.quitSafely();
                localRunnerThread.interrupt();
            }
            localRunnerThread = new HandlerThread("LocalTransferThread");
            localRunnerThread.start();
            localRunnerThreadHandler = new Handler(localRunnerThread.getLooper());
            localRunnerBusy = false;
            return;
        }

        if (serverIP == null) return;

        this.setupComm();
        this.startBackgroundThread();
    }

    int getPort() {
        // make sure there is a scheme before we use URI.create()
        String endpoint = this.serverIP;
        if (!endpoint.matches("^[a-zA-Z]+://.*$")) {
            endpoint = "ws://" + endpoint;
        }

        int port = URI.create(endpoint).getPort();
        if (port == -1) {
            return Const.PORT;
        }
        return port;
    }

    void setupComm() {
        int port = getPort();
        Log.d("CHECKPOINT: ", "Port_discovery ServerIP " + serverIP);
        Log.d("CHECKPOINT: ", "Port_discovery PORT: " + port);
        this.openrtistComm = OpenrtistComm.createOpenrtistComm(
                this.serverIP, port, this, this.returnMsgHandler, Const.TOKEN_LIMIT);
    }

    void setOpenrtistComm(OpenrtistComm openrtistComm) {
        this.openrtistComm = openrtistComm;
    }

    /**************** Convert imageformat to Byte arrays ***********************/
    /**
     * Convert imageformat YUV_420_888 to Byte arrays.
     */
    private static byte[] imageToByte(Image image) {
        byte[] byteArray = null;
        byteArray = NV21toJPEG(YUV420toNV21(image), image.getWidth(), image.getHeight(), 100);
        return byteArray;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(Image image) {
        byte[] nv21;
        // Get the three planes.
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();


        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    /**
     * Convert imageformat DEPTH16 to Byte arrays.
     */
    private static byte[] DEPTH16toBYTEs(Image image) {
        byte[] ans;
        // DEPTH16 has 1 plane.
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        int size = buffer.remaining();
        ans = new byte[size];
        buffer.get(ans, 0, size);
        return ans;
    }
    /**************** End of onItemSelected ****************/

    private Runnable fpsCalculator = new Runnable() {

        @Override
        public void run() {
            if(true){ //if(Const.SHOW_FPS) {
                if (fpsLabel.getVisibility() == View.INVISIBLE) {
                    fpsLabel.setVisibility(View.VISIBLE);

                }
                String msg= "FPS: " + framesProcessed;
                fpsLabel.setText( msg );
            }
            framesProcessed=0;
            fpsHandler.postDelayed(this, 1000);
        }
    };

    /**
     * Handles messages passed from streaming threads and result receiving threads.
     */
    Handler returnMsgHandler = new ReturnHandler(this);

    static class ReturnHandler extends Handler {
        private final WeakReference<GabrielClientActivity> mGabrielClientActivity;

        ReturnHandler(GabrielClientActivity gabrielClientActivity) {
            this.mGabrielClientActivity = new WeakReference<GabrielClientActivity>(
                    gabrielClientActivity);
        }

        public void handleMessage(Message msg) {
            final GabrielClientActivity gabrielClientActivity = this.mGabrielClientActivity.get();
            if (gabrielClientActivity == null) {
                return;
            }

            if (msg.what == NetworkProtocol.NETWORK_RET_FAILED) {
                //suppress this error when screen recording as we have to temporarily leave this
                // activity causing a network disruption
                if (!gabrielClientActivity.recordingInitiated) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            gabrielClientActivity, AlertDialog.THEME_HOLO_DARK);
                    builder.setMessage(msg.getData().getString("message"))
                            .setTitle(R.string.connection_error)
                            .setNegativeButton(R.string.back_button,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            gabrielClientActivity.finish();
                                        }
                                    }).setCancelable(false);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
            else if (msg.what == Const.REFERENCE_IMAGE) {
                if (!Const.STEREO_ENABLED) {
                    Bitmap refImage = null;
                    if (msg.obj != null) {
                        Log.v(LOG_TAG, "Setting reference image.");
                        refImage = (Bitmap) msg.obj;
                        gabrielClientActivity.iconView.setImageBitmap(refImage);
                    }
                    else {
                        gabrielClientActivity.iconView.setImageResource(
                                R.drawable.ic_question_mark);
                    }
                }
            }
            if (msg.what == NetworkProtocol.NETWORK_RET_IMAGE) {
                if (gabrielClientActivity.styleType.equals("none")) {
                    return;
                }

                if (!Const.STEREO_ENABLED && gabrielClientActivity.styleType.equals("?")) {
                    Spinner spinner = (Spinner)gabrielClientActivity.findViewById(R.id.spinner);
                    ((ArrayAdapter<String>) spinner.getAdapter()).notifyDataSetChanged();
                    gabrielClientActivity.styleType = "none";
                }

                gabrielClientActivity.cleared = false;
                Bitmap feedbackImg = (Bitmap) msg.obj;
                gabrielClientActivity.set_image(feedbackImg);
                gabrielClientActivity.framesProcessed++;
            }
        }
    }

    public void set_image(Bitmap feedbackImg) {
        if (Const.STEREO_ENABLED) {
            this.stereoView1 = (ImageView)this.findViewById(R.id.guidance_image1);
            this.stereoView1.setVisibility(View.VISIBLE);
            this.stereoView1.setImageBitmap(feedbackImg);
            this.stereoView2 = (ImageView)this.findViewById(R.id.guidance_image2);
            this.stereoView2.setVisibility(View.VISIBLE);
            this.stereoView2.setImageBitmap(feedbackImg);
        } else {
            this.imgView = (ImageView)this.findViewById(R.id.guidance_image);
            this.imgView.setVisibility(View.VISIBLE);
            this.imgView.setImageBitmap(feedbackImg);
        }
    }

    /**
     * Terminates all services.
     */
    private void terminate() {
        Log.v(LOG_TAG, "++terminate");

        isRunning = false;

        if ((localRunnerThread != null) && (localRunnerThread.isAlive())) {
            localRunnerThread.quitSafely();
            localRunnerThread.interrupt();
            localRunnerThread = null;
            localRunnerThreadHandler = null;
        }
        if (rs != null) {
            rs.destroy();
        }

        // Allow this.backgroundHandler to return if it is currently waiting on this.engineInputLock
        synchronized (this.engineInputLock) {
            this.engineInputLock.notify();
        }

        if (this.backgroundThread != null) {
            this.stopBackgroundThread();
        }

        if (this.openrtistComm != null) {
            this.openrtistComm.stop();
            this.openrtistComm = null;
        }
        if (preview != null) {

            reusedBuffer = null;
            preview = null;
        }
    }

    /**************** End of Camera Preview ****************/

    /**************** onItemSelected ***********************/

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        if(styleIds.get(position) == "none"){
            if(!Const.STYLES_RETRIEVED)
                styleType = "?";
            else
                styleType = "none";
            if(Const.STEREO_ENABLED) {
                stereoView1.setVisibility(View.INVISIBLE);
                stereoView2.setVisibility(View.INVISIBLE);
            } else {
                imgView.setVisibility(View.INVISIBLE);
                if(Const.DISPLAY_REFERENCE) {
                    iconView.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            styleType = styleIds.get(position);

            if(Const.STEREO_ENABLED) {
                if (stereoView1.getVisibility() == View.INVISIBLE) {
                    stereoView1.setVisibility(View.VISIBLE);
                    stereoView2.setVisibility(View.VISIBLE);
                }
            } else {
                if(Const.DISPLAY_REFERENCE) {
                    iconView.setVisibility(View.VISIBLE);
                }
                if (imgView.getVisibility() == View.INVISIBLE) {
                    imgView.setVisibility(View.VISIBLE);
                }
            }

            if (!styleType.equals("?") && runLocally) {
                localRunnerThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            localRunner.load(getApplicationContext(),
                                    String.format("%s.pt", styleType));
                        } catch (FileNotFoundException e) {
                            styleType = "none";
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
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {

    }

}
/**************** End of onItemSelected ****************/