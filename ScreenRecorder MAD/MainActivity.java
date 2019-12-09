package com.example.ether.screenrecordernew;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE=1000,REQUEST_PERMISSION=1001,DISPLAY_WIDTH=720,DISPLAY_HEIGHT=1280;
    private static final SparseIntArray spArry=new SparseIntArray();
    private int denseScree;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private  MediaProjectionCallback mediaProjectionCallBack;
    private MediaRecorder mediaRecorder;

    static {
        spArry.append(Surface.ROTATION_0,90);
        spArry.append(Surface.ROTATION_90,0);
        spArry.append(Surface.ROTATION_180,270);
        spArry.append(Surface.ROTATION_270,180);


    }

    private RelativeLayout rootLayout;
    private ToggleButton toggleButton;
    private VideoView videoView;
    private String vurl="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        denseScree=metrics.densityDpi;


        mediaRecorder=new MediaRecorder();
        mediaProjectionManager=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);


        videoView=(VideoView)findViewById(R.id.vvView);
        toggleButton=(ToggleButton)findViewById(R.id.tbButton);
        rootLayout=(RelativeLayout)findViewById(R.id.rootLayout);

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        +ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){

                    if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)||
                            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.RECORD_AUDIO)){

                        toggleButton.setChecked(false);
                        Snackbar.make(rootLayout,"Permission",Snackbar.LENGTH_INDEFINITE)
                                .setAction("Enable", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.RECORD_AUDIO
                                        },REQUEST_PERMISSION);
                                    }
                                }).show();

                    }

                    else{
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO
                        },REQUEST_PERMISSION);
                    }
                }

                else{
                    screenShare(v);
                }
            }
        });

    }

    private void screenShare(View v) {
        if(((ToggleButton)v).isChecked()){
            record();
            recordScreen();

        }
        else{
            mediaRecorder.stop();
            mediaRecorder.reset();
            stopRecordScreen();
            //videoView.setVisibility(View.VISIBLE);
            //videoView.setVideoURI(Uri.parse(vurl));
            //videoView.start();
        }
    }

    private void recordScreen() {

        if(mediaProjection==null){
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),REQUEST_CODE);
            return;
        }
        virtualDisplay=createVirtualDisplay();
        mediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay("MainActivity",DISPLAY_WIDTH,DISPLAY_HEIGHT,denseScree,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mediaRecorder.getSurface(),null,null);
    }

    private void record() {

        try {
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            vurl=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    + new StringBuilder("/MADproject_").append(new SimpleDateFormat("dd-MM-yyyy-hh_mm_ss")
                    .format(new Date())).append(".mp4").toString();

            mediaRecorder.setOutputFile(vurl);
            mediaRecorder.setVideoSize(DISPLAY_WIDTH,DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);

            mediaRecorder.setCaptureRate(20);
            mediaRecorder.setVideoFrameRate(20);

            int rotation=getWindowManager().getDefaultDisplay().getRotation();
            int orientation=spArry.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);

            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode!=REQUEST_CODE){
            Toast.makeText(this,"Error",Toast.LENGTH_LONG).show();
            return;
        }
        if(resultCode!=RESULT_OK){
            Toast.makeText(this,"Permission Denied",Toast.LENGTH_LONG).show();
            toggleButton.setChecked(false);
            return;
        }
        mediaProjectionCallBack=new MediaProjectionCallback();
        mediaProjection=mediaProjectionManager.getMediaProjection(resultCode,data);
        mediaProjection.registerCallback(mediaProjectionCallBack,null);
        virtualDisplay=createVirtualDisplay();
        mediaRecorder.start();
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {

            if(toggleButton.isChecked()){
                toggleButton.setChecked(false);
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
            mediaProjection=null;
            stopRecordScreen();
            super.onStop();
        }
    }

    private void stopRecordScreen() {

        if(virtualDisplay==null){
            return;
        }
        virtualDisplay.release();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if(mediaProjection!=null){
            mediaProjection.unregisterCallback(mediaProjectionCallBack);
            mediaProjection.stop();
            mediaProjection=null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_PERMISSION:{
                if((grantResults.length>0) && (grantResults[0]+grantResults[1]==PackageManager.PERMISSION_GRANTED)){
                    screenShare(toggleButton);
                }
                else{
                    toggleButton.setChecked(false);
                    Snackbar.make(rootLayout,"Permission",Snackbar.LENGTH_INDEFINITE)
                            .setAction("Enable", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.RECORD_AUDIO
                                    },REQUEST_PERMISSION);
                                }
                            }).show();
                }
                return;
            }
        }
    }
}
