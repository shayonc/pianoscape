package piano.pianotrainer.fragments;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-10.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.RotatedRect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import piano.pianotrainer.R;
import piano.pianotrainer.scoreImport.ImageUtils;
import piano.pianotrainer.scoreImport.PDFHelper;
import piano.pianotrainer.scoreImport.ScoreProcessor;

public class MusicScoreViewerFragment extends Fragment implements View.OnClickListener{
    /**
     * Key string for saving the state of current page index.
     */
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    private static final String FILENAME = "twinkle_twinkle_little_star.pdf";
    private ParcelFileDescriptor mFileDescriptor;
    private ImageView mImageView;
    private PDFHelper mPdfHelper;
    //used for loading saved page index from save states before we re-init PDF Helper object
    private int mPageIndexSaved;

    /**
     * {@link android.widget.Button} to move to the previous page.
     */
    private Button mButtonPrevious;
    private Button mButtonImport;
    private TextView mDebugView;
    /**
     * {@link android.widget.Button} to move to the next page.
     */
    private Button mButtonNext;

    //store appContext which is helpful for accessing internal file storage if we go that route
    private Context appContext;

    public MusicScoreViewerFragment() {
    }

    // The onCreate method is called when the Fragment instance is being created, or re-created.
    // Use onCreate for any standard setup that does not require the activity to be fully created
    //Use this to set vars
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Pass variables
        //fragment is already tied to an activity
        this.appContext = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_score_viewer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Retain view references.
        mImageView = (ImageView) view.findViewById(R.id.image);
        mDebugView = (TextView) view.findViewById(R.id.debug_view);
        mButtonPrevious = (Button) view.findViewById(R.id.previous);
        mButtonImport = (Button) view.findViewById(R.id.import_sheet);
        mButtonNext = (Button) view.findViewById(R.id.next);
        // Bind events.
        mButtonPrevious.setOnClickListener(this);
        mButtonImport.setOnClickListener(this);
        mButtonNext.setOnClickListener(this);
        mPageIndexSaved = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            mPageIndexSaved = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            openRenderer(getActivity());
            showPage(mPdfHelper.getCurPage().getIndex());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //TODO: Bug that crashes when you click back on this page
    @Override
    public void onStop() {
        try {
            mPdfHelper.closeRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mPdfHelper.getCurPage()) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mPdfHelper.getCurPageIndex());
        }
    }

    /**
     * Sets up a {@link android.graphics.pdf.PdfRenderer} and related resources.
     */
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
            // the cache directory.
            InputStream asset = context.getAssets().open(FILENAME);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        // This is the PdfRenderer we use to render the PDF.
        if (mFileDescriptor != null) {
            //EC mPdfRenderer = new PdfRenderer(mFileDescriptor);
            mPdfHelper = new PDFHelper(mFileDescriptor);
            mPdfHelper.setCurPage(mPageIndexSaved);
        }
    }



    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    private void showPage(int index) {
        Bitmap curPageBitmap = mPdfHelper.toBinImg(index);
        // We are ready to show the Bitmap to user.
        mImageView.setImageBitmap(curPageBitmap);

        //Save img internally - help analyze pixels
        mDebugView.setText(ImageUtils.saveImageToExternal(curPageBitmap,"testImg.png"));
        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi() {
        int index = mPdfHelper.getCurPageIndex();
        int pageCount = mPdfHelper.getPageCount();
        mButtonPrevious.setEnabled(0 != index);
        mButtonNext.setEnabled(index + 1 < pageCount);
            getActivity().setTitle(getString(R.string.music_score_name_with_index, index + 1, pageCount));
    }

    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing.
     *
     * @return The number of pages.
     */
    public int getPageCount() {
        return mPdfHelper.getPageCount();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.previous: {
                // Move to the previous page
                showPage(mPdfHelper.getCurPage().getIndex() - 1);
                break;
            }
            case R.id.next: {
                // Move to the next page
                showPage(mPdfHelper.getCurPage().getIndex() + 1);
                break;
            }
            case R.id.import_sheet: {
                // import sheet workflow

                File file = new File(getActivity().getCacheDir(), FILENAME);
                ParcelFileDescriptor pfd = null;
                PdfRenderer pdfRenderer = null;
                try {
                    pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                    pdfRenderer = new PdfRenderer(pfd);
                }
                catch(Exception e) {
                    mDebugView.setText("pfd not instantiated.");
                }

                if (pfd != null && pdfRenderer != null) {
                    PDFHelper pdfHelper = new PDFHelper(pfd);
                    pdfHelper.setCurPage(mPageIndexSaved);

                    //Bitmap curPageBitmap = pdfHelper.toBinImg(pdfHelper.getCurPage().getIndex());
                    PdfRenderer.Page curPage = pdfRenderer.openPage(pdfHelper.getCurPage().getIndex());

                    mDebugView.setText(String.format("width: %d, height: %d", curPage.getWidth(), curPage.getHeight()));
                    Bitmap bitmap = Bitmap.createBitmap(curPage.getWidth()*3, curPage.getHeight()*3,
                            Bitmap.Config.ARGB_8888);
                    curPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);


                    ScoreProcessor scoreProc = new ScoreProcessor(bitmap);
                    scoreProc.binarize();
                    scoreProc.removeStaffLines(true);
                    //Bitmap binBitmap = scoreProc.getNoStaffLinesImg();

                    scoreProc.refineStaffLines();

                    List<List<Rect>> staffObjects = scoreProc.detectObjects();


                    List<Integer> bCounts = scoreProc.classifyObjects();
                    mDebugView.setText(staffObjects.toString() + "\n\n" + bCounts.toString());


                    scoreProc.writeXML();


                    mImageView.setImageBitmap(bitmap);
                }
                else {
                    mDebugView.setText("pfd or pdfRenderer not instantiated.");
                }

                break;
            }
        }
    }
}
