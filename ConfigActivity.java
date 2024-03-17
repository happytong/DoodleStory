package com.tongs.doodlestory;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import views.DrawView;


public class ConfigActivity extends AppCompatActivity {
    private String TAG="[dbg]ConfigActivity";

    private int nPlayColor=0, nPlayWidth=5;
    private int nBackgroundSelect = 0;
    public static SettingsManager.Settings settings;
    TextView tvSpeed, tvTextSize, tvPlaybackWidth;
    EditText editTextInfo;
    Spinner spinnerLocation, spinnerBold, spinnerPlaybackColor;
    static private SeekBar seekBar;
    static private Button btnPlus, btnMinus;
    private int nSpeed=10, nSpinnerPlayback = 0;
    boolean bPlaySingleColor=false, bGradient = false;
    boolean bGradientChange = false; //true then need to disable main Playback button
    private int nSize=10, nLocation=0, nBold=0, nColorText=0; //text info
    private int nGridV = 0, nGridH = 0;
    TextView tvGridMarginV, tvGridMarginH;
    private int nColor = 0;//seekbar color
    private int nHighlightInput = 0; //for each TextView to update
    private int nColorFor = 0; //color seekbar: 0 for text info, 1 for playback single color

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        //settings has been loaded when app starts
        nSpeed = settings.getSpeed();
        nSize = settings.getTextSize();
        nLocation = settings.getTextLocation();
        nBold = settings.getTextBold();
        nColor = settings.getTextColor();
        nColorText = nColor;
        bPlaySingleColor = settings.getPlaySingleColor();
        nPlayColor = settings.getPlayColor();
        nPlayWidth = settings.getPlayWidth();
        bGradient = settings.getGradient();
        nBackgroundSelect =settings.getBackground();
        nGridV = settings.getGridMarginTopBottom();
        nGridH = settings.getGridMarginLeftRight();

        //set nSpinnerPlayback
        if (bPlaySingleColor) nSpinnerPlayback = 2;
        else if (bGradient)  nSpinnerPlayback = 0;
        else nSpinnerPlayback = 1; //as drawn

        tvSpeed = findViewById(R.id.tvSpeed);
        tvSpeed.setText(getResources().getString(R.string.speed)+":"+nSpeed+"ms");

        tvTextSize = findViewById(R.id.tvTextSize);
        tvTextSize.setText(getResources().getString(R.string.text_size)+":"+nSize);

        tvGridMarginH = findViewById(R.id.tvGridLeftRight);
        tvGridMarginH.setText(getResources().getString(R.string.grid_left_right)+":"+nGridH);
        tvGridMarginV = findViewById(R.id.tvGridTopBottom);
        tvGridMarginV.setText(getResources().getString(R.string.grid_top_bottom)+":"+nGridV);

        tvPlaybackWidth = findViewById(R.id.tvWidth);
        tvPlaybackWidth.setText(getResources().getString(R.string.playback_width)+":"+nSize);
        tvPlaybackWidth.setTextColor(nPlayColor);

        //settings = SettingsManager.loadSetting(this); //loaded by Main
        editTextInfo = findViewById(R.id.editTextInfoTop);
        String str = settings.getText();
        if ("(null)".equals(str)) str="";
        editTextInfo.setText(str);
        editTextInfo.setTextColor(nColorText);

