package views;

import static com.tongs.doodlestory.OperationCtrl.STATE.CREATE_PATTERN;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


import androidx.core.content.res.ResourcesCompat;

import com.tongs.doodlestory.BuildConfig;
import com.tongs.doodlestory.ConfigActivity;
import com.tongs.doodlestory.CreatePattern;
import com.tongs.doodlestory.FileSaveLoad;
import com.tongs.doodlestory.FlyingPaths;
import com.tongs.doodlestory.MainActivity;
import com.tongs.doodlestory.OperationCtrl;
import com.tongs.doodlestory.PointInfoList;
import com.tongs.doodlestory.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class DrawView extends View {
    private static String TAG="[dbg]DrawView";
    static private int SHORTEST_PATH = 10; //at least 100 points to detect as a new path
    static public boolean bPathAvailable = false; //true: enable save
    Paint paint = new Paint();
    static public ArrayList<Point> alTouchPoints = new ArrayList<>();
    static public ArrayList<Integer> alDrawThickness = new ArrayList<>();
    static public ArrayList<Integer> alDrawColor = new ArrayList<>();
    public static String Title = "";
    private static int nCurrentPlayPoint = 0; //current playback point
    //20231121
    public static int nLastChangePoint = 0;
    private static Bitmap bufferBitmap = null;
    private static Canvas bufferCanvas = null;

    static public int nTouchCount;
    static private int nLastDrawPoint=0; //finger moving drawn  //20231213
    public boolean bAllowShowAllPaths = false;
    private Point preTouchPoint = new Point(0,0);
    private boolean bShowAllPaths= false;
    private boolean bPointAdded = false;
    static private Context context;

    static public void setConext(Context context) {
        DrawView.context = context;
    }
    private void init() {
        paint.setColor(Color.BLACK);
        nTouchCount =0;

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public DrawView(Context context) {
        super(context);
        init();
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void onDraw(Canvas canvas) {
        //set background
        setBackgroundColor(ConfigActivity.getCfgBackground(ConfigActivity.settings.getBackground())); //background, but not showup when export to PNG

        switch (OperationCtrl.nState)
        {
            case CREATE_PATTERN:
                Log.i(TAG, "onDraw CREATE_PATTERN");
                DrawNewTouchPoints(canvas);

                DrawGrid(canvas);
                if (BuildConfig.DEBUG) {
                    paint.setTextSize(40);
                    canvas.drawText("Color="+CreatePattern.nDrawColor + ", "+R.color.red, 0, 40, paint);
                }

                break;
            case PLAYBACK:
                Log.i(TAG, "onDraw PLAYBACK");
                Playback(canvas);
                break;
            case END_PLAYBACK://ANSWER_PATTERN:
                Log.i(TAG, "onDraw END_PLAYBACK");
                DrawFinalPicture(canvas);

                break;
            case PENDING_STOP:
                Log.i(TAG, "onDraw PENDING_STOP");
                break;
            default: //case NOT_ANSWERED:
                Log.i(TAG, "onDraw default");
                break;
        }
    }

    public static void createBitmap()
    {
        if (bufferBitmap != null) return;
        // Create a bitmap with the size of the view
        bufferBitmap = Bitmap.createBitmap(MainActivity.ScreenSize.x, MainActivity.ScreenSize.y, Bitmap.Config.ARGB_8888);
        bufferCanvas = new Canvas(bufferBitmap);
    }
    public static void clearBitmap(){
        Log.i(TAG, "clearBitmap, touchpoints size=" + alTouchPoints.size());
        nLastDrawPoint = 0; //20231213
        nCurrentPlayPoint = 0;
        nLastChangePoint = 0;
        bPathAvailable = false;
        if (bufferCanvas == null) return;
        bufferCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }
    public void Draw()
    {
        Log.i(TAG, "postInvalidate");
        postInvalidate();
    }
    public void RestartPlayback()
    {
        Log.i(TAG, "RestartPlayback");
        clearBitmap();
    }
    public int GetCurrentPoint()
    {
        return nCurrentPlayPoint;
    }

    private void DrawGrid(Canvas canvas)
    {
        Log.i(TAG, "DrawGrid = "+CreatePattern.nGrid);
        if (CreatePattern.nGrid == 0)
        {
            return;
        }
        //set color of the grid
        int nColor = reverseColor(ConfigActivity.getCfgBackground(ConfigActivity.settings.getBackground()));
        nColor = alphaColor(nColor, 0x20);

        //set lines of the grid (2 lines or 4 lines nine-square grid)
        Line[] lines;
        switch (CreatePattern.nGrid)
        {
            case 1: lines = getLines(MainActivity.ScreenSize, 1, 1); break;
            case 2: lines = getLines(MainActivity.ScreenSize, 1, 2); break;
            case 3: lines = getLines(MainActivity.ScreenSize, 1, 3); break;
            case 4: lines = getLines(MainActivity.ScreenSize, 2, 2); break;
            case 5: lines = getLines(MainActivity.ScreenSize, 2, 3); break;
            case 6: lines = getLines(MainActivity.ScreenSize, 3, 3); break;
            default: return;
        }
        //draw the lines
        paint.setColor(nColor);
        paint.setStrokeWidth(5);
        // Draw each line on the canvas
        for (Line line : lines) {
            canvas.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, paint);
        }
    }
    public int reverseColor(int originalColor) {
        // Extract the RGB components
        int red = (originalColor >> 16) & 0xFF;
        int green = (originalColor >> 8) & 0xFF;
        int blue = originalColor & 0xFF;

        // Invert each component
        red = 255 - red;
        green = 255 - green;
        blue = 255 - blue;

        // Combine the components to get the reversed color
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
    public int alphaColor(int originalColor, int alpha) {
        int maskedAlpha = (alpha & 0xFF) << 24;
        int rgb = originalColor & 0x00ffffff;
        return maskedAlpha | rgb;
    }

    public static Line[] getLines(Point screen_size, int nVertical, int nHorizontal) {
        //given a margin to mark out the area
        int nMarginV = ConfigActivity.settings.getGridMarginTopBottom();
        int nMarginH = ConfigActivity.settings.getGridMarginLeftRight();

        Line[] lines = new Line[nVertical + nHorizontal];

        // Draw Vertical Lines
        for (int i = 0; i < nVertical; i++) {
            int x = (i + 1) * (screen_size.x - 2*nMarginH) / (nVertical + 1) + nMarginH;
            Point start = new Point(x, nMarginV);
            Point end = new Point(x, screen_size.y-nMarginV);
            lines[i] = new Line(start, end);
        }

        // Draw Horizontal Lines
        for (int i = 0; i < nHorizontal; i++) {
            int y = (i + 1) * (screen_size.y - 2*nMarginV) / (nHorizontal + 1) + nMarginV;
            Point start = new Point(nMarginH, y);
            Point end = new Point(screen_size.x-nMarginH, y);
            lines[i + nVertical] = new Line(start, end);
        }
        return lines;
    }
    public static class Line {
        public Point start;
        public Point end;

        public Line(Point start, Point end) {
            this.start = start;
            this.end = end;
        }
    }

    public void DrawLinesFixed(Canvas canvas, ArrayList<Point> points, int lastPoint, int color, int width)
    {
        //draw lines with fixed color and fixed width

        if (points.size() == 0) return;
        int lineIndex = 0;
        if (lastPoint >= points.size() ) {
            //DrawPattern(canvas,  alTouchPoints);
            DrawFinalPicture(canvas);
            return;
        }
        float[] lines = new float[points.size() * 4];
        for (int i=0; i<lastPoint; i++) {
            if   (FlyingPaths.BREAKPOINT.equals(points.get(i))
                            || FlyingPaths.BREAKPOINT.equals(points.get(i + 1))) {
                continue;
            }

            lines[lineIndex++] = points.get(i).x;
            lines[lineIndex++] = points.get(i).y;
            lines[lineIndex++] = points.get(i + 1).x;
            lines[lineIndex++] = points.get(i + 1).y;
            //Log.i(TAG, "DrawLinesFixed "+i+ ":"+points.get(i)+ "-"+points.get(i+1));

        }
        paint.setColor(color);
        paint.setStrokeWidth(width);
        canvas.drawLines(lines, paint);
        Log.i(TAG, "DrawLinesFixed done");
    }

    private void DrawNewTouchPoints(Canvas canvas)
    {
        //only draw new lines

        if (OperationCtrl.nState != CREATE_PATTERN) return;
        createBitmap();
        if (alTouchPoints.size() <= nLastDrawPoint+1)
        {
            canvas.drawBitmap(bufferBitmap, 0, 0, null);
            Log.i(TAG, "DrawNewTouchPoints no draw, size="+alTouchPoints.size() + ", last=" + nLastDrawPoint);
            return;
        }
        while (alTouchPoints.size() > nLastDrawPoint+1) {
            nLastDrawPoint++;
            if (FlyingPaths.BREAKPOINT.equals(alTouchPoints.get(nLastDrawPoint-1))
                    || FlyingPaths.BREAKPOINT.equals(alTouchPoints.get(nLastDrawPoint))
            ) {
                continue;
            }
            int thickness = alDrawThickness.get(nLastDrawPoint);
            int color = alDrawColor.get(nLastDrawPoint);

            int lineIndex = 0;
            float[] lines = new float[4];
            lines[lineIndex++] = alTouchPoints.get(nLastDrawPoint-1).x;
            lines[lineIndex++] = alTouchPoints.get(nLastDrawPoint-1).y;
            lines[lineIndex++] = alTouchPoints.get(nLastDrawPoint).x;
            lines[lineIndex++] = alTouchPoints.get(nLastDrawPoint).y;
            paint.setColor(color);
            paint.setStrokeWidth(thickness);

            bufferCanvas.drawLines(lines, paint);
        }
        canvas.drawBitmap(bufferBitmap, 0, 0, null);
        Log.i(TAG, "DrawNewTouchPoints drawn, size="+alTouchPoints.size() + ", last=" + nLastDrawPoint);
    }
    public void DrawLinesFineDelta(Canvas canvas, ArrayList<Point> points, int lastPoint, PointInfoList changeList)
    {
        //only draw new lines
        createBitmap();
        //draw with original color and width
        if (points.size() == 0) {
            Log.i(TAG, "DrawLinesFineDelta no draw "+lastPoint);
            return;
        }
        if (lastPoint >= points.size() ) {
            Log.i(TAG, "DrawLinesFineDelta no draw "+lastPoint + ">=" + points.size());
            canvas.drawBitmap(bufferBitmap, 0, 0, null);//20231212
            return;
        }
        if (lastPoint < 1) return;
        int prevPoint = lastPoint - 1;
        if   (FlyingPaths.BREAKPOINT.equals(points.get(prevPoint)) ){
            Log.i(TAG, "DrawLinesFineDelta BREAKPOINT prev "+ prevPoint);
            return;
        }
        if   (FlyingPaths.BREAKPOINT.equals(points.get(lastPoint))) {
            Log.i(TAG, "DrawLinesFineDelta BREAKPOINT last "+ lastPoint);
            return;
        }

        int changeSize = changeList.getSize();
        Log.i(TAG, "DrawLinesFineDelta to point "+ lastPoint  +"/"+ points.size() + ", changing "+nLastChangePoint+"/"+changeSize
                + " at "+ changeList.getPoint(nLastChangePoint).getIndex()
                + ", "+prevPoint + "="+ points.get(prevPoint)
                + " next=" +(nLastChangePoint+1 < changeSize ? changeList.getPoint(nLastChangePoint+1).getIndex() : 0));
        //get the change point
        if (nLastChangePoint+1 < changeSize && lastPoint > changeList.getPoint(nLastChangePoint+1).getIndex()){

            Log.i(TAG, "DrawLinesFineDelta change at point "+ lastPoint  +"/"+ points.size()
                    + ", changing "+nLastChangePoint+"/"+changeSize
                    + " from "+ changeList.getPoint(nLastChangePoint).getIndex()
                    + " to "+ changeList.getPoint(nLastChangePoint+1).getIndex());
            nLastChangePoint ++;
        }

        int thickness = 20;//changeList.getPoint(0).getWidth(); //first point
        int color = Color.RED;//changeList.getPoint(0).getColor();

        if (changeSize > nLastChangePoint) {
            thickness = changeList.getPoint(nLastChangePoint).getWidth(); //first point
            color = changeList.getPoint(nLastChangePoint).getColor();
        }
        int lineIndex = 0;
        float[] lines = new float[4];
        lines[lineIndex++] = points.get(prevPoint).x;
        lines[lineIndex++] = points.get(prevPoint).y;
        lines[lineIndex++] = points.get(lastPoint).x;
        lines[lineIndex++] = points.get(lastPoint).y;
        paint.setColor(color);
        paint.setStrokeWidth(thickness);

        bufferCanvas.drawLines(lines, paint);
        Log.i(TAG, "DrawLinesFineDelta drawn line "+prevPoint+ ","+lastPoint + " changeItem (C"+color+", T"+thickness+")"+ nLastChangePoint+ "/"+changeSize);
    }
    public void DrawFinalPicture(Canvas canvas)
    {
        Log.i(TAG, "DrawFinalPicture start" );
        if (ConfigActivity.settings.getPlaySingleColor())
            DrawLinesFixed(canvas, FlyingPaths.alBasePath, FlyingPaths.alBasePath.size()-1, ConfigActivity.settings.getPlayColor(), ConfigActivity.settings.getPlayWidth());
        else
        {
            canvas.drawBitmap(bufferBitmap, 0, 0, null);//20231212
            //DrawLinesFine(canvas, FlyingPaths.alBasePath, FlyingPaths.alBasePath.size()-1, FlyingPaths.pointInfoChangeList);
        }
        Log.i(TAG, "DrawFinalPicture end" );
    }
    public void Playback(Canvas canvas) //one time one dot
    {
        if (nCurrentPlayPoint >= FlyingPaths.alBasePath.size())
        {
            Log.i(TAG, "Playback just show buffered image, " + nCurrentPlayPoint + ">=" + FlyingPaths.alBasePath.size());
            //nCurrentPlayPoint = 0;
            //nLastChangePoint = 0;
            canvas.drawBitmap(bufferBitmap, 0, 0, null); //20240102
            return;
        }
        nCurrentPlayPoint ++;
        if (ConfigActivity.settings.getPlaySingleColor())
            DrawLinesFixed(canvas, FlyingPaths.alBasePath, nCurrentPlayPoint, ConfigActivity.settings.getPlayColor(), ConfigActivity.settings.getPlayWidth());
        else
        {
            //if (bDrawDelta) {
            //this method improve playback much by redraw only one line
            //compare: for 1100 points, at the same speed, original playback 55s, this one 18s
                DrawLinesFineDelta(canvas, FlyingPaths.alBasePath, nCurrentPlayPoint, FlyingPaths.pointInfoChangeList);
                canvas.drawBitmap(bufferBitmap, 0, 0, null);
            //}
            //else DrawLinesFine(canvas, FlyingPaths.alBasePath, nCurrentPlayPoint, FlyingPaths.pointInfoChangeList);
        }

    }

    public boolean IsEndOfPlayback()
    {
        return nCurrentPlayPoint >= FlyingPaths.alBasePath.size();
    }
    public void Undo()
    {
        int n = alTouchPoints.size();
        if (n>0) {
            boolean breakpoint = false;
            if ( alTouchPoints.get(n-1).equals(FlyingPaths.BREAKPOINT)
                && n>1) breakpoint = true;

            alTouchPoints.remove(n-1);
            alDrawThickness.remove(n-1);
            alDrawColor.remove(n-1);

            if (breakpoint) {
                alTouchPoints.remove(n-2);
                alDrawThickness.remove(n-2);
                alDrawColor.remove(n-2);
                AddBreakpoint();
            }
            //redraw all
            clearBitmap();

            postInvalidate();
        }
    }
    public void UndoFast()
    {
        int n = alTouchPoints.size();
        if (n>10) {
            alTouchPoints.subList(n-10, n).clear();
            alDrawThickness.subList(n-10, n).clear();
            alDrawColor.subList(n-10, n).clear();
            AddBreakpoint();
            clearBitmap();
            postInvalidate();
        }
        else if (n>0)
        {
            alTouchPoints.subList(0, n).clear();
            alDrawThickness.subList(0, n).clear();
            alDrawColor.subList(0, n).clear();
            AddBreakpoint();
            clearBitmap();
            postInvalidate();
        }
    }
    public void Clear()
    {
        Log.i(TAG, "Clear");
        bPathAvailable = false;
        alTouchPoints.clear();
        alDrawThickness.clear();
        alDrawColor.clear();

        clearBitmap();
        postInvalidate();
    }
    private void AddBreakpoint()
    {

        alTouchPoints.add(new Point(FlyingPaths.BREAKPOINT));
        alDrawColor.add(new Integer(CreatePattern.nDrawColor));
        alDrawThickness.add(new Integer(CreatePattern.nDrawThickness));
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        /* purpose: use Press to bring up configuration, to allow Move for future development
            - otherwise just use Down (0) to call up configuration intent
        but there is no Press event, so to detect it here
        the scanning is about 10-20ms cycle, and single touch could be easily reach 100ms which keeps giving Move (2)
        solution: count the Move (2) before Up (1), if less than 10, then it is Press, otherwise it is Move
         */
        boolean bPressed = false;
        boolean bNewPath = false;//if moving, add a path
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                Log.i(TAG, "onTouchEvent ACTION_DOWN state="+OperationCtrl.nState);
                if (OperationCtrl.nState == CREATE_PATTERN) CreatePattern.HideWarning();
                if (bAllowShowAllPaths) {
                    bShowAllPaths = true;
                    postInvalidate();
                }

                break;
            case MotionEvent.ACTION_UP:
                //if teleport, add a breakpoint
                if (bPointAdded)
                {
                    bPointAdded = false;
                    AddBreakpoint();
                }
                Title="";
                postInvalidate();
                nTouchCount = 0;
                break;
            case MotionEvent.ACTION_MOVE:

                nTouchCount ++;
                if (preTouchPoint.x != ((int)event.getX()) || preTouchPoint.y != ((int)event.getY()))
                {
                    preTouchPoint.x = ((int)event.getX());
                    preTouchPoint.y = ((int)event.getY());
                    if (OperationCtrl.nState == CREATE_PATTERN) {
                        alTouchPoints.add(new Point(preTouchPoint));
                        alDrawColor.add(new Integer(CreatePattern.nDrawColor));
                        alDrawThickness.add(new Integer(CreatePattern.nDrawThickness));
                        bPointAdded = true;
                        postInvalidate();//show up immediately
                        int i = alTouchPoints.size() - 1;
                        Log.i(TAG, "onTouchEvent Moving: count " + nTouchCount + "(" + (int) event.getX() + "," + (int) event.getY() + ") as item " + i + ": " + alTouchPoints.get(i)
                            + " C="+alDrawColor.get(i));
                    }
                }
                if (nTouchCount >= SHORTEST_PATH)
                {
                    bPathAvailable = true;
                    CreatePattern.EnableButtonSaveExport(true);
                }
                break;
        }

        return true;
    }
    static public void SavePattern()
    {
        bPathAvailable = false;
        //filename fixed
        FileSaveLoad file=new FileSaveLoad(context, FileSaveLoad.FILE_OPERATION.SAVE);

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "SavePattern: "+file.getFileName());
        }
    }
    public void LoadPatternPrev()
    {
        Clear();
        FileSaveLoad file=new FileSaveLoad(getContext(), FileSaveLoad.FILE_OPERATION.PREV);
        postInvalidate();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "LoadPatternPrev: "+file.getFileName() + ": "+ ConfigActivity.settings.getText());
        }
    }
    public void LoadPatternNext()
    {
        Clear();
        FileSaveLoad file=new FileSaveLoad(getContext(), FileSaveLoad.FILE_OPERATION.NEXT);
        postInvalidate();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "LoadPatternNext: "+file.getFileName() + ": "+ ConfigActivity.settings.getText());
        }
    }
    public void DeletePattern()
    {
        Clear();
        FileSaveLoad file=new FileSaveLoad(getContext(), FileSaveLoad.FILE_OPERATION.DELETE);
        postInvalidate();
    }
    public void ExportPattern()
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageName = "FPattern_" + timeStamp + ".jpg";

        File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File file = new File(sdCard, imageName);
        FileOutputStream fos = null;
        try {
            // Make sure the Pictures directory exists.
            sdCard.mkdirs();

            fos = new FileOutputStream(file);  //android.system.ErrnoException: open failed: EACCES (Permission denied)
        } catch (FileNotFoundException e) {
            Toast.makeText(getContext(), file + getResources().getString(R.string.text_open_failed) + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        //the dark image is due to color value, should set color=0xFFrrggbb
        /*  //this method will get dark image
        setDrawingCacheEnabled(true);
        Bitmap b = getDrawingCache();
        //b.setHasAlpha(true); //to set transparency
        //Canvas canvas = new Canvas(b);
        //canvas.drawColor(ConfigActivity.getCfgBackground(ConfigActivity.cfgBackground)); //trying to solve darken image
        b.compress(Bitmap.CompressFormat.PNG, 100, fos);
        setDrawingCacheEnabled(false); //for next screen update
        */
        Bitmap b = screenShot(this);
        b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        try {
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Toast.makeText(getContext(), file + getResources().getString(R.string.text_save_failed) + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        Toast.makeText(getContext(), file + "", Toast.LENGTH_SHORT).show();
        //add to gallary
        getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.toString())));
    }
    public static Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable =view.getBackground();
        if (bgDrawable!=null)
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        else
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(ConfigActivity.getCfgBackground(ConfigActivity.settings.getBackground()));
        view.draw(canvas);
        return returnedBitmap;
    }
    public Bitmap screenShot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }
}
