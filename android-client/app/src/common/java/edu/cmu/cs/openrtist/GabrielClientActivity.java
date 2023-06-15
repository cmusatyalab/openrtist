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

package edu.cmu.cs.openrtist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.camera.CameraCapture;
import edu.cmu.cs.gabriel.camera.ImageViewUpdater;
import edu.cmu.cs.gabriel.camera.YuvToNV21Converter;
import edu.cmu.cs.gabriel.camera.YuvToJPEGConverter;
import edu.cmu.cs.gabriel.client.comm.ServerComm;
import edu.cmu.cs.gabriel.client.results.ErrorType;
import edu.cmu.cs.gabriel.network.OpenrtistComm;
import edu.cmu.cs.gabriel.network.StereoViewUpdater;
import edu.cmu.cs.gabriel.protocol.Protos.InputFrame;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.gabriel.util.Screenshot;
import edu.cmu.cs.localtransfer.LocalTransfer;
import edu.cmu.cs.localtransfer.Utils;
import edu.cmu.cs.openrtist.Protos.Extras;
import edu.cmu.cs.openrtist.R;

public class GabrielClientActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener {
    private static final String LOG_TAG = "GabrielClientActivity";
    private static final int DISPLAY_WIDTH = 640;
    private static final int DISPLAY_HEIGHT = 480;
    private static final int BITRATE = 1024 * 1024;
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
    private ImageView imgView;
    private ImageView iconView;
    private Handler iterationHandler;
    private Handler fpsHandler;
    private TextView fpsLabel;
    private PreviewView preview;

    // Stereo views
    private ImageView stereoView1;
    private ImageView stereoView2;

    private boolean cleared = false;

    private int framesProcessed = 0;
    private YuvToNV21Converter yuvToNV21Converter;
    private YuvToJPEGConverter yuvToJPEGConverter;
    private CameraCapture cameraCapture;

    private final List<String> styleDescriptions = new ArrayList<>(
            Collections.singletonList("Clear Display"));