        spinnerLocation = (Spinner) findViewById(R.id.spinnerLocation);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.array_location, R.layout.spinner_format);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(adapter);
        spinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nLocation = position;
                HighlightInput(0);
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });
        spinnerLocation.setSelection(nLocation);

        spinnerBold = (Spinner) findViewById(R.id.spinnerBold);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.array_bold, R.layout.spinner_format);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBold.setAdapter(adapter);
        spinnerBold.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nBold = position;
                HighlightInput(0);
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });
        spinnerBold.setSelection(nBold);

        spinnerPlaybackColor = (Spinner) findViewById(R.id.spinnerPlaybackColor);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.array_playback_color, R.layout.spinner_format);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlaybackColor.setAdapter(adapter);
        spinnerPlaybackColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nColorFor = 0;

                switch (position)
                {
                    case 0: //gradient
                        bGradient = true; bPlaySingleColor = false; break;
                    case 1: //as drawn
                        bGradient = false; bPlaySingleColor = false; break;
                    default: //fixed color and width
                        bGradient = false; bPlaySingleColor = true;
                        nColorFor = 1; //set the fixed color for playback
                        break;
                }
                nSpinnerPlayback = position;

                Log.i(TAG, "spinner color select "+position + ", set for "+nColorFor);
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });
        Log.i(TAG, "onCreate playcolor=" + nPlayColor + ", spinner color=" + nSpinnerPlayback);
        spinnerPlaybackColor.setSelection(nSpinnerPlayback);

        //cbFollow.setChecked(cfgFollow);
        ((GridLayout)findViewById(R.id.layoutCfg)).setBackgroundColor(getCfgBackground(nBackgroundSelect));

        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setMax(360);
        seekBar.getProgressDrawable().setColorFilter(nColor, PorterDuff.Mode.SRC_ATOP);
        // This listener listen to seek bar change event.
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                DecimalFormat decimalFormat = new DecimalFormat("0.00%");
                // Calculate progress value percentage.
                float progressPercentageFloat = (float) progress / (float) seekBar.getMax();
                String progressPercentage = decimalFormat.format(progressPercentageFloat);
                float[] hsv = {progress, 1, 1};
                if (progress == 0) nColor = Color.BLACK;
                else if (progress == 360) nColor = Color.WHITE;
                else
                    nColor = Color.HSVToColor(hsv);
                seekBar.getProgressDrawable().setColorFilter(nColor, PorterDuff.Mode.SRC_ATOP);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                HighlightInput(0);
                // When seek bar start slip.
                Log.i(TAG, "Start Slip.");
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // When seek bar stop slip.
                Log.i(TAG, "Stop Slip. set color for " + nColorFor);
                if (nColorFor == 0)
                {
                    nColorText = nColor;
                    editTextInfo.setTextColor(nColor);
                }
                else{
                    nPlayColor = nColor;
                    tvPlaybackWidth.setTextColor(nColor);
                }

            }
        });

        editTextInfo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    nColorFor = 0;
                } else {
                }
                Log.i(TAG, " editTextInfo focus change, set for "+nColorFor);
            }
        });

        final Button btnUpdate = (Button) findViewById(R.id.btnConfirm);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //save the setting
                if (nSpeed < 5) nSpeed = 5;
                if (nSpeed > 500) nSpeed = 500;

                //Log.i(TAG, "btnUpdate finish: mode="+ cfgMode );
                boolean ori = settings.getGradient();

                EditText editTextInfoTop = findViewById(R.id.editTextInfoTop);
                String str = String.valueOf(editTextInfoTop.getText( ));
                SettingsManager.saveSetting(getApplicationContext(), nSpeed, str, nSize,
                    nLocation, nBold, nColorText, bPlaySingleColor, nPlayColor, nPlayWidth, bGradient, nBackgroundSelect,
                        nGridV, nGridH);
                settings = SettingsManager.loadSetting(getApplicationContext()); //updated
                Log.i(TAG, "Updated spinnerSize "+ settings.getTextSize() + ", playback gradient=" + settings.getGradient()
                        + " fixed="+ settings.getPlaySingleColor());

                if (ori != settings.getGradient()){
                    //OperationCtrl.nState = OperationCtrl.STATE.START_UP; //to disable PLAYBACK button
                    if (DrawView.alTouchPoints.size() > 0)
                        if (BuildConfig.DEBUG) {Log.i(TAG, "GenerateTargetPattern() 3 config");}
                        OperationCtrl.GenerateTargetPattern();
                }

                finish();
            }
        });

        final Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "btnCancel ");
                finish();
            }
        });

        tvSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HighlightInput(1);
            }
        });
        tvTextSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HighlightInput(2);
            }
        });
        tvGridMarginV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HighlightInput(4);
            }
        });
        tvGridMarginH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HighlightInput(5);
            }
        });
        tvPlaybackWidth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HighlightInput(3);
            }
        });
        btnPlus = (Button) findViewById(R.id.btnPlus);
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (nHighlightInput)
                {
                    case 1:  //Speed
                        nSpeed ++;
                        tvSpeed.setText(getResources().getString(R.string.speed)+": "+nSpeed+"ms");
                    break;
                    case 2:  //text info size
                        nSize ++;
                        tvTextSize.setText(getResources().getString(R.string.text_size)+": "+nSize);
                    break;
                    case 3:  //playback width
                        nPlayWidth ++;
                        tvPlaybackWidth.setText(getResources().getString(R.string.playback_width)+": "+nPlayWidth);
                        break;
                    case 4:  //grid margin vertical (header and footer) height
                        if ( nGridV < MainActivity.ScreenSize.y/4 ) nGridV ++;
                        tvGridMarginV.setText(getResources().getString(R.string.grid_top_bottom)+":"+nGridV);
                        break;
                    case 5:  //grid margin horizontal (left and right) width
                        if ( nGridH <  MainActivity.ScreenSize.x/4 ) nGridH ++;
                        tvGridMarginH.setText(getResources().getString(R.string.grid_left_right)+":"+nGridH);
                        break;

                }
            }
        });
        btnPlus.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switch (nHighlightInput)
                {
                    case 1:  //Speed
                        if (nSpeed >= 10) nSpeed *=1.1;
                        else nSpeed +=2;
                        tvSpeed.setText(getResources().getString(R.string.speed)+": "+nSpeed+"ms");
                        break;
                    case 2:  //text info size
                        nSize +=2;
                        tvTextSize.setText(getResources().getString(R.string.text_size)+": "+nSize);
                        break;
                    case 3:  //playback width
                        nPlayWidth +=2;
                        tvPlaybackWidth.setText(getResources().getString(R.string.playback_width)+": "+nPlayWidth);
                        break;
                    case 4:  //grid margin vertical (header and footer) height
                        if ( nGridV < MainActivity.ScreenSize.y/4 ) {
                            if (nGridV >= 10) nGridV *=1.1;
                            else nGridV += 5;
                        }
                        tvGridMarginV.setText(getResources().getString(R.string.grid_top_bottom)+":"+nGridV);
                        break;
                    case 5:  //grid margin horizontal (left and right) width
                        if ( nGridH <  MainActivity.ScreenSize.x/4 ) {
                            if (nGridH >= 10) nGridH *=1.1;
                            else nGridH += 5;
                        }
                        tvGridMarginH.setText(getResources().getString(R.string.grid_left_right)+":"+nGridH);
                        break;

                }
                return true; // Return true to consume the long click event
            }
        });
        btnMinus = (Button) findViewById(R.id.btnMinus);
        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(nHighlightInput)
                {
                    case 1:
                    if (nSpeed>=12) nSpeed *=0.9;
                    else if (nSpeed > 5) nSpeed --;
                    tvSpeed.setText(getResources().getString(R.string.speed)+": "+nSpeed+"ms");
                break;
                    case 2:
                    if (nSize > 10) nSize --;
                    tvTextSize.setText(getResources().getString(R.string.text_size)+": "+nSize);
                case 3:
                    if (nPlayWidth > 5) nPlayWidth --;
                    tvPlaybackWidth.setText(getResources().getString(R.string.playback_width)+": "+nPlayWidth);
                    break;
                case 4:
                    if (nGridV > 1) nGridV --;
                    tvGridMarginV.setText(getResources().getString(R.string.grid_top_bottom)+":"+nGridV);
                    break;
                case 5:
                    if (nGridH > 1) nGridH --;
                    tvGridMarginH.setText(getResources().getString(R.string.grid_left_right)+":"+nGridH);
                    break;
                }
           }
        });
        btnMinus.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switch(nHighlightInput)
                {
                    case 1:
                        if (nSpeed>=12) nSpeed *=0.9;
                        else if (nSpeed > 5) nSpeed -=2;
                        tvSpeed.setText(getResources().getString(R.string.speed)+": "+nSpeed+"ms");
                        break;
                    case 2:
                        if (nSize > 10) nSize -=2;
                        tvTextSize.setText(getResources().getString(R.string.text_size)+": "+nSize);
                    case 3:
                        if (nPlayWidth > 5) nPlayWidth -=2;
                        tvPlaybackWidth.setText(getResources().getString(R.string.playback_width)+": "+nPlayWidth);
                        break;
                    case 4:
                        if (nGridV > 10) nGridV *= 0.9;
                        if (nGridV > 5) nGridV -=5;
                        tvGridMarginV.setText(getResources().getString(R.string.grid_top_bottom)+":"+nGridV);
                        break;
                    case 5:
                        if (nGridH > 10) nGridH *= 0.9;
                        if (nGridH > 5) nGridH -= 5;
                        tvGridMarginH.setText(getResources().getString(R.string.grid_left_right)+":"+nGridH);
                        break;
                }
                return true;
            }
        });
    }
    private void ShowFonts()
    {
        // Get the list of available fonts
        List<String> fontList = getFontList();

        // Create an ArrayAdapter for the Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fontList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the adapter to the Spinner
        spinnerBold.setAdapter(adapter);
    }
    private void HighlightInput(int n)
    {
        tvSpeed.setBackgroundColor(nBackgroundSelect);
        tvTextSize.setBackgroundColor(nBackgroundSelect);
        tvPlaybackWidth.setBackgroundColor(nBackgroundSelect);
        tvGridMarginV.setBackgroundColor(nBackgroundSelect);
        tvGridMarginH.setBackgroundColor(nBackgroundSelect);

        nHighlightInput =n;
        switch (n)
        {
            case 1:
                tvSpeed.setBackgroundColor(Color.CYAN);
                btnPlus.setEnabled(true);
                btnMinus.setEnabled(true);
                break;
            case 2:
                tvTextSize.setBackgroundColor(Color.CYAN);
                btnPlus.setEnabled(true);
                btnMinus.setEnabled(true);
                break;
            case 3:
                tvPlaybackWidth.setBackgroundColor(Color.CYAN);
                btnPlus.setEnabled(true);
                btnMinus.setEnabled(true);
                break;
            case 4:
                tvGridMarginV.setBackgroundColor(Color.CYAN);
                btnPlus.setEnabled(true);
                btnMinus.setEnabled(true);
                break;
            case 5:
                tvGridMarginH.setBackgroundColor(Color.CYAN);
                btnPlus.setEnabled(true);
                btnMinus.setEnabled(true);
                break;
            default:
                btnPlus.setEnabled(false);
                btnMinus.setEnabled(false);
                break;
        }
    }
    private List<String> getFontList() {
        List<String> fontList = new ArrayList<>();
        AssetManager assetManager = getApplicationContext().getAssets();

        try {
            String[] fontFiles = assetManager.list("fonts"); //should create the folder app/src/main/assets/fonts/ and put fonts inside (*.ttf)
            if (fontFiles != null) {
                for (String fontFile : fontFiles) {
                    fontList.add(fontFile);
                    Log.d(TAG, "Font File: " + fontFile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fontList;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        HighlightInput(0);
        Log.i(TAG, "onTouchEvent "+ event.getAction());
        switch (event.getAction())
        {
            case MotionEvent.ACTION_UP:
                //change background
                nBackgroundSelect ++;
                if (nBackgroundSelect > 8) nBackgroundSelect = 0;

                GridLayout rl = (GridLayout)findViewById(R.id.layoutCfg);
                rl.setBackgroundColor(getCfgBackground(nBackgroundSelect));
                Log.i(TAG, "onTouchEvent setBackgroundColor " + nBackgroundSelect + ","+ getCfgBackground(nBackgroundSelect));
                break;
        }
        return true;
    }
    static public int getCfgBackground(int n)
    {
        switch (n)
        {
            //only with 0xFFrrggbb, Export to JPG will show the same background...
            case 0: return 0xFFeeeeee;//0x44b5e1f2;
            case 1: return 0xFFdddddd;//220000ff;
            case 2: return 0xFFffeeee;//2200ff00;
            case 3: return 0xFFffeeff;//336fc4e1;
            case 4: return 0xFFeeddee;//0x222200;//0x22ffff00;
            case 5: return 0xFFddeeee;//22ff00ff;
            case 6: return 0xFFeeffee;
            case 7: return 0xFFeeeedd;//2200ffff;
            default: return 0xffffffdd;//22ffffff;
        }
    }
}