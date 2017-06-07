package com.android.cgcxy.customview;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class TouchActivity extends AppCompatActivity implements View.OnClickListener {

    private Button button01;
    private Button button02;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch);

        button01 = (Button) findViewById(R.id.button01);
        button02 = (Button) findViewById(R.id.button02);

        button01.setOnClickListener(this);
        button02.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        if (v.getId()==R.id.button01){
            if (button02.getVisibility()==View.VISIBLE){
                button02.setVisibility(View.GONE);
            }else {
                button02.setVisibility(View.VISIBLE);
            }
        }else if (v.getId()==R.id.button02){

            if (button01.getVisibility()==View.VISIBLE){
                button01.setVisibility(View.GONE);
            }else {
                button01.setVisibility(View.VISIBLE);
            }
        }
    }
}
