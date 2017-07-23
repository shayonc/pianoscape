package piano.pianotrainer.score_importing;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-15.
 */

public class ScoreImgProc {
    private static final String TAG = "ScoreImgProc";

    private Mat originalImg;
    private Mat grayImg;
    private Mat binarizedImg;
    private Mat noStaffLinesImg;
    private final int MAX_STAFF_LINE_THICKNESS = 2; //TODO detect this dynamically
    //something to store rows of stafflines
    private ArrayList<Integer> staffLineRowIndicies;

    private ArrayList<ArrayList<Integer>> staffs;
    private int staffLineDiff;
    private ArrayList<ArrayList<Rect>> staffObjects;
    private boolean[][] staffsVisited;
    private int curObjectTop;
    private int curObjectBottom;
    private int curObjectLeft;
    private int curObjectRight;


    public ScoreImgProc(Bitmap bmpImg){
        staffLineRowIndicies = new ArrayList<Integer>();
        originalImg = new Mat();
        grayImg = new Mat();
        binarizedImg = new Mat();
        noStaffLinesImg = new Mat();
        Utils.bitmapToMat(bmpImg,originalImg);
        Log.d(TAG,String.format("Converted original image to %d by %d MAT",originalImg.cols(),
                originalImg.rows()));
    }

    //takes original image -> grayscales -> binarize it
    public void binarize(){
        grayImg = ImageUtils.bgrToGrayscale(this.originalImg);
        Log.d(TAG,String.format("Created grayscale %d by %d img MAT",grayImg.cols(),grayImg.rows()));
        try {
            //TODO: See if adaptive threshold is better
            //Imgproc.adaptiveThreshold(grayImg, binarizedImg, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, -2);
            //Lots of salt pepper noise so a lower threshold value will give stronger blacks which will help
            Imgproc.threshold(grayImg,binarizedImg,50,255,THRESH_BINARY);
            Log.d(TAG,String.format("Created binarized %d by %d img MAT",binarizedImg.cols(),binarizedImg.rows()));
        }
        catch(Exception e){
            Log.d(TAG,e.getMessage());
        }
    }

    //TODO: Find out if other one is suffice and scrap this
    //EC Uses Vertical Morphology but results weren't great for some music elements with lines thru em
    public void removeStaffLines(){
        // Create structure element for extracting vertical lines through morphology operations
        Point pt = new Point(-1,-1); //"default"
        //via paint max staff line width is 2
        Size kernelHeight = new Size(1,5);
        Mat verticalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelHeight);
        //erode out the lines
        Imgproc.erode(binarizedImg,noStaffLinesImg,verticalStructure,pt,1);
        //should re-amp the blackness of remaining black things in the picture
        //Imgproc.dilate(noStaffLinesImg,noStaffLinesImg,verticalStructure,pt,1);
    }

    //Uses horizontal morphology and subtracts from the original img
    public void removeStaffLines(boolean horzMorph){
        Mat isoStaffLinesImg = new Mat();
        // Relative measure which seemed ok
        int horizontalsize = binarizedImg.cols() / 30;
        Size kernelWidth = new Size(horizontalsize,1);
        Point pt = new Point(-1,-1); //current pixel is the 'center' when applying operations
        // Create structure element for extracting horizontal lines through morphology operations
        Mat horizontalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelWidth);
        // Apply morphology operations
        Imgproc.erode(binarizedImg, isoStaffLinesImg, horizontalStructure, pt,2);
        // "reamps" the remaining elements on the page (trailing parts of staffline_
        Imgproc.dilate(isoStaffLinesImg, noStaffLinesImg, horizontalStructure, pt,2);

        //ideally the image after morphology only contains staff lines which are no subtracted out
        Core.subtract(binarizedImg,noStaffLinesImg,noStaffLinesImg);
        //now lets try vertically dilating it to stich gaps
        Point pt2 = new Point(-1,-1); //"default"
        //via paint max staff line width is 2
        Size kernelHeight = new Size(1,2);
        Mat verticalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelHeight);
        //vertical dilate will look 1 pixel away vertically and take max
        Imgproc.dilate(noStaffLinesImg,noStaffLinesImg,verticalStructure,pt,2);

