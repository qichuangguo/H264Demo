package com.android.cgcxy.view;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import rd4.media.MediaJniUtil;

/**
 * Created by chuangguo.qi on 2017/6/7.
 */

public class MyVideoView extends RelativeLayout implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int VIDEO_WIDTH = 1920;
    private final static int VIDEO_HEIGHT = 1080;
    private final static int TIME_INTERNAL = 80;
    private byte[] data = new byte[1048*720*2];
    private String tag="MyVideoView";
    private boolean stop=true;
    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    public boolean finish=true;
    private MediaCodec mCodec;

    public MyVideoView(Context context) {
        this(context,null);
    }

    public MyVideoView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MyVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        surfaceView = new SurfaceView(getContext());
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(this);
        RelativeLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(surfaceView,params);

    }

    public void play(){

        new VideoPlayTask().execute();
    }

    public void initDecoder()  {

        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                VIDEO_WIDTH, VIDEO_HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, VIDEO_WIDTH*VIDEO_HEIGHT);
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 50);
//        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25000);
//        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
         mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2500000);
         mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);

        mCodec.configure(mediaFormat, surfaceView.getHolder().getSurface(),
                null, 0);
        mCodec.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(tag,"surfaceCreated");
        initDecoder();
        finish=true;
        stop= true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(tag,"surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(tag,"surfaceDestroyed");
        mCodec.stop();
        mCodec.release();
        mCodec = null;
        stop= false;
    }

    public class VideoPlayTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... params) {
            boolean b = MediaJniUtil.openVideo(2);
            System.out.println("===b=="+b);
            if (b){
                while (finish){
                    if (stop) {
                        int[] len = new int[6];
                        boolean video2 = MediaJniUtil.getVideo2(data, len);
                        if (video2) {
                            onFrame(data, 0, len[0]);
                            Log.i(tag, "===len===" + len[0] + "::" + len[1] + "::" + len[2] + "::" + len[3]);
                        } else {
                            Log.i(tag, "======video2::" + video2);
                        }
                        try {
                            Thread.sleep(0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return null;
        }
    }

    int mCount=0;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean onFrame(byte[] buf, int offset, int length) {
        Log.i("Media", "onFrame start");
        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(11000);

        Log.e("Media", "onFrame index:" + inputBufferIndex);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount
                    * TIME_INTERNAL, 0);
            mCount++;
        } else {
            return false;
        }

        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputBufferIndex >= 0) {

            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 11000);
        }

        Log.e("Media", "onFrame end");
        return true;
    }
}
