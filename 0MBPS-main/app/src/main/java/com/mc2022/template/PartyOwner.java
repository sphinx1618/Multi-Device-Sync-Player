package com.mc2022.template;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;


public class PartyOwner extends AppCompatActivity {

    private static final int SOCKET_TIMEOUT = 5000;
    static String TAG = "Wifi Direct Broadcast";


    String host_ip;
    String host_ip_map;
    private ArrayList<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private EditText party_code;
    private TextView file_selected;
    Button btnPickMedia;
    Button btnStartHosting;
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    private static final int REQUEST_PERMISSION = 1002;
    static ServerSocket serverSocket;
    static ArrayList<Socket> clientSockets;

    String lastSelectedMediaPath;
    File lastSelectedMediaFile;
    Uri lastSelectedUri;

    WifiManager wifiManager;



    Date now;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_owner);


        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);




        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        host_ip = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
        host_ip_map = Integer.toString(ip);
        Log.i("my_ip",host_ip);
        Log.i("my_ip",Integer.toString(ip));

        file_selected = (TextView) findViewById(R.id.selected_media);
        btnPickMedia = findViewById(R.id.btn_pickMedia);
        btnStartHosting = findViewById(R.id.btn_startHosting);
        party_code = (EditText) findViewById(R.id.editPartyCode);
        party_code.setText(host_ip_map);

        btnPickMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);

                activityResultLaunch.launch(intent);
            }
        });

        btnStartHosting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                System.out.println("Start Hosting");
                if(lastSelectedMediaFile != null && lastSelectedMediaPath != null) {
                    System.out.println("Start Hosting");
                    TransferMusic();
                    System.out.println("Execute called");
                } else {
                    showToast("Choose a File",getApplicationContext());

                    System.out.println("Sorry");
                }
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }


        Thread server = new Thread(){
            @Override
            public void run(){
                RunServer();
            }
        };
        server.start();


    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Save the user's current workout state
        savedInstanceState.putBoolean("server_socket",true);


        super.onSaveInstanceState(savedInstanceState);
    }

    public  String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    ActivityResultLauncher<Intent> activityResultLaunch = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        lastSelectedUri = result.getData().getData();


                        lastSelectedMediaPath = lastSelectedUri.getPath();
                        lastSelectedMediaFile = new File(lastSelectedUri.getPath());
                        String path = null;
                        try {
                            path = getPath(getApplicationContext(), lastSelectedUri);
                            File file = new File(path);
                            String filename = file.getName();
                            file_selected.setText(filename);

                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            showToast(e.toString(),getApplicationContext());
                        }


                    }
                }
            });


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
            }
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

    public void RunServer(){

        if(serverSocket ==null){
            try {
                serverSocket = new ServerSocket(1618);
                clientSockets = new ArrayList<Socket>();

            } catch (IOException e) {
                Log.e(TransferService.TAG, Objects.requireNonNull(e.getMessage()));

            }
        }


        while(true){


            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            String ipString = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
            System.out.println(ipString);
            Log.d(TransferService.TAG,"Checking.....");

            try {
                Socket socket = serverSocket.accept();
                clientSockets.add(socket);
                Log.d(TransferService.TAG,"socket accepted");
                showToast("Device added",getApplicationContext());

            } catch (IOException e) {
                Log.e(TransferService.TAG, Objects.requireNonNull(e.getMessage()));
                showToast(e.toString(),getApplicationContext());

            }

        }



    }

    public void showToast(final String toast, Context context)
    {
        runOnUiThread(() -> Toast.makeText(context, toast, Toast.LENGTH_SHORT).show());
    }
    static byte[] myByteArray;
    public  byte[] getBytesFromInputStream(InputStream is) throws IOException {
        System.out.println("function getBytes called");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);

        }
        System.out.println("Function ended");
        return os.toByteArray();
    }

    public void TransferMusic(){


        String result;

        ObjectInputStream in;

        try {

            InputStream inputStream = getContentResolver().openInputStream(lastSelectedUri);
            System.out.println("entering bytes");
            myByteArray = getBytesFromInputStream(inputStream);
            System.out.println("Got bytes");
            System.out.println("streams created");
            Thread tr = new Thread(){
                @Override
                public void run(){
                    ObjectOutputStream out;
                    for(Socket clientSocket:clientSockets){
                        try {
                            out = new ObjectOutputStream(clientSocket.getOutputStream());
                            out.writeObject(myByteArray);
                            showToast("Music Transferred",getApplicationContext());


                        } catch (IOException e) {
                            e.printStackTrace();
                            showToast(e.toString(),getApplicationContext());

                        }
                    }
                    Intent intent = new Intent(PartyOwner.this,PlayerActivity.class);

                    intent.putExtra("Owner",true);


                    startActivity(intent);


                }
            };
            tr.start();

        }
        catch (IOException e) {
            e.printStackTrace();
            showToast(e.toString(),getApplicationContext());

        }


    }

}

