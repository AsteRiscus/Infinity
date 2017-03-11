package com.ankita.dja;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;

import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;

import static android.content.ContentValues.TAG;

/**
 * Created by Ankita on 10-Mar-17.
 */

//Service is to be used for activities like network operation or playing mp3 in background
//They run in background & is not a separate process
    //service call from activity will block activity's as well as OS's UI as it works on the main thread
    //A service is used more for something that should happen on an interval or keep running/checking for something when there is no UI shown.
    // The Service runs on the main thread, and therefore you should offload the work to an AsyncTask
// or something like that inside the Service.
    // if your service is going to do any CPU intensive (such as MP3 playback) or blocking (such as networking)
// operations, it should spawn its own thread in which to do that work.

    //What makes a service object special is that it is registered with the Android system as a service.
// This let's the system know that this object provides some sort of service and should be kept alive as long as possible,
// or until it is stopped. Normal application threads do not have this special meaning to the Android system
// and will be terminated much more generously at the discretion of the system.

//So, if you need some background activities to go on only while your application/Activity is active, a thread can do what you need.

//If you need a component that will not be purged even when, after a while,
// the Android system decides to remove your Activities from memory, you should go for the service
//A service is the way to go if you want to provide some service(s) to other applications, which can "bind" to a service only.

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
AudioManager.OnAudioFocusChangeListener{

    //pass the list of songs into the Service class,
    // playing from it using the MediaPlayer class and keeping track of
    // the position of the current song using the songPosn instance variable.

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosn;

    private final IBinder musicBind = new MusicBinder();

    //A bound service is the server in a client-server interface.
    // It allows components (such as activities) to bind to the service, send requests, receive responses,
    // and perform interprocess communication (IPC).
    // A bound service typically lives only while it serves another application component and does not run in the background indefinitely.

    //can use JobScheduler to execute background services == recommended for API>5

    //A bound service is an implementation of the Service class that allows other applications to bind to it and interact with it.
    // To provide binding for a service, you must implement the onBind() callback method.
    // This method returns an IBinder object that defines the programming interface that clients can use to interact with the service.

    private String songTitle="";
    private static final int NOTIFY_ID=1;
    private boolean shuffle=false;
    private Random rand;

    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;


    @Override
    public void onAudioFocusChange(int focusChange) {

    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void onCreate(){
        //create the service
        super.onCreate();
        //initialize position
        songPosn=0;
        //create player
        player = new MediaPlayer();

        rand=new Random();
        mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener(){

            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Log.i(TAG, "AUDIOFOCUS_GAIN");
                        // Set volume level to desired levels
                        playSong();
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        Log.i(TAG, "AUDIOFOCUS_GAIN_TRANSIENT");
                        // You have audio focus for a short time
                        playSong();
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                        Log.i(TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
                        // Play over existing audio
                        playSong();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        Log.e(TAG, "AUDIOFOCUS_LOSS");
                        player.stop();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Log.e(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                        // Temporary loss of audio focus - expect to get it back - you can keep your resources around
                        pausePlayer();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        Log.e(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                        // Lower the volume
                        break;
                }
            }
        };


        initMusicPlayer();
    }

    // method to initialize the mediaplayer class & configure the music player

    public void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

       // The wake lock will let playback continue when the device becomes idle and
        // we set the stream type to music.

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(ArrayList<Song> theSongs){
        songs=theSongs;
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    public void playPrev(){
        songPosn--;
        if(songPosn<0) songPosn=songs.size()-1;
        playSong();
    }
    //skip to next
    public void playNext(){
        if(shuffle){
            int newSong = songPosn;
            while(newSong==songPosn){
                newSong=rand.nextInt(songs.size());
            }
            songPosn=newSong;
        }
        else {
            songPosn++;
            if (songPosn >= songs.size()) songPosn = 0;
        }
        playSong();
    }

    //shuffle
    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return musicBind;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition()<0){
            mp.reset();
            playNext();
        }

    }

    @Override
    public boolean onUnbind(Intent intent){
       //   player.stop();
       //  player.release();

        return false;
    }

    @Override
    public void onDestroy(){
        player.stop();  //for backpressed


        stopForeground(true);

    }
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        //start playback
        mp.start();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.ic_menu_send)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
        .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);

    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }

    public void playSong(){
        //play a song
        player.reset();
        //get song
        Song playSong = songs.get(songPosn);
        //get title
        songTitle=playSong.getTitle();
        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.prepareAsync();
    }



}
