package com.android.cgcxy.customview;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceivingDelegate;
import com.vilyever.socketclient.helper.SocketClientSendingDelegate;
import com.vilyever.socketclient.helper.SocketConfigure;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.server.SocketServer;
import com.vilyever.socketclient.server.SocketServerClient;
import com.vilyever.socketclient.server.SocketServerDelegate;
import com.vilyever.socketclient.util.CharsetUtil;
import com.vilyever.socketclient.util.IPUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketActivity extends AppCompatActivity {
    SocketClient socketClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket);

        init02();
        init();

    }

    private void init02() {

        SocketServer s = new SocketServer();
        s.setCharsetName(CharsetUtil.UTF_8); // 设置编码为UTF-8
        boolean b = s.beginListen(21998);
        s.registerSocketServerDelegate(new SocketServerDelegate() {
            @Override
            public void onServerBeginListen(SocketServer socketServer, int port) {
                System.out.println("===port===="+port);
            }

            @Override
            public void onServerStopListen(SocketServer socketServer, int port) {

            }

            @Override
            public void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient) {
                byte[] bytes = new byte[1];
               // socketServerClient.sendString(bytes);
                socketServerClient.sendData(bytes);
                System.out.println("====onClientConnected========");
                socketServerClient.registerSocketClientDelegate(new SocketClientDelegate() {
                    @Override
                    public void onConnected(SocketClient client) {

                    }

                    @Override
                    public void onDisconnected(SocketClient client) {

                    }

                    @Override
                    public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {

                        System.out.println("===responsePacket==="+responsePacket.getMessage());
                    }
                });

            }

            @Override
            public void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient) {

            }
        });
        System.out.println("== b="+ b);
    }


    private void init() {
        socketClient = new SocketClient();
        socketClient.setCharsetName(CharsetUtil.UTF_8); // 设置编码为UTF-8
       // socketClient.getAddress().setRemoteIP(IPUtil.getLocalIPAddress(true)); // 远程端IP地址
        socketClient.getAddress().setRemoteIP("192.168.1.106"); // 远程端IP地址
        System.out.println("====001=="+IPUtil.getLocalIPAddress(true));
        socketClient.getAddress().setRemotePort("21998"); // 远程端端口号
        socketClient.getAddress().setConnectionTimeout(15 * 1000); // 连接超时时长，单位毫秒
        socketClient.connect();
        socketClient.registerSocketClientDelegate(new SocketClientDelegate() {
            @Override
            public void onConnected(SocketClient client) {
                System.out.println("===111====");
                client.sendString("sfasf");
            }

            @Override
            public void onDisconnected(SocketClient client) {

            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                byte[] data = responsePacket.getData();
                System.out.println("==="+data.length);
                String message = responsePacket.getMessage();
                System.out.println("==message="+message);
            }
        });

        socketClient.registerSocketClientSendingDelegate(new SocketClientSendingDelegate() {
            @Override
            public void onSendPacketBegin(SocketClient client, SocketPacket packet) {
                System.out.println("==onSendPacketBegin===");
            }

            @Override
            public void onSendPacketEnd(SocketClient client, SocketPacket packet) {
                System.out.println("==onSendPacketEnd===");
            }

            @Override
            public void onSendPacketCancel(SocketClient client, SocketPacket packet) {
                System.out.println("==onSendPacketCancel===");
            }

            @Override
            public void onSendingPacketInProgress(SocketClient client, SocketPacket packet, float progress, int sendedLength) {
                System.out.println("==onSendingPacketInProgress===");
            }
        });


    }
}
