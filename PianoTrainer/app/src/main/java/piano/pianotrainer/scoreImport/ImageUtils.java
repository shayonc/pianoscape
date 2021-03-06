package piano.pianotrainer.scoreImport;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-14.
 */

public final class ImageUtils {

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private ImageUtils() {

    }

    public static Mat bitmapToMat(Bitmap bmp32){
        Mat imgMat = new Mat();
        Utils.bitmapToMat(bmp32,imgMat);
        return imgMat;
    }

    public static Mat bgrToGrayscale(Mat imgMat){
        //Channel Conversion since colors -> RGB
        Mat result = new Mat();
        Log.d("ImageUtils", String.format("imgMat: %d", imgMat.get(0,0).length));
        Log.d("ImageUtils", String.format("imgMat channels: %d", imgMat.channels()));
        Imgproc.cvtColor(imgMat,result,Imgproc.COLOR_BGRA2GRAY);
        Log.d("ImageUtils", String.format("result: %d", result.get(0,0).length));
        return result;
    }

    public static String saveImageToExternal(Bitmap finalBitmap, String imgName) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/Piano/Images");
        myDir.mkdirs();
        File file = new File (myDir, imgName);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return root + "/Piano/Images";
    }

    //TODO: Generalize for saving across the app
    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


}
