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
import piano.pianotrainer.scoreModels.Note;
import piano.pianotrainer.scoreModels.NoteGroup;

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

    public boolean isMeasureBar(Rect rect) {
        return false;
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
                knnResults.get(i).add(testKnnMat(extractFromNoStaffImg(curRect)));
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

    public void dotFilter(){
        List<Integer> curStaffResults;
        Rect curRect;
        for(int i = 0; i < knnResults.size(); i++){
            for(int j = 0; j < knnResults.get(i).size(); j++){
                if(knnResults.get(i).get(j) == KnnLabels.DOT){
                    curRect = staffObjects.get(i).get(j);
                    //length bigger than width by a big factor say its barline
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

    public NoteGroup classifyNoteGroup(Rect rect, int staffNum)  {
        Mat objMat = extractFromNoStaffImg(rect);
        Mat allCircles = new Mat();

        Imgproc.HoughCircles(objMat, allCircles, CV_HOUGH_GRADIENT, 1, ((double)staffLineDiff)*0.8, 200, 4, (int)(((double)staffLineDiff)*0.5), (int)(((double)staffLineDiff)*0.75));
        Map<Point, Integer> circles = new HashMap<Point, Integer>();
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
            populatePitchAndScale(note, circle.getKey(), rect.top, inTreble, staffNum);

            noteGroup.notes.add(note);
        }

//        String root = Environment.getExternalStorageDirectory().toString();
//        Imgcodecs.imwrite(root + "/Piano/Images/full_score_mask.png", mask);
        return null;
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

            if (topDist < bottomDist && topDist < middleDist) {

            }
            else if (bottomDist < topDist && bottomDist < middleDist) {

            }
            else {

            }

        }
        else if (i > 5 && i < 10) {
            // bass in between
        }
        else if (i == 0){
            // top of treble

        }
        else if (i == 5) {
            // in between treble and bass
        }
        else {
            // bottom of bass
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
            circle(colorFullMat, pt, radius, new Scalar(0,0,150), 1);
        }
        String root = Environment.getExternalStorageDirectory().toString();
        Imgcodecs.imwrite(root + "/Piano/Images/full_score_circles.png", colorFullMat);
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
                    if (noStaffLinesImg.get(row, col)[0] == 255.0) {
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
                    int padding = 1;
                    int col = obj.left + padding;
                    int rowAvg = 0;
                    int rowCount = 0;

                    for (int row = obj.top + padding; row < obj.bottom - padding; row++) {
                        if (noStaffLinesImg.get(row, col)[0] == 255.0) {
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
