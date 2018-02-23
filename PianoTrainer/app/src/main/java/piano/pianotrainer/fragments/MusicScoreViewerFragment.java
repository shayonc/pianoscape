package piano.pianotrainer.fragments;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-10.
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import piano.pianotrainer.R;
import piano.pianotrainer.scoreImport.ImageUtils;
import piano.pianotrainer.scoreImport.KnnLabels;
import piano.pianotrainer.scoreImport.PDFHelper;
import piano.pianotrainer.scoreImport.ScoreProcessor;
import piano.pianotrainer.scoreModels.ElementType;
import piano.pianotrainer.scoreModels.Measure;
import piano.pianotrainer.scoreModels.Score;
import piano.pianotrainer.scoreModels.Staff;

public class MusicScoreViewerFragment extends Fragment implements View.OnClickListener{
    /**
     * Key string for saving the state of current page index.
     */
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    private static final String FILENAME = "zimmer_pirate.pdf";

    private static final String TRAINING = "training_set";

    private ParcelFileDescriptor mFileDescriptor;
    private ImageView mImageView;
    private PDFHelper mPdfHelper;
    //used for loading saved page index from save states before we re-init PDF Helper object
    private int mPageIndexSaved;

    private ScoreProcessor scoreProc;
    public int objectIndex = 0;

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

