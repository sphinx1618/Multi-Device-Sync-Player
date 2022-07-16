package com.mc2022.template;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;

public class PartyClient extends AppCompatActivity {

    private static final int SOCKET_TIMEOUT = 5000;
    static String TAG = "Wifi Direct Broadcast";
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;

    private String host_ip;

    private Button pair_button;
    private EditText party_code;
    private ImageView Tick,Cross;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_client);

        pair_button = (Button) findViewById(R.id.pair_button);
        party_code = (EditText) findViewById(R.id.editTextPartyCode);

        Tick = (ImageView) findViewById(R.id.Tick);
        Cross = (ImageView) findViewById(R.id.cross);

        Tick.setVisibility(View.INVISIBLE);
        Cross.setVisibility(View.INVISIBLE);

        pair_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int ip = Integer.parseInt(party_code.getText().toString());
                host_ip = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
                new SocketAsyncTask().execute();
            }
        });



        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method

        }


    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();

    }


    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();


    }

    static  byte[] arr;
    static Socket socket;
    private class SocketAsyncTask extends AsyncTask<Void, Void, String> {


        @Override
        protected String doInBackground (Void... params) {

            Log.d(TAG,"connection success");
            // Create client socket


            try {
                // Create a client socket with the host, port, and timeout information.

                socket = new Socket();
                socket.bind(null);
                int port = 1618;
                socket.connect((new InetSocketAddress(host_ip, port)), SOCKET_TIMEOUT);
                Log.d(TAG, "Client connected socket - " + socket.isConnected());
                showToast("Pairing Done",getApplicationContext());
                TickDone();

                ObjectInputStream in;
                ObjectOutputStream out;

                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());

                arr = (byte [])in.readObject();
                Log.d(TAG,"Audio Received");
                Log.d(TAG,arr.toString());

            } catch (IOException|ClassNotFoundException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                showToast("Pairing Failed",getApplicationContext());
                TickNotDone();
                return "unsuccess";

            }


            return "Successful socket creation";
        }

        @Override
        protected void onPostExecute (String result) {
            super.onPostExecute(result);
            Log.d(PartyOwner.TAG,result);

            if(result.equals("Successful socket creation")){

                Toast.makeText(getApplicationContext(),"Audio received",Toast.LENGTH_LONG).show();
                Intent intent = new Intent(PartyClient.this,PlayerActivity.class);
                intent.putExtra("Owner",false);
                startActivity(intent);

            }

        }

    }

    public void showToast(final String toast, Context context)
    {
        runOnUiThread(() -> Toast.makeText(context, toast, Toast.LENGTH_SHORT).show());
    }

    public void TickDone()
    {
        runOnUiThread(() -> {
            Tick.setVisibility(View.VISIBLE);
            Cross.setVisibility(View.INVISIBLE);
        });
    }
    public void TickNotDone()
    {
        runOnUiThread(() -> {
            Tick.setVisibility(View.VISIBLE);
            Cross.setVisibility(View.INVISIBLE);
        });
    }





}