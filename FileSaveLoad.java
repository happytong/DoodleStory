package com.tongs.doodlestory;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import views.DrawView;

public class FileSaveLoad {
    static private String TAG="[dbg]FileSaveLoad";
    Context context;

    public enum FILE_OPERATION {
        PREV,
        NEXT,
        DELETE,
        SAVE,
    }
    static String filePattern = "story";  //pattern of the data files: storyXX, X=0-9, maximum 1-99 files
    static String fileName = "story";
    static int currNum = 0; //current file number
    static int maxNum = 0;  //max file number stored
    static int MAX_FILE_NUM = 99;
    static public boolean IsDataExist(Context context, String filename)
    {

        String path=context.getFilesDir().getAbsolutePath()+"/"+filename;
        File file = new File ( path );
        return file.exists();
    }
    public static int getMaxNum(){
        return maxNum;
    }
    public static int updateMaxFileNum(Context context) {
        // Get a list of files in the application's private directory
        String[] fileList = context.fileList();
        int n = filePattern.getBytes().length;
        // Iterate through the list of files to find the maximum XX value
        for (String filename : fileList) {
            Log.i(TAG, "updateMaxFileNum checking "+filename);
            if (filename.matches(filePattern+"\\d{1,2}")) {
                // Extract XX part from the filename and convert it to an integer
                Log.i(TAG, "updateMaxFileNum got "+filename);
                int xxValue = Integer.parseInt(filename.substring(n, filename.getBytes().length));
                if (xxValue > maxNum) {
                    maxNum = xxValue;
                }
            }
        }
        Log.i(TAG, "updateMaxFileNum got "+maxNum + " files");
        //Toast.makeText(context, "data files found: "+maxNum, Toast.LENGTH_SHORT).show();
        currNum = maxNum;
        return maxNum;
    }

    public String getFileName(){
        return fileName;
    }
    public FileSaveLoad(Context context, FILE_OPERATION operation)
    {
        this.context = context;
        //SetFilename();
        switch (operation) {
            case PREV:
                FileLoadPrev(context);
                break;
            case NEXT:
                FileLoadNext(context);
                break;
            case SAVE:
                WriteToFile(context);
                break;
            case DELETE:
                DeleteFile(context);
                SetFilenamePrev(context);
                break;
        }
    }
    private void SetFilenamePrev(Context context)
    {
        if (maxNum == 0) return;
        this.context = context;
        Log.i(TAG, "SetFilenamePrev ori="+ currNum );
        currNum --;
        if (currNum <= 0) currNum = maxNum;
        fileName = filePattern + String.format("%02d", currNum);;
        Log.i(TAG, "SetFilenamePrev "+fileName );
    }
    public void FileLoadPrev(Context context)
    {
        if (maxNum == 0) return;
        SetFilenamePrev(context);
        ReadFile();
    }
    public void FileLoadNext(Context context)
    {
        if (maxNum == 0) return;
        this.context = context;
        Log.i(TAG, "FileLoadNext ori="+ currNum );
        currNum ++;
        if (currNum > maxNum) currNum = 1;
        fileName = filePattern + String.format("%02d", currNum);;
        ReadFile();
    }
    void SetFilenameSave() {
        maxNum ++;
        if (maxNum > MAX_FILE_NUM) {
            maxNum = MAX_FILE_NUM;
            currNum = 1;
        }
        else currNum = maxNum;

        if (currNum < 1) currNum = 1;
        fileName = filePattern + String.format("%02d", currNum);
        Log.i(TAG, "SetFilenameSave "+fileName + " max="+maxNum);
    }

