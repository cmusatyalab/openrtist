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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.renderscript.RenderScript;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.protobuf.Any;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.gabriel.camera.CameraCapture;
import edu.cmu.cs.gabriel.camera.YuvToJpegConverter;
import edu.cmu.cs.gabriel.camera.YuvToNv21Converter;
import edu.cmu.cs.gabriel.network.OpenrtistComm;
import edu.cmu.cs.gabriel.protocol.Protos;
import edu.cmu.cs.gabriel.util.Screenshot;
import edu.cmu.cs.localtransfer.LocalTransfer;
import edu.cmu.cs.localtransfer.Utils;
import edu.cmu.cs.openrtist.Protos.Extras;
import edu.cmu.cs.openrtist.R;

public class GabrielClientActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener {
    private static final String LOG_TAG = "GabrielClientActivity";
    private static final int REQUEST_CODE = 1000;
    private static final int DISPLAY_WIDTH = 640;
    private static final int DISPLAY_HEIGHT = 480;
    private static final int BITRATE = 1*1024*1024;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    // major components for streaming sensor data and receiving information
    String serverIP = null;
    private String styleType = "?";

    private OpenrtistComm openrtistComm;

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
    private ImageView camView2 = null;
    private ImageView iconView = null;
    private Handler iterationHandler = null;
    private Handler fpsHandler = null;
    private boolean imageRotate = false;
    private TextView fpsLabel = null;
    private boolean cleared = false;

    private int framesProcessed = 0;
    private YuvToNv21Converter yuvToNv21Converter;
    private YuvToJpegConverter yuvToRgbConverter;
    private CameraCapture cameraCapture;
    private PreviewView preview;

    private ArrayAdapter<String> spinner_adapter = null;
    private List<String> styleDescriptions = new ArrayList<>(Arrays.asList("Clear Display"));

    private List<String> styleIds = new ArrayList<>(Arrays.asList("none"));

    public void addStyles(Set<Map.Entry<String, String>> entrySet) {
        this.styleType = "none";
        for (Map.Entry<String, String> entry : entrySet) {
            Log.v(LOG_TAG, "style: " + entry.getKey() + ", desc: " + entry.getValue());
            styleDescriptions.add(entry.getValue().trim());
            styleIds.add(entry.getKey().trim());
        }
    }

    // local execution
    private boolean runLocally = false;
    private LocalTransfer localRunner = null;
    private HandlerThread localRunnerThread = null;
    private Handler localRunnerThreadHandler = null;
    private volatile boolean localRunnerBusy = false;
    private RenderScript rs = null;
    private Bitmap localRunnerBitmapCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        camView2 = (ImageView) findViewById(R.id.camera_preview2);
        imgView = (ImageView) findViewById(R.id.guidance_image);
        iconView = (ImageView) findViewById(R.id.style_image);
        fpsLabel = (TextView) findViewById(R.id.fpsLabel);

        if (Const.SHOW_RECORDER) {
            final ImageView recButton = (ImageView) findViewById(R.id.imgRecord);
            recButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (capturingScreen) {
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

        if (Const.ITERATE_STYLES) {
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

        if (!Const.STEREO_ENABLED) {
            final ImageView camButton = (ImageView) findViewById(R.id.imgSwitchCam);
            final ImageView rotateButton = (ImageView) findViewById(R.id.imgRotate);
            camButton.setVisibility(View.VISIBLE);
            camButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    camButton.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS);
                    if (Const.USING_FRONT_CAMERA) {
                        camButton.setImageDrawable(getResources().getDrawable(
                                R.drawable.ic_baseline_camera_front_24px));

                        cameraCapture = new CameraCapture(
                                GabrielClientActivity.this, analyzer, Const.IMAGE_WIDTH,
                                Const.IMAGE_HEIGHT, preview, CameraSelector.DEFAULT_BACK_CAMERA);

                        Const.USING_FRONT_CAMERA = false;
                        rotateButton.setVisibility(View.GONE);
                    } else {
                        camButton.setImageDrawable(getResources().getDrawable(
                                R.drawable.ic_baseline_camera_rear_24px));

                        cameraCapture = new CameraCapture(
                                GabrielClientActivity.this, analyzer, Const.IMAGE_WIDTH,
                                Const.IMAGE_HEIGHT, preview, CameraSelector.DEFAULT_FRONT_CAMERA);

                        Const.USING_FRONT_CAMERA = true;
                        rotateButton.setVisibility(View.VISIBLE);
                    }
                }
            });

            rotateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    imageRotate = !imageRotate;
                    Const.FRONT_ROTATION = !Const.FRONT_ROTATION;
                    // TODO: fix
                    // preview.setRotation(); will rotate the preview frame, but the image doesn't
                    // get rotated. Rotating 90 degrees will sho what I mean. Rotating 180 degrees
                    // does nothing because the image isn't changed.
                    rotateButton.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS);
                }
            });
        }

        if (Const.SHOW_FPS) {
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

    public void addFrameProcessed() {
        framesProcessed++;
    }

    private final Runnable fpsCalculator = new Runnable() {
        @Override
        public void run() {
            if (fpsLabel.getVisibility() == View.INVISIBLE) {
                fpsLabel.setVisibility(View.VISIBLE);

            }
            String msg= "FPS: " + framesProcessed;
            fpsLabel.setText( msg );

            framesProcessed = 0;
            fpsHandler.postDelayed(this, 1000);
        }
    };

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
                // wait until styles are retrieved before iterating
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
                                        android.R.style.Theme_Material_Light_Dialog_Alert);
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
//                    if (stereoView1.getVisibility() == View.INVISIBLE) {
//                        stereoView1.setVisibility(View.VISIBLE);
//                        stereoView2.setVisibility(View.VISIBLE);
//                    }
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
        if(Const.ITERATE_STYLES)
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

        // Media controller
        if (mediaController == null) {
            mediaController = new MediaController(this);
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
        if (Const.STEREO_ENABLED) {
            // TODO set preview
        } else {
            preview = findViewById(R.id.camera_preview);
        }

        yuvToNv21Converter = new YuvToNv21Converter();
        yuvToRgbConverter = new YuvToJpegConverter(this);

        cameraCapture = new CameraCapture(
                this, analyzer, Const.IMAGE_WIDTH, Const.IMAGE_HEIGHT,
                preview, CameraSelector.DEFAULT_BACK_CAMERA);
    }

    // Based on
    // https://github.com/protocolbuffers/protobuf/blob/master/src/google/protobuf/compiler/java/java_message.cc#L1387
    private static Any pack(edu.cmu.cs.openrtist.Protos.Extras extras) {
        return Any.newBuilder()
                .setTypeUrl("type.googleapis.com/openrtist.Extras")
                .setValue(extras.toByteString())
                .build();
    }

    private void localExecution(@NonNull ImageProxy image) {
        long st = SystemClock.elapsedRealtime();
        final float[] rgbImage = Utils.convertYuvToRgb(
                rs,
                yuvToNv21Converter.convertToBuffer(image).toByteArray(),
                image.getWidth(),
                image.getHeight()
        );
        Log.d(LOG_TAG, String.format("YuvToRGBA takes %d ms", SystemClock.elapsedRealtime() - st));

        localRunnerThreadHandler.post(() -> {
            localRunnerBusy = true;
            int[] output = localRunner.infer(rgbImage);
            // send results back to UI as Gabriel would
            if (localRunnerBitmapCache == null){
                localRunnerBitmapCache = Bitmap.createBitmap(
                        image.getWidth(),
                        image.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
            }
            localRunnerBitmapCache.setPixels(output, 0,
                    image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            imgView.post(() -> imgView.setImageBitmap(localRunnerBitmapCache));
            localRunnerBusy = false;
        });
    }

    private void sendFrameCloudlet(@NonNull ImageProxy image) {
        openrtistComm.sendSupplier(() -> {
            Extras extras = Extras.newBuilder().setStyle(styleType).build();

            Protos.InputFrame inputFrame = Protos.InputFrame.newBuilder()
                    .setPayloadType(Protos.PayloadType.IMAGE)
                    .addPayloads(yuvToRgbConverter.convertToJpeg(image))
                    .setExtras(GabrielClientActivity.pack(extras))
                    .build();

            return inputFrame;
        });
    }

    final private ImageAnalysis.Analyzer analyzer = new ImageAnalysis.Analyzer() {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            if (styleType.equals("?") || !styleType.equals("none")) {
                if (runLocally && !styleType.equals("?")) {
                    if (!localRunnerBusy) {
                        //local execution
                        localExecution(image);
                    }
                } else if (GabrielClientActivity.this.openrtistComm != null) {
                    sendFrameCloudlet(image);
                }
                runOnUiThread(() -> imgView.setVisibility(View.VISIBLE));
            } else if (!cleared) {
                Log.v(LOG_TAG, "Display Cleared");

                if (Const.STEREO_ENABLED) {
                    byte[] bytes = yuvToRgbConverter.convertToJpeg(image).toByteArray();
                    final Bitmap camView = BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.length);
//                    stereoView1.setVisibility(View.INVISIBLE);
//                    stereoView2.setVisibility(View.INVISIBLE);
                    runOnUiThread(() -> camView2.setImageBitmap(camView));
                } else {
                    runOnUiThread(() -> imgView.setVisibility(View.INVISIBLE));
                }
                cleared = true;
            }
            image.close();
        }
    };

    int getPort() {
        int port = URI.create(this.serverIP).getPort();
        if (port == -1) {
            return Const.PORT;
        }
        return port;
    }

    void setupComm() {
        int port = getPort();
        this.openrtistComm = OpenrtistComm.createOpenrtistComm(
                this.serverIP, port, this, this.iconView, this.imgView, Const.TOKEN_LIMIT);
    }

    // Used by measurement build variant
    void setOpenrtistComm(OpenrtistComm openrtistComm) {
        this.openrtistComm = openrtistComm;
    }

    public void showNetworkErrorMessage(String message) {
        //suppress this error when screen recording as we have to temporarily leave this
        // activity causing a network disruption
        if (!recordingInitiated) {
            this.runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        this, android.R.style.Theme_Material_Light_Dialog_Alert);
                builder.setMessage(message)
                        .setTitle(R.string.connection_error)
                        .setNegativeButton(R.string.back_button,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        GabrielClientActivity.this.finish();
                                    }
                                }).setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.show();
            });
        }
    }

    /**
     * Terminates all services.
     */
    private void terminate() {
        Log.v(LOG_TAG, "++terminate");

        if ((localRunnerThread != null) && (localRunnerThread.isAlive())) {
            localRunnerThread.quitSafely();
            localRunnerThread.interrupt();
            localRunnerThread = null;
            localRunnerThreadHandler = null;
        }
        if (rs != null) {
            rs.destroy();
        }

        if (this.openrtistComm != null) {
            this.openrtistComm.stop();
            this.openrtistComm = null;
        }
        cameraCapture.shutdown();
    }

    /**************** onItemSelected ***********************/

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        if (styleIds.get(position) == "none"){
            if (!Const.STYLES_RETRIEVED) {
                styleType = "?";
            } else {
                styleType = "none";
            }

            if(Const.STEREO_ENABLED) {
//                stereoView1.setVisibility(View.INVISIBLE);
//                stereoView2.setVisibility(View.INVISIBLE);
            } else {
                imgView.setVisibility(View.INVISIBLE);
                if (Const.DISPLAY_REFERENCE) {
                    iconView.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            styleType = styleIds.get(position);

            if(Const.STEREO_ENABLED) {
//                if (stereoView1.getVisibility() == View.INVISIBLE) {
//                    stereoView1.setVisibility(View.VISIBLE);
//                    stereoView2.setVisibility(View.VISIBLE);
//                }
            } else {
                if (Const.DISPLAY_REFERENCE) {
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

    /**************** End of onItemSelected ****************/
}
