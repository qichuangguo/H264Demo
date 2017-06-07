package com.android.cgcxy.customview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;



import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class VideoDecode extends AppCompatActivity {

    private ImageView iv;
    private long startTime =0;
    private String uriStr;
    private List<Bitmap> bitmaps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_decode);
        iv = (ImageView) findViewById(R.id.iv);
       init();
        uriStr = "android.resource://" + this.getPackageName() + "/"+ R.raw.bbb;
        System.out.println("==uriStr=="+ uriStr);

    }

    private void init() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String path = Environment.getExternalStorageDirectory().getPath()+"/aaa/bbb.mp4";
                /*String path1 = Uri.parse(uriStr).getPath();
                Uri parse = Uri.parse(path1);
                System.out.println("==path1==="+path1+"======="+parse.getScheme());*/
                File file = new File(path);
                System.out.println("====="+file.exists());
              //  File file = new File(uriStr);
                int data[] = new int[5];
                if (file.exists()){
                   // int i = AnbotJniUtil.initMp4File(path, data);
                    byte[] mPixel = new byte[data[0] * data[1]*2];
                    //ByteBuffer buffer = ByteBuffer.wrap(mPixel);
                    //Bitmap mVideoBit = Bitmap.createBitmap(data[0], data[1], Bitmap.Config.RGB_565);
                    while (true){
                        //buffer.rewind();
                       // mVideoBit.copyPixelsFromBuffer(buffer);
                        //buffer.clear();
                        int list[] = new int[5];
                        byte[] datas = mPixel;
                        //int result = AnbotJniUtil.readNextFrame(datas, list);
                        //final Bitmap bitmap = Bitmap.createBitmap(mVideoBit);
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(mPixel,0,list[1]);
                        if (bitmap!=null){
                           // bitmaps.add(bitmap);
                        }
                        Log.i("video", "run: ========");
                        if (bitmap!=null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                               iv.setImageBitmap(bitmap);

                                }
                            });
                        }else {

                            System.out.println("==bitmap==="+bitmap);
                        }
                       /* if (result==-1){
                            return;
                        }*/
                    }
                }
            }
        }).start();



    }
}
