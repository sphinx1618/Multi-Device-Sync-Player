package com.mc2022.template;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;



import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import pl.droidsonroids.gif.GifImageView;


public class PlayerActivity extends AppCompatActivity {

    ImageView start,pause,Back,Forward;
    GifImageView play_anim;
    Socket clientsocketForclient;
    Date now;
    MediaPlayer mediaPlayer;
    Boolean connected;
    TimeInfo timeInfo;
    NTPUDPClient timeClient;
    InetAddress inetAddress;
    SeekBar progressBar;
    TextView progressTime;
    TextView totalTime;
    long offset;
    ArrayList<ObjectOutputStream> out;
    boolean owner;
    Thread t;
    boolean flag_timer;
    Thread playpause;



    public static final String TIME_SERVER = "time-a.nist.gov";
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        timeClient = new NTPUDPClient();

        flag_timer = true;


        start = (ImageView) findViewById(R.id.Play);
        pause = (ImageView) findViewById(R.id.Pause);
        progressBar = (SeekBar) findViewById(R.id.progressBar);
        progressTime = (TextView) findViewById(R.id.progressTime);
        totalTime = (TextView) findViewById(R.id.totalTime);
        play_anim = (GifImageView)  findViewById(R.id.playimg);

        Back = (ImageView) findViewById(R.id.bk);
        Forward = (ImageView) findViewById(R.id.fw);

        play_anim.setVisibility(View.INVISIBLE);
        Intent intent = getIntent();
        boolean owner = intent.getBooleanExtra("Owner", false);
        byte[] audioFile;

        out = new ArrayList<ObjectOutputStream>();


        if (!owner) {
            audioFile = PartyClient.arr;
            Toast.makeText(getApplicationContext(),Integer.toString(audioFile.length),Toast.LENGTH_LONG);
            start.setEnabled(false);
            start.setVisibility(View.VISIBLE);
            pause.setVisibility(View.INVISIBLE);
            pause.setEnabled(false);
            clientsocketForclient = PartyClient.socket;
            connected = true;
            Thread playpause = new Thread(){
                @Override
                public void run(){
                    PlayPauseSync();
                }
            };
            playpause.start();

        } else {

            audioFile = PartyOwner.myByteArray;
            start.setVisibility(View.VISIBLE);
            start.setEnabled(true);
            pause.setVisibility(View.INVISIBLE);
            pause.setEnabled(false);

        }
        mediaPlayer = new MediaPlayer();

        try {
            // create temp file that will hold byte array
            File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(audioFile);
            fos.close();

            // resetting mediaplayer instance to evade problems
            mediaPlayer.reset();

            // In case you run into issues with threading consider new instance like:
            // MediaPlayer mediaPlayer = new MediaPlayer();

            // Tried passing path directly, but kept getting
            // "Prepare failed.: status=0x1"
            // so using file descriptor instead
            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());

