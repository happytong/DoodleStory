package com.tongs.doodlestory;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;

import views.DrawView;

public class FlyingPaths {
    //BREAKPOINT is a flog of end of a line, not showing up in the path, to allow multiple lines of a path
    static public Point BREAKPOINT = new Point(MainActivity.ScreenSize.x+100, MainActivity.ScreenSize.y+100);

    static private String TAG="[dbg]FlyingPaths";
    static public ArrayList<Point> alBasePath = new ArrayList<>();
    static public PointInfoList pointInfoChangeList = new PointInfoList();
    static private PointInfoList oriPointInfoChangeList = new PointInfoList();
    static public ArrayList<ArrayList<Point>> alPaths = new ArrayList<ArrayList<Point>>(); //2D for multiple paths

    static public ArrayList<Integer> alPosition = new ArrayList<Integer>(); //current position for the each path

    static public void SetBasePath(ArrayList<Point> points)
    {
        alBasePath.clear();
        oriPointInfoChangeList.resetPoints();

        if (ConfigActivity.settings.getGradient())
        {
            SetOriginalChangePointsGradient(points);
            //insert more points if the original points distance too big
            SetBasePathTeleport(points);
        }
        else
        {
            SetOriginalChangePoints(points);
            //insert more points if the original points distance too big
            SetBasePathTeleport(points);
        }
    }
    static private void SetOriginalChangePointsGradient(ArrayList<Point> points)
    {
        //change means: color or width which need different pen when drawing
        //gradient: at start point: width from 1 to actual every point with step 2
        //  at the end point: reverse, but by step 1
        int size = points.size();
        if (size == 0) return;

        //1. find all the breakbpoints, every point has start x and end y as one line
        ArrayList<Point> alLines = new ArrayList<>();
        //at least has one line
        int x = 0, y = 0;
        for (int i=1; i< size; i++) {
            if (points.get(i).equals(BREAKPOINT)) {
                y = i;  //point index
                //add to the line: point x to point y
                alLines.add(new Point(x, y));
                Log.i(TAG, "SetOriginalChangePointsGradient BREAKPOINT at point " + i + " ["+x+", "+y+"]");
                x = i+1; //next line starting point
            }
        }
        Log.i(TAG, "SetOriginalChangePointsGradient orig total lines="+ alLines.size() + ", last point="+(size-1));

        //2. for each line, at the start and end, color and width gradient change
        int length = 0; //y-x, each line length for full change, at least >= width/2 + width
        int color = 0;
        int width = 0;
        //changing point
        int changeRange = 0; //range of start and end
        int changingC = 0;  //changing color
        int changingW = 0; //changing width
        int changingP = 0; //changing point (position)
        int changeStep = 0; //how big each for the gradient change

        int lineNum = 0;
        for (Point line : alLines)
        {
            lineNum ++;
            length = line.y - line.x; //length is the point number in this line, not actual line lehgth (physical pixels)
            color = DrawView.alDrawColor.get(line.x); //one line has the same color
            width = DrawView.alDrawThickness.get(line.x); //orignal line has the same width
            Log.i(TAG, "SetOriginalChangePointsGradient line "+lineNum + " length="+length+" original "
                    + " (" + color + ", " + width + ") P="+ points.get(line.x) );
            //assume the center is the real Color and Width
            //in front of the center, small to Width
            changeRange = width > length ? length/2 : width; //the range of a lime to be changed with new lines
            if (changeRange == 0) {
                Log.i(TAG, "SetOriginalChangePointsGradient line "+lineNum + " too short, length="+length+" width=" + width
                        + ", p"+line.x + ", c"+color);

                oriPointInfoChangeList.addPoint(line.x, color, width);// no difference?
                continue;
            }
            else if (changeRange == 1)
            {
                Log.i(TAG, "SetOriginalChangePointsGradient dbg grad "+lineNum + " changeRange=1");
            }

            changeStep =  width/changeRange > 2 ? width/changeRange : 2;  //how many steps to change to this range
            changingC = color;
            changingW = 1; //from smallest to width
            for (int n=1; n<=changeRange; n++){
                changingP = line.x+n-1;
                changingC = color;
                changingW += changeStep;
                if (changingW > width) {
                    if (n == 1) // at least should add once
                        oriPointInfoChangeList.addPoint(changingP, changingC, changingW/2);//take half of the width
                    break;
                }
                oriPointInfoChangeList.addPoint(changingP, changingC, changingW);
                Log.i(TAG, "SetOriginalChangePointsGradient grad "+lineNum + " start range="+changeRange+"@ " +changingP
                        + " P="+ points.get(changingP)
                        + " (" + changingC + ", " + changingW + ")/" + oriPointInfoChangeList.getSize());
            }
            //ending part
            changeRange = width > length/2 ? length/2 : width;
            changeStep =  width/changeRange > 0 ? width/changeRange : 1;
            changingC = color;
            changingW = width; //from width to smallest 1
            for (int n=changeRange; n>1; n--){
                changingP = line.y - n;
                changingC = color;
                changingW -= changeStep;
                if (changingW < 1) break;
                oriPointInfoChangeList.addPoint(changingP, changingC, changingW);
                Log.i(TAG, "SetOriginalChangePointsGradient grad "+lineNum + " end range="+changeRange+"@ " +changingP
                        + " (" + changingC + ", " + changingW + ")/" + oriPointInfoChangeList.getSize());
            }
        }

    }
    static private void SetOriginalChangePoints(ArrayList<Point> points)
    {
        int size = points.size();
        if (size == 0) return;

        int color = DrawView.alDrawColor.get(0);
        int width = DrawView.alDrawThickness.get(0);
        oriPointInfoChangeList.addPoint(0, color, width); //first point
        Log.i(TAG, "SetOriginalChangePoints point 0/"+size
                + " (" + color + ", " + width + ")/" + oriPointInfoChangeList.getSize());
        //read from DrawView
        for (int i=1; i< size; i++)
        {
            if (points.get(i).equals(BREAKPOINT)) {
                Log.i(TAG, "SetOriginalChangePoints BREAKPOINT at point "+i);
                continue;
            }
            if (color != DrawView.alDrawColor.get(i) || width != DrawView.alDrawThickness.get(i))
            {
                color = DrawView.alDrawColor.get(i);
                width = DrawView.alDrawThickness.get(i);
                oriPointInfoChangeList.addPoint(i, color, width);
                Log.i(TAG, "SetOriginalChangePoints "+oriPointInfoChangeList.getSize() + ", point "+ i +"/"+size
                        + " (" + color + ", " + width + ")/" + oriPointInfoChangeList.getSize());
            }
        }

    }
    static public void SetBasePathTeleport(ArrayList<Point> points)
    {
        //this function to insert some points to make the path more fluent
        alBasePath.clear();
        pointInfoChangeList.resetPoints();
        int nNextChnageIndex = 0;
        int nNextChangePoint = oriPointInfoChangeList.getPoint(0).getIndex();

        //copy and refill the points if not BREAKPOINT
        int nOriSize = points.size();
        int nOriChangeSize = oriPointInfoChangeList.getSize();
        for (int i=0; i<nOriSize-1; i++)
        {
            Point p1 = new Point(points.get(i));
            alBasePath.add(p1);
            if (p1.equals(BREAKPOINT)) {
                Log.i(TAG, "SetBasePathTeleport BREAKPOINT p1 "+i+"/"+nOriSize+", new size="+ alBasePath.size() +", change index="+nNextChnageIndex );
                continue;
            }

            Point p2 = points.get(i+1);
            if (p2.equals(BREAKPOINT)) {
                alBasePath.add(p2);
                Log.i(TAG, "SetBasePathTeleport BREAKPOINT p2 "+(i+1)+"/"+nOriSize+", new size="+ alBasePath.size() +", change index="+nNextChnageIndex );
                continue;
            }
            if (i >= 170 && i <= 185)
                Log.i(TAG, "SetBasePathTeleport at "+i + ", P="+p1 + ", C"+oriPointInfoChangeList.getPoint(nNextChnageIndex).getColor());
            int offsetX =p2.x - p1.x;
            int offsetY =p2.y - p1.y;
            int timeX = Math.abs(offsetX/20);
            int timeY = Math.abs(offsetY/20);
            if (timeX > 0 || timeY > 0)
            {
                int times = Math.max(timeX, timeY);
                int stepX = offsetX/times;
                int stepY = offsetY/times;
                while (--times >0)
                {
                    Point point = new Point(p1.x+stepX, p1.y+stepY);
                    alBasePath.add(point);
                    Log.i(TAG, "SetBasePathTeleport refill insert "+times+" @"+(alBasePath.size()-1)+point);
                }
            }
            Log.i(TAG, "SetBasePathTeleport check point="+i + ", change index "+nNextChnageIndex+"/"+nOriChangeSize + " next point="+ nNextChangePoint );
            if ( nOriChangeSize > nNextChnageIndex && i >= nNextChangePoint)
            {
                int color = oriPointInfoChangeList.getPoint(nNextChnageIndex).getColor();
                int width = oriPointInfoChangeList.getPoint(nNextChnageIndex).getWidth();
                pointInfoChangeList.addPoint(alBasePath.size()-1, color, width );

                Log.i(TAG, "SetBasePathTeleport added pointInfo="+nNextChnageIndex + " from "+i + " to "+ (alBasePath.size()-1)
                    + "("+color+", "+width+")");
                nNextChnageIndex ++;
                if ( nOriChangeSize > nNextChnageIndex) nNextChangePoint = oriPointInfoChangeList.getPoint(nNextChnageIndex).getIndex();
            }
        }
        alBasePath.add(new Point(points.get(points.size()-1)));//add last point (of original path)

        Log.i(TAG, "SetBasePathTeleport done, size="+alBasePath.size() + " last=" +points.get(points.size()-1)
            + ", change="+pointInfoChangeList.getSize() + ", ori chg="+nOriChangeSize);

        if (BuildConfig.DEBUG) {
            int nNewChangeSize = pointInfoChangeList.getSize();
            for (int n=0; n<nNewChangeSize; n++)
            {
                //Log.i(TAG, "SetBasePathTeleport change "+n + "/"+nNewChangeSize);
                Log.i(TAG, "SetBasePathTeleport change "+n+ " at point "+ pointInfoChangeList.getPoint(n).getIndex()
                      + ", C" + pointInfoChangeList.getPoint(n).getColor() + ", T"+ pointInfoChangeList.getPoint(n).getWidth()
                        + ", P="+alBasePath.get(pointInfoChangeList.getPoint(n).getIndex()) );
            }
        }
    }

}

