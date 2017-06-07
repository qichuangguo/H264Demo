package com.android.cgcxy.customview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.android.cgcxy.view.MediaController;

public class CustomCircleProgressActivity extends AppCompatActivity {

    private MediaController.CustomCircleProgress customCircleProgress;
    int  ss=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_circle_progress);

        customCircleProgress = (MediaController.CustomCircleProgress) findViewById(R.id.custom);
        customCircleProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customCircleProgress.setStatus();
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        while (ss<=100){
                            customCircleProgress.post(new Runnable() {
                                @Override
                                public void run() {
                                    customCircleProgress.setProgress(ss+=50);
                                    customCircleProgress.invalidate();
                                }
                            });


                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });

    }
}
