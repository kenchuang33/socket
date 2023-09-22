package com.example.socket;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;


import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class socketserverchatroom extends AppCompatActivity {
    private Thread mThread = null;
    private TextView tvMessages;
    private EditText etMessage;
    private int SERVER_PORT = 7100; //傳送埠號
    private String message;
    private BufferedReader reader;
    private Socket socket;
    private String tmp;
    private PrintWriter out;
    private List<ClientHandler> clients = new ArrayList<>();

    private List<String> messages = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socketserverchatroom);
        TextView tvIP = findViewById(R.id.tvIP);
        TextView tvPort = findViewById(R.id.tvPort);
        TextView socketserverNameAccept = findViewById(R.id.socketserverNameaccept);
        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        Button btnSend = findViewById(R.id.btnSend);

        Intent receivedIntent = getIntent();
        String receivedData = receivedIntent.getStringExtra("servername");
        socketserverNameAccept.setText("Hi  "+receivedData);

        try {
            String SERVER_IP = getLocalIpAddress();
            tvIP.setText("IP: " + SERVER_IP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        tvMessages.setText("Not connected");
        tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
        mThread = new Thread(new serverThread());
        mThread.start();


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = etMessage.getText().toString();
                if (!message.isEmpty()) {
                    new Thread(new SendData(message)).start();
                }
            }
        });
    }


//    class serverThread implements Runnable {
//        @Override
//        public void run() {
//            try {
//                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
//
//
//                try {
//                    while(true) {
//                        socket = serverSocket.accept();
//
//
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                tvMessages.setText("Connected\n");
//                            }
//                        });
//                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                        //取得網路輸出串流
//                        out = new PrintWriter(new BufferedWriter(
//                                new OutputStreamWriter(socket.getOutputStream())),
//                                true);
//                        while ((tmp = reader.readLine()) != null) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    tvMessages.append("收 " + socket.getInetAddress().getHostAddress()
//                                            + ": " + tmp + "\n");
//                                }
//                            });
//                        }
//                    }
//
//
//
//                } catch (IOException e) {
//                    Log.d("edapple",e.getMessage());
//
//                    e.printStackTrace();
//
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    class serverThread implements Runnable {
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

                try {
                    while (true) {
                        socket = serverSocket.accept();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.setText("Connected\n");
                            }
                        });

                        // 为每个客户端连接创建一个新的线程
                        ClientHandler clientHandler = new ClientHandler(socket);
                        clients.add(clientHandler);
                        new Thread(clientHandler).start();
                    }
                } catch (IOException e) {
                    Log.d("edapple", e.getMessage());
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            clientSocket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream())), true);

                // 发送保存的消息给新客户端
                for (String message : messages) {
                    out.println(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String tmp = reader.readLine();
                    if (tmp == null) {
                        break; // 客户端断开连接
                    }

                    // 解析用户名和消息文本
                    int colonIndex = tmp.indexOf(":");
                    if (colonIndex != -1) {
                        final String senderName = tmp.substring(0, colonIndex);
                        final String message = tmp.substring(colonIndex + 2); // 加 2 是为了跳过冒号和空格

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append("收 " + senderName + ": " + message + "\n");
                            }
                        });

                        // 将消息保存到列表中
                        messages.add(tmp);

                        // 将消息广播给其他客户端
                        for (ClientHandler client : clients) {
                            if (client != this) {
                                client.out.println(tmp);
                            }
                        }
                    }
                }

                // 客户端断开连接，关闭相关资源
                reader.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



//    class SendData implements Runnable {
//        private String message;
//
//        SendData(String message) {
//            this.message = message;
//        }
//
//        @Override
//        public void run() {
//            if (out != null) {
//                out.println(message);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tvMessages.append("發 " + socket.getLocalAddress().getHostAddress()
//                                + ": " + message + "\n");
//                    }
//                });
//            } else {
//                Log.e("senderror",message);// 处理 out 为空的情况，可能需要日志记录或其他操作
//            }
//        }
//    }

    class SendData implements Runnable {
        private String message;


        SendData(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            for (ClientHandler client : clients) {
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(client.clientSocket.getOutputStream())), true);
                    out.println(message);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvMessages.append("發 " + client.clientSocket.getLocalAddress().getHostAddress()
                                    + ":"+ message + "\n");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
        mThread.interrupt();
    }
}