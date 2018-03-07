package com.example.sucharita.cameraapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by SuCharita on 02/03/2018.
 */

public class PreviewVideoActivity extends AppCompatActivity {

    VideoView videoView;
    Button cancel, ok;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_preview_video);
        setTitle("Preview Video");

        videoView = (VideoView) findViewById(R.id.video);
        cancel = (Button) findViewById(R.id.btn_cancel);
        ok = (Button) findViewById(R.id.btn_ok);


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playVideo();
            }
        });

        final Uri videoUri = getIntent().getParcelableExtra("video");
        MediaController controller = new MyMediaController(videoView.getContext());
        controller.setMediaPlayer(videoView);
        videoView.setMediaController(controller);
        videoView.setVideoURI(videoUri);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Permissions.check(PreviewVideoActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        "Please Allow Permissions", new Permissions.Options()
                                .setSettingsDialogTitle("Warning!")
                                .setRationaleDialogTitle("Info"),
                        new PermissionHandler() {
                            @Override
                            public void onGranted() {
                                saveVideo(videoUri);
                            }

                            @Override
                            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                                Toast.makeText(context, "Some Permissions Denied", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

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

    void playVideo() {
        if (videoView.isPlaying()) return;
        videoView.start();
    }

    int fileNo = 1;

    private void saveVideo(Uri uri) {
        if(fileNo==4) fileNo = 1;

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
                if(dest.exists()) dest.delete();
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
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public int getFilesCount() {
        File childfile[] = new File(PathUtils.photoSavePath).listFiles();
        return childfile.length;
    }
}
