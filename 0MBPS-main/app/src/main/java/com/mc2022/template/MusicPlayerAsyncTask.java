package com.mc2022.template;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class MusicPlayerAsyncTask extends AsyncTask<Void, Void, String> {

    byte[] arr;
    Context context;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    public MusicPlayerAsyncTask(byte[] arr,Context context){
        this.arr = arr;
        this.context = context;
        mediaPlayer = new MediaPlayer();
    }

    @Override
    protected String doInBackground (Void... params) {
        try {
            // create temp file that will hold byte array
            File tempMp3 = File.createTempFile("kurchina", "mp3", context.getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(arr);
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
            mediaPlayer.start();
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
            return "Song Failed";
        }
        return "Song played well";

    }

    @Override
    protected void onPostExecute (String result) {
        super.onPostExecute(result);
        Log.d(PartyOwner.TAG,result);
    }

}