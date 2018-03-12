package piano.pianotrainer.scoreImport;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Interpolator;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Mat;
import org.opencv.features2d.FeatureDetector;
import org.opencv.ml.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.KNearest;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.*;

import piano.pianotrainer.scoreModels.ElementType;
import piano.pianotrainer.scoreModels.Line;
import piano.pianotrainer.scoreModels.Note;
import piano.pianotrainer.scoreModels.NoteGroup;
import piano.pianotrainer.scoreModels.Pitch;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.CV_HOUGH_GRADIENT;
import static org.opencv.imgproc.Imgproc.HoughCircles;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.ellipse;
import static org.opencv.imgproc.Imgproc.isContourConvex;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.resize;


public class ScoreProcessor {
    static final String TAG = "ScoreProcessor";

    static final int TRAIN_WIDTH = 30;
    static final int TRAIN_HEIGHT = 80;

    //how much W/L or L/W is greater than for it to be wide or long
    static final double WL_RATIO = 1.5;

    //Information channel (alpha) - inv since 0 is white, 255 is black
    Mat originalGrayInvImg;
    //Just the first 3 channels (all 0s)
    Mat originalImgRgb;
    Mat grayImg;
    Mat binarizedImg;
    Mat isoStaffLinesImg;
    public Mat noStaffLinesImg;

    public Mat tmpImg;

    final int MAX_STAFF_LINE_THICKNESS = 2; //TODO detect this dynamically
    List<Integer> staffLineRowIndices;

    int staffLineDiff;
    List<List<Integer>> staffs;
    List<List<Rect>> staffObjects;
    List<List<Integer>> knnResults;

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
        Mat originalImgRgba = new Mat(bmpImg.getHeight(), bmpImg.getWidth(), CvType.CV_8UC4);
        //load into 8UC4 where the information is stored in the last (alpha) channel
        Utils.bitmapToMat(bmpImg, originalImgRgba);
        //structure for extracting channels needs a list of Mat for inputs and outputs
        List<Mat> inputMats = new ArrayList<Mat>();
        inputMats.add(originalImgRgba);
        List<Mat> outputMats = new ArrayList<Mat>();
        originalImgRgb = new Mat(bmpImg.getHeight(), bmpImg.getWidth(), CvType.CV_8UC3);
        originalGrayInvImg = new Mat(bmpImg.getHeight(), bmpImg.getWidth(), CvType.CV_8UC1);
        outputMats.add(originalImgRgb);
        outputMats.add(originalGrayInvImg);
        //one to one mapping; first channel of input -> first channel of output
        //total channel size of inputs and outputs must be equal: 8UC4 = 8UC3 + 8UC1
        int[] channelMapArray = {0,0,1,1,2,2,3,3};
        MatOfInt channelMapping = new MatOfInt(channelMapArray);
        try{
            Core.mixChannels(inputMats, outputMats, channelMapping);
        }
        catch(Exception e){
            String exc = e.toString();
        }
        originalGrayInvImg = outputMats.get(1);

        //---
        binarizedImg = new Mat(bmpImg.getHeight(), bmpImg.getWidth(), CvType.CV_8UC1);
        isoStaffLinesImg = new Mat(bmpImg.getHeight(), bmpImg.getWidth(), CvType.CV_8UC1);
        noStaffLinesImg = new Mat(bmpImg.getHeight(), bmpImg.getWidth(), CvType.CV_8UC1);
        tmpImg = new Mat();
        //android uses BGR default -> switch B and R channels
//        Imgproc.cvtColor(originalImg, originalImg, Imgproc.COLOR_BGRA2RGBA);
        //Used to train for various symbol by passing in a label and test data
        knn = KNearest.create();
        trainData = new Mat();
        testData = new Mat();
        trainData.convertTo(trainData, CvType.CV_32F);
        testData.convertTo(testData, CvType.CV_32F);

