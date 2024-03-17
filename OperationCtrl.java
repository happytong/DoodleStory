package com.tongs.doodlestory;

import android.graphics.Point;
import android.icu.util.Calendar;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Random;

import views.DrawView;

public class OperationCtrl {
    static private String TAG="[dbg]OperationCtrl";
    public enum STATE
    {
        START_UP,  //app just open, setting loaded
        READY_TO_PLAY, //with loaded points to playback
        CREATE_PATTERN, //draw the pattern onscreen to creating the pattern
        PLAYBACK, //animation for observing
        END_PLAYBACK,   //end of one round playback, remain 1s of final screen (if single color then just single color)
        PENDING_STOP,   //show original picture (colorful and relative width), show button to stop looping animation
    };
    static public STATE nState = STATE.START_UP;
    static public int nQuestionIndex = 0; //the pattern index of the answer (0-3)
    static long lObserveStartTime = 0, lAnswerStartTime=0;

    static public void Reset()
    {
        nState = STATE.START_UP;
        DrawView.alTouchPoints.clear();
        FlyingPaths.alBasePath.clear();
    }
    static public void GenerateTargetPattern()
    {

        FlyingPaths.SetBasePath(DrawView.alTouchPoints);

    }


    static boolean IsPatternAvailable()
    {
        return FlyingPaths.alBasePath.size() > 0;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    static public void StartObserveTime()
    {
        lObserveStartTime = Calendar.getInstance().getTimeInMillis();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    static public void StartAnswerTime()
    {
        lAnswerStartTime = Calendar.getInstance().getTimeInMillis()/1000;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    static public long GetObserveTime()
    {
        return Calendar.getInstance().getTimeInMillis() - lObserveStartTime;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    static public long GetAnswerTime()
    {
        return Calendar.getInstance().getTimeInMillis()/1000 - lAnswerStartTime;
    }
}
