package com.android.cgcxy.customview;

import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.cgcxy.view.MediaController;

import java.io.File;


public class VideoDemo extends AppCompatActivity implements View.OnClickListener {

    private MediaController.VideoView videoView;
    private LinearLayout back;
    private TextView tv_title;
    private LinearLayout ll_title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_demo);
        getSupportActionBar().hide();

        videoView = (MediaController.VideoView) findViewById(R.id.VideoView);
        back = (LinearLayout) findViewById(R.id.back);
        tv_title = (TextView) findViewById(R.id.tv_title);
        ll_title = (LinearLayout) findViewById(R.id.ll_title);
        ll_title.setVisibility(View.GONE);
        back.setOnClickListener(this);

        String path = getFilePath()+"20170511145503.mp4";

        tv_title.setText("20170511145503.mp4");
        videoView.setVideoPath(path);
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController,ll_title);
        videoView.start();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                ll_title.setVisibility(View.VISIBLE);
            }
        });


    }



    public static String getFilePath(){

        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
            return sdDir.toString() + File.separator + "ecovacs" + File.separator + "dr935" + File.separator;
        } else {
            return "";
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id==R.id.back){

            finish();
        }
    }

    @Override
    protected void onResume() {
        if(getRequestedOrientation()!=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onResume();
    }
}