            mediaPlayer.prepare();
//            mediaPlayer.start();

//            mediaPlayer.seekTo();
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();

        }


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play_anim.setVisibility(View.VISIBLE);
                start.setVisibility(View.INVISIBLE);
                start.setEnabled(false);
                pause.setVisibility(View.VISIBLE);
                pause.setEnabled(true);
                Thread send_sync = new Thread(){
                    @Override
                    public void run(){
                        SendSync("play");

                    }
                };
                send_sync.start();
                try {
                    send_sync.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.pause();
                play_anim.setVisibility(View.INVISIBLE);
                start.setVisibility(View.VISIBLE);
                pause.setVisibility(View.INVISIBLE);
                pause.setEnabled(false);
                start.setEnabled(true);
                Thread send_sync = new Thread(){
                    @Override
                    public void run(){
                        SendSync("pause");

                    }
                };
                send_sync.start();
                try {
                    send_sync.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause.callOnClick();
                int pos=Math.max(mediaPlayer.getCurrentPosition()-15000,0);
                mediaPlayer.seekTo(pos);
                start.callOnClick();
            }
        });

        Forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pause.callOnClick();
                int pos=Math.min(mediaPlayer.getCurrentPosition()+15000, mediaPlayer.getDuration());
                mediaPlayer.seekTo(pos);
                start.callOnClick();
            }
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pause.callOnClick();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int i = seekBar.getProgress();
                mediaPlayer.seekTo((int) (mediaPlayer.getDuration()*i*0.01));
                start.callOnClick();
            }
        });


        t = new Thread() {

            public void run() {

                while (progressBar.getProgress() < 100 && flag_timer) {
                    runOnUiThread(new Runnable() {
                        public void run() {

                            int value = (mediaPlayer.getCurrentPosition() * 100) / (mediaPlayer.getDuration());
                            progressBar.setProgress(value);

                            long minutes = TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getCurrentPosition());
                            long seconds = TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getCurrentPosition());
                            seconds = seconds%60;
                            String progtime = Long.toString(minutes) + " : " + Long.toString(seconds);
                            progressTime.setText(progtime);

                            minutes = TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getDuration());
                            seconds = TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getDuration());
                            seconds = seconds%60;
                            String tottime = Long.toString(minutes) + " : " + Long.toString(seconds);
                            totalTime.setText(tottime);


                        }
                    });
                    try{
                        Thread.sleep(500);
                    }
                    catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }

                }
            }

        };
        t.start();

    }

    @Override
    public void onBackPressed() {

        flag_timer = false;

        if(owner){
            Thread send_sync = new Thread(){
                @Override
                public void run(){
                    SendSync("back");
                }
            };
            send_sync.start();
            try {
                send_sync.join();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        mediaPlayer.stop();
        super.onBackPressed();
    }


    public void SendSync(String str){
        String result;
        String op=str;


        try {
            inetAddress = InetAddress.getByName(TIME_SERVER);
            timeInfo = timeClient.getTime(inetAddress);
            timeInfo.computeDetails();
            offset = timeInfo.getOffset();
            Log.i("Offset",String.valueOf(offset));
            System.out.println("streams created");
            System.out.println("entering bytes");
            int pos = new Integer(mediaPlayer.getCurrentPosition());

            mediaPlayer.seekTo(pos);


            long val = System.currentTimeMillis()+offset + 5000;
            for(int i=0;i<PartyOwner.clientSockets.size();i++){
                ObjectOutputStream outy = new ObjectOutputStream(PartyOwner.clientSockets.get(i).getOutputStream());
                outy.writeObject(op);
                if(op.equals("play")) {

                    outy.writeObject(val);
                    outy.writeObject(pos);


                }
            }
            if(op.equals("play")){
                long curr = System.currentTimeMillis()+offset;

                Thread.sleep(val-curr);

                System.out.println("Client Time"+(System.currentTimeMillis()+offset));
                mediaPlayer.start();
            }





        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }
        result =  "Send Sync Successful";
        Log.d(PartyOwner.TAG, result);
    }

    public void back_click_jugaad()
    {
        runOnUiThread(() -> onBackPressed());
    }

    public void PlayPauseSync(){
        String result;

        while(connected) {
            try {
                // Create a client socket with the host, port, and timeout information.

                ObjectInputStream in;
//                ObjectOutputStream out;
                in = new ObjectInputStream(clientsocketForclient.getInputStream());
//                out = new ObjectOutputStream(clientsocketForclient.getOutputStream());

                String op = (String) in.readObject();
                if(op.equals("pause"))
                {
                    mediaPlayer.pause();
                    PlayDone();
                }

                else if(op.equals("back")){
                    back_click_jugaad();
                    connected=false;
                }
                else if(op.equals("play")) {

                    inetAddress = InetAddress.getByName(TIME_SERVER);
                    timeInfo = timeClient.getTime(inetAddress);
                    timeInfo.computeDetails();
                    offset = timeInfo.getOffset();
                    Log.i("Offset",String.valueOf(offset));
                    Long time = (Long) in.readObject();
                    int pos = (Integer) in.readObject();

                    mediaPlayer.seekTo(pos);

                    long curr = System.currentTimeMillis()+offset;

                    Thread.sleep(time-curr);


                    System.out.println("Client Time"+(System.currentTimeMillis()+offset));
                    mediaPlayer.start();
                    PauseDone();

                }


            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                Log.e("Player info", Objects.requireNonNull(e.getMessage()));
                result = "unsuccess";
            }
        }
        mediaPlayer.pause();
        mediaPlayer.reset();


        result =  "Successful socket creation";
        Log.d(PartyOwner.TAG,result);
    }

    public void PauseDone()
    {
        runOnUiThread(() -> {
            pause.setVisibility(View.VISIBLE);
            start.setVisibility(View.INVISIBLE);
        });
    }
    public void PlayDone()
    {
        runOnUiThread(() -> {
            start.setVisibility(View.VISIBLE);
            pause.setVisibility(View.INVISIBLE);
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mediaPlayer.release();
        connected=false;
    }



}