    private final List<String> styleIds = new ArrayList<>(Collections.singletonList("none"));

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
    private Bitmap bitmapCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "++onCreate");
        super.onCreate(savedInstanceState);
        Const.STYLES_RETRIEVED = false;
        Const.ITERATION_STARTED = false;

        // Set ContentView based on the mode
        if (Const.STEREO_ENABLED) {
            setContentView(R.layout.activity_stereo);
        } else {
            setContentView(R.layout.activity_main);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                + WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imgView = findViewById(R.id.guidance_image);
        iconView = findViewById(R.id.style_image);
        fpsLabel = findViewById(R.id.fpsLabel);

        stereoView1 = findViewById(R.id.guidance_image1);
        stereoView2 = findViewById(R.id.guidance_image2);

        ImageView imgRecord =  findViewById(R.id.imgRecord);
        ImageView screenshotButton = findViewById(R.id.imgScreenshot);

        if (Const.SHOW_RECORDER) {
            imgRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (capturingScreen) {
                        imgRecord.setImageResource(R.drawable.ic_baseline_videocam_24px);
                        stopRecording();
                        MediaActionSound m = new MediaActionSound();
                        m.play(MediaActionSound.STOP_VIDEO_RECORDING);
                    } else {
                        recordingInitiated = true;
                        MediaActionSound m = new MediaActionSound();
                        m.play(MediaActionSound.START_VIDEO_RECORDING);

                        imgRecord.setImageResource(R.drawable.ic_baseline_videocam_off_24px);
                        initRecorder();
                        shareScreen();
                    }
                    imgRecord.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS);
                }
            });

            screenshotButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap b = Screenshot.takescreenshotOfRootView(imgView);
                    storeScreenshot(b,getOutputMediaFile(MEDIA_TYPE_IMAGE).getPath());
                    screenshotButton.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS);
                    }
            });
        } else if (!Const.STEREO_ENABLED){
            //this view doesn't exist when stereo is enabled (activity_stereo.xml)
            imgRecord.setVisibility(View.GONE);
            findViewById(R.id.imgScreenshot).setVisibility(View.GONE);
        }

        if (Const.STEREO_ENABLED) {
            if (Const.ITERATE_STYLES) {
                // artificially start iteration since we don't display
                // any buttons in stereo view
                Const.ITERATION_STARTED = true;

                iterationHandler = new Handler();
                iterationHandler.postDelayed(styleIterator, 100);
            }
        } else {
            Spinner spinner = findViewById(R.id.spinner);
            ImageView playPauseButton = findViewById(R.id.imgPlayPause);
            ImageView camButton = findViewById(R.id.imgSwitchCam);

            if (Const.ITERATE_STYLES) {
                playPauseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!Const.ITERATION_STARTED) {
                            Const.ITERATION_STARTED = true;
                            playPauseButton.setImageResource(R.drawable.ic_pause);

                            Toast.makeText(
                                    playPauseButton.getContext(),
                                    getString(R.string.iteration_started),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Const.ITERATION_STARTED = false;
                            playPauseButton.setImageResource(R.drawable.ic_play);
                            Toast.makeText(
                                    playPauseButton.getContext(),
                                    getString(R.string.iteration_stopped),
                                    Toast.LENGTH_LONG).show();
                        }
                        playPauseButton.performHapticFeedback(
                                android.view.HapticFeedbackConstants.LONG_PRESS);
                    }
                });

                spinner.setVisibility(View.GONE);
                iterationHandler = new Handler();
                iterationHandler.postDelayed(styleIterator, 100);
            } else {
                playPauseButton.setVisibility(View.GONE);

                ArrayAdapter<String> spinner_adapter = new ArrayAdapter<>(
                        this, R.layout.mylist, styleDescriptions);
                // Spinner click listener
                spinner.setOnItemSelectedListener(this);
                spinner.setAdapter(spinner_adapter);
            }

            camButton.setVisibility(View.VISIBLE);
            camButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    camButton.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS);
                    if (Const.USING_FRONT_CAMERA) {
                        camButton.setImageResource(R.drawable.ic_baseline_camera_front_24px);

                        cameraCapture = new CameraCapture(
                                GabrielClientActivity.this, analyzer, Const.IMAGE_WIDTH,
                                Const.IMAGE_HEIGHT, preview, CameraSelector.DEFAULT_BACK_CAMERA);

                        Const.USING_FRONT_CAMERA = false;
                    } else {
                        camButton.setImageResource(R.drawable.ic_baseline_camera_rear_24px);

                        cameraCapture = new CameraCapture(
                                GabrielClientActivity.this, analyzer, Const.IMAGE_WIDTH,
                                Const.IMAGE_HEIGHT, preview, CameraSelector.DEFAULT_FRONT_CAMERA);

                        Const.USING_FRONT_CAMERA = true;
                    }
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
        File imageFile = new File(path);

        try {
            MediaActionSound m = new MediaActionSound();
            m.play(MediaActionSound.SHUTTER_CLICK);
            OutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(imageFile)));
