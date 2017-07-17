package piano.pianotrainer.score_importing;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.threshold;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-15.
 */

public class ScoreImgProc {
    private static final String TAG = "ScoreImgProc";

    private Mat originalImg;
    private Mat grayImg;
    private Mat binarizedImg;
    private Mat noStaffLinesImg;

    public ScoreImgProc(Bitmap bmpImg){
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
        Size kernelHeight = new Size(1,3);
        Mat verticalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelHeight);
        //erode out the lines
        Imgproc.erode(binarizedImg,noStaffLinesImg,verticalStructure,pt,1);
        //should re-amp the blackness of remaining black things in the picture
        Imgproc.dilate(noStaffLinesImg,noStaffLinesImg,verticalStructure,pt,1);
    }

    //Uses horizontal morphology and subtracts from the original img
    public void removeStaffLines(boolean horzMorph){
        // Relative measure which seemed ok
        int horizontalsize = binarizedImg.cols() / 30;
        Size kernelWidth = new Size(horizontalsize,1);
        Point pt = new Point(-1,-1); //current pixel is the 'center' when applying operations
        // Create structure element for extracting horizontal lines through morphology operations
        Mat horizontalStructure = Imgproc.getStructuringElement(MORPH_RECT, kernelWidth);
        // Apply morphology operations
        Imgproc.erode(binarizedImg, noStaffLinesImg, horizontalStructure, pt,1);
        // "reamps" the remaining elements on the page as their contours were previously eroded
        Imgproc.dilate(noStaffLinesImg, noStaffLinesImg, horizontalStructure, pt,1);
        //ideally the image after morphology only contains staff lines which are no subtracted out
        Core.subtract(binarizedImg,noStaffLinesImg,noStaffLinesImg);
    }

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
}
