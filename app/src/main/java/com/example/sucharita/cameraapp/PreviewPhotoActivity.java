package com.example.sucharita.cameraapp;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;
import com.otaliastudios.cameraview.CameraUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by SuCharita on 02/03/2018.
 */

public class PreviewPhotoActivity extends AppCompatActivity {

    private static WeakReference<byte[]> image;
    static ImageView imageView;
    Button cancel, ok;
    Bitmap bitmap = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_preview_photo);

        imageView = (ImageView) findViewById(R.id.image);
        cancel = (Button) findViewById(R.id.btn_cancel);
        ok = (Button) findViewById(R.id.btn_ok);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Permissions.check(PreviewPhotoActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        "Please Allow Permissions", new Permissions.Options()
                                .setSettingsDialogTitle("Warning!")
                                .setRationaleDialogTitle("Info"),
                        new PermissionHandler() {
                            @Override
                            public void onGranted() {
                                saveImage(bitmap);
                            }

                            @Override
                            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                                Toast.makeText(context, "Some Permissions Denied", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        byte[] b = image == null ? null : image.get();
        if (b == null) {
            finish();
            return;
        }

        CameraUtils.decodeBitmap(b, 1000, 1000, new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
                PreviewPhotoActivity.this.bitmap = bitmap;
            }
        });
    }

    private void saveImage(Bitmap bitmap) {
        int fileNo = 1;
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
            while (dest.exists()) {
                fileNo++;
                if (fileNo == 5) {
                    fileNo = 1;
                    dest = new File(PathUtils.photoSavePath, PathUtils.photoName + fileNo + fileExtn);
                    dest.delete();
                } else
                    dest = new File(PathUtils.photoSavePath, PathUtils.photoName + fileNo + fileExtn);
            }

            dest.createNewFile();

            FileOutputStream fos = new FileOutputStream(dest);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "Picture Saved To : " + dest.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Log.d("save photo", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("save photo", "Error accessing file: " + e.getMessage());
        }
    }

    private static float getApproximateFileMegabytes(Bitmap bitmap) {
        return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024 / 1024;
    }

    public static void setImage(@Nullable byte[] im) {
        image = im != null ? new WeakReference<>(im) : null;
    }
}