//            MediaScannerConnection.scanFile(context,
//                    new String[]{file.toString()},
//                    null, null);
            Toast.makeText(this, getString(R.string.screenshot_taken, path), Toast.LENGTH_LONG).show();
            out.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException when attempting to store screenshot", e);
        }
    }

    private final Runnable styleIterator = new Runnable() {
        private int position = 1;

        @Override
        public void run() {
            if(Const.STYLES_RETRIEVED && Const.ITERATION_STARTED) {
                // wait until styles are retrieved before iterating
                if (++position == styleIds.size()) {
                    position = 1;
                }
                styleType = styleIds.get(position);

                if (runLocally) {
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
        super.onResume();
        Log.v(LOG_TAG, "++onResume");

        initOnce();
        Intent intent = getIntent();
        serverIP = intent.getStringExtra("SERVER_IP");
        initPerRun(serverIP);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(LOG_TAG, "++onPause");

        if(iterationHandler != null) {
            iterationHandler.removeCallbacks(styleIterator);
        }

        if(capturingScreen) {
            stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "++onDestroy");

        if (iterationHandler != null) {
            iterationHandler.removeCallbacks(styleIterator);
        }

        if (capturingScreen) {
            stopRecording();
        }

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
            return null;
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

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() != RESULT_OK) {
                        Toast.makeText(GabrielClientActivity.this,
                                "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();

                        return;
                    }

                    mMediaProjection = mProjectionManager.getMediaProjection(
                            result.getResultCode(), result.getData());

                    mVirtualDisplay = createVirtualDisplay();
                    mMediaRecorder.start();
                    capturingScreen = true;
                    if (Const.ITERATE_STYLES) {
                        iterationHandler.postDelayed(styleIterator, 100 * Const.ITERATE_INTERVAL);
                    }
                }
            });

    private void shareScreen() {
        if (mMediaProjection == null) {
            activityResultLauncher.launch(mProjectionManager.createScreenCaptureIntent());
        }
    }

    private VirtualDisplay createVirtualDisplay() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(),
                null /* Callbacks */,
                null /* Handler */);
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
            preview = findViewById(R.id.camera_preview1);
        } else {
            preview = findViewById(R.id.camera_preview);
        }

        yuvToNV21Converter = new YuvToNV21Converter();
        yuvToJPEGConverter = new YuvToJPEGConverter(this);

        cameraCapture = new CameraCapture(
                this, analyzer, Const.IMAGE_WIDTH, Const.IMAGE_HEIGHT,
                preview, CameraSelector.DEFAULT_BACK_CAMERA);
    }

    // Based on
    // https://github.com/protocolbuffers/protobuf/blob/2f6a7546e4539499bc08abc6900dc929782f5dcd/src/google/protobuf/compiler/java/java_message.cc#L1374
    private static Any pack(Extras extras) {
        return Any.newBuilder()
                .setTypeUrl("type.googleapis.com/openrtist.Extras")
                .setValue(extras.toByteString())
                .build();
    }

    private void localExecution(@NonNull ImageProxy image) {
        long st = SystemClock.elapsedRealtime();
        final float[] rgbImage = Utils.convertYuvToRgb(
                rs,
                yuvToNV21Converter.convert(image).toByteArray(),
                image.getWidth(),
                image.getHeight()
        );
        Log.d(LOG_TAG, String.format("YuvToRGBA takes %d ms", SystemClock.elapsedRealtime() - st));

        localRunnerThreadHandler.post(() -> {
            localRunnerBusy = true;
            int[] output = localRunner.infer(rgbImage);
            // send results back to UI as Gabriel would
            if (bitmapCache == null){
                bitmapCache = Bitmap.createBitmap(
                        image.getWidth(),
                        image.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
            }
            bitmapCache.setPixels(
                    output, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            imgView.post(() -> imgView.setImageBitmap(bitmapCache));
            localRunnerBusy = false;
        });
    }

    private void sendFrameCloudlet(@NonNull ImageProxy image) {
        openrtistComm.sendSupplier(() -> {
            Extras extras = Extras.newBuilder().setStyle(styleType).build();

            return InputFrame.newBuilder()
                    .setPayloadType(PayloadType.IMAGE)
                    .addPayloads(yuvToJPEGConverter.convert(image))
                    .setExtras(GabrielClientActivity.pack(extras))
                    .build();
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
                if (Const.STEREO_ENABLED) {
                    runOnUiThread(() -> {
                        stereoView1.setVisibility(View.VISIBLE);
                        stereoView2.setVisibility(View.VISIBLE);
                    });
                } else {
                    runOnUiThread(() -> imgView.setVisibility(View.VISIBLE));
                }
            } else if (!cleared) {
                Log.v(LOG_TAG, "Display Cleared");

                if (Const.STEREO_ENABLED) {
                    runOnUiThread(() -> {
                        stereoView1.setVisibility(View.INVISIBLE);
                        stereoView2.setVisibility(View.INVISIBLE);
                    });
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

        Consumer<ByteString> imageViewUpdater = Const.STEREO_ENABLED
                ? new StereoViewUpdater(stereoView1, stereoView2)
                : new ImageViewUpdater(this.imgView);
        this.openrtistComm = OpenrtistComm.createOpenrtistComm(
                this.serverIP, port, this, this.iconView, imageViewUpdater, Const.TOKEN_LIMIT);
    }

    // Used by measurement build variant
    void setOpenrtistComm(OpenrtistComm openrtistComm) {
        this.openrtistComm = openrtistComm;
    }

    public void showNetworkErrorMessage(String message) {
        // suppress this error when screen recording as we have to temporarily leave this
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

    // **************** onItemSelected ***********************

    // Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        if (styleIds.get(position).equals("none")) {
            if (!Const.STYLES_RETRIEVED) {
                styleType = "?";
            } else {
                styleType = "none";
            }

            if (Const.STEREO_ENABLED) {
                stereoView1.setVisibility(View.INVISIBLE);
                stereoView2.setVisibility(View.INVISIBLE);
            } else {
                imgView.setVisibility(View.INVISIBLE);
                if (Const.DISPLAY_REFERENCE) {
                    iconView.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            styleType = styleIds.get(position);

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
                                    android.R.style.Theme_Material_Light_Dialog_Alert);
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

    // **************** End of onItemSelected ****************
}
