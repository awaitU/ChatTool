package com.awaitu.chatserver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    private final int UPDATE_MESSAGE_FROM_CLIENT = 1;
    private EditText inputMsg;
    private TextView mTextView;
    private boolean mLoop = true;
    private  Socket msocket;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_MESSAGE_FROM_CLIENT:
                    String message = (String)msg.obj;
                    if(!TextUtils.isEmpty(message)) {
                        mTextView.setText(message);
                    }
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputMsg = (EditText)findViewById(R.id.inputMsg);
        mTextView = (TextView)findViewById(R.id.tv_text);
        String tIP = getIp(this);
        setTitle("Server:"+tIP);
        init();
    }

    private void init() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        startServer();
                    }
                }.start();
            }
        }, 100);
    }

    private void startServer() {
        try {
            while (mLoop) {
                ServerSocket serverSocket = new ServerSocket(9988);
                msocket = serverSocket.accept();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reader(final Socket socket) {
        new Thread() {
            @Override
            public void run() {
                try {
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    String messageFromClient = dataInputStream.readUTF();
                    Message message = new Message();
                    message.what = UPDATE_MESSAGE_FROM_CLIENT;
                    message.obj = messageFromClient;
                    mHandler.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    public void submitMsg(View view){
        sendMessage();

    }
    private void sendMessage() {
        new Thread() {
            @Override
            public void run() {
                if (msocket != null) {
                    try {
                        String editText = inputMsg.getText().toString();
                        DataOutputStream writer = new DataOutputStream(msocket.getOutputStream());
                        writer.writeUTF(editText);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        Toast.makeText(this,"发送成功",Toast.LENGTH_SHORT).show();
    }
    public void readMsg(View view){
        reader(msocket);
    }
    /**
     * 获取IP
     * @param context
     * @return
     */
    public static String getIp(final Context context) {
        String ip = null;
        ConnectivityManager conMan = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        // mobile 3G Data Network
        android.net.NetworkInfo.State mobile = conMan.getNetworkInfo(
                ConnectivityManager.TYPE_MOBILE).getState();
        // wifi
        android.net.NetworkInfo.State wifi = conMan.getNetworkInfo(
                ConnectivityManager.TYPE_WIFI).getState();

        // 如果3G网络和wifi网络都未连接，且不是处于正在连接状态 则进入Network Setting界面 由用户配置网络连接
        if (mobile == android.net.NetworkInfo.State.CONNECTED
                || mobile == android.net.NetworkInfo.State.CONNECTING) {
            ip =  getLocalIpAddress();
        }
        if (wifi == android.net.NetworkInfo.State.CONNECTED
                || wifi == android.net.NetworkInfo.State.CONNECTING) {
            //获取wifi服务
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            //判断wifi是否开启
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            ip =(ipAddress & 0xFF ) + "." +
                    ((ipAddress >> 8 ) & 0xFF) + "." +
                    ((ipAddress >> 16 ) & 0xFF) + "." +
                    ( ipAddress >> 24 & 0xFF) ;
        }
        return ip;

    }

    /**
     *
     * @return 手机GPRS网络的IP
     */
    private static String getLocalIpAddress()
    {
        try {
            //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {//获取IPv4的IP地址
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }
    @Override
    public void finish() {
        super.finish();
        mLoop = false;
    }

}
