package piano.pianotrainer.scoreImport;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Interpolator;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Mat;
import org.opencv.ml.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.KNearest;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.CV_HOUGH_GRADIENT;
import static org.opencv.imgproc.Imgproc.HoughCircles;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.ellipse;
import static org.opencv.imgproc.Imgproc.resize;


public class ScoreProcessor {
    static final String TAG = "ScoreProcessor";

    static final int TRAIN_WIDTH = 30;
    static final int TRAIN_HEIGHT = 30;

    Mat originalImg;
    Mat grayImg;
    Mat binarizedImg;
    Mat noStaffLinesImg;
    final int MAX_STAFF_LINE_THICKNESS = 2; //TODO detect this dynamically
    List<Integer> staffLineRowIndices;

    int staffLineDiff;
    List<List<Integer>> staffs;
    List<List<Rect>> staffObjects;

    // intermediate values to keep track of object bounds
    int curObjectTop;
    int curObjectBottom;
    int curObjectLeft;
    int curObjectRight;

    KNearest knn;
    Mat trainData, testData;
    List<Integer> trainLabs = new ArrayList<Integer>(),
            testLabs = new ArrayList<Integer>();

    public ScoreProcessor(Bitmap bmpImg){
        staffLineRowIndices = new ArrayList<Integer>();
        originalImg = new Mat();
        grayImg = new Mat();
        binarizedImg = new Mat();
        noStaffLinesImg = new Mat();
        Utils.bitmapToMat(bmpImg,originalImg);
        //Used to train for various symbol by passing in a label and test data
        knn = KNearest.create();
        trainData = new Mat();
        testData = new Mat();
        trainData.convertTo(trainData, CvType.CV_32F);
        testData.convertTo(testData, CvType.CV_32F);

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

        //ideally the image after morphology only contains staff lines which are not subtracted out
        Core.subtract(binarizedImg,noStaffLinesImg,noStaffLinesImg);
        //now lets try vertically dilating it to stich gaps
        Point pt2 = new Point(-1,-1); //"default"
        //via paint max staff line width is 2
        //Will need to make sure flood fill works 100% of the time
        Size kernelHeight = new Size(1,3);
        Mat verticalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelHeight);
        //vertical dilate will look 1 pixel away vertically and take max
        Imgproc.dilate(noStaffLinesImg,noStaffLinesImg,verticalStructure,pt,2);

