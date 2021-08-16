package com.example.previewdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {

    private Button screen_btn;
    private Button camera_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera_btn = findViewById(R.id.camera_display_btn);
        screen_btn = findViewById(R.id.screen_display_btn);

        camera_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TAG","hello 点击了Camera采集");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CameraDisplayActivity.class);
                startActivity(intent);
//                MainActivity.this.finish();
            }
        });

        screen_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TAG","hello 点击了Screen采集");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ScreenDisplayActivity.class);
                startActivity(intent);
//                MainActivity.this.finish();
            }
        });
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}