        Log.d(TAG,String.format("Converted original image to %d by %d MAT",originalGrayInvImg.cols(),
                originalGrayInvImg.rows()));
    }

    //Threshold the alpha values of the original image
    public void binarize(){
        try {
            //TODO: See if adaptive threshold is better
//            Lots of salt pepper noise so a lower threshold value will give stronger blacks which will help
            Imgproc.threshold(originalGrayInvImg,binarizedImg,50,255,THRESH_BINARY);
            Log.d(TAG,String.format("Created binarized %d by %d img MAT",binarizedImg.cols(),binarizedImg.rows()));
        }
        catch(Exception e){
            Log.d(TAG,e.getMessage());
        }
    }

    //Uses horizontal morphology and subtracts from the original img
    public void removeStaffLines(){

        // Relative measure which seemed ok
        int horizontalsize = binarizedImg.cols() / 10;
        Size kernelWidth = new Size(horizontalsize,1);
        Point pt = new Point(-1,-1); //current pixel is the 'center' when applying operations
        // Create structure element for extracting horizontal lines through morphology operations
        Mat horizontalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelWidth);
        // Apply morphology operations
        Imgproc.erode(binarizedImg, isoStaffLinesImg, horizontalStructure, pt,2);
        // "reamps" the remaining elements on the page (trailing parts of staffline which were eliminated)
        Imgproc.dilate(isoStaffLinesImg, noStaffLinesImg, horizontalStructure, pt,2);
        //ideally the image after morphology only contains staff lines which are not subtracted out
        Core.subtract(binarizedImg,noStaffLinesImg,noStaffLinesImg);
        //now lets try vertically dilating it to stich gaps
        //via paint max staff line width is 2
        //Will need to make sure flood fill works 100% of the time
        Size kernelHeight = new Size(1,3);
        Mat verticalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelHeight);
        //vertical dilate will look 1 pixel away vertically and take max
        Imgproc.dilate(noStaffLinesImg,noStaffLinesImg,verticalStructure,pt,2);
        //internally make note of stafflines and locations
        staffLineDetect(isoStaffLinesImg);
        invertImgColor(noStaffLinesImg);
    }

    public void removeStaffLines2(){
        // Create structure element for extracting vertical lines through morphology operations
        Point pt = new Point(-1,-1); //"default"
        //via paint max staff line width is 2
        Size kernelHeight = new Size(1,5);
        Mat verticalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelHeight);
        //erode out the lines
        Imgproc.erode(binarizedImg,noStaffLinesImg,verticalStructure,pt,1);
        //should re-amp the blackness of remaining black things in the picture
         Imgproc.dilate(noStaffLinesImg,noStaffLinesImg,verticalStructure,pt,1);
    }


    public void staffLineDetect(Mat staffLinesImg){
        //TODO: Make structured val not hacky estimate for threshold
        int thresholdForStaffline = binarizedImg.cols()/4;
        double[] rowTotalVals;
        int curRowTotal;
        double[] mVal;
        for(int i = 0; i<staffLinesImg.rows(); i++){
            mVal = staffLinesImg.get(i,150);
            rowTotalVals = Core.sumElems(staffLinesImg.row(i)).val;
            curRowTotal = (int) rowTotalVals[0]/255;
            if(curRowTotal > thresholdForStaffline){
                staffLineRowIndices.add(i);
            }
        }
        //Note due to inconsistent staff line thickness the length isn't guarenteed to be mod 5
        //TODO: Loop through and cluster adjacent stafflines
        Log.d(TAG,String.format("Detected %d staff line positions!", staffLineRowIndices.size()));
    }

    //Returns the image after staff line removal
    public Bitmap getNoStaffLinesImg(){
        Bitmap bmp = Bitmap.createBitmap(noStaffLinesImg.cols(),noStaffLinesImg.rows(),Bitmap.Config.ARGB_8888);
        Mat noStaffLinesImgRgba = new Mat(noStaffLinesImg.rows(), noStaffLinesImg.cols(), CvType.CV_8UC4);
        List<Mat> inputMats = new ArrayList<Mat>();
        inputMats.add(originalImgRgb);
        inputMats.add(noStaffLinesImg);
        List<Mat> outputMats = new ArrayList<Mat>();
        outputMats.add(noStaffLinesImgRgba);
        int[] channelMapArray = {0,0,1,1,2,2,3,3};
        MatOfInt channelMap = new MatOfInt(channelMapArray);
        Core.mixChannels(inputMats, outputMats, channelMap);
        Utils.matToBitmap(noStaffLinesImgRgba,bmp);
        return bmp;
    }
    //isoStaffLinesImg
    public Bitmap getIsoStaffImg(){
        Bitmap bmp = Bitmap.createBitmap(isoStaffLinesImg.cols(),noStaffLinesImg.rows(),Bitmap.Config.ARGB_8888);
        Mat isoStaffLinesImgRgba = new Mat(isoStaffLinesImg.rows(), isoStaffLinesImg.cols(), CvType.CV_8UC4);
        List<Mat> inputMats = new ArrayList<Mat>();
        inputMats.add(originalImgRgb);
        inputMats.add(isoStaffLinesImg);
        List<Mat> outputMats = new ArrayList<Mat>();
        outputMats.add(isoStaffLinesImgRgba);
        int[] channelMapArray = {0,0,1,1,2,2,3,3};
        MatOfInt channelMap = new MatOfInt(channelMapArray);
        Core.mixChannels(inputMats, outputMats, channelMap);
        Utils.matToBitmap(isoStaffLinesImgRgba,bmp);
        return bmp;
    }
    //Returns the original image after binarization as a Bitmap
    public Bitmap getBinImg(){
        Bitmap bmp = Bitmap.createBitmap(binarizedImg.cols(),binarizedImg.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(binarizedImg,bmp);
        Log.d(TAG,String.format("Creating binarized %d by %d img",bmp.getWidth(),bmp.getHeight()));
        return bmp;
    }

    public Bitmap getGrayImg(){
        Bitmap bmp = Bitmap.createBitmap(grayImg.cols(),grayImg.rows(),Bitmap.Config.ARGB_8888);
        //reconvert to rgb format to use with android bitmap
        Mat rgbFormatMat = new Mat();
        cvtColor(grayImg,rgbFormatMat,Imgproc.COLOR_GRAY2RGBA, 4 );
        Utils.matToBitmap(rgbFormatMat,bmp);
        Log.d(TAG,String.format("Creating binarized %d by %d img",bmp.getWidth(),bmp.getHeight()));
        return bmp;
    }

    public Bitmap getGrayNoStaffImg(){
        Bitmap bmp = Bitmap.createBitmap(noStaffLinesImg.cols(),noStaffLinesImg.rows(),Bitmap.Config.ARGB_8888);
        //reconvert to rgb format to use with android bitmap
        Mat rgbFormatMat = new Mat();
        cvtColor(grayImg,rgbFormatMat,Imgproc.COLOR_GRAY2RGBA, 4 );
        Utils.matToBitmap(rgbFormatMat,bmp);
        Log.d(TAG,String.format("Creating binarized %d by %d img",bmp.getWidth(),bmp.getHeight()));
        return bmp;
    }

    public Bitmap getOriginalImg() {
        Bitmap bmp = Bitmap.createBitmap(originalGrayInvImg.cols(),originalGrayInvImg.rows(),Bitmap.Config.ARGB_8888);
//        Mat endImg = new Mat();
//        cvtColor(originalGrayInvImg, endImg, Imgproc.COLOR_GRAY2RGB);
//        Utils.matToBitmap(endImg,bmp);
//        Log.d(TAG,String.format("Creating original %d by %d img",bmp.getWidth(),bmp.getHeight()));
        Utils.matToBitmap(originalGrayInvImg, bmp);
        return bmp;
    }

    //clustering to classify distinct staffs from the raw staff line information
    public List<List<Integer>> refineStaffLines() {
        staffs = new ArrayList<List<Integer>>();
        staffs.add(new ArrayList<Integer>());

        int lineAvg = 0;
        int rowCountPerLine = 0;
        int prevRow = staffLineRowIndices.get(0)-1;
        int lineCount = 0;
        int staffCount = 0;

        String s = staffLineRowIndices.toString();

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

                lineAvg = curRow;
                rowCountPerLine = 1;
                prevRow = curRow;
            }
        }
        if (rowCountPerLine > 0) {
            lineAvg = lineAvg / rowCountPerLine;
            staffs.get(staffCount).add(lineAvg);
        }

        int staffLineDiffInit = staffs.get(0).get(1)-staffs.get(0).get(0);
        int lineDiffAvg = 0;
        int numDiffs = 0;
        for (int i = 0; i < staffs.size(); i++) {
            for (int j = 1; j < staffs.get(i).size(); j++) {
                if ((staffs.get(i).get(j) - staffs.get(i).get(j-1)) < (int)(1.5*staffLineDiffInit)) {
                    lineDiffAvg += (staffs.get(i).get(j) - staffs.get(i).get(j - 1));
                    numDiffs++;
                }
            }
        }
        if (numDiffs > 0) {
            staffLineDiff = (int)(lineDiffAvg / numDiffs);
        }
        else {
            staffLineDiff = staffs.get(0).get(3)-staffs.get(0).get(2);
        }

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
            //use this as a relative measure for elements on top of the staffline
