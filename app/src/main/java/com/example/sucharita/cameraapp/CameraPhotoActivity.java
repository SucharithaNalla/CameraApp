package com.example.sucharita.cameraapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.sucharita.helpers.MqttHelper;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.SessionType;
import com.otaliastudios.cameraview.Size;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;


public class CameraPhotoActivity extends AppCompatActivity implements View.OnClickListener, ControlView.Callback {

    private CameraView camera;
    private MqttHelper mqttHelper;
    private ViewGroup controlPanel;
    ImageButton photo, video;
    ImageView one, two, three, four;
    public static String message = "";

    private boolean mCapturingPicture;

    // To show stuff in the callback
    private Size mCaptureNativeSize;
    private long mCaptureTime;

    String[] fileList = null;
    Bitmap bmThumbnail = null;

    private static WeakReference<byte[]> image;

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
        setContentView(R.layout.activity_main_picture);

        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        one = (ImageView) findViewById(R.id.one);
        two = (ImageView) findViewById(R.id.two);
        three = (ImageView) findViewById(R.id.three);
        four = (ImageView) findViewById(R.id.four);

        one.setOnClickListener(this);
        two.setOnClickListener(this);
        three.setOnClickListener(this);
        four.setOnClickListener(this);

        updatePhotoThumbnails();

        camera = findViewById(R.id.camera);
        camera.addCameraListener(new CameraListener() {
            public void onCameraOpened(CameraOptions options) {
                startMqtt();
                onOpened();
            }

            public void onPictureTaken(byte[] jpeg) {
                onPicture(jpeg);
            }
        });

