package com.tongs.doodlestory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

import views.DrawView;

public class CreatePattern extends AppCompatActivity {
    public String TAG = "[dbg]CreatePattern";
    static DrawView drawView;
    static Button btnUndo, btnReset, btnConfirm, btnSave, btnLoad, btnExport;
    static LinearLayout layoutDraw;
    static TextView tvCountDown, tvThick, tvThin, tvHideCtrl, tvUndoSimple, tvInfo, tvGrid;
    static int nAlphaTvInfo = 0xff;
    static TextView tvLoadPrev, tvLoadNext;
    static private SeekBar seekBar;
    static private int nSeekBarProgress=0; // trying to solve inconsistent result of seekbar moving for Samsung phone
    static private boolean bHideCtrl = false; //true for full screen for drawing
    static public int nDrawColor = Color.RED;
    static public int nDrawThickness = 10;
    static public int nGrid = 0; //0: no grid; 1: 4 cells; 2: ...
    static private boolean bLoopFilesDelay = true; // move here from the runable thread
    static final Handler handler = new Handler(); //to prevent multiple timers
    private static boolean settingsLoaded = false; // only load when app starts up

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //app startup and can draw directly
        if (!settingsLoaded){ // only load at starts up
            settingsLoaded = true;
            ConfigActivity.settings = SettingsManager.loadSetting(this);
            Log.i(TAG, "onCreate " + OperationCtrl.nState + " changing at STARTUP, C="+ConfigActivity.settings.getTextColor()
                    + ": "+ ConfigActivity.settings.getText());
        }

        if (OperationCtrl.nState == OperationCtrl.STATE.START_UP) OperationCtrl.nState = OperationCtrl.STATE.CREATE_PATTERN;

        setContentView(R.layout.activity_create_pattern);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        layoutDraw = findViewById(R.id.groupDraw);
        tvThick = findViewById(R.id.tvThickness);
        tvThin = findViewById(R.id.tvThin);
        tvGrid = findViewById(R.id.tvGrid);
        tvHideCtrl = findViewById(R.id.tvHideCtrl);
        tvUndoSimple = findViewById(R.id.tvUndoSimple);
        tvLoadPrev = findViewById(R.id.tvLoadPrev);
        tvLoadNext = findViewById(R.id.tvLoadNext);
        tvCountDown = findViewById(R.id.tvWarning);
        drawView = (DrawView) findViewById(R.id.mDrawView);
        btnUndo = (Button) findViewById(R.id.btnUndo);
        btnLoad = (Button) findViewById(R.id.btnLoad);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnExport = (Button) findViewById(R.id.btnExport);

        drawView.setConext(this);

        nDrawColor = ConfigActivity.settings.getTextColor();
        tvInfo = (TextView) findViewById(R.id.tvInfoTop);
        ShowTvInfo();

        int nLocation = RelativeLayout.ALIGN_PARENT_BOTTOM;
        switch (ConfigActivity.settings.getTextLocation())
        {
            case 0: nLocation = Gravity.TOP| Gravity.LEFT; break;
            case 1: nLocation = Gravity.TOP | Gravity.CENTER_HORIZONTAL; break;
            case 2: nLocation = Gravity.TOP | Gravity.RIGHT; break;
            case 3: nLocation = Gravity.CENTER_VERTICAL| Gravity.LEFT; break;
            case 4: nLocation = Gravity.CENTER_VERTICAL| Gravity.CENTER_HORIZONTAL; break;
            case 5: nLocation = Gravity.CENTER_VERTICAL| Gravity.RIGHT; break;
            case 6: nLocation = Gravity.BOTTOM| Gravity.LEFT; break;
            case 7: nLocation = Gravity.BOTTOM| Gravity.CENTER_HORIZONTAL; break;
            case 8: nLocation = Gravity.BOTTOM| Gravity.RIGHT;break;
        }
        Log.i(TAG, "nDrawColor: "+ nDrawColor + ", setting:"+ R.color.purple);
        SetTextViewLocation(tvInfo, nLocation);