    public void WriteToFile(Context context) {
        if (DrawView.alTouchPoints.size() < 10) return;

        this.context = context;

        SetFilenameSave();
        try (  //try-with-resources
               FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
               DataOutputStream dos = new DataOutputStream(fos)) {

            String str = ConfigActivity.settings.getText();
            if (str.isEmpty()) str = "(null)";
            dos.writeUTF(str);

            int n=0;
            for (Point point : DrawView.alTouchPoints){
                dos.writeInt(point.x);
                dos.writeInt(point.y);
               // Log.i(TAG, "save "+point);
                dos.writeInt( DrawView.alDrawColor.get(n));
                dos.writeInt(DrawView.alDrawThickness.get(n));
                n++;
            }
            dos.flush();

            //Toast.makeText(context, context.getResources().getString(R.string.text_pattern_saved)  + " ("+maxNum+"): "+str, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "WriteToFile "+fileName + ", info: " + str);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void ReadFile() {
        boolean bFileException = false;

        try (FileInputStream fis = context.openFileInput(fileName);
            DataInputStream dis = new DataInputStream(fis)) {

            // Read text information from the file
            String textInfo = dis.readUTF();

            if (textInfo == "(null)") ConfigActivity.settings.setText("");
            else ConfigActivity.settings.setText(textInfo);

            while (dis.available() > 0)
            {
                int x = dis.readInt();
                if (dis.available() > 0)
                {
                    int y = dis.readInt();
                    DrawView.alTouchPoints.add(new Point(x,y));

                    //set default
                    DrawView.alDrawColor.add(dis.readInt());
                    DrawView.alDrawThickness.add(dis.readInt());
                    //Log.i(TAG, "load "+x +"," +y);
                }
            }
            //Toast.makeText(context,  " ReadFile ("+fileName+"): "+textInfo, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "ReadFile "+fileName + ", info: " + textInfo);
        } catch (IOException e) {
            e.printStackTrace(); // Log the error message
            Log.i(TAG, "ReadFile "+fileName + ", exception: " + e);
            bFileException = true;
        }

        if (bFileException ) housekeeping(context);
    }

    // re-arrange the existing files in the same sequence with continuous numbers
    public void housekeeping(Context context) {

        // Create a map to store existing filenames and their current number
        Map<String, Integer> existingFiles = new LinkedHashMap<>();

        // Collect existing filenames and their current number
        for (int i = 1; i <= maxNum; i++) {
            String filename = filePattern + String.format("%02d", i);
            if (fileExists(context, filename)) {
                existingFiles.put(filename, i);
                Log.i(TAG, "housekeeping "+i+ ": "+filename);
            }
        }

        // Rename existing files with continuous numbers
        int nextAvailableNumber = 1;
        for (String existingFileName : existingFiles.keySet()) {
            String newFileName = filePattern + String.format("%02d", nextAvailableNumber);
            if (!existingFileName.equals(newFileName)) {
                renameFile(context, existingFileName, newFileName);
            }
            nextAvailableNumber++;
        }

        Log.i(TAG, "housekeeping ori max number="+maxNum +", new="+(nextAvailableNumber - 1));
        maxNum = nextAvailableNumber - 1;
    }
    public boolean fileExists(Context context, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        return file.exists();
    }
    public void renameFile(Context context, String oldFileName, String newFileName) {
        File oldFile = new File(context.getFilesDir(), oldFileName);
        File newFile = new File(context.getFilesDir(), newFileName);
        Log.i(TAG, "renameFile "+oldFileName+ " to "+newFileName);
        if (oldFile.exists()) {
            if (oldFile.renameTo(newFile)) {
                // File renamed successfully
            } else {
                // Handle the case where renaming failed
                Log.i(TAG, "cannot rename " + oldFile + " to " + newFileName);
            }
        }
    }

    public void DeleteFile(Context context)
    {
        File file = new File(context.getFilesDir(), fileName);
        Log.i(TAG, "Data file to delete: "+ fileName + ", remaining files: "+maxNum);
        if (file.exists()) {
            if (file.delete()) {
                // File was successfully deleted
                Log.i(TAG, "Data file deleted: "+ fileName);
                housekeeping(context);

                Toast.makeText(context, context.getResources().getString(R.string.text_deleted) + " ("+maxNum+")", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