        // horizontalsize = 2; //Try this for neighbours
        // kernelWidth = new Size(horizontalsize,1);
        // pt = new Point(-1,-1); //current pixel is the 'center' when applying operations
        // // Create structure element for extracting horizontal lines through morphology operations
        // horizontalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelWidth);
        // Imgproc.dilate(noStaffLinesImg, noStaffLinesImg, horizontalStructure, pt,2); //you can play around with iterations here

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
                staffLineRowIndices.add(i);
            }
        }
        //Note due to inconsistent staff line thickness the length isn't guarenteed to be mod 5
        //TODO: Loop through and cluster adjacent stafflines
        Log.d(TAG,String.format("Detected %d staff line positions!", staffLineRowIndices.size()));
    }

    //    public void vertGapStich(Mat staffLinesImg){
    //        double[] curStaffImgValVec;
    //        int curStaffImgVal;
    //        for(int rowIndex : staffLineRowIndices){
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

    public List<List<Integer>> refineStaffLines() {
        staffs = new ArrayList<List<Integer>>();
        staffs.add(new ArrayList<Integer>());

        int lineAvg = 0;
        int rowCountPerLine = 0;
        int prevRow = staffLineRowIndices.get(0)-1;
        int lineCount = 0;
        int staffCount = 0;

        // gets one row index for each staff line
        for (int i = 0; i < staffLineRowIndices.size(); i++) {
            int curRow = staffLineRowIndices.get(i);
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
                if (i+1 < staffLineRowIndices.size()) {
                    prevRow = staffLineRowIndices.get(i+1)-1;
                }
                lineAvg = 0;
                rowCountPerLine = 0;
            }
        }

        // TODO: maybe get the average row diff between staff lines
        staffLineDiff = staffs.get(0).get(3)-staffs.get(0).get(2);

        //TODO: instead of this hack, we need to fix the bug of why the last staff line is not detected
        int lastSize = staffs.get(staffCount).size();
        staffs.get(staffCount).add( staffs.get(staffCount).get(lastSize-1) + staffLineDiff );

        return staffs;
    }




    public List<List<Rect>> detectObjects() {
        //TODO: iterate through all staffs
        staffObjects = new ArrayList<List<Rect>>();
        boolean[][] staffsVisited = new boolean[noStaffLinesImg.height()][noStaffLinesImg.width()];

        //int i = 0;
        List<Integer> staffLines;
        int topBound, bottomBound, leftBound, rightBound;
        for(int i = 0 ; i < staffs.size(); i++){
            staffLines = staffs.get(i);
            staffObjects.add(new ArrayList<Rect>());
            topBound = staffLines.get(0);
            bottomBound = staffLines.get(9);
            leftBound = 0;
            rightBound = noStaffLinesImg.width();

            //ArrayList<Rect> tabuRects = new ArrayList<Rect>();
            curObjectTop = noStaffLinesImg.height();
            curObjectBottom = 0;
            curObjectLeft = noStaffLinesImg.width();
            curObjectRight = 0;
            int padding = 4;

            for (int col = leftBound; col < rightBound; col++) {
                for (int row = topBound; row < bottomBound; row++) {

                    if (!staffsVisited[row][col]) {
                        double[] data = noStaffLinesImg.get(row, col);

                        if (data[data.length-1] == 255.0) {
                            fillSearch(row, col, staffsVisited);
                            staffObjects.get(i).add(new Rect(curObjectLeft-padding, curObjectTop-padding, curObjectRight+padding, curObjectBottom+padding));
                            //tabuRects.add(new Rect(curObjectLeft, curObjectTop, curObjectRight, curObjectBottom));
                            markObjectRects(padding, staffsVisited);
                            curObjectTop = noStaffLinesImg.height();
                            curObjectBottom = 0;
                            curObjectLeft = noStaffLinesImg.width();
                            curObjectRight = 0;
                            //cleanTabuRects(row, col, tabuRects);
                        }
                    }
                }
            }
        }


        return staffObjects;
    }

    //Trains the knn with a label (ID for symbol) and the bitmap img
    //All images must be formatted to be the same size (resize) and a horizontal vector (reshape)
    public void addSample(Bitmap bmp , int label){
        //most notes around 30x80
        Size size = new Size(TRAIN_WIDTH,TRAIN_HEIGHT);
        Mat curFeature = new Mat();
        Mat resizedImg = new Mat();
        Utils.bitmapToMat(bmp, curFeature);
        Imgproc.resize(curFeature, resizedImg, size);
        resizedImg.convertTo(resizedImg, CvType.CV_32F);
        resizedImg = resizedImg.reshape(1,1);
        trainData.push_back(resizedImg);
        trainLabs.add(label);

    }

    public boolean testMusicObjects(){
        List<List<Integer>> results = new ArrayList<List<Integer>>();

        Rect curRect;
        boolean passed = true;
        for(int i = 0; i < staffObjects.size(); i++){
            results.add(new ArrayList<Integer>());
            for(int j = 0; j < staffObjects.get(i).size(); j++){
                curRect = staffObjects.get(i).get(j);
                results.get(i).add(testKnnMat(extractFromNoStaffImg(curRect)));
            }
        }
        return true;
    }

    public Mat extractFromNoStaffImg(Rect r){
        org.opencv.core.Rect cvRect = new org.opencv.core.Rect(r.left, r.top, r.width(), r.height());
        return new Mat(noStaffLinesImg, cvRect);
    }

    public Bitmap getStaffObject(int staffLine, int index) {
        if(index < 0 || index >= staffObjects.get(staffLine).size()){
            throw new IndexOutOfBoundsException();
        }
        Bitmap bmp = Bitmap.createBitmap(staffObjects.get(staffLine).get(index).width(),staffObjects.get(staffLine).get(index).height(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(extractFromNoStaffImg(staffObjects.get(staffLine).get(index)), bmp);
        return bmp;
    }

    //Finds the nearest neighbour of a bitmap img and returns whether its label is what we expect
    public boolean testKnn(Bitmap bmp, int label){
        //Convert to Mat
        Mat curFeature = new Mat();
        Utils.bitmapToMat(bmp, curFeature);
        return testKnnMat(curFeature, label);
    }

    public boolean testKnnMat(Mat symbolToTest, int label){
        symbolToTest.convertTo(symbolToTest, CvType.CV_32F);
        Size size = new Size(TRAIN_WIDTH, TRAIN_HEIGHT);
        Mat resizedImg = new Mat();
        Imgproc.resize(symbolToTest, resizedImg, size);
        resizedImg.convertTo(resizedImg, CvType.CV_32F);
        resizedImg = resizedImg.reshape(1,1);
        Mat res = new Mat();
        res.convertTo(res, CvType.CV_32F);
        //Test Mat against KNN
        float p = knn.findNearest(resizedImg.reshape(1,1), 1 ,res);
        return Math.round(p) == label;
    }

    public int testKnnMat(Mat symbolToTest){
        symbolToTest.convertTo(symbolToTest, CvType.CV_32F);
        Size size = new Size(TRAIN_WIDTH, TRAIN_HEIGHT);
        Mat resizedImg = new Mat();
        Imgproc.resize(symbolToTest, resizedImg, size);
        resizedImg.convertTo(resizedImg, CvType.CV_32F);
        resizedImg = resizedImg.reshape(1,1);
        Mat res = new Mat();
        res.convertTo(res, CvType.CV_32F);
        //Test Mat against KNN
        float p = knn.findNearest(resizedImg.reshape(1,1), 1 ,res);
        return Math.round(p);
    }

    public void trainKnn(){
        knn.train(trainData, Ml.ROW_SAMPLE, Converters.vector_int_to_Mat(trainLabs));
        //Load the image
        //Didn't work likely using train overwrites previous data?
//        Mat curSymbol = new Mat();
//        Mat trainData = new Mat();
//        trainData.convertTo(trainData, CvType.CV_32F);
//        Mat testData = new Mat();
//        List<Integer> trainLabs = new ArrayList<Integer>(),
//                testLabs = new ArrayList<Integer>();
//        Size size = new Size(20,30);
//        Mat resizedImg = new Mat();
//
//        Utils.bitmapToMat(bmp, curSymbol);
//        Imgproc.resize(curSymbol, resizedImg, size);
//        resizedImg.convertTo(resizedImg, CvType.CV_32F);
//        testData.push_back(resizedImg.reshape(1,1));
//        trainLabs.add(label);
//        this.knn.train(resizedImg.reshape(1,1), Ml.ROW_SAMPLE, Converters.vector_int_to_Mat(trainLabs).reshape(1,1));
//


    }


    private void fillSearch(int row, int col, boolean[][] staffsVisited) {

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
            fillSearch(row-1, col, staffsVisited);
        }
        if (!staffsVisited[row+1][col] && noStaffLinesImg.get(row+1, col)[3] == 255.0) {
            fillSearch(row+1, col, staffsVisited);
        }
        if (!staffsVisited[row][col-1] && noStaffLinesImg.get(row, col-1)[3] == 255.0) {
            fillSearch(row, col-1, staffsVisited);
        }
        if (!staffsVisited[row][col+1] && noStaffLinesImg.get(row, col+1)[3] == 255.0) {
            fillSearch(row, col+1, staffsVisited);
        }
    }


    private void markObjectRects(int padding, boolean[][] staffsVisited) {
        for (int row = curObjectTop-padding; row < curObjectBottom+padding; row++) {
            for (int col = curObjectLeft-padding; col < curObjectRight+padding; col++) {
                staffsVisited[row][col] = true;
            }
        }
    }


    public List<Integer> classifyObjects() {
        int i = 0;
        List<Rect> objects = staffObjects.get(i);
        List<Integer> bCounts = new ArrayList<Integer>();
        Map<Double, Character> notes = new HashMap<Double, Character>();
        // treble
        notes.put(0.0,'F');
        notes.put(0.5,'E');
        notes.put(1.0,'D');
        notes.put(1.5,'C');
        notes.put(2.0,'B');
        notes.put(2.5,'A');
        notes.put(3.0,'G');
        notes.put(3.5,'F');
        notes.put(4.0,'E');

        // bass
        notes.put(5.0,'A');
        notes.put(5.5,'G');
        notes.put(6.0,'F');
        notes.put(6.5,'E');
        notes.put(7.0,'D');
        notes.put(7.5,'C');
        notes.put(8.0,'B');
        notes.put(8.5,'A');
        notes.put(9.0,'G');


        for (Rect obj : objects) {
            int count = 0;
            for (int row = obj.top; row < obj.bottom; row++) {
                for (int col = obj.left; col < obj.right; col++) {
                    if (noStaffLinesImg.get(row, col)[3] == 255.0) {
                        count++;
                    }
                }
            }
            bCounts.add(count);
            if (count > 600) {
                // add measure line
            }
            else {
                // determine pitch
                int cThres = 27;
                char note = 'A';
                if (obj.right-obj.left >= cThres) {
                    note = 'C';
                }
                else {
                    int padding = 4;
                    int col = obj.left + padding;
                    int rowAvg = 0;
                    int rowCount = 0;

                    for (int row = obj.top + padding; row < obj.bottom - padding; row++) {
                        if (noStaffLinesImg.get(row, col)[3] == 255.0) {
                            rowAvg += row;
                            rowCount++;
                        }
                    }
                    rowAvg = rowAvg / rowCount;

                    List<Integer> staffLines = staffs.get(i);
                    //for (int j = 0; j < staffLines.size(); j++) {


                    //}


                }



                if (count > 400) {
                    // add quarter note

                } else {
                    // add half note
                }
            }
        }
        return bCounts;
    }

    public double[] getImageData(int row, int col) {
        return noStaffLinesImg.get(row, col);
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

    public void writeXML() {
        String beginning = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n";
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/Piano/XML");
        myDir.mkdirs();
        File file = new File (myDir, "twinkle_twinkle.xml");
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(beginning.getBytes());
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
