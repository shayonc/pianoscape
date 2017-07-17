package piano.pianotrainer.score_importing;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-10.
 */

public class PDFHelper {
    private PdfRenderer mPdfRenderer;
    private PdfRenderer.Page mCurrentPage;
    private int mPageIndex;
    private Bitmap curPageBmp; //will be used for multi-page scores

    public PDFHelper(ParcelFileDescriptor fileDescriptor) {
        try {
            mPdfRenderer = new PdfRenderer(fileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
            //logging
        }
    }

    //USED FOR IMG PROCESSING TESTING: can apply any algorithms on the bmp of pdf and output it
    public Bitmap toBinImg(int index){
        //close current page
        if(mCurrentPage != null){
            mCurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        //IMG PROCESSING:
        //feed in the bitmap to be binarized
        ScoreImgProc scoreProc = new ScoreImgProc(bitmap);
        scoreProc.binarize();
        scoreProc.removeStaffLines(true);
        //after binarization and staff line elimination
        Bitmap binBitmap = scoreProc.getNoStaffLinesImg();
        return binBitmap;
    }

    //Can be used for just displaying the actual Bitmaps for the PDFs generated with no processing
    public Bitmap toImg(int index){
        //close current page
        if(mCurrentPage != null){
            mCurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        //pass the return bitmap to imageview if needed/opencv
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        return bitmap;
    }

    /**
     * Closes the {@link android.graphics.pdf.PdfRenderer} and related resources.
     *
     * @throws java.io.IOException When the PDF file cannot be closed.
     */
    public void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        mPdfRenderer.close();
    }

    public int getPageCount() {
        return mPdfRenderer.getPageCount();
    }

    public PdfRenderer.Page getCurPage(){
        return mCurrentPage;
    }

    public int getCurPageIndex() {
        return mPageIndex;
    }

    //useful for saved instance usage where Views are re-created
    public void setCurPageIndex(int index) {
        mPageIndex = index;
    }

    public void closeCurPage() {
        mCurrentPage.close();
    }

    public void setCurPage(int index) {
        mCurrentPage = mPdfRenderer.openPage(index);
    }






}
