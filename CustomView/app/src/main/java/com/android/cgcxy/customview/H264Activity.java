package com.android.cgcxy.customview;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.cgcxy.view.MyVideoView;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import rd4.media.MediaJniUtil;


public class H264Activity extends AppCompatActivity implements View.OnClickListener{
    private final String PATH = "vt.ecouser.net";
   // private final String PATH = "ecosphere-netsrv.ecovacs.cn";
    private Button button;
    private byte[] data = new byte[720 * 480];
    private ImageView imageView;
    private String tag="H264Activity";
    private MyVideoView myVideoView;
    private RelativeLayout relativeLayout;
    private FrameLayout fragmeng;
    private boolean isMax=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setContentView(R.layout.activity_h264);
        myVideoView = new MyVideoView(this);
        getSupportActionBar().hide();
        findViewById(R.id.button).setOnClickListener(this);
        button = (Button) findViewById(R.id.button02);
        button.setOnClickListener(this);
        imageView = (ImageView) findViewById(R.id.iv);
        relativeLayout = (RelativeLayout) findViewById(R.id.RelativeLayout);
        relativeLayout.addView(myVideoView);
        findViewById(R.id.button01).setOnClickListener(this);
        fragmeng = (FrameLayout) findViewById(R.id.fragmeng);
    }


    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.button){
            boolean init = MediaJniUtil.init(PATH, 5222, PATH, 3478, PATH, 5389);
            if (init){

                new LoginTask().execute();

            }else {
                showToast("初始化失败");
            }
        }else if(v.getId()==R.id.button02){
           myVideoView.play();
        }else if (v.getId()==R.id.button01){
            if (isMax) {
                relativeLayout.removeView(myVideoView);
                fragmeng.addView(myVideoView);
                isMax=false;

            }else {

                fragmeng.removeView(myVideoView);
                relativeLayout.addView(myVideoView);
                isMax=true;
            }

        }
    }

    public void showToast(String str){
        Toast.makeText(this,str,Toast.LENGTH_SHORT).show();
    }

    public class LoginTask extends AsyncTask<Void,Void,Boolean>{
        @Override
        protected Boolean doInBackground(Void... params) {
           boolean kvstream = MediaJniUtil.loginServer("00000000000"+91942, "123456","kvstream");
          // boolean kvstream = MediaJniUtil.loginServer("00000000000"+93837, "123456","kvstream");
            if (kvstream){
                boolean b = MediaJniUtil.bindTerm("E0000801016005490329", "E0000801016005490329");
                if (b){
                    return b;
                }else {
                    System.out.println("====绑定失败===");
                }
            }else {
                System.out.println("====登录服务器失败===");
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean){
                //initDecoder();
                showToast("登录成功");
                button.setEnabled(true);
            }
        }
    }

    // Video Constants



    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("===onDestroy==");
        myVideoView.setFinish(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaJniUtil.stopVideo();
                boolean unbind = MediaJniUtil.unbindTerm();
                boolean logout = MediaJniUtil.logoutServer();
                boolean release= MediaJniUtil.release();
            }
        }).start();



    }
}
