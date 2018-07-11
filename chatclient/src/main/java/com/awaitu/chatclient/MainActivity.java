package com.awaitu.chatclient;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mBtnSendMsg;
    private Button saveIPAddress;
    private TextView socketStatus;
    private EditText sendMsg;
    private EditText clientIP;
    private TextView showMsg;
    private Socket mSocket;
    private Button buildConnect;
    private Button btn_get_message;
    private final int UPDATE_MESSAGE_FROM_CLIENT = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_MESSAGE_FROM_CLIENT:
                    String message = (String) msg.obj;
                    if (!TextUtils.isEmpty(message)) {
                        showMsg.setText(message);
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
        initViews();
    }

    private void initViews() {
        sendMsg = (EditText) this.findViewById(R.id.sendMsg);
        clientIP = (EditText) this.findViewById(R.id.clientIP);
        showMsg = (TextView) this.findViewById(R.id.showMsg);
        socketStatus = (TextView) this.findViewById(R.id.socketStatus);
        mBtnSendMsg = (Button) this.findViewById(R.id.btn_send_message);
        saveIPAddress = (Button) this.findViewById(R.id.saveIPAddress);
        buildConnect = (Button) this.findViewById(R.id.buildConnect);
        btn_get_message = (Button) this.findViewById(R.id.btn_get_message);
        btn_get_message.setOnClickListener(this);
        mBtnSendMsg.setOnClickListener(this);
        saveIPAddress.setOnClickListener(this);
        buildConnect.setOnClickListener(this);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send_message:
                String editText = sendMsg.getText().toString();
                if (TextUtils.isEmpty(editText)) {
                    sendMsg.setError("请您先输入数据");
                } else {
                    SharedPreferences sp = getSharedPreferences("ipKey", MODE_PRIVATE);
                    String ip = sp.getString("ip", null);
                    if (TextUtils.isEmpty(ip)) {
                        Toast.makeText(this, "您没有保存过服务端的IP地址", Toast.LENGTH_SHORT).show();
                    } else {
                        sendMessage();
                    }
                }
                break;
            case R.id.saveIPAddress:
                String ipAddress = clientIP.getText().toString();
                if (TextUtils.isEmpty(ipAddress)) {
                    clientIP.setError("请输入服务端IP");
                } else {
                    SharedPreferences msp = getSharedPreferences("ipKey", MODE_PRIVATE);
                    msp.edit().putString("ip", ipAddress).apply();
                }
                break;
            case R.id.buildConnect:
                connectServer();
                break;
            case R.id.btn_get_message:
                reader(mSocket);
                break;
        }

    }

    private void connectServer() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            SharedPreferences sp = getSharedPreferences("ipKey", MODE_PRIVATE);
                            String ip = sp.getString("ip", null);
                            mSocket = new Socket(ip, 9988);
                            equalSocketStatus();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }.start();
            }

        }, 100);
    }
    public void equalSocketStatus(){
        if(mSocket.isConnected()){
            socketStatus.setText(R.string.socketTrue);
        }else {
            socketStatus.setText(R.string.socketFalse);
        }

    }
    private void sendMessage() {
        new Thread() {
            @Override
            public void run() {
                if (mSocket != null) {
                    try {
                        String editText = sendMsg.getText().toString();
                        DataOutputStream writer = new DataOutputStream(mSocket.getOutputStream());
                        writer.writeUTF(editText);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        Toast.makeText(this, "发送成功", Toast.LENGTH_SHORT).show();
    }
}
