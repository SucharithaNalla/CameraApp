package com.example.sucharita.cameraapp;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

/**
 * Created by SuCharita on 03/03/2018.
 */

public class ComparePhotosActivity extends AppCompatActivity {

    ImageView first, second;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_photos);
        setTitle("Compare Photos");
        first = (ImageView) findViewById(R.id.first);
        second = (ImageView) findViewById(R.id.second);

        if (getIntent().getStringExtra("first") != null) {
            first.setImageBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("first")));
        }

        if (getIntent().getStringExtra("second") != null) {
            second.setImageBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("second")));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }
}