//            increment = staffLines.get(1) - staffLines.get(0);
            topBound = staffLines.get(0);
            bottomBound = staffLines.get(9);
            leftBound = 0;
            rightBound = noStaffLinesImg.width();

            //ArrayList<Rect> tabuRects = new ArrayList<Rect>();
            curObjectTop = noStaffLinesImg.height();
            curObjectBottom = 0;
            curObjectLeft = noStaffLinesImg.width();
            curObjectRight = 0;
            int padding = 1;

            for (int col = leftBound; col < rightBound; col++) {
                for (int row = topBound; row < bottomBound; row++) {

                    if (!staffsVisited[row][col]) {
                        double[] data = noStaffLinesImg.get(row, col);
                        //nostafflinesimg should be 8UC1
                        if (data[0] == 0.0) {
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

    public boolean isGrandStaff() {
        if (staffs.size() == 0) return false;
        for (int i = 0; i < staffs.size(); i++) {
            List<Integer> staff = staffs.get(i);
            if (staff.size() != 10) return false;
        }
        return true;
    }

    public boolean isMeasureBar(int staffIndex, int objIndex) {
        List<Boolean[]> list = new ArrayList<>();

        Boolean[] staff1 = new Boolean[] { false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true};
        Boolean[] staff2 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, false, true, false, true, true, true, true, true, true, false, true, true, true, true, true, true, true, false};
        Boolean[] staff3 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, true, false, true, true, false, true, true, true, false, false, false, true, false, true, true, false, true, true, true, true, false, false};
        Boolean[] staff4 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, true, false, true, true, false, true, true, true, false, true, false, true, true, false, true, true, true, false, true, false};
        Boolean[] staff5 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, false, true, false, true, true, false, true, true, true, true, false, true, false, true, true, false, true, true, true, false, true, false} ;
        Boolean[] staff6 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, true, false, true, true, false, true, true, true, true, false, true, true, true, true, true, true, false, true, true, true, false, false};

        list.add(staff1);
        list.add(staff2);
        list.add(staff3);
        list.add(staff4);
        list.add(staff5);
        list.add(staff6);
        return list.get(staffIndex)[objIndex];
    }

    public boolean isDot(Rect rect) {
        return false;
    }





    //Trains the knn with a label (ID for symbol) and the bitmap img
    //All images must be formatted to be the same size (resize) and a horizontal vector (reshape)
    public void addSample(Bitmap bmp , int label, boolean save){
        //most notes around 30x80
        Size size = new Size(TRAIN_WIDTH,TRAIN_HEIGHT);
        Mat resizedImg = new Mat(TRAIN_WIDTH, TRAIN_HEIGHT, CvType.CV_8UC1);
        Mat curFeatureRgba = new Mat();
        //we only want one 8bit channel of interest to train the knn process (curFeatureR)
        Mat curFeatureGba, curFeatureR;
        Utils.bitmapToMat(bmp, curFeatureRgba);
        List<Mat> inputMats = new ArrayList<Mat>();
        inputMats.add(curFeatureRgba);
        List<Mat> outputMats = new ArrayList<Mat>();
        curFeatureGba = new Mat(curFeatureRgba.rows(), curFeatureRgba.cols(), CvType.CV_8UC3);
        curFeatureR = new Mat(curFeatureRgba.rows(), curFeatureRgba.cols(), CvType.CV_8UC1);
        outputMats.add(curFeatureR);
        outputMats.add(curFeatureGba);
        //one to one mapping; first channel of input -> first channel of output
        //total channel size of inputs and outputs must be equal: 8UC4 = 8UC3 + 8UC1
        int[] channelMapArray = {0,0,1,1,2,2,3,3};
        MatOfInt channelMapping = new MatOfInt(channelMapArray);
        try{
            Core.mixChannels(inputMats, outputMats, channelMapping);
        }
        catch(Exception e){
            String exc = e.toString();
        }

        Imgproc.resize(curFeatureR, resizedImg, size);
        if(save){
            tmpImg = resizedImg.clone();
        }
        //most examples suggest we need float data for knn
        resizedImg.convertTo(resizedImg, CvType.CV_32F);
        //for opencv ml, each feature has to be a single row

        resizedImg = resizedImg.reshape(1, 1);

        trainData.push_back(resizedImg);
        trainLabs.add(label);
    }

    public Mat getStaffObjectMat(int staffline, int objIndex){
        if(staffline < 0 && staffline > staffObjects.size() - 1){
            throw new Error("staffline index not in range!");
        }
        if(objIndex < 0 && objIndex > staffObjects.get(staffline).size()){
            throw new Error("objIndex not in range!");
        }
        Rect staffObjRect = staffObjects.get(staffline).get(objIndex);
        Mat noStaffLineImgRoi = noStaffLinesImg.submat(staffObjRect.top, staffObjRect.bottom,
                                                        staffObjRect.left, staffObjRect.right);
        return noStaffLineImgRoi;

    }

    public Mat invertGrayImg(Mat grayImg){
        double curVal;
        for(int i = 0; i < grayImg.rows(); i++){
            for(int j = 0; j < grayImg.cols(); j++){
                curVal = grayImg.get(i,j)[0];
                grayImg.put(i,j, 255 - curVal);
            }
        }
        return grayImg;
    }

    public boolean testMusicObjects(){
        knnResults = new ArrayList<List<Integer>>();

        Rect curRect;
        boolean passed = true;
        for(int i = 0; i < staffObjects.size(); i++){
            knnResults.add(new ArrayList<Integer>());
            for(int j = 0; j < staffObjects.get(i).size(); j++){
                curRect = staffObjects.get(i).get(j);
                //for very thin bars susceptible to noise..
                if(isBarLine(curRect)){
                    Log.d(TAG, String.format("Bar: %d, %d", i, j));
                    knnResults.get(i).add(KnnLabels.BAR);
                }
                else{
                    knnResults.get(i).add(testKnnMat(extractFromNoStaffImg(curRect)));
                }
            }
        }
        return true;
    }

    public boolean isRectLong(Rect r){
        return ((double)r.height()) / ((double)r.width()) > WL_RATIO;
    }

    public boolean isRectWide(Rect r){
        return ((double)r.width()) / ((double)r.height()) > WL_RATIO;
    }

    public boolean isLongerThanStafflineDiff(Rect r){
        return r.height() > staffLineDiff;
    }

    public int wholeHalfRestFilter(Rect r){
        int rY = (r.top + r.bottom )/2;
        int curStaffPos = 0;
        for(int i = 0; i < staffs.size(); i++){
            for(int j = 0; j < staffs.get(i).size() - 1 ; j++){
                curStaffPos = staffs.get(i).get(j);
                if(rY - staffs.get(i).get(j) < staffLineDiff){
                    if(rY -curStaffPos < staffLineDiff){
                        if(rY - curStaffPos < staffs.get(i).get(j+1) - rY){
                            return KnnLabels.WHOLE_REST;
                        }
                        else{
                            return KnnLabels.HALF_REST;
                        }
                    }
                }
            }
        }
        //if the midpoint doesn't exist within stafflines
        return 10;
    }

    //too thin so its too affected by noise
    public boolean isBarLine(Rect r){
        return r.height() > staffLineDiff*10 && r.width() <= staffLineDiff;
    }

    public void dotFilter(){
        List<Integer> curStaffResults;
        Rect curRect;
        for(int i = 0; i < knnResults.size(); i++){
            for(int j = 0; j < knnResults.get(i).size(); j++){
                if(knnResults.get(i).get(j) == KnnLabels.DOT){
                    curRect = staffObjects.get(i).get(j);
                    //length bigger than width by a big factor say its barline
                    //we still need this additional check for thick lines - which are not susceptible to noise
                    if(isRectLong(curRect) && isLongerThanStafflineDiff(curRect)){
                        curStaffResults = knnResults.get(i);
                        curStaffResults.set(j, KnnLabels.BAR);
                        knnResults.set(i, curStaffResults);
                    }
                    //if its noticably wider - its a whole/half rest
                    if(isRectWide(curRect) && !isLongerThanStafflineDiff(curRect)){
                        curStaffResults = knnResults.get(i);
                        curStaffResults.set(j, wholeHalfRestFilter(curRect));
                        knnResults.set(i, curStaffResults);
                    }
                    //if neither ifs prompted its a dot

                }
            }
        }
    }

    //getter
    public List<List<Integer>> getKnnResults() {
        return knnResults;
    }

    public Mat extractFromNoStaffImg(Rect r){
        org.opencv.core.Rect cvRect = new org.opencv.core.Rect(r.left, r.top, r.width(), r.height());
        return new Mat(noStaffLinesImg, cvRect);
    }

    public Bitmap getStaffObject(int staffLine, int index) {
        if(index < 0 || index >= staffObjects.get(staffLine).size()){
            return null;
        }
        Bitmap bmp = Bitmap.createBitmap(staffObjects.get(staffLine).get(index).width(),staffObjects.get(staffLine).get(index).height(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(extractFromNoStaffImg(staffObjects.get(staffLine).get(index)), bmp);
        return bmp;
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

        if (!staffsVisited[row-1][col] && noStaffLinesImg.get(row-1, col)[0] == 0.0) {
            fillSearch(row-1, col, staffsVisited);
        }
        if (!staffsVisited[row+1][col] && noStaffLinesImg.get(row+1, col)[0] == 0.0) {
            fillSearch(row+1, col, staffsVisited);
        }
        if (!staffsVisited[row][col-1] && noStaffLinesImg.get(row, col-1)[0] == 0.0) {
            fillSearch(row, col-1, staffsVisited);
        }
        if (!staffsVisited[row][col+1] && noStaffLinesImg.get(row, col+1)[0] == 0.0) {
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

    public void invertImgColor(Mat mat) {
        for (int r = 0; r < mat.rows(); r++) {
            for (int c = 0; c < mat.cols(); c++) {
                double[] pixel = mat.get(r, c);
                pixel[0] = 255.0-pixel[0];
                mat.put(r, c, pixel);
            }
        }
    }

    //TODO: Pitch and scale population from center Y positions of key sigs
    public Pitch getPitchFromAccCenter(double yPosCenter){
        return Pitch.A;
    }

    public int getScaleFromAccCenter(double yPosCenter){
        return 1;
    }

    public Map<Pitch, Integer> getPitchScaleFromKeySig(List<Double> keySigCentres, List<Boolean> inTreble, int staffNum){
        Map<Pitch, Integer> keySigs = new LinkedHashMap<>();

        List<Integer> staffLines = staffs.get(staffNum);
        for (int keySig = 0; keySig < keySigCentres.size(); keySig++) {
            double yPos = keySigCentres.get(keySig);
            int i;
            for (i = 0; i < staffLines.size(); i++) {
                if (yPos < staffLines.get(i)) {
                    break;
                }
            }
            Pitch pitch = Pitch.C;
            int scale = 4;

            if (i > 0 && i < 5) {
                // treble in between
                double top = (double)staffLines.get(i-1);
                double bottom = (double)staffLines.get(i);
                double middle = (top+bottom)/2;

                double topDist = Math.abs(top-yPos);
                double bottomDist = Math.abs(bottom-yPos);
                double middleDist = Math.abs(middle-yPos);

                double staffLine = 0.0;
                if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(i-1);
                else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)i;
                else staffLine = ((double)i + (double)(i-1))/2;

                if (staffLine <= 2.5) scale = 5;
                else scale = 4;
                int temp = (int)(staffLine * 2);
                temp = 8 - temp;
                temp -= 3;
                if (temp < 0) temp += 7;
                int pitchIndex = temp % 7;
                pitch = Pitch.values()[pitchIndex];
            }
            else if (i > 5 && i < 10) {
                // bass in between
                double top = (double)staffLines.get(i-1);
                double bottom = (double)staffLines.get(i);
                double middle = (top+bottom)/2;

                double topDist = Math.abs(top-yPos);
                double bottomDist = Math.abs(bottom-yPos);
                double middleDist = Math.abs(middle-yPos);

                double staffLine = 0.0;
                if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(i-1);
                else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)i;
                else staffLine = ((double)i + (double)(i-1))/2;

                staffLine -= 5.0;
                if (staffLine == 0) scale = 4;
                else if (staffLine == 4) scale = 2;
                else scale = 3;

                int temp = (int)(staffLine * 2);
                temp = 8 - temp;
                temp -= 1;
                if (temp < 0) temp += 7;
                int pitchIndex = temp % 7;
                pitch = Pitch.values()[pitchIndex];
            }
            else if (i == 0){
                // top of treble
                double top = (double)staffLines.get(0);
                int lineIndex = 5;
                while (yPos < top && lineIndex > 0) {
                    top -= staffLineDiff;
                    lineIndex--;
                }
                double bottom = top + staffLineDiff;
                double middle = (top+bottom)/2;
                double topDist = Math.abs(top-yPos);
                double bottomDist = Math.abs(bottom-yPos);
                double middleDist = Math.abs(middle-yPos);

                double staffLine = 0.0;
                if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(lineIndex-1);
                else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)lineIndex;
                else staffLine = ((double)lineIndex + (double)(lineIndex-1))/2;

                if (staffLine <= 3) scale = 6;
                else scale = 5;

                int temp = (int)(staffLine * 2);
                temp = 8 - temp;
                temp -= 2;
                if (temp < 0) temp += 7;
                int pitchIndex = temp % 7;
                pitch = Pitch.values()[pitchIndex];
            }
            else if (i == 5 && inTreble.get(keySig)) {
                // in between treble and bass and in treble
                double bottom = staffLines.get(4);
                int lineIndex = 0;
                while (yPos > bottom && lineIndex < 4) {
                    bottom += staffLineDiff;
                    lineIndex++;
                }
                double top = bottom - staffLineDiff;
                double middle = (top+bottom)/2;
                double topDist = Math.abs(top-yPos);
                double bottomDist = Math.abs(bottom-yPos);
                double middleDist = Math.abs(middle-yPos);

                double staffLine = 0.0;
                if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(lineIndex-1);
                else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)lineIndex;
                else staffLine = ((double)lineIndex + (double)(lineIndex-1))/2;

                if (staffLine > 3) scale = 3;
                else scale = 4;

                int temp = (int)(staffLine * 2);
                temp = 8 - temp;
                temp -= 4;
                if (temp < 0) temp += 7;
                int pitchIndex = temp % 7;
                pitch = Pitch.values()[pitchIndex];
            }
            else if (i == 5 && !inTreble.get(keySig)) {
                // in between treble and bass and in bass
                double top = staffLines.get(5);
                int lineIndex = 5;
                while (yPos < top && lineIndex > 0) {
                    top -= staffLineDiff;
                    lineIndex--;
                }
                double bottom = top + staffLineDiff;
                double middle = (top+bottom)/2;
                double topDist = Math.abs(top-yPos);
                double bottomDist = Math.abs(bottom-yPos);
                double middleDist = Math.abs(middle-yPos);

                double staffLine = 0.0;
                if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(lineIndex-1);
                else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)lineIndex;
                else staffLine = ((double)lineIndex + (double)(lineIndex-1))/2;

                if (staffLine <= 0.5) scale = 5;
                else scale = 4;

                int temp = (int)(staffLine * 2);
                temp = 8 - temp;
                if (temp < 0) temp += 7;
                int pitchIndex = temp % 7;
                pitch = Pitch.values()[pitchIndex];
            }
            else {
                // bottom of bass
                double bottom = staffLines.get(9);
                int lineIndex = 0;
                while (yPos > bottom && lineIndex < 6) {
                    bottom += staffLineDiff;
                    lineIndex++;
                }
                double top = bottom - staffLineDiff;
                double middle = (top+bottom)/2;
                double topDist = Math.abs(top-yPos);
                double bottomDist = Math.abs(bottom-yPos);
                double middleDist = Math.abs(middle-yPos);

                double staffLine = 0.0;
                if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(lineIndex-1);
                else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)lineIndex;
                else staffLine = ((double)lineIndex + (double)(lineIndex-1))/2;

                if (staffLine <= 3) scale = 2;
                else scale = 1;

                int temp = (int)(staffLine * 2);
                temp = 8 - temp;
                temp -= 2;
                if (temp < 0) temp += 7;
                int pitchIndex = temp % 7;
                pitch = Pitch.values()[pitchIndex];
            }

            keySigs.put(pitch, scale);
        }

        return keySigs;
    }

    public double getCenterYOfFlat(Rect rect, ElementType elementType, int staffIndex){
        double centerPosY;
//        int intCenterPosY;
        //get the center pos based on acc type
        if(elementType == ElementType.Flat){
            //midpoint of the midpoint of the Y of the rect
            centerPosY = (((double)rect.top+(double)rect.bottom)/2 + (double)rect.bottom)/2;
        }
        else if(elementType == ElementType.Sharp || elementType == ElementType.Natural){
            //return midpoint since symmetry
            centerPosY = ((double)rect.top + (double)rect.bottom)/2;
        }
        else{
            //not an accidental
            Log.e(TAG, "Non acc passed into getCenterYOfFlat()");
            return -1.0;
        }
        return centerPosY;
    }

    public boolean isNoteGroup(Rect rect) {
        Mat objMat = extractFromNoStaffImg(rect);
        Mat cannyMat = new Mat();
        Mat colorMat = new Mat();
        cvtColor(objMat, colorMat, Imgproc.COLOR_GRAY2BGR);
        Imgproc.Canny(objMat, cannyMat, 100, 200);
        Mat allLines = new Mat();
        Imgproc.HoughLinesP(cannyMat, allLines, 1, Math.PI/180, 4, 15, 5);

        List<Line> vertLines = new ArrayList<>();
//        Log.d(TAG, String.format("# of lines: %d", allLines.rows()));
        for (int i = 0; i < allLines.rows(); i++) {
            double[] val = allLines.get(i, 0);
//            Imgproc.line(colorMat, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 0, 255), 1);
            Line line = new Line(val[0], val[1], val[2], val[3]);
            if (line.getSlope() == Integer.MAX_VALUE) {
                vertLines.add(line);
            }
        }
        Collections.sort(vertLines, new Comparator<Line>() {
            @Override
            public int compare(Line l1, Line l2) {
                return Double.compare(l1.x1, l2.x1);
            }
        });


        // Combining similar vertical lines
        double VERT_LINE_PROXIMITY = 10.0;
        Map<Line, Integer> lineIDs = new LinkedHashMap<>();
        for (int i = 0; i < vertLines.size(); i++) {
            lineIDs.put(vertLines.get(i),i);
        }
        for (int i = 1; i < vertLines.size(); i++) {
            Line curLine = vertLines.get(i);
            Line prevLine = vertLines.get(i-1);
            if ((curLine.x1 - prevLine.x1) < VERT_LINE_PROXIMITY) {
                lineIDs.put(curLine, lineIDs.get(prevLine));
            }
        }
        List<Line> reducedVertLines = new ArrayList<>();
        Map<Integer, List<Line>> IDLines = new LinkedHashMap<>();
        for (Map.Entry<Line, Integer> entry : lineIDs.entrySet()) {
            if (!IDLines.containsKey(entry.getValue())) {
                IDLines.put(entry.getValue(), new ArrayList<Line>());
            }
            IDLines.get(entry.getValue()).add(entry.getKey());
        }
        for (Map.Entry<Integer, List<Line>> entry : IDLines.entrySet()) {
            List<Line> lines = entry.getValue();
            double xAvg = 0.0;
            double yMin = (double) Integer.MAX_VALUE;
            double yMax = (double) Integer.MIN_VALUE;
            for (Line line : lines) {
                xAvg += line.x1;
                if (line.y1 < yMin) yMin = line.y1;
                if (line.y2 < yMin) yMin = line.y2;
                if (line.y1 > yMax) yMax = line.y1;
                if (line.y2 > yMax) yMax = line.y2;
            }
            xAvg = xAvg / lines.size();
            Line reducedLine = new Line(xAvg, yMin, xAvg, yMax);
            reducedVertLines.add(reducedLine);
        }

        if (reducedVertLines.size() == 0) return false;
        else {
            double avgHeight = 0;
            for (Line line : reducedVertLines) {
//                if (Math.abs(line.y2 - line.y1) > Math.abs(maxLine.y2 - maxLine.y1)) {
//                    maxLine = line;
//                }
                avgHeight += Math.abs(line.y2 - line.y1);
            }
            avgHeight /= reducedVertLines.size();

            double lowestArea = 704.0;
            double curArea = ((rect.bottom - rect.top) * (rect.right - rect.left));
            if (curArea > lowestArea && avgHeight >= staffLineDiff*2 && avgHeight <= staffLineDiff*8) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    public NoteGroup classifyNoteGroup(Rect rect, int staffNum, boolean isTreble)  {
        Mat objMat = extractFromNoStaffImg(rect);
        Mat allCircles = new Mat();

        Imgproc.HoughCircles(objMat, allCircles, CV_HOUGH_GRADIENT, 1, ((double)staffLineDiff)*0.8, 200, 4, (int)(((double)staffLineDiff)*0.5), (int)(((double)staffLineDiff)*0.75));
        Map<Point, Integer> circles = new TreeMap<Point, Integer>(new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                if (o1.x != o2.x) {
                    return Double.compare(o1.x, o2.x);
                }
                else {
                    return Double.compare(o1.y, o2.y);
                }
            }
        });
        Map<Point, Double> circlesBlackRatios = new HashMap<>();

        for (int k = 0; k < allCircles.cols(); k++) {
            double[] vCircle = allCircles.get(0, k);
            Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
            int radius = (int)Math.round(vCircle[2]);

            Mat mask = Mat.zeros(objMat.size(), CV_8UC1);
            circle(mask, pt, radius, new Scalar(255), -1);

            int blackCount = 0;
            int totCount = 0;
            for (int r = 0; r < objMat.rows(); r++) {
                for (int c = 0; c < objMat.cols(); c++) {
                    if (mask.get(r,c)[0] > 0.0) {
                        totCount++;
                        if (objMat.get(r,c)[0] < 255.0) {
                            blackCount++;
                        }
                    }
                }
            }

            // filtering out false positives
            double blackRatio = (double)blackCount / (double)totCount;
            if (blackRatio > 0.6) {
                circles.put(pt, radius);
                circlesBlackRatios.put(pt, blackRatio);
            }
        }

        if (circles.size() == 0) return null;
        boolean inTreble = inTrebleCleff(rect.top, rect.bottom, staffNum);
        NoteGroup noteGroup = new NoteGroup(new ArrayList<Note>(), 0, 0);

        for (Map.Entry<Point, Integer> circle : circles.entrySet()) {
            Note note = new Note();
            note.circleCenter = circle.getKey();
            note.circleRadius = circle.getValue();
            populatePitchAndScale(note, circle.getKey(), rect.top, inTreble, staffNum);
            noteGroup.notes.add(note);
        }
        noteGroup.sortCircles();
        populateDuration(rect, noteGroup);

        noteGroup.clef = isTreble? 0 : 1;

        return noteGroup;
    }

    public void populateDuration(Rect rect, NoteGroup noteGroup) {
        Mat objMat = extractFromNoStaffImg(rect);
        Mat cannyMat = new Mat();
        Mat colorMat = new Mat();
        cvtColor(objMat, colorMat, Imgproc.COLOR_GRAY2BGR);
        Imgproc.Canny(objMat, cannyMat, 100, 200);
        Mat allLines = new Mat();
        Imgproc.HoughLinesP(cannyMat, allLines, 1, Math.PI/180, 4, 15, 5);
        if (allLines.rows() == 0) {
            for (Note note : noteGroup.notes) {
                note.weight = 1.0;
            }
            return;
        }

        List<Line> vertLines = new ArrayList<>();
        List<Line> nonVertLines = new ArrayList<>();
//        Log.d(TAG, String.format("# of lines: %d", allLines.rows()));
        for (int i = 0; i < allLines.rows(); i++) {
            double[] val = allLines.get(i, 0);
//            Imgproc.line(colorMat, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 0, 255), 1);
            Line line = new Line(val[0], val[1], val[2], val[3]);
            if (line.getSlope() == Integer.MAX_VALUE) {
                vertLines.add(line);
            }
            else {
                nonVertLines.add(line);
            }
        }
        Collections.sort(vertLines, new Comparator<Line>() {
            @Override
            public int compare(Line l1, Line l2) {
                return Double.compare(l1.x1, l2.x1);
            }
        });
        Collections.sort(nonVertLines, new Comparator<Line>() {
            @Override
            public int compare(Line l1, Line l2) {
                if (l1.getIntercept() != l2.getIntercept()) {
                    return Double.compare(l1.getIntercept(), l2.getIntercept());
                }
                else {
                    return Double.compare(l1.getSlope(), l2.getSlope());
                }
            }
        });

        // Combining similar vertical lines
        double VERT_LINE_PROXIMITY = 10.0;
        Map<Line, Integer> lineIDs = new LinkedHashMap<>();
        for (int i = 0; i < vertLines.size(); i++) {
            lineIDs.put(vertLines.get(i),i);
        }
        for (int i = 1; i < vertLines.size(); i++) {
            Line curLine = vertLines.get(i);
            Line prevLine = vertLines.get(i-1);
            if ((curLine.x1 - prevLine.x1) < VERT_LINE_PROXIMITY) {
                lineIDs.put(curLine, lineIDs.get(prevLine));
            }
        }
        List<Line> reducedVertLines = new ArrayList<>();
        Map<Integer, List<Line>> IDLines = new LinkedHashMap<>();
        for (Map.Entry<Line, Integer> entry : lineIDs.entrySet()) {
            if (!IDLines.containsKey(entry.getValue())) {
                IDLines.put(entry.getValue(), new ArrayList<Line>());
            }
            IDLines.get(entry.getValue()).add(entry.getKey());
        }
        for (Map.Entry<Integer, List<Line>> entry : IDLines.entrySet()) {
            List<Line> lines = entry.getValue();
            double xAvg = 0.0;
            double yMin = (double) Integer.MAX_VALUE;
            double yMax = (double) Integer.MIN_VALUE;
            for (Line line : lines) {
                xAvg += line.x1;
                if (line.y1 < yMin) yMin = line.y1;
                if (line.y2 < yMin) yMin = line.y2;
                if (line.y1 > yMax) yMax = line.y1;
                if (line.y2 > yMax) yMax = line.y2;
            }
            xAvg = xAvg / lines.size();
            Line reducedLine = new Line(xAvg, yMin, xAvg, yMax);
            reducedVertLines.add(reducedLine);
        }


        // Combine each reduced vertical line with circle note
        double LINE_CIRCLE_PROMIXIMITY = 15.0;
        double[][] lineCircleDist = new double[reducedVertLines.size()][noteGroup.notes.size()];
        Map<Line, List<Note>> lineCircleMap = new LinkedHashMap<>();
        for (int i = 0; i < reducedVertLines.size(); i++) {
            for (int j = 0; j < noteGroup.notes.size(); j++) {
                lineCircleDist[i][j] = (double)Integer.MAX_VALUE;
            }
        }
        List<Note> notes = noteGroup.notes;
        for (int l = 0; l < reducedVertLines.size(); l++) {
            Line line = reducedVertLines.get(l);
            for (int c = 0; c < notes.size(); c++) {
                Point pt = notes.get(c).circleCenter;
                double dist = Math.abs(line.x1 - pt.x);
                if (dist < LINE_CIRCLE_PROMIXIMITY) {
                    lineCircleDist[l][c] = dist;
                }
            }
        }
        for (Line line : reducedVertLines) {
            lineCircleMap.put(line, new ArrayList<Note>());
        }
        for (int c = 0; c < notes.size(); c++) {
            double minDist = (double) Integer.MAX_VALUE;
            Line minDistLine = reducedVertLines.get(0);
            for (int l = 0; l < reducedVertLines.size(); l++) {
                if (lineCircleDist[l][c] < minDist) {
                    minDist = lineCircleDist[l][c];
                    minDistLine = reducedVertLines.get(l);
                }
            }
            lineCircleMap.get(minDistLine).add(notes.get(c));
        }


        // Assign each vertical line's notes duration
        if (nonVertLines.size() == 0) {
            for (Map.Entry<Line, List<Note>> entry : lineCircleMap.entrySet()) {
                for (Note note : entry.getValue()) {
                    note.weight = 0.25;
                }
            }
        }
        if (nonVertLines.size() != 0 && reducedVertLines.size() == 1) {
            for (Map.Entry<Line, List<Note>> entry : lineCircleMap.entrySet()) {
                for (Note note : entry.getValue()) {
                    note.weight = 0.125;
                }
            }
        }
        if (nonVertLines.size() != 0 && reducedVertLines.size() > 1) {
            // Combining similar non-vertical lines
            double SLOPE_PROXIMITY = 0.2;
            double INTERCEPT_PROXIMITY = 4.0;
            double VERT_NONVERT_REGION_PROXIMITY = 15.0;
            double STAFF_LINE_DIFF_TOLERANCE = 2.5;
            Map<Line, Integer> nonVertLineIDs = new LinkedHashMap<>();
            for (int i = 0; i < nonVertLines.size(); i++) {
                nonVertLineIDs.put(nonVertLines.get(i), i);
            }
            for (int i = 1; i < nonVertLines.size(); i++) {
                Line curLine = nonVertLines.get(i);
                Line prevLine = nonVertLines.get(i - 1);
                double curInt = curLine.getIntercept();
                double prevInt = prevLine.getIntercept();
                double curSlope = curLine.getSlope();
                double prevSlope = prevLine.getSlope();
                if (Math.abs(curLine.getIntercept() - prevLine.getIntercept()) < INTERCEPT_PROXIMITY &&
                        Math.abs(curLine.getSlope() - prevLine.getSlope()) < SLOPE_PROXIMITY) {
                    nonVertLineIDs.put(curLine, nonVertLineIDs.get(prevLine));
                }
            }
            List<Line> reducedNonVertLines = new ArrayList<>();
            Map<Integer, List<Line>> IDNonVertLines = new LinkedHashMap<>();
            for (Map.Entry<Line, Integer> entry : nonVertLineIDs.entrySet()) {
                if (!IDNonVertLines.containsKey(entry.getValue())) {
                    IDNonVertLines.put(entry.getValue(), new ArrayList<Line>());
                }
                IDNonVertLines.get(entry.getValue()).add(entry.getKey());
            }
            for (Map.Entry<Integer, List<Line>> entry : IDNonVertLines.entrySet()) {
                List<Line> lines = entry.getValue();
                double slopeAvg = 0.0;
                double interceptMin = (double) Integer.MAX_VALUE;
                double xMin = (double) Integer.MAX_VALUE;
                double xMax = (double) Integer.MIN_VALUE;
                for (Line line : lines) {
                    slopeAvg += line.getSlope();
                    if (line.getIntercept() < interceptMin) interceptMin = line.getIntercept();
                    if (line.x1 < xMin) xMin = line.x1;
                    if (line.x2 < xMin) xMin = line.x2;
                    if (line.x1 > xMax) xMax = line.x1;
                    if (line.x2 > xMax) xMax = line.x2;
                }
                slopeAvg = slopeAvg / lines.size();
                Line reducedLine = new Line(xMin, (slopeAvg * xMin) + interceptMin, xMax, (slopeAvg * xMax) + interceptMin);
                reducedNonVertLines.add(reducedLine);
            }

            for (Map.Entry<Line, List<Note>> entry : lineCircleMap.entrySet()) {

                List<Line> regionLines = new ArrayList<>();
                double xMid = entry.getKey().x1;
                double xMin = xMid - VERT_NONVERT_REGION_PROXIMITY;
                double xMax = xMid + VERT_NONVERT_REGION_PROXIMITY;
                for (Line line : reducedNonVertLines) {
                    if (Math.abs(line.getSlope()) < 0.5 && line.existsInXRegion(xMin, xMax)) {
                        regionLines.add(line);
                    }
                }
                if (regionLines.size() < 2) {
                    for (Note note : entry.getValue()) {
                        note.weight = 0.125;
                    }
                }
                else {
                    Line top = regionLines.get(0);
                    Line bottom = regionLines.get(regionLines.size()-1);
                    double heightDiff = Math.abs(bottom.getIntercept()-top.getIntercept());
                    if (heightDiff <= staffLineDiff+STAFF_LINE_DIFF_TOLERANCE) {
                        for (Note note : entry.getValue()) {
                            note.weight = 0.125;
                        }
                    }
                    else {
                        for (Note note : entry.getValue()) {
                            note.weight = 0.0625;
                        }
                    }
                }
            }
        }






//        if (nonVertLines.size() != 0 && reducedVertLines.size() > 1) {
//            List
////            for (Map.Entry<Line, List<Note>> entry : lineCircleMap.entrySet()) {
////                for (Note note : entry.getValue()) {
////                    note.weight = 0.125;
////                }
////            }
//        }

//        String root = Environment.getExternalStorageDirectory().toString();
//        Imgcodecs.imwrite(root + "/Piano/Images/full_score_canny.png", cannyMat);
//        Imgcodecs.imwrite(root + "/Piano/Images/full_score_lines.png", colorMat);
    }

    public void populatePitchAndScale(Note note, Point pt, int topOffset, boolean inTreble, int staffNum) {
        List<Integer> staffLines = staffs.get(staffNum);
        double yPos = pt.y + topOffset;
        int i;
        for (i = 0; i < staffLines.size(); i++) {
            if (yPos < staffLines.get(i)) {
                break;
            }
        }

        if (i > 0 && i < 5) {
            // treble in between
            double top = (double)staffLines.get(i-1);
            double bottom = (double)staffLines.get(i);
            double middle = (top+bottom)/2;

            double topDist = Math.abs(top-yPos);
            double bottomDist = Math.abs(bottom-yPos);
            double middleDist = Math.abs(middle-yPos);

            double staffLine = 0.0;
            if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(i-1);
            else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)i;
            else staffLine = ((double)i + (double)(i-1))/2;

            if (staffLine <= 2.5) note.scale = 5;
            else note.scale = 4;
            int temp = (int)(staffLine * 2);
            temp = 8 - temp;
            temp -= 3;
            if (temp < 0) temp += 7;
            int pitchIndex = temp % 7;
            note.pitch = Pitch.values()[pitchIndex];
        }
        else if (i > 5 && i < 10) {
            // bass in between
            double top = (double)staffLines.get(i-1);
            double bottom = (double)staffLines.get(i);
            double middle = (top+bottom)/2;

            double topDist = Math.abs(top-yPos);
            double bottomDist = Math.abs(bottom-yPos);
            double middleDist = Math.abs(middle-yPos);

            double staffLine = 0.0;
            if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(i-1);
            else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)i;
            else staffLine = ((double)i + (double)(i-1))/2;

            staffLine -= 5.0;
            if (staffLine == 0) note.scale = 4;
            else if (staffLine == 4) note.scale = 2;
            else note.scale = 3;

            int temp = (int)(staffLine * 2);
            temp = 8 - temp;
            temp -= 1;
            if (temp < 0) temp += 7;
            int pitchIndex = temp % 7;
            note.pitch = Pitch.values()[pitchIndex];
        }
        else if (i == 0){
            // top of treble
            double top = (double)staffLines.get(0);
            int lineIndex = 5;
            while (yPos < top && lineIndex > 0) {
                top -= staffLineDiff;
                lineIndex--;
            }
            double bottom = top + staffLineDiff;
            double middle = (top+bottom)/2;
            double topDist = Math.abs(top-yPos);
            double bottomDist = Math.abs(bottom-yPos);
            double middleDist = Math.abs(middle-yPos);

            double staffLine = 0.0;
            if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(lineIndex-1);
            else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)lineIndex;
            else staffLine = ((double)lineIndex + (double)(lineIndex-1))/2;

            if (staffLine <= 3) note.scale = 6;
            else note.scale = 5;

            int temp = (int)(staffLine * 2);
            temp = 8 - temp;
            temp -= 2;
            if (temp < 0) temp += 7;
            int pitchIndex = temp % 7;
            note.pitch = Pitch.values()[pitchIndex];
        }
        else if (i == 5 && inTreble) {
            // in between treble and bass and in treble
            double bottom = staffLines.get(4);
            int lineIndex = 0;
            while (yPos > bottom && lineIndex < 4) {
                bottom += staffLineDiff;
                lineIndex++;
            }
            double top = bottom - staffLineDiff;
            double middle = (top+bottom)/2;
            double topDist = Math.abs(top-yPos);
            double bottomDist = Math.abs(bottom-yPos);
            double middleDist = Math.abs(middle-yPos);

            double staffLine = 0.0;
            if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(lineIndex-1);
            else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)lineIndex;
            else staffLine = ((double)lineIndex + (double)(lineIndex-1))/2;

            if (staffLine > 3) note.scale = 3;
            else note.scale = 4;

            int temp = (int)(staffLine * 2);
            temp = 8 - temp;
            temp -= 4;
            if (temp < 0) temp += 7;
            int pitchIndex = temp % 7;
            note.pitch = Pitch.values()[pitchIndex];
        }
        else if (i == 5 && !inTreble) {
            // in between treble and bass and in bass
            double top = staffLines.get(5);
            int lineIndex = 5;
            while (yPos < top && lineIndex > 0) {
                top -= staffLineDiff;
                lineIndex--;
            }
            double bottom = top + staffLineDiff;
            double middle = (top+bottom)/2;
            double topDist = Math.abs(top-yPos);
            double bottomDist = Math.abs(bottom-yPos);
            double middleDist = Math.abs(middle-yPos);

            double staffLine = 0.0;
            if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(lineIndex-1);
            else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)lineIndex;
            else staffLine = ((double)lineIndex + (double)(lineIndex-1))/2;

            if (staffLine <= 0.5) note.scale = 5;
            else note.scale = 4;

            int temp = (int)(staffLine * 2);
            temp = 8 - temp;
            if (temp < 0) temp += 7;
            int pitchIndex = temp % 7;
            note.pitch = Pitch.values()[pitchIndex];
        }
        else {
            // bottom of bass
            double bottom = staffLines.get(9);
            int lineIndex = 0;
            while (yPos > bottom && lineIndex < 6) {
                bottom += staffLineDiff;
                lineIndex++;
            }
            double top = bottom - staffLineDiff;
            double middle = (top+bottom)/2;
            double topDist = Math.abs(top-yPos);
            double bottomDist = Math.abs(bottom-yPos);
            double middleDist = Math.abs(middle-yPos);

            double staffLine = 0.0;
            if (topDist < bottomDist && topDist < middleDist) staffLine = (double)(lineIndex-1);
            else if (bottomDist < topDist && bottomDist < middleDist) staffLine = (double)lineIndex;
            else staffLine = ((double)lineIndex + (double)(lineIndex-1))/2;

            if (staffLine <= 3) note.scale = 2;
            else note.scale = 1;

            int temp = (int)(staffLine * 2);
            temp = 8 - temp;
            temp -= 2;
            if (temp < 0) temp += 7;
            int pitchIndex = temp % 7;
            note.pitch = Pitch.values()[pitchIndex];
        }
    }

    public boolean inTrebleCleff(int rectTop, int rectBottom, int staffNum) {
        List<Integer> staffLines = staffs.get(staffNum);
        int trebleTop = staffLines.get(0);
        int trebleBottom = staffLines.get(4);
        int bassTop = staffLines.get(5);
        int bassBottom = staffLines.get(9);

        int trebleDistance = Math.abs(rectTop-trebleTop) + Math.abs(rectBottom-trebleBottom);
        int bassDistance = Math.abs(rectTop-bassTop) + Math.abs(rectBottom-bassBottom);
        if (trebleDistance <= bassDistance) {
            return true;
        }
        else {
            return false;
        }
    }

    public void getAllCircles()  {
        Mat colorFullMat = new Mat();
        cvtColor(noStaffLinesImg, colorFullMat, Imgproc.COLOR_GRAY2BGR);
        Mat circles = new Mat();

        Imgproc.HoughCircles(noStaffLinesImg, circles, CV_HOUGH_GRADIENT, 1, ((double)staffLineDiff)*0.8, 200, 4, (int)(((double)staffLineDiff)*0.5), (int)(((double)staffLineDiff)*0.75));
        for (int k = 0; k < circles.cols(); k++) {
            double[] vCircle = circles.get(0, k);

            Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
            int radius = (int)Math.round(vCircle[2]);
//            circle(colorFullMat, pt, radius, new Scalar(0,0,255), 2); draws the circle.. scalar is color, 2 thickness
        }
        String root = Environment.getExternalStorageDirectory().toString();
        Imgcodecs.imwrite(root + "/Piano/Images/full_score.png", colorFullMat);
    }

    public void exportRects(Context context) {
        String root = Environment.getExternalStorageDirectory().toString();

//        File file = new File(root + "/Piano/Images/", "staffLines.txt");
//        try {
//            FileOutputStream stream = new FileOutputStream(file);
//            stream.write(staffLineRowIndices.toString().getBytes());
//            stream.write("\n\n".getBytes());
//            stream.write(staffs.toString().getBytes());
//            stream.close();
//        }
//        catch (Exception e){
//            Log.d(TAG, "Exception: File write failed: " + e.toString());
//        }

        Imgcodecs.imwrite(root + "/Piano/Images/full_score_lines.png", binarizedImg);
        Imgcodecs.imwrite(root + "/Piano/Images/full_score.png", noStaffLinesImg);

        Bitmap bitmap = Bitmap.createBitmap(noStaffLinesImg.width(),noStaffLinesImg.height(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(noStaffLinesImg, bitmap);
        Canvas cnvs = new Canvas(bitmap);
        Paint paint=new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        for(List<Rect> rectList : staffObjects){
            for(Rect symbolRect : rectList){
                cnvs.drawRect(symbolRect, paint);
            }
        }
        Mat noStaffLinesImgBoxed = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, noStaffLinesImgBoxed);
        Imgcodecs.imwrite(root + "/Piano/Images/full_score_boxed.png", noStaffLinesImgBoxed);

//        for (int i = 0; i < staffObjects.size(); i++) {
//            for (int j = 0; j < staffObjects.get(i).size(); j++) {
//                Rect rect_j = staffObjects.get(i).get(j);
//                Mat printMat = extractFromNoStaffImg(rect_j);
////                invertImgColor(printMat);
//                Imgcodecs.imwrite(root + String.format("/Piano/Images/staff_%02d_element_%02d.png", i, j), printMat);
//            }
//        }
    }

    public List<Boolean[]> getSonatinaNoteGroups() {
        List<Boolean[]> list = new ArrayList<>();

        Boolean[] staff1 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, true, true, false, true, false, true, true, false, true, true, true, false, true, false, true, true, false, true, true, true, false, true, false};
//        Boolean[] staff1 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, true, false, true, true, false, true, true, true, false, true, false, true, true, false, true, true, true, false, true, false};
        Boolean[] staff2 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, false, true, false, true, true, true, true, true, true, false, true, true, true, true, true, true, true, false};
        Boolean[] staff3 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, true, false, true, true, false, true, true, true, false, false, false, true, false, true, true, false, true, true, true, true, false, false};
        Boolean[] staff4 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, true, false, true, true, false, true, true, true, false, true, false, true, true, false, true, true, true, false, true, false};
        Boolean[] staff5 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, false, true, false, true, true, false, true, true, true, true, false, true, false, true, true, false, true, true, true, false, true, false} ;
        Boolean[] staff6 = new Boolean[] { false, false, false, false, false, false, false, false, false, false, true, true, false, true, true, true, true, false, true, true, false, true, true, true, true, false, true, true, true, true, true, true, false, true, true, true, false, false};

        list.add(staff1);
        list.add(staff2);
        list.add(staff3);
        list.add(staff4);
        list.add(staff5);
        list.add(staff6);
        return list;
    }

    public double[] getImageData(int row, int col) {
        return noStaffLinesImg.get(row, col);
    }
}