//        horizontalsize = 2; //Try this for neighbours
//        kernelWidth = new Size(horizontalsize,1);
//        pt = new Point(-1,-1); //current pixel is the 'center' when applying operations
//        // Create structure element for extracting horizontal lines through morphology operations
//        horizontalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelWidth);
//        Imgproc.dilate(noStaffLinesImg, noStaffLinesImg, horizontalStructure, pt,2); //you can play around with iterations here

        //gapstich
        staffLineDetect(isoStaffLinesImg);
    }

    public void staffLineDetect(Mat staffLinesImg){
        Mat curRow;
        //TODO: Make structured val not hacky estimate
        int thresholdForStaffline = binarizedImg.cols()/2;
        double[] rowTotalVals;
        int curRowTotal;
        int y;
        double[] mVal;
        for(int i = 0; i<staffLinesImg.rows(); i++){
            mVal = staffLinesImg.get(i,150);
            rowTotalVals = Core.sumElems(staffLinesImg.row(i)).val;
            curRowTotal = (int) rowTotalVals[rowTotalVals.length-1]/255;
            if(curRowTotal > thresholdForStaffline){
                staffLineRowIndicies.add(i);
            }
        }
        //Note due to inconsistent staff line thickness the length isn't guarenteed to be mod 5
        //TODO: Loop through and cluster adjacent stafflines
        Log.d(TAG,String.format("Detected %d staff line positions!", staffLineRowIndicies.size()));
    }