        tvInfo.setTextColor(drawView.alphaColor(ConfigActivity.settings.getTextColor(), nAlphaTvInfo));
        tvInfo.setTextSize(ConfigActivity.settings.getTextSize());
        tvInfo.setBackgroundColor(ConfigActivity.settings.getBackground());
        int nBold = 0;//normal
        switch(ConfigActivity.settings.getTextBold())
        {
            case 0: break;
            case 1: nBold=Typeface.BOLD; break;
            case 2: nBold=Typeface.ITALIC; break;
            case 3: nBold= (Typeface.BOLD | Typeface.ITALIC); break;

        }
        if (nBold != 0) tvInfo.setTypeface(null, nBold);

        tvInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case PLAYBACK:
                        OperationCtrl.nState = OperationCtrl.STATE.READY_TO_PLAY;
                        drawView.RestartPlayback();
                        finish();
                        break;
                    case CREATE_PATTERN:
                        if (nAlphaTvInfo == 0xff) nAlphaTvInfo= 0x80;
                        else if (nAlphaTvInfo == 0x80) nAlphaTvInfo= 0x40;
                        else nAlphaTvInfo = 0xff;
                        tvInfo.setTextColor(drawView.alphaColor(ConfigActivity.settings.getTextColor(), nAlphaTvInfo));
                        break;
                }
            }
        });

        tvLoadPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        Log.i(TAG, "tvLoadPrev");
                        if (DrawView.bPathAvailable) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(CreatePattern.this);
                            builder.setTitle(getResources().getString(R.string.text_alert_title));
                            builder.setMessage(getResources().getString(R.string.text_discard_drawing));

                            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Handle "Yes" button click
                                    drawView.LoadPatternPrev();
                                    ShowTvInfo();
                                }
                            });

                            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Handle "No" button click
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        else {
                            drawView.LoadPatternPrev();
                            ShowTvInfo();
                        }

                        break;
                }
            }
        });
        tvLoadNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        Log.i(TAG, "tvLoadNext");
                        if (DrawView.bPathAvailable) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(CreatePattern.this);
                            builder.setTitle(getResources().getString(R.string.text_alert_title));
                            builder.setMessage(getResources().getString(R.string.text_discard_drawing));

                            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Handle "Yes" button click
                                    drawView.LoadPatternNext();
                                }
                            });

                            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Handle "No" button click
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        else
                        {
                            drawView.LoadPatternNext();
                            ShowTvInfo();
                        }
                        break;
                }
            }
        });

        if (FileSaveLoad.currNum == 0) // add condition to check data files only when the app starts
            FileSaveLoad.updateMaxFileNum(getBaseContext());
        ShowLoadMenu();

        EnableButtonSaveExport(false);

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        drawView.Undo();
                        break;
                    default:
                        finish();
                        break;
                }
            }
        });
        btnUndo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        drawView.UndoFast();
                        break;
                    default:

                        break;
                }
                return true;
            }
        });
        tvUndoSimple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        drawView.Undo();
                        break;
                    default:
                        finish();
                        break;
                }
            }
        });
        tvUndoSimple.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        drawView.UndoFast();
                        break;
                    default:

                        break;
                }
                return true;
            }
        });
        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        // change to delete the pattern
                        if (FileSaveLoad.maxNum == 0) break;
                        AlertDialog.Builder builder = new AlertDialog.Builder(CreatePattern.this);
                        builder.setTitle(getResources().getString(R.string.text_alert_title));
                        builder.setMessage(getResources().getString(R.string.text_alert_delete));

                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Handle "Yes" button click
                                drawView.DeletePattern();
                                ShowLoadMenu();
                                EnableButtonSaveExport(false);
                            }
                        });

                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();

                        break;
                    default:
                        break;
                }
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        DrawView.SavePattern();
                        ShowLoadMenu();
                        break;
                    default:
                        break;
                }
            }
        });
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        if (hasStoragePermission(1))
                            drawView.ExportPattern();
                        else {

                        }
                        break;
                    default:
                        break;
                }
            }
        });
        btnReset = (Button) findViewById(R.id.btnReset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (DrawView.alTouchPoints.size() == 0) return;

                AlertDialog.Builder builder = new AlertDialog.Builder(CreatePattern.this);
                builder.setTitle(getResources().getString(R.string.text_alert_title));
                builder.setMessage(getResources().getString(R.string.text_alert_content));

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle "Yes" button click
                        drawView.Clear();
                        EnableButtonSaveExport(false);
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }


        });
        btnConfirm = (Button) findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "btnConfirm: "+OperationCtrl.nState);
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        if (DrawView.alTouchPoints.size() > 0) {
                            if (BuildConfig.DEBUG) Log.i(TAG, "GenerateTargetPattern() 2 Confirm");
                            OperationCtrl.GenerateTargetPattern();
                            OperationCtrl.nState = OperationCtrl.STATE.PLAYBACK;
                        }
                        else{
                            OperationCtrl.nState = OperationCtrl.STATE.START_UP;
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.text_no_pattern), Toast.LENGTH_SHORT).show();
                        }

                        finish();
                        // Start the MainActivity.
                        Intent intent = new Intent(CreatePattern.this, MainActivity.class);
                        startActivity(intent);
                        break;
                    case PLAYBACK:
                        break;
                    case PENDING_STOP:
                        OperationCtrl.nState = OperationCtrl.STATE.READY_TO_PLAY;
                        drawView.RestartPlayback();
                        finish();
                        break;
                }
            }
        });

        seekBar = (SeekBar)findViewById(R.id.seekBarDrawColor);
        seekBar.setMax(360);
        // This listener listen to seek bar change event.
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                //debug for Samsung A135F and A115F, inconsistent range detectection, eg. 0-75% skipped, or only 0 and 360 detected
                // but never happened for Huawei (Mate 20 pro and P30 pro)
                ViewGroup.LayoutParams params = seekBar.getLayoutParams();

                DecimalFormat decimalFormat = new DecimalFormat("0.00%");
                // Calculate progress value percentage.
                float progressPercentageFloat = (float)progress / (float)seekBar.getMax();
                String progressPercentage = decimalFormat.format(progressPercentageFloat);

                float[] hsv={progress, 1, 1};
                if (progress == 0) nDrawColor = Color.BLACK;
                else if (progress == 360) nDrawColor = Color.WHITE;
                else nDrawColor = Color.HSVToColor(hsv);
                StringBuffer strBuf = new StringBuffer();
                strBuf.append(params.width + "/"+ (float)seekBar.getMax()+ ", current Progress is " + progress + ", %="+ progressPercentageFloat+ ", "+progressPercentage + " color="+ nDrawColor + ", BLACK=" + Color.BLACK);
                Log.i(TAG, strBuf.toString());
                seekBar.getProgressDrawable().setColorFilter(nDrawColor, PorterDuff.Mode.SRC_ATOP);
                nSeekBarProgress = progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // When seek bar start slip.
                Log.i(TAG, "Start Slip: "+ nSeekBarProgress);
                ViewGroup.LayoutParams params = seekBar.getLayoutParams();
                params.height = 100;
                params.width = (int) (MainActivity.ScreenSize.x*0.8);
                seekBar.setLayoutParams(params);
                seekBar.setProgress(0); // still not consistent for Samsung A11/A13
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // When seek bar stop slip.
                Log.i(TAG, "Stop Slip: " + nSeekBarProgress);
                UpdateThickColor();
                ViewGroup.LayoutParams params = seekBar.getLayoutParams();
                params.height = 100;
                params.width = 100;
                seekBar.setLayoutParams(params);
            }
        });
        tvThick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nDrawThickness < 100)
                    nDrawThickness += 5;
            }
        });
        tvThin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nDrawThickness > 5)
                    nDrawThickness -= 5;
                else if (nDrawThickness > 2)
                    nDrawThickness --;

            }
        });
        tvGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nGrid++;
                if (nGrid > 6) nGrid = 0;
                drawView.Draw();
            }
        });
        tvHideCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bHideCtrl = !bHideCtrl;
                HideControl();
                SetTextviewHide();
            }
        });

    }
    private void ShowTvInfo()
    {
        String str = ConfigActivity.settings.getText();
        if ("(null)".equals(str)) str="";
        tvInfo.setText(str);
    }
    public void ShowLoadMenu()
    {
        if (FileSaveLoad.getMaxNum()>0) {

            UpdateLoadMenu(true);
        }
        else {

            UpdateLoadMenu(false);
        }
    }
    static public void EnableButtonSaveExport(boolean b)
    {
        EnableButton(btnSave, b, 0xFF99EAA4);
        EnableButton(btnExport, b, 0xFF99EAA4);
    }
    static private void EnableButton(Button btn, boolean enable, int color)
    {
        btn.setEnabled(enable);
        if (enable) {
            btn.setBackgroundColor(color);
            btn.setTextColor(Color.BLACK);
        }
        else
        {
            btn.setBackgroundColor(Color.GRAY);
            btn.setTextColor(Color.BLACK);
        }
    }
    private void SetTextviewHide()
    {
        if (bHideCtrl) {
            tvHideCtrl.setText(">>>");
        }
        else {
            tvHideCtrl.setText("<<<");
        }
    }
    private void UpdateLoadMenu(boolean b)
    {
        Log.i(TAG, "UpdateLoadMenu show=" + b);
        int showup = b ? View.VISIBLE:View.INVISIBLE;
        tvLoadPrev.setVisibility(showup);
        tvLoadNext.setVisibility(showup);
        if (b)
        {
            EnableButton(btnLoad, true, 0xFF99EAA4);

        }
        else {
            EnableButton(btnLoad, false, 0);

        }
    }
    private void HideControl()
    {
        Log.i(TAG, "HideControl "+bHideCtrl);
        int showup = bHideCtrl ? View.INVISIBLE:View.VISIBLE;
        int reverse = bHideCtrl ? View.VISIBLE:View.INVISIBLE;
        tvThick.setVisibility(showup);
        tvThin.setVisibility(showup);
        tvGrid.setVisibility(showup);
        tvUndoSimple.setVisibility(reverse);
        seekBar.setVisibility(showup);
        btnConfirm.setVisibility(showup);
        btnUndo.setVisibility(showup);
        btnReset.setVisibility(showup);
        btnSave.setVisibility(showup);
        btnExport.setVisibility(showup);
        btnLoad.setVisibility(showup);
        if (bHideCtrl)
        {
            tvLoadPrev.setVisibility(showup);
            tvLoadNext.setVisibility(showup);
        }
        else {
            ShowLoadMenu();
        }
        drawView.Draw();

    }

    static private void ShowWarning(String info)
    {
        tvCountDown.setTextColor(Color.RED);
        tvCountDown.setText(info);
        tvCountDown.setVisibility(View.VISIBLE);
    }
    static private void SetTextViewLocation(TextView tv, int location)
    {
        // Create layout parameters for the TextView
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, // Width
                FrameLayout.LayoutParams.WRAP_CONTENT  // Height
        );

        // Set the position of the TextView within the FrameLayout
        layoutParams.gravity = location;

        // Set the layout parameters for the TextView
        tv.setLayoutParams(layoutParams);
    }
    static public void HideWarning()
    {
        tvCountDown.setVisibility(View.INVISIBLE);
    }

    private void UpdateThickColor()
    {
        tvThick.setBackgroundColor(nDrawColor);
        tvThick.setTextColor(Color.rgb(255-((nDrawColor>>16)&0xff), 255- ((nDrawColor>>8)&0xff), 255- (nDrawColor&0xff)));
        tvThin.setBackgroundColor(nDrawColor);
        tvThin.setTextColor(Color.rgb(255-((nDrawColor>>16)&0xff), 255- ((nDrawColor>>8)&0xff), 255- (nDrawColor&0xff)));
    }
    private void UpdateButtons() {
        Log.i(TAG, "UpdateButtons state="+OperationCtrl.nState);
        btnConfirm.setTextColor(Color.RED);
        btnConfirm.setBackgroundColor(Color.GREEN);
        btnConfirm.setTextColor(Color.BLACK);
        btnReset.setBackgroundColor(Color.GREEN);
        btnUndo.setBackgroundColor(Color.GREEN);
        tvCountDown.setVisibility(View.INVISIBLE);
        tvUndoSimple.setVisibility(View.INVISIBLE);
        switch (OperationCtrl.nState) {
            case CREATE_PATTERN:

                layoutDraw.setVisibility(View.VISIBLE);
                tvHideCtrl.setVisibility(View.VISIBLE);
                UpdateThickColor();
                SetTextviewHide();

                HideControl();
                break;
            case PLAYBACK:

                btnConfirm.setVisibility(View.INVISIBLE);
                btnReset.setVisibility(View.INVISIBLE);
                btnLoad.setVisibility(View.INVISIBLE);
                btnSave.setVisibility(View.INVISIBLE);
                btnExport.setVisibility(View.INVISIBLE);
                btnUndo.setVisibility(View.INVISIBLE);

                layoutDraw.setVisibility(View.INVISIBLE);
                tvLoadPrev.setVisibility(View.INVISIBLE);
                tvLoadNext.setVisibility(View.INVISIBLE);
                tvHideCtrl.setVisibility(View.INVISIBLE);
                break;
            case PENDING_STOP:
                btnConfirm.setText(getResources().getString(R.string.button_confirm));
                btnConfirm.setEnabled(true);
                btnConfirm.setVisibility(View.VISIBLE);
                btnReset.setVisibility(View.INVISIBLE);
                btnLoad.setVisibility(View.INVISIBLE);
                btnSave.setVisibility(View.INVISIBLE);

                btnUndo.setVisibility(View.INVISIBLE);
                break;
            default:

                break;
        }
    }

    private boolean hasStoragePermission(int requestCode) {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;//performAction(...);
        }  else {
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    requestCode);
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    drawView.ExportPattern();
                } else {
                    Toast.makeText(this, getResources().getString(R.string.text_need_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume "+ OperationCtrl.nState);
        //
        Display display = getWindowManager().getDefaultDisplay();
        MainActivity.ScreenSize = new Point(0, 0);
        display.getSize(MainActivity.ScreenSize);

        UpdateButtons();

        drawView.RestartPlayback();
        if (OperationCtrl.nState == OperationCtrl.STATE.PLAYBACK) {

            Flying();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                OperationCtrl.StartObserveTime();
            }
        }

    }


    private void Flying()
    {
        handler.post(new Runnable(){
            int nTimeCount = 0;
            int nWaitingQuit = 0;  //time duration to quit current state
            int nPlaybackTime = 0; //debug only
            public void run(){
                //main task: OBSERVE_PATTERN: playback by drawing line by line for animation
                // END_PLAYBACK: after finish playback, remain the screen for 1s
                // PENDING_STOP: then show the Stop button to quit looping animation
                nTimeCount++;
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Flying at nState="+OperationCtrl.nState +", nTimeCount="+ nTimeCount+", " + FileSaveLoad.fileName);
                }
                //  END_PLAYBACK
                if (OperationCtrl.nState == OperationCtrl.STATE.END_PLAYBACK)
                {
                    if (BuildConfig.DEBUG){
                        Log.i(TAG, "nWaitingQuit1 nTimeCount="+ nTimeCount);
                        tvInfo.setText(OperationCtrl.nState + " "+ nTimeCount + " waiting "+ nWaitingQuit+ ", " +ConfigActivity.settings.getSpeed()+"ms");
                    }
                    if (nTimeCount%(500/ConfigActivity.settings.getSpeed()) == 0) //every 500ms
                    {
                        if (BuildConfig.DEBUG) Log.i(TAG, "nWaitingQuit1 "+OperationCtrl.nState + ", "+nWaitingQuit + ", nTimeCount="+ nTimeCount);
                        nWaitingQuit ++;
                        if (nWaitingQuit > 2){
                            nWaitingQuit = 0;
                            OperationCtrl.nState = OperationCtrl.STATE.PENDING_STOP;
                        }
                        if (BuildConfig.DEBUG)  Log.i(TAG, "nWaitingQuit1 "+OperationCtrl.nState + ", "+nWaitingQuit);

                    }
                    handler.postDelayed(this, ConfigActivity.settings.getSpeed());
                    return;
                }
                // PENDING_STOP
                if (OperationCtrl.nState == OperationCtrl.STATE.PENDING_STOP)
                {
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "nWaitingQuit2 nTimeCount="+ nTimeCount);
                        tvInfo.setText(OperationCtrl.nState + " "+ nTimeCount + " waiting "+ nWaitingQuit+ ", " +ConfigActivity.settings.getSpeed()+"ms");
                    }
                    if (nTimeCount%(500/ConfigActivity.settings.getSpeed()) == 0) //every 500ms
                    {
                        if (BuildConfig.DEBUG) Log.i(TAG, "nWaitingQuit2 "+OperationCtrl.nState + ", "+nWaitingQuit+", nTimeCount="+ nTimeCount
                                + ", " +ConfigActivity.settings.getSpeed()+"ms");
                        nWaitingQuit ++;
                        if (nWaitingQuit > 5){
                            DrawView.nLastChangePoint = 0;
                            DrawView.clearBitmap();

                            nWaitingQuit = 0;
                            drawView.RestartPlayback();
                            OperationCtrl.nState = OperationCtrl.STATE.PLAYBACK;
                            UpdateButtons();

                            if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                OperationCtrl.StartObserveTime();
                            }
                        }
                        if (nWaitingQuit > 1) UpdateButtons();
                        if (BuildConfig.DEBUG) Log.i(TAG, "nWaitingQuit2 "+OperationCtrl.nState + ", "+nWaitingQuit);

                    }
                    handler.postDelayed(this, ConfigActivity.settings.getSpeed());
                    return;
                }

                if (OperationCtrl.nState != OperationCtrl.STATE.PLAYBACK) {
                    if (BuildConfig.DEBUG) {
                        //Log.i(TAG, "Flying stop, nState="+OperationCtrl.nState +", nTimeCount="+ nTimeCount);
                        tvInfo.setText(OperationCtrl.nState + " "+ nTimeCount );
                    }
                    handler.postDelayed(this, ConfigActivity.settings.getSpeed());

                    return;
                }

                //playback
                if (BuildConfig.DEBUG) Log.i(TAG, "Flying nState="+OperationCtrl.nState + ", curr point=" + drawView.GetCurrentPoint()+", nTimeCount="+ nTimeCount);

                if (drawView.IsEndOfPlayback()){
                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "Flying end, loop="+MainActivity.bLoopDataFiles + ": " + FileSaveLoad.fileName
                                + ", delay=" + bLoopFilesDelay + ", "
                            + OperationCtrl.nState);

                    if (MainActivity.bLoopDataFiles)
                    {
                        if (bLoopFilesDelay)
                        {
                            bLoopFilesDelay = false;
                            handler.postDelayed(this, 2000); //show static full drawing for 2s
                            return;
                        }
                        bLoopFilesDelay = true;
                        //load next file
                        drawView.LoadPatternNext();
                        ShowTvInfo();
                        if (BuildConfig.DEBUG) Log.i(TAG, "drawView.LoadPatternNext: "+ FileSaveLoad.fileName);
                        OperationCtrl.GenerateTargetPattern();

                    }
                    else {
                        OperationCtrl.nState = OperationCtrl.STATE.END_PLAYBACK;
                        if (BuildConfig.DEBUG) {
                            Log.i(TAG, "Flying end, no loop: " + FileSaveLoad.fileName
                                    + ", "
                                    + OperationCtrl.nState +" -> " + OperationCtrl.STATE.END_PLAYBACK);
                            tvInfo.setText(DrawView.alTouchPoints.size() + " - " + drawView.GetCurrentPoint() + "/" + FlyingPaths.alBasePath.size()
                                    + ", chg " + FlyingPaths.pointInfoChangeList.getSize() + ", " + ConfigActivity.settings.getSpeed() + "ms, Total "
                                    + OperationCtrl.GetObserveTime() +"ms " + FileSaveLoad.fileName);
                        }
                    }
                }
                else {

                    if (BuildConfig.DEBUG) {
                        nPlaybackTime ++;
                        tvInfo.setText(DrawView.alTouchPoints.size() + " - " + drawView.GetCurrentPoint() + "/" + FlyingPaths.alBasePath.size()
                                + ", chg " + FlyingPaths.pointInfoChangeList.getSize() + ", " + ConfigActivity.settings.getSpeed()
                                + "ms " + FileSaveLoad.fileName + ":"+ConfigActivity.settings.getText()); //testing
                    }
                    drawView.Draw();
                }

                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(this, ConfigActivity.settings.getSpeed());

            }
        });
    }
}