package com.android.cgcxy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by chuangguo.qi on 2017/6/1.
 */

public class TouchView extends RelativeLayout {

    private static final String TAG ="TouchView" ;
    private int lastX;
    private int lastY;
    private DisplayMetrics dm;
    private int screenWidth;
    private int screenHeight;
    private int l;
    private int b;
    private int r;
    private int t;
    private View childAt;

    public TouchView(Context context) {
        this(context,null);
    }

    public TouchView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TouchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels-50;

    }

    @Override
    protected void onLayout(boolean changed, int ll, int tt, int rr, int bb) {
        super.onLayout(changed, l, t, r, b);
        //layout(l,t,r,b);
        Log.i(TAG,"ll="+ll+"::::tt"+tt+"::::rr="+rr+"::::bb="+bb);
        //childAt.layout(l, t, r, b);

       // rootView.layout(l,t,r,b);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //获取触摸事件触摸位置的原始X坐标
            lastX = (int)event.getRawX();
            lastY = (int)event.getRawY();
            Log.i(TAG,"ACTION_DOWN");

        }else if (event.getAction() == MotionEvent.ACTION_MOVE){
            int dx=(int)event.getRawX()-lastX;
            int dy=(int)event.getRawY()-lastY;

            l = getLeft()+dx;
            b = getBottom()+dy;
            r = getRight()+dx;
            t = getTop()+dy;
            Log.i(TAG,"l="+ l +"b="+ b +"r="+ r +"t="+ t);

            //下面判断移动是否超出屏幕
            if(l <=0){
                l =0;
                r = l +getWidth();
            }

            if(t <=0){
                t =0;
                b = t +getHeight();
            }

            if(b >=screenHeight){
                b =screenHeight;
                t = b -getHeight();
            }

            if(r >=screenWidth){
                r =screenWidth;
                l = r -getWidth();
            }
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.getLayoutParams();
            params.setMargins(l, t, 0, 0);// 通过自定义坐标来放置你的控件
            //layout(l, t, r, b);
            setLayoutParams(params);
            lastX=(int)event.getRawX();
            lastY=(int)event.getRawY();
            //postInvalidate();
        }

        return true;
    }
}