//    public void vertGapStich(Mat staffLinesImg){
//        double[] curStaffImgValVec;
//        int curStaffImgVal;
//        for(int rowIndex : staffLineRowIndicies){
//            for(int colIndex = 0; colIndex < staffLinesImg.cols(); colIndex++){
//                curStaffImgValVec = staffLinesImg.get(rowIndex,colIndex);
//                curStaffImgVal = (int) curStaffImgValVec[curStaffImgValVec.length - 1];
//                //white
//                if(curStaffImgVal == 0){
//
//                }
//            }
//        }
//    }

    //Returns the image after staff line removal
    public Bitmap getNoStaffLinesImg(){
        Bitmap bmp = Bitmap.createBitmap(noStaffLinesImg.cols(),noStaffLinesImg.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(noStaffLinesImg,bmp);
        return bmp;
    }

    //Returns the original image after binarization as a Bitmap
    public Bitmap getBinImg(){
        Bitmap bmp = Bitmap.createBitmap(binarizedImg.cols(),binarizedImg.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(binarizedImg,bmp);
        Log.d(TAG,String.format("Creating binarized %d by %d img",bmp.getWidth(),bmp.getHeight()));
        return bmp;
    }

    public ArrayList<ArrayList<Integer>> refineStaffLines() {
        staffs = new ArrayList<ArrayList<Integer>>();
        staffs.add(new ArrayList<Integer>());

        int lineAvg = 0;
        int rowCountPerLine = 0;
        int prevRow = staffLineRowIndicies.get(0)-1;
        int lineCount = 0;
        int staffCount = 0;
        for (int i = 0; i < staffLineRowIndicies.size(); i++) {
            int curRow = staffLineRowIndicies.get(i);
            if (curRow == prevRow+1) {
                lineAvg += curRow;
                rowCountPerLine++;
                prevRow = curRow;
            }
            else {
                lineAvg = lineAvg / rowCountPerLine;
                staffs.get(staffCount).add(lineAvg);
                lineCount++;
                if (lineCount % 10 == 0 && lineCount != 0) {
                    staffCount++;
                    staffs.add(new ArrayList<Integer>());
                }
                if (i+1 < staffLineRowIndicies.size()) {
                    prevRow = staffLineRowIndicies.get(i+1)-1;
                }
                lineAvg = 0;
                rowCountPerLine = 0;
            }
        }

        //TODO: get an average from all staff lines instead of from one set of lines
        staffLineDiff = staffs.get(0).get(3)-staffs.get(0).get(2);

        //TODO: instead of this hack, we need to fix the bug of why the last staff line is not detected
        int lastSize = staffs.get(staffCount).size();
        staffs.get(staffCount).add( staffs.get(staffCount).get(lastSize-1) + staffLineDiff );

        return staffs;
    }

    public ArrayList<ArrayList<Rect>> detectObjects() {
        //TODO: iterate through all staffs
        staffObjects = new ArrayList<ArrayList<Rect>>();
        staffsVisited = new boolean[noStaffLinesImg.height()][noStaffLinesImg.width()];

        int i = 0;
        ArrayList<Integer> staffLines = staffs.get(i);
        staffObjects.add(new ArrayList<Rect>());
        int topBound = staffLines.get(0);
        int bottomBound = staffLines.get(9);
        int leftBound = 225;
        int rightBound = 1610;

        //ArrayList<Rect> tabuRects = new ArrayList<Rect>();
        curObjectTop = noStaffLinesImg.height();
        curObjectBottom = 0;
        curObjectLeft = noStaffLinesImg.width();
        curObjectRight = 0;
        for (int col = leftBound; col < rightBound; col++) {
            for (int row = topBound; row < bottomBound; row++) {

                if (!staffsVisited[row][col]) {
                    double[] data = noStaffLinesImg.get(row, col);

                    if (data[data.length-1] == 255.0) {
                        fillSearch(row, col);
                        staffObjects.get(i).add(new Rect(curObjectLeft, curObjectTop, curObjectRight, curObjectBottom));
                        //tabuRects.add(new Rect(curObjectLeft, curObjectTop, curObjectRight, curObjectBottom));
                        markObjectRects();
                        curObjectTop = noStaffLinesImg.height();
                        curObjectBottom = 0;
                        curObjectLeft = noStaffLinesImg.width();
                        curObjectRight = 0;
                        //cleanTabuRects(row, col, tabuRects);
                    }
                }
            }
        }

        return staffObjects;
    }

    public void fillSearch(int row, int col) {

        if (row > curObjectBottom) {
            curObjectBottom = row;
        }
        if (row < curObjectTop) {
            curObjectTop = row;
        }
        if (col > curObjectRight) {
            curObjectRight = col;
        }
        if (col < curObjectLeft) {
            curObjectLeft = col;
        }
        staffsVisited[row][col] = true;

        if (!staffsVisited[row-1][col] && noStaffLinesImg.get(row-1, col)[3] == 255.0) {
            fillSearch(row-1, col);
        }
        if (!staffsVisited[row+1][col] && noStaffLinesImg.get(row+1, col)[3] == 255.0) {
            fillSearch(row+1, col);
        }
        if (!staffsVisited[row][col-1] && noStaffLinesImg.get(row, col-1)[3] == 255.0) {
            fillSearch(row, col-1);
        }
        if (!staffsVisited[row][col+1] && noStaffLinesImg.get(row, col+1)[3] == 255.0) {
            fillSearch(row, col+1);
        }
    }

    public double[] getImageData(int row, int col) {
        return noStaffLinesImg.get(row, col);
    }

    public void markObjectRects() {
        for (int row = curObjectTop; row < curObjectBottom; row++) {
            for (int col = curObjectLeft; col < curObjectRight; col++) {
                staffsVisited[row][col] = true;
            }
        }
    }

    public boolean inTabuRects(int row, int col, ArrayList<Rect> tabuRects) {
        for (Rect rect : tabuRects) {
            if (row >= rect.top && row <= rect.bottom
                    && col >= rect.left && col <= rect.right) {
                return true;
            }
        }
        return false;
    }

    public void cleanTabuRects(int row, int col, ArrayList<Rect> tabuRects) {
        ArrayList<Integer> toDelete = new ArrayList<Integer>();
        for (int i = 0; i < tabuRects.size(); i++) {
            if (col > tabuRects.get(i).right) {
                tabuRects.remove(i);
                i--;
            }
        }
    }
}
