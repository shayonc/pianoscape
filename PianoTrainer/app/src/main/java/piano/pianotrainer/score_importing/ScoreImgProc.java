package piano.pianotrainer.score_importing;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
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

    public ScoreImgProc(Bitmap bmpImg){
        originalImg = new Mat();
        grayImg = new Mat();
        binarizedImg = new Mat();
        Utils.bitmapToMat(bmpImg,originalImg);
        int x = originalImg.rows();
        int y = originalImg.cols();
    }

    public void detectStaffLines(){
        grayImg = ImageUtils.bgrToGrayscale(this.originalImg);
        int x = grayImg.cols();
        int y = grayImg.rows();
        try {
            //Imgproc.adaptiveThreshold(grayImg, binarizedImg, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, -2);
            Imgproc.threshold(grayImg,binarizedImg,150,255,THRESH_BINARY);
            int a = binarizedImg.cols();
            int b = binarizedImg.rows();
        }
        catch(Exception e){
            Log.d(TAG,e.getMessage());
        }
    }

    public Bitmap getBinImg(){
        int x = binarizedImg.cols();
        int y = binarizedImg.rows();
        Bitmap bmp = Bitmap.createBitmap(binarizedImg.cols(),binarizedImg.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(binarizedImg,bmp);
        return bmp;
    }
}
