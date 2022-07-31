package com.infinite.mediacodecdemo.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.infinite.mediacodecdemo.R;
import com.infinite.mediacodecdemo.util.PermissionHelper;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate +");
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        checkPermission();
        Log.d(TAG,"onCreate -");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    private void checkPermission() {
        Log.d(TAG,"checkPermission +");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //API30 Android 11
            String[] permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 200);
                    return;
                }
            }
            if (!Environment.isExternalStorageManager()) {// 判断有没有权限,如果没有权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //API23  Android 6
            String[] permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 200);
                    return;
                }
            }
        }
        Log.d(TAG,"checkPermission -");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG,"onRequestPermissionsResult +");
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, 200);
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //API30 Android 11
            if (!Environment.isExternalStorageManager()) {// 判断有没有权限,如果没有权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);
            }
        }
        Log.d(TAG,"onRequestPermissionsResult -");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"onActivityResult +");
        if (requestCode == 200 && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Log.d(TAG,"onActivityResult >= R version");
                if (!Environment.isExternalStorageManager()) {
                    Toast.makeText(this,"Get permission write storage fail",Toast.LENGTH_LONG).show();
                }
            }
            if (!PermissionHelper.hasCameraPermission(this)) {
                PermissionHelper.requestCameraPermission(this, true);
            }else if(!PermissionHelper.hasWriteStoragePermission(this)){
                PermissionHelper.requestWriteStoragePermission(this);
            }else{
                onResume();
            }
        }
        Log.d(TAG,"onActivityResult -");
    }

    public void encodeVideo(View view) {
        Intent intent = new Intent(MainActivity.this, EncodeActivity.class);
        startActivity(intent);
    }

    public void decodeVideo(View view) {
        Intent intent = new Intent(MainActivity.this, DecodeActivity.class);
        startActivity(intent);
    }

    public void syncEncodeVideo(View view) {
        Intent intent = new Intent(MainActivity.this, SyncEncodeActivity.class);
        startActivity(intent);
    }
    public void syncEncodeVideo2(View view) {
        Intent intent = new Intent(MainActivity.this, SyncEncodeActivity2.class);
        startActivity(intent);
    }
    public void asyncEncodeVideo(View view) {

    }


}