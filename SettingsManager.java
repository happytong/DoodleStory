package com.tongs.doodlestory;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class SettingsManager {
    public static String TAG = "[dbg]SettingManager";
    private static final String PREFS_NAME = "DoodleStorySetting";
    public static void saveSetting(Context context, int speed, String strInfo, int textSize, int textLocation,
                                   int textBold, int textColor,
                                   boolean playSingleColor, int playColor, int playWidth,
                                   boolean playGradient, int background,
                                   int gridV, int gridH) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt("speed", speed);
        editor.putString("info", strInfo);
        editor.putInt("text_size", textSize);
        editor.putInt("text_location", textLocation);
        editor.putInt("text_bold", textBold);
        editor.putInt("text_color", textColor);
        editor.putBoolean("play_single_color", playSingleColor);
        editor.putInt("play_color", playColor);
        editor.putInt("play_width", playWidth);
        editor.putBoolean("play_gradient", playGradient);
        editor.putInt("background", background);
        editor.putInt("gridMarginV", gridV);
        editor.putInt("gridMarginH", gridH);
        editor.apply();
    }

    // Function to load settings
    public static Settings loadSetting(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int speed = prefs.getInt("speed", 10); // 0 is the default value if age is not found
        String strInfo = prefs.getString("info", ""); // "" is the default value if name is not found
        int textSize = prefs.getInt("text_size", 10);
        int textLocation = prefs.getInt("text_location", 0);
        int textBold = prefs.getInt("text_bold", 0);
        int defaultColor = ContextCompat.getColor(context, R.color.purple);
        int textColor = prefs.getInt("text_color", defaultColor);
        boolean playSingleColor = prefs.getBoolean("play_single_color", false);
        defaultColor = ContextCompat.getColor(context, R.color.blue_200);
        int playColor = prefs.getInt("play_color", defaultColor);
        int playWidth = prefs.getInt("play_width", 5);
        boolean playGradient = prefs.getBoolean("play_gradient", true);
        int background = prefs.getInt("background", 0);
        int gridV = prefs.getInt("gridMarginV", 0);
        int gridH = prefs.getInt("gridMarginH", 0);
        Log.i(TAG, "loadSetting text color="+textColor);
        return new Settings(speed, strInfo, textSize, textLocation, textBold, textColor, playSingleColor, playColor,
                playWidth, playGradient, background, gridV, gridH);
    }

    // Helper class to hold the loaded settings
    public static class Settings {
        private int speed;
        private int playColor, playWidth; //when playback with fixed color and thickness
        private int gridMarginTopBottom, gridMarginLeftRight;
        private boolean isPlaySingleColorSize;
        private boolean gradient = false; //changing size to the end and start, and color

        public int getBackground() {
            return background;
        }

        private int background;
        private String textInfo;
        private int textSize, textLocation, textBold /*bold, italic*/, textColor;

        public Settings(int speed, String strInfo, int textSize, int textLocation, int textBold, int textColor,
                        boolean playSingleColor, int playColor, int playWidth, boolean gradient, int background,
                        int gridV, int gridH)
        {
            this.speed = speed ; //ms
            this.textInfo = strInfo;
            this.textLocation = textLocation;
            this.textBold = textBold;
            this.textSize = textSize;
            this.textColor = textColor;
            this.isPlaySingleColorSize = playSingleColor;
            this.playColor = playColor;
            this.playWidth = playWidth;
            this.gradient = gradient;
            this.background = background;
            gridMarginLeftRight = gridH;
            gridMarginTopBottom = gridV;
        }

        public boolean getGradient() {return gradient;}
        public void setGradient(boolean b) { gradient = b;}
        public int getSpeed() {
            return speed;
        }
        public int getTextSize() {
            return textSize;
        }
        public int getTextColor() {
            return textColor;
        }
        public int getTextLocation() {
            return textLocation;
        }
        public int getTextBold() {
            return textBold;
        }
        public int getPlayColor() {
            return playColor;
        }
        public int getGridMarginTopBottom() {
            return gridMarginTopBottom;
        }
        public int getGridMarginLeftRight() {
            return gridMarginLeftRight;
        }
        public boolean getPlaySingleColor() {
            return isPlaySingleColorSize;
        }
        public int getPlayWidth() {
            return playWidth;
        }
        public String getText() {
            return textInfo;
        }
        public String setText(String str) {
            return textInfo = str;
        }
    }

}
