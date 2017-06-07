package com.android.cgcxy.customview;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.android.cgcxy.view.MediaController;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttono1).setOnClickListener(this);
        findViewById(R.id.buttono2).setOnClickListener(this);
        findViewById(R.id.buttono3).setOnClickListener(this);
        findViewById(R.id.buttono5).setOnClickListener(this);
        findViewById(R.id.buttono4).setOnClickListener(this);
        findViewById(R.id.buttono6).setOnClickListener(this);
        findViewById(R.id.buttono7).setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id==R.id.buttono1){
            startActivity(new Intent(this,CustomCircleProgressActivity.class));
        }else if(id==R.id.buttono2){
            startActivity(new Intent(this,VideoDemo.class));
        }else if (id==R.id.buttono3){

            startActivity(new Intent(this,MediaController.MyCheckBox.class));
        }else if (id==R.id.buttono5){
            startActivity(new Intent(this,TouchActivity.class));
        }else if (id==R.id.buttono4){
            startActivity(new Intent(this,H264Activity.class));
        }else if (id==R.id.buttono6){

            startActivity(new Intent(this,VideoDecode.class));
        }else if (id==R.id.buttono7){
            startActivity(new Intent(this,SocketActivity.class));
        }

    }
}
