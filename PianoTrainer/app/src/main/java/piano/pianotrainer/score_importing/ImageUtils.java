package piano.pianotrainer.score_importing;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-14.
 */

public final class ImageUtils {

    private ImageUtils() {

    }

    public static Mat bitmapToMat(Bitmap bmp32){
        Mat imgMat = new Mat();
        Utils.bitmapToMat(bmp32,imgMat);
        return imgMat;
    }

    public static Mat bgrToGrayscale(Mat imgMat){
        //Mat grayImg = new Mat(imgMat.rows(),imgMat.cols(),imgMat.type());
        //Channel Conversion since colors -> RGB
        if(imgMat.channels() == 3){
            Imgproc.cvtColor(imgMat,imgMat,Imgproc.COLOR_BGR2GRAY);
        }
        return imgMat;
    }



}
