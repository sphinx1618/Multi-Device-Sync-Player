package com.mc2022.template;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

public class TransferService extends IntentService {

    static String TAG = "Wifi Direct Broadcast";

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_STRING = "sendString";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";



    public TransferService (String name) {
        super(name);
    }

    public TransferService () {
        super("TransferService");
    }



    @Override
    protected void onHandleIntent (Intent intent) {
        Context context = getApplicationContext();
        Log.d(TAG,"Transfer Service called");
        if (intent.getAction().equals(ACTION_SEND_STRING)) {
            Log.d(TAG,"Action send");
            String toSend = "string to send";

            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            Socket socket = null;
            DataOutputStream stream = null;
            try {
                // Create a client socket with the host, port, and timeout information.
                socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(TAG, "Client connected socket - " + socket.isConnected());

                // Send string
                stream = new DataOutputStream(socket.getOutputStream());
                stream.writeUTF(toSend);
                stream.close();
                Toast.makeText(context, "Sent! :)", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
