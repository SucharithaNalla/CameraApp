package com.example.sucharita.cameraapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.sucharita.helpers.MqttHelper;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.SessionType;
import com.otaliastudios.cameraview.Size;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import static com.example.sucharita.cameraapp.CameraPhotoActivity.setMessage;


public class CameraVideoActivity extends AppCompatActivity implements View.OnClickListener, ControlView.Callback {

    private CameraView camera;
    private ViewGroup controlPanel;
    private FrameLayout videoviewLayout;
    VideoView videoView;
    ImageButton photo1, video1;
    public CameraPhotoActivity cameraPhotoActivity;
    private MqttHelper mqttHelper;

    ImageView one, two, three, four;

    private boolean mCapturingVideo;

    String[] fileList = null;
    Bitmap bmThumbnail = null;

    Uri selectedUri = null;

    Chronometer chronometer;

    @Override
    protected void onStart() {
        super.onStart();
        if (checkPermissions()) {

        }
    }

    public boolean checkPermissions() {
        final boolean[] allowed = {false};
        Permissions.check(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                "Please Allow Permissions", new Permissions.Options()
                        .setSettingsDialogTitle("Warning!")
                        .setRationaleDialogTitle("Info"),
                new PermissionHandler() {
                    @Override
                    public void onGranted() {
                        allowed[0] = true;
                    }

                    @Override
                    public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                        allowed[0] = false;
                        Toast.makeText(context, "Some Permissions Denied", Toast.LENGTH_LONG).show();
                    }
                });
        return allowed[0];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_main_video);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);

        one = (ImageView) findViewById(R.id.one);
        two = (ImageView) findViewById(R.id.two);
        three = (ImageView) findViewById(R.id.three);
        four = (ImageView) findViewById(R.id.four);
        chronometer = (Chronometer) findViewById(R.id.chronometer);

        one.setOnClickListener(this);
        two.setOnClickListener(this);
        three.setOnClickListener(this);
        four.setOnClickListener(this);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                updateVideoThumbnails();
            }
        });
        t.start();

        videoviewLayout = (FrameLayout) findViewById(R.id.layout_videoveiw);
        videoView = (VideoView) findViewById(R.id.video);
        camera = findViewById(R.id.camera);
        camera.addCameraListener(new CameraListener() {
            public void onCameraOpened(CameraOptions options) {
                start();
                onOpened();
            }

            @Override
            public void onVideoTaken(File video) {
                super.onVideoTaken(video);
                onVideo(video);
            }
        });

        findViewById(R.id.edit).setOnClickListener(this);
        photo1 = (ImageButton) findViewById(R.id.cameraimg);
        photo1.setOnClickListener(this);
        video1 = (ImageButton) findViewById(R.id.captureVideo);
        video1.setOnClickListener(this);
        findViewById(R.id.toggleCamera).setOnClickListener(this);

        controlPanel = findViewById(R.id.controls);
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        Control[] controls = Control.values();
        for (Control control : controls) {
            ControlView view = new ControlView(this, control, this);
            group.addView(view, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        controlPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
                b.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

//        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
//            @Override
//            public void onChronometerTick(Chronometer chronometerChanged) {
//                chronometer = chronometerChanged;
//            }
//        });

        chronometer.setVisibility(View.GONE);
    }

    public void updateVideoThumbnails() {
        File videoFiles = new File(PathUtils.videoSavePath);
        Log.d("***video path***", videoFiles.toString());

        if (videoFiles.isDirectory()) {
            fileList = videoFiles.list();
        }
        if (fileList == null) {
            System.out.println("File does not exist");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "There are no videos available", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            System.out.println("fileList****************" + fileList);
            for (int i = 0; i < fileList.length; i++) {
                Log.d("Video:" + i + " File name", fileList[i]);
                bmThumbnail = ThumbnailUtils.createVideoThumbnail(PathUtils.videoSavePath + fileList[i],
                        MediaStore.Video.Thumbnails.MINI_KIND);

                ImageView imageView = null;

                if (i == 0) imageView = one;
                else if (i == 1) imageView = two;
                else if (i == 2) imageView = three;
                else if (i == 3) imageView = four;

                final ImageView finalImageView = imageView;
                final int finalI = i;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalImageView != null)
                            if (bmThumbnail != null) {
                                finalImageView.setImageBitmap(bmThumbnail);
                                if (finalI == 0)
                                    findViewById(R.id.one_preview).setVisibility(View.VISIBLE);
                                if (finalI == 1)
                                    findViewById(R.id.two_preview).setVisibility(View.VISIBLE);
                                if (finalI == 2)
                                    findViewById(R.id.three_preview).setVisibility(View.VISIBLE);
                                if (finalI == 3)
                                    findViewById(R.id.four_preview).setVisibility(View.VISIBLE);
                            } else {
                                finalImageView.setImageResource(R.drawable.novideo);
                                if (finalI == 0)
                                    findViewById(R.id.one_preview).setVisibility(View.GONE);
                                if (finalI == 1)
                                    findViewById(R.id.two_preview).setVisibility(View.GONE);
                                if (finalI == 2)
                                    findViewById(R.id.three_preview).setVisibility(View.GONE);
                                if (finalI == 3)
                                    findViewById(R.id.four_preview).setVisibility(View.GONE);
                            }
                    }
                });
            }
        }
    }

    private void message(String content, boolean important) {
        int length = important ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, content, length).show();
    }

    private void onOpened() {
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        for (int i = 0; i < group.getChildCount(); i++) {
            ControlView view = (ControlView) group.getChildAt(i);
            view.onCameraOpened(camera);
        }
    }

    private void onVideo(File video) {
        mCapturingVideo = false;

//        Intent intent = new Intent(CameraVideoActivity.this, PreviewVideoActivity.class);
//        intent.putExtra("video", Uri.fromFile(video));
//        startActivity(intent);
//        chronometer.setBase(SystemClock.elapsedRealtime());
//        chronometer.stop();
        saveVideo(Uri.fromFile(video));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit:
                edit();
                break;
            case R.id.cameraimg:
                camera();
                break;
            case R.id.captureVideo:
                captureVideo();
//                chronometer.setBase(SystemClock.elapsedRealtime());
//                chronometer.start();
                break;
            case R.id.toggleCamera:
                toggleCamera();
                break;
            case R.id.one:
                selectedUri = Uri.fromFile(new File(PathUtils.videoSavePath + PathUtils.videoName + "1.mp4"));
                videoviewLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.layout_option_video).setVisibility(View.GONE);
                playVideo(selectedUri);
                break;
            case R.id.two:
                selectedUri = Uri.fromFile(new File(PathUtils.videoSavePath + PathUtils.videoName + "2.mp4"));
                videoviewLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.layout_option_video).setVisibility(View.GONE);
                playVideo(selectedUri);
                break;
            case R.id.three:
                selectedUri = Uri.fromFile(new File(PathUtils.videoSavePath + PathUtils.videoName + "3.mp4"));
                videoviewLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.layout_option_video).setVisibility(View.GONE);
                playVideo(selectedUri);
                break;
            case R.id.four:
                selectedUri = Uri.fromFile(new File(PathUtils.videoSavePath + PathUtils.videoName + "4.mp4"));
                videoviewLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.layout_option_video).setVisibility(View.GONE);
                playVideo(selectedUri);
                break;
        }
    }

    private void camera() {
        startActivity(new Intent(CameraVideoActivity.this, CameraPhotoActivity.class));
    }

    void playVideo() {
        if (videoView.isPlaying()) return;
        videoView.start();
    }

    private void playVideo(Uri uri) {
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playVideo();
            }
        });

        MediaController controller = new MyMediaController(videoView.getContext());
        controller.setMediaPlayer(videoView);
        videoView.setMediaController(controller);
        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                ViewGroup.LayoutParams lp = videoView.getLayoutParams();
                float videoWidth = mp.getVideoWidth();
                float videoHeight = mp.getVideoHeight();
                float viewWidth = videoView.getWidth();
                lp.height = (int) (viewWidth * (videoHeight / videoWidth));
                videoView.setLayoutParams(lp);
                playVideo();
            }
        });
    }

    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        super.onBackPressed();
        finish();
    }

    private void edit() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void captureVideo() {
        if (camera.getSessionType() != SessionType.VIDEO) {
            message("Can't record video while session type is 'picture'.", false);
            return;
        }
        if (mCapturingVideo) return;
        mCapturingVideo = true;
        message("Recording for 8 seconds...", true);
        camera.startCapturingVideo(null, 8000);
    }

    private void toggleCamera() {
        switch (camera.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
                message("Switched to front camera!", false);
                break;
        }
    }

    @Override
    public boolean onValueChanged(Control control, Object value, String name) {
        if (!camera.isHardwareAccelerated() && (control == Control.WIDTH || control == Control.HEIGHT)) {
            if ((Integer) value > 0) {
                message("This device does not support hardware acceleration. " +
                        "In this case you can not change width or height. " +
                        "The view will act as WRAP_CONTENT by default.", true);
                return false;
            }
        }
        control.applyValue(camera, value);
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_HIDDEN);
        message("Changed " + control.getName() + " to " + name, false);
        return true;
    }

    //region Boilerplate

    @Override
    protected void onResume() {
        super.onResume();
        camera.start();
        updateVideoThumbnails();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.destroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isStarted()) {
            camera.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_photo) {
            startActivity(new Intent(CameraVideoActivity.this, CameraPhotoActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    int fileNo = 1;

    private void saveVideo(Uri uri) {
        String fileExtn = ".mp4";

        File file = new File(PathUtils.folderPath);
        if (!file.exists()) {
            file.mkdir();
        }

        file = new File(PathUtils.videoSavePath);
        if (!file.exists()) {
            file.mkdir();
        }

        FileInputStream fis = null;
        AssetFileDescriptor videoAsset = null;
        try {
            File dest = new File(PathUtils.videoSavePath, PathUtils.videoName + fileNo + fileExtn);
            if (getFilesCount() == 4) {
                fileNo++;
                if (fileNo == 5) fileNo = 1;

                if (dest.exists()) dest.delete();
            } else {
                while (dest.exists()) {
                    fileNo++;
                    if (fileNo == 5) {
                        fileNo = 1;
                        dest = new File(PathUtils.videoSavePath, PathUtils.videoName + fileNo + fileExtn);
                        dest.delete();
                    } else
                        dest = new File(PathUtils.videoSavePath, PathUtils.videoName + fileNo + fileExtn);
                }
            }

            dest.createNewFile();

            videoAsset = getContentResolver().openAssetFileDescriptor(uri, "r");
            fis = videoAsset.createInputStream();
            FileOutputStream outputStream = new FileOutputStream(dest);

            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            fis.close();
            outputStream.close();
            Toast.makeText(this, "Video Saved To : " + dest.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            updateVideoThumbnails();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public int getFilesCount() {
        File childfile[] = new File(PathUtils.photoSavePath).listFiles();
        if (childfile != null) {
            return childfile.length;
        } else return 0;
    }

    public void start() {
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("Debug", "Connected");
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug", mqttMessage.toString());
                if ("Camera".equals(mqttMessage.toString())) {
                    photo1.callOnClick();
                }
                if ("Video".equals(mqttMessage.toString())) {
                    video1.callOnClick();
                }
                Toast.makeText(getApplicationContext(), mqttMessage.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }
}