    private void showObject(int index) {
        Bitmap bmpObject = scoreProc.getStaffObject(0, index);
        mImageView.setImageBitmap(bmpObject);
        mDebugView.setText(String.format("Object -> Width: %d, Height: %d", bmpObject.getWidth(), bmpObject.getHeight()));
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    private void showPage(int index) {
        Bitmap curPageBitmap = mPdfHelper.toImg(index);
        // We are ready to show the Bitmap to user.
        mImageView.setImageBitmap(curPageBitmap);

        //Save img internally - help analyze pixels
        //mDebugView.setText(ImageUtils.saveImageToExternal(curPageBitmap,"testImg.png"));
        //updateUi();
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

//    public boolean addTrainingImages(String fileDir){
//        Resources res = getResources();
//        AssetManager am = res.getAssets();
//        Bitmap curBmp;
//        InputStream inputstream;
//        try {
//            //Primitive testing only two different training sets first
//            //get all the file names under the specified training set
//            //train with all the even indexed images in each of the directories
//            String fileList[] = am.list(fileDir);
//
//            if (fileList != null)
//            {
//                for ( int i = 0;i<fileList.length;i++)
//                {
//                    inputstream = appContext.getAssets().open(fileDir + fileList[i]);
//                    curBmp = BitmapFactory.decodeStream(inputstream);
//                    Log.d("",fileList[i]);
//                    if(i % 2 == 0){
//                        scoreProc.addSample(curBmp, 10);
//                    }
//                }
//
//            }
//            else{
//                Log.d("","NULL filelist!!");
//            }
//    }

    public boolean addTrainingImages( String fileDir, int label){
        Resources res = getResources();
        AssetManager am = res.getAssets();
        Bitmap curBmp;
        InputStream inputstream;
        try{
            String fileList[] = am.list(fileDir);

            if (fileList != null)
            {
                for ( int i = 0;i<fileList.length;i++)
                {
                    inputstream=appContext.getAssets().open(fileDir + "/" +fileList[i]);
                    curBmp = BitmapFactory.decodeStream(inputstream);
                    Log.d("",fileList[i]);
                    scoreProc.addSample(curBmp, label);
                }
                return true;
            }
            else{
                Log.d("","NULL filelist!!");
                return false;
            }
        }

        catch(IOException exc){
            Log.d("","IOException caught!");
            return false;
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.previous: {
                // Move to the previous page
//                showPage(mPdfHelper.getCurPage().getIndex() - 1);
                if (objectIndex > 0) {
                    objectIndex--;
                }
                showObject(objectIndex);
                break;
            }
            case R.id.next: {
                // Move to the next page
//                showPage(mPdfHelper.getCurPage().getIndex() + 1); EC: testing objects
                objectIndex++;
                showObject(objectIndex);
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

                    //image processing
                    //loads the Mat object of the image
                    // TODO: get from user
                    String scoreTitle = "Twinkle Twinkle Little Star";
                    scoreProc = new ScoreProcessor(bitmap);
                    Score score = new Score(scoreTitle);

                    //Threshold so Mat will only have 0s and 255s
                    scoreProc.binarize();
                    scoreProc.removeStaffLines();
                    scoreProc.refineStaffLines();

                    boolean grandStaff = scoreProc.isGrandStaff();
                    if (!grandStaff) {
                        // TODO: return error to user in a dialog
                        mDebugView.setText("Grand staffs not found.");
                        break;
                    }

                    int numPulses = 0;
                    double basicPulse = 0;
                    List<List<Rect>> staffObjects = scoreProc.detectObjects();
                    for (int i = 0; i < staffObjects.size(); i++) {
                        Staff staff = new Staff(true);
                        score.addStaff(staff);
                        List<Rect> objects = staffObjects.get(i);

                        Measure curMeasure = new Measure();
                        staff.addMeasure(curMeasure);

                        boolean firstVertBar = true;
                        int numElemsInMeasure = 0;
                        Map<Integer, ElementType> elementTypeMap = new HashMap<>(objects.size());

                        for (int j = 0; j < objects.size(); j++) {
                            Rect obj = objects.get(j);
                            // TODO: add symbol detection here
                            // if not a symbol, then run following code
                            if (j < 10) continue;
                            if (scoreProc.isMeasureBar(obj) && firstVertBar) {
                                elementTypeMap.put(j, ElementType.MeasureBar);
                                firstVertBar = false;
                            }
                            else if (scoreProc.isMeasureBar(obj) && !firstVertBar) {
                                elementTypeMap.put(j, ElementType.MeasureBar);
                                staff.addMeasure(curMeasure);
                                numElemsInMeasure = 0;
                                curMeasure = new Measure();
                            }

                        }
                    }

                    //TRAINING SETS FOR SYMBOLS
                    //load the training images and train symbol detection
                    addTrainingImages("training_set/g_clef", KnnLabels.G_CLEF);
                    addTrainingImages("training_set/f_clef", KnnLabels.F_CLEF);
                    addTrainingImages("training_set/brace", KnnLabels.BRACE);

                    addTrainingImages("training_set/time_four_four", KnnLabels.TIME_44);
                    addTrainingImages("training_set/time_three_four", KnnLabels.TIME_34);
                    addTrainingImages("training_set/time_six_eight", KnnLabels.TIME_68);
                    addTrainingImages("training_set/time_two_four", KnnLabels.TIME_24);
                    addTrainingImages("training_set/common_time", KnnLabels.TIME_C);
                    //Rests
                    //TODO: Distinguish whole/half or ignore (85%)
                    addTrainingImages("training_set/whole_half_rest", KnnLabels.WHOLE_HALF_REST);
                    addTrainingImages("training_set/quarter_rest", KnnLabels.QUARTER_REST);
                    addTrainingImages("training_set/eight_rest", KnnLabels.EIGHTH_REST);
                    addTrainingImages("training_set/one_16th_rest", KnnLabels.ONE_SIXTEENTH_REST);
                    addTrainingImages("training_set/whole_note", KnnLabels.WHOLE_NOTE);
                    addTrainingImages("training_set/whole_note_2", KnnLabels.WHOLE_NOTE_2);
                    //accidentals
                    addTrainingImages("training_set/sharp", KnnLabels.SHARP_ACC);
                    addTrainingImages("training_set/natural", KnnLabels.NATURAL_ACC);
                    addTrainingImages("training_set/flat", KnnLabels.FLAT_ACC);
                    //others
                    addTrainingImages("training_set/slur", KnnLabels.TIE);
                    addTrainingImages("training_set/dynamics_f", KnnLabels.DYNAMICS_F);
                    addTrainingImages("training_set/dot_set", KnnLabels.DOT);

                    //Train
                    scoreProc.trainKnn();
                    //test all
                    scoreProc.testMusicObjects();
                    try {
//                        int numCircles = scoreProc.classifyNoteGroup();
//                        mDebugView.setText(String.format("Number of circles: %d", numCircles));
                        Mat circles = scoreProc.classifyNoteGroup();
                        mDebugView.setText(String.format("%d", circles.cols()));
//                        mImageView.setImageBitmap(ngBmp);
                    }
                    catch (Exception e) {
                        mDebugView.setText(e.toString());
                    }

                    List<List<Integer>> knnResults = scoreProc.getKnnResults();
                    Bitmap testBmp = Bitmap.createBitmap(scoreProc.noStaffLinesImg.width(),scoreProc.noStaffLinesImg.height(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(scoreProc.noStaffLinesImg, testBmp);

                    Canvas cnvs = new Canvas(testBmp);
                    //...setting paint objects with brute force >_>
                    Paint paintR=new Paint();
                    paintR.setStyle(Paint.Style.STROKE);
                    paintR.setColor(Color.RED);

                    Paint paintB=new Paint();
                    paintB.setStyle(Paint.Style.STROKE);
                    paintB.setColor(Color.BLUE);

                    Paint paintG =new Paint();
                    paintG.setStyle(Paint.Style.STROKE);
                    paintG.setColor(Color.GREEN);

                    Paint paintM =new Paint();
                    paintM.setStyle(Paint.Style.STROKE);
                    paintM.setColor(Color.MAGENTA);

                    Paint paintC =new Paint();
                    paintC.setStyle(Paint.Style.STROKE);
                    paintC.setColor(Color.CYAN);

                    Paint paintY =new Paint();
                    paintY.setStyle(Paint.Style.STROKE);
                    paintY.setColor(Color.YELLOW);

                    Paint paintTxt =new Paint();
                    paintTxt.setStyle(Paint.Style.FILL);
                    paintTxt.setColor(Color.RED);
                    paintTxt.setTextSize(30);

                    for(int i = 0; i < staffObjects.size(); i++){
                        for(int j = 0; j < staffObjects.get(i).size(); j++){
                            if(knnResults.get(i).get(j)/10 == 0){
                                cnvs.drawRect(staffObjects.get(i).get(j), paintR);
                            }
                            if(knnResults.get(i).get(j)/10 == 1){
                                cnvs.drawRect(staffObjects.get(i).get(j), paintB);
                            }
                            if(knnResults.get(i).get(j)/10 == 2){
                                cnvs.drawRect(staffObjects.get(i).get(j), paintG);
                            }
                            if(knnResults.get(i).get(j)/10 == 3){
                                cnvs.drawRect(staffObjects.get(i).get(j), paintM);
                            }
                            if(knnResults.get(i).get(j)/10 == 4){
                                cnvs.drawRect(staffObjects.get(i).get(j), paintC);
                            }
                            cnvs.drawText(knnResults.get(i).get(j).toString(),
                                    staffObjects.get(i).get(j).left, staffObjects.get(i).get(j).top, paintTxt);
                        }
                    }
                    mImageView.setImageBitmap(testBmp);

//                    Canvas cnvs = new Canvas(bitmap);
//                    Paint paint=new Paint();
//                    paint.setStyle(Paint.Style.STROKE);
//                    paint.setColor(Color.RED);
//                    for(List<Rect> rectList : staffObjects){
//                        for(Rect symbolRect : rectList){
//                            cnvs.drawRect(symbolRect, paint);
//                        }
//                    }
//                    Bitmap testBmp = Bitmap.createBitmap(scoreProc.noStaffLinesImg.width(),scoreProc.noStaffLinesImg.height(),Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(scoreProc.noStaffLinesImg, testBmp);
                    mImageView.setImageBitmap(testBmp);
                    ImageUtils.saveImageToExternal(testBmp, "testSonatina.bmp");
                    mDebugView.setText("Rects exporting...");
                    scoreProc.exportRects(getActivity());
                    mDebugView.setText("Rects exported!");
                }
                else {
                    mDebugView.setText("pfd or pdfRenderer not instantiated.");
                }

                break;
            }
        }
    }
}