        findViewById(R.id.edit).setOnClickListener(this);
        photo = (ImageButton) findViewById(R.id.capturePhoto);
        photo.setOnClickListener(this);
        //  findViewById(R.id.toggleCamera).setOnClickListener(this);
        video = (ImageButton) findViewById(R.id.videoimg);
        video.setOnClickListener(this);

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
    }

    public void updatePhotoThumbnails() {
        File photoFiles = new File(PathUtils.photoSavePath);
        Log.d("***photo path***", photoFiles.toString());

        if (photoFiles.isDirectory()) {
            fileList = photoFiles.list();
        }
        if (fileList == null) {
            System.out.println("File does not exist");
            Toast.makeText(this, "There are no pictures taken", Toast.LENGTH_SHORT).show();
        } else {
            System.out.println("fileList****************" + fileList);
            for (int i = 0; i < fileList.length; i++) {
                Log.d("Photo:" + i + " File name", fileList[i]);

                bmThumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(PathUtils.photoSavePath + fileList[i]),
                        100, 100);

                ImageView imageView = null;

                if (i == 0) imageView = one;
                else if (i == 1) imageView = two;
                else if (i == 2) imageView = three;
                else if (i == 3) imageView = four;

                if (imageView != null)
                    if (bmThumbnail != null) {
                        imageView.setImageBitmap(bmThumbnail);
                    } else {
                        imageView.setImageResource(R.drawable.nomage);
                    }
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

    private void onPicture(byte[] jpeg) {
        mCapturingPicture = false;
        long callbackTime = System.currentTimeMillis();

        // This can happen if picture was taken with a gesture.
        if (mCaptureTime == 0) mCaptureTime = callbackTime - 300;
        if (mCaptureNativeSize == null) mCaptureNativeSize = camera.getPictureSize();

        setImage(jpeg);
//        PreviewPhotoActivity.setImage(jpeg);
//        Intent intent = new Intent(CameraPhotoActivity.this, PreviewPhotoActivity.class);
//        intent.putExtra("delay", callbackTime - mCaptureTime);
//        intent.putExtra("nativeWidth", mCaptureNativeSize.getWidth());
//        intent.putExtra("nativeHeight", mCaptureNativeSize.getHeight());
//        startActivity(intent);

        mCaptureTime = 0;
        mCaptureNativeSize = null;

        byte[] b = image == null ? null : image.get();
        if (b == null) {
            finish();
            return;
        }

        CameraUtils.decodeBitmap(b, 1000, 1000, new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                saveImage(bitmap);
            }
        });
    }

    public static void setImage(@Nullable byte[] im) {
        image = im != null ? new WeakReference<>(im) : null;
    }

    @Override
    public void onClick(View view) {

        if (getFilesCount() == 0) {
            findViewById(R.id.one).setEnabled(false);
            findViewById(R.id.two).setEnabled(false);
            findViewById(R.id.three).setEnabled(false);
            findViewById(R.id.four).setEnabled(false);
        } else if (getFilesCount() == 1) {
            findViewById(R.id.one).setEnabled(true);
            findViewById(R.id.two).setEnabled(false);
            findViewById(R.id.three).setEnabled(false);
            findViewById(R.id.four).setEnabled(false);
        } else if (getFilesCount() == 2) {
            findViewById(R.id.one).setEnabled(true);
            findViewById(R.id.two).setEnabled(true);
            findViewById(R.id.three).setEnabled(false);
            findViewById(R.id.four).setEnabled(false);
        } else if (getFilesCount() == 3) {
            findViewById(R.id.one).setEnabled(true);
            findViewById(R.id.two).setEnabled(true);
            findViewById(R.id.three).setEnabled(true);
            findViewById(R.id.four).setEnabled(false);
        } else if (getFilesCount() == 4) {
            findViewById(R.id.one).setEnabled(true);
            findViewById(R.id.two).setEnabled(true);
            findViewById(R.id.three).setEnabled(true);
            findViewById(R.id.four).setEnabled(true);
        }

        switch (view.getId()) {
            case R.id.edit:
                edit();
                isImageClicked = false;
                break;
            case R.id.videoimg:
                startActivity(new Intent(CameraPhotoActivity.this, CameraVideoActivity.class));
                isImageClicked = false;
                break;
            case R.id.capturePhoto:
                capturePhoto();
                isImageClicked = false;
                break;
            case R.id.toggleCamera:
                toggleCamera();
                isImageClicked = false;
                break;
            case R.id.one:
                if (getFilesCount() > 0)
                    if (findViewById(R.id.one_select).getVisibility() == View.GONE) {
                        findViewById(R.id.one_select).setVisibility(View.VISIBLE);
                        isImageClicked = true;
                        selected = PathUtils.photoSavePath + PathUtils.photoName + "1.jpg";
                    } else {
                        findViewById(R.id.one_select).setVisibility(View.GONE);
                        isImageClicked = false;
                    }
                break;
            case R.id.two:
                if (getFilesCount() > 1)
                    if (findViewById(R.id.two_select).getVisibility() == View.GONE) {
                        findViewById(R.id.two_select).setVisibility(View.VISIBLE);
                        isImageClicked = true;
                        selected = PathUtils.photoSavePath + PathUtils.photoName + "2.jpg";
                    } else {
                        findViewById(R.id.two_select).setVisibility(View.GONE);
                        isImageClicked = false;
                    }
                break;
            case R.id.three:
                if (getFilesCount() > 2)
                    if (findViewById(R.id.three_select).getVisibility() == View.GONE) {
                        findViewById(R.id.three_select).setVisibility(View.VISIBLE);
                        isImageClicked = true;
                        selected = PathUtils.photoSavePath + PathUtils.photoName + "3.jpg";
                    } else {
                        findViewById(R.id.three_select).setVisibility(View.GONE);
                        isImageClicked = false;
                    }
                break;
            case R.id.four:
                if (getFilesCount() > 3)
                    if (findViewById(R.id.four_select).getVisibility() == View.GONE) {
                        findViewById(R.id.four_select).setVisibility(View.VISIBLE);
                        isImageClicked = true;
                        selected = PathUtils.photoSavePath + PathUtils.photoName + "4.jpg";
                    } else {
                        findViewById(R.id.four_select).setVisibility(View.GONE);
                        isImageClicked = false;
                    }
                break;
        }
        if (isImageClicked) {
            count++;
            if (count == 1) {
                first = selected;
            } else if (count == 2) {
                second = selected;
                count = 0;
                Intent intent = new Intent(CameraPhotoActivity.this, ComparePhotosActivity.class);
                intent.putExtra("first", first);
                intent.putExtra("second", second);

                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        findViewById(R.id.one_select).setVisibility(View.GONE);
                        findViewById(R.id.two_select).setVisibility(View.GONE);
                        findViewById(R.id.three_select).setVisibility(View.GONE);
                        findViewById(R.id.four_select).setVisibility(View.GONE);
                    }
                }, 200);

                isImageClicked = false;
                startActivity(intent);
            }
        } else {
            count--;
        }
    }

    String first = null, second = null, selected = null;
    int count = 0;
    boolean isImageClicked = false;

    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        super.onBackPressed();
        this.finish();
    }

    private void edit() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void capturePhoto() {
        if (mCapturingPicture) return;
        mCapturingPicture = true;
        mCaptureTime = System.currentTimeMillis();
        mCaptureNativeSize = camera.getPictureSize();
        message("Capturing picture...", false);
        camera.capturePicture();
    }

    private void toggleCamera() {
        if (mCapturingPicture) return;
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
        updatePhotoThumbnails();
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
        getMenuInflater().inflate(R.menu.video, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_video) {
            startActivity(new Intent(CameraPhotoActivity.this, CameraVideoActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    int fileNo = 1;

    private void saveImage(Bitmap bitmap) {

        String fileExtn = ".jpg";

        File file = new File(PathUtils.folderPath);
        if (!file.exists()) {
            file.mkdir();
        }

        file = new File(PathUtils.photoSavePath);
        if (!file.exists()) {
            file.mkdir();
        }

        File dest = new File(PathUtils.photoSavePath, PathUtils.photoName + fileNo + fileExtn);
        try {
            if (getFilesCount() == 4) {
                fileNo++;
                if (fileNo == 5) fileNo = 1;
                if (dest.exists()) dest.delete();
            } else {
                while (dest.exists()) {
                    fileNo++;
                    if (fileNo == 5) {
                        fileNo = 1;
                        dest = new File(PathUtils.photoSavePath, PathUtils.photoName + fileNo + fileExtn);
                        dest.delete();
                    } else
                        dest = new File(PathUtils.photoSavePath, PathUtils.photoName + fileNo + fileExtn);
                }
            }

            dest.createNewFile();

            FileOutputStream fos = new FileOutputStream(dest);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "Picture Saved To : " + dest.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            updatePhotoThumbnails();
        } catch (FileNotFoundException e) {
            Log.d("save photo", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("save photo", "Error accessing file: " + e.getMessage());
        }
    }

    public int getFilesCount() {
        File childfile[] = new File(PathUtils.photoSavePath).listFiles();
        if (childfile != null) {
            return childfile.length;
        } else return 0;
    }

    private void startMqtt() {
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
//                message = mqttMessage.toString();
                setMessage(mqttMessage.toString());
                Toast.makeText(getApplicationContext(), mqttMessage.toString(), Toast.LENGTH_LONG).show();
                if("Camera".equals(getMessage())){
                    Toast.makeText(getApplicationContext(), mqttMessage.toString(), Toast.LENGTH_LONG).show();
                    photo.callOnClick();
                }
                if("Video".equals(getMessage())){
                    Toast.makeText(getApplicationContext(), "VIDEO ENABLED", Toast.LENGTH_SHORT).show();
                    video.callOnClick();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    public static String getMessage() {
        return message;
    }

    public static void setMessage(String message) {
        CameraPhotoActivity.message = message;
    }
}