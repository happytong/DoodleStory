package com.tongs.doodlestory;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import views.DrawView;

public class MainActivity extends AppCompatActivity {
    private String TAG="[dbg]MainActivity";
    static Button btnCreate, btnObserve, btnSetting;
    static public Point ScreenSize;
    CheckBox cbLoopFiles;
    static boolean bLoopDataFiles = false; // playback the data files one and another
    static TextView tvHelp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCreate = (Button) findViewById(R.id.btnCreate);
        btnObserve = (Button) findViewById(R.id.btnObserve);
        cbLoopFiles = findViewById(R.id.cbLoopFiles);
        btnSetting = (Button) findViewById(R.id.btnSettting);
        tvHelp = findViewById(R.id.tvHelp);
        tvHelp.setText(getResources().getString(R.string.text_help) + "\n"+ getResources().getString(R.string.app_name) +" " + BuildConfig.VERSION_NAME);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OperationCtrl.nState = OperationCtrl.STATE.CREATE_PATTERN;
                Intent intent = new Intent(MainActivity.this, CreatePattern.class);

                startActivity(intent);
            }
        });

        btnObserve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawView.Title ="";
                OperationCtrl.nState = OperationCtrl.STATE.PLAYBACK;
                Intent intent = new Intent(MainActivity.this, CreatePattern.class);
                startActivity(intent);
                Log.i(TAG, "btnObserve end "+ OperationCtrl.nState);
            }
        });

        cbLoopFiles.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Handle the checkbox state change
                if (isChecked) {
                    bLoopDataFiles = true;
                    if (DrawView.bPathAvailable) DrawView.SavePattern();

                } else {
                    bLoopDataFiles = false;
                }
            }
        });

        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, ConfigActivity.class);

                startActivity(intent);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        //move to CreatePattern which startup first
        Display display = getWindowManager().getDefaultDisplay();
        ScreenSize = new Point(0, 0);
        display.getSize(ScreenSize);

        if (!OperationCtrl.IsPatternAvailable())  OperationCtrl.nState= OperationCtrl.STATE.START_UP;

        UpdateButtons();

    }
    private void DisableButton(Button btn)
    {
        btn.setEnabled(false);
        btn.setBackgroundColor(Color.GRAY);
        btn.setTextColor(Color.WHITE);
    }
    private void HighlightButton(Button btn)
    {
        btn.setEnabled(true);
        btn.setBackgroundColor(Color.RED);
        btn.setTextColor(Color.WHITE);
    }
    private void UpdateButtons()
    {
        Log.i(TAG, "UpdateButtons: "+OperationCtrl.nState);
        btnCreate.setTextColor(Color.BLACK);

        btnSetting.setTextColor(Color.BLACK);
        btnCreate.setBackgroundColor(Color.GREEN);
        btnSetting.setBackgroundColor(Color.GREEN);

        switch (OperationCtrl.nState)
        {
            case START_UP:
                DisableButton(btnObserve);
                cbLoopFiles.setEnabled(false);
                break;

            default:
                HighlightButton(btnObserve);
                cbLoopFiles.setEnabled(true);
                if (bLoopDataFiles) cbLoopFiles.setChecked(true);
                else cbLoopFiles.setChecked(false);
                break;
        }
    }
}