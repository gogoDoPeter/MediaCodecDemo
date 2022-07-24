package com.infinite.mediacodecdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.infinite.mediacodecdemo.activity.DecodeActivity;
import com.infinite.mediacodecdemo.activity.EncodeActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


    }

    public void encodeVideo(View view) {
        Intent intent=new Intent(MainActivity.this, EncodeActivity.class);
        startActivity(intent);
    }

    public void decodeVideo(View view) {
        Intent intent=new Intent(MainActivity.this, DecodeActivity.class);
        startActivity(intent);
    }
}