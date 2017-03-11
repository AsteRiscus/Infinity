package com.ankita.dja;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.MediaController;

//to control the media attributes
//A view containing controls for a MediaPlayer.
// Typically contains the buttons like "Play/Pause", "Rewind", "Fast Forward" and a progress slider.

public class MusicController extends MediaController {

    public MusicController(Context c){
        super(c);
    }

    public void hide(){}

}