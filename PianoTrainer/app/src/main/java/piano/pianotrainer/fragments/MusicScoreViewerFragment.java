package piano.pianotrainer.fragments;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-10.
 */

import android.app.ProgressDialog;
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
import org.opencv.core.RotatedRect;

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
import piano.pianotrainer.scoreImport.SymbolMapper;
import piano.pianotrainer.scoreModels.ElementType;
import piano.pianotrainer.scoreModels.Measure;
import piano.pianotrainer.scoreModels.Note;
import piano.pianotrainer.scoreModels.NoteGroup;
import piano.pianotrainer.scoreModels.Score;
import piano.pianotrainer.scoreModels.Staff;
import piano.pianotrainer.fragments.ScoreImportToXmlParser;

public class MusicScoreViewerFragment extends Fragment implements View.OnClickListener{
    /**
     * Key string for saving the state of current page index.
     */

    static final String TAG = "ScoreViewer";

    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    private static final String SCORE_NAME = "handel_sonatina_shifted44";

    private static final String FILENAME = SCORE_NAME + ".pdf";

    private static final String IMGNAME = "final_" + SCORE_NAME + ".bmp";

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
//        mButtonPrevious = (Button) view.findViewById(R.id.previous);
        mButtonImport = (Button) view.findViewById(R.id.import_sheet);
//        mButtonNext = (Button) view.findViewById(R.id.next);
        // Bind events.
//        mButtonPrevious.setOnClickListener(this);
        mButtonImport.setOnClickListener(this);
//        mButtonNext.setOnClickListener(this);
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
//        mButtonPrevious.setEnabled(0 != index);
//        mButtonNext.setEnabled(index + 1 < pageCount);
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
        Bitmap tmpBmp;
        try{
            String fileList[] = am.list(fileDir);

            if (fileList != null)
            {
                for ( int i = 0;i<fileList.length;i++)
                {
                    inputstream=appContext.getAssets().open(fileDir + "/" +fileList[i]);
                    curBmp = BitmapFactory.decodeStream(inputstream);
                    Log.d("",fileList[i]);
                    if(fileDir.contains("g_clef") && i == 0){
                        scoreProc.addSample(curBmp, label, true);
                        tmpBmp = Bitmap.createBitmap(scoreProc.tmpImg.width(),scoreProc.tmpImg.height(),Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(scoreProc.tmpImg, tmpBmp);
                        ImageUtils.saveImageToExternal(tmpBmp, "resizedClef.bmp");
                        ImageUtils.saveImageToExternal(curBmp, "originalClef.bmp");
                    }
                    else{
                        scoreProc.addSample(curBmp, label, false);
                    }
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
//            case R.id.previous: {
//                // Move to the previous page
////                showPage(mPdfHelper.getCurPage().getIndex() - 1);
//                if (objectIndex > 0) {
//                    objectIndex--;
//                }
//                showObject(objectIndex);
//                break;
//            }
//            case R.id.next: {
//                // Move to the next page
////                showPage(mPdfHelper.getCurPage().getIndex() + 1); EC: testing objects
//                objectIndex++;
//                showObject(objectIndex);
//                break;
//            }
            case R.id.import_sheet: {
                // import sheet workflow

                ProgressDialog pr;

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

                    // Generating hard-coded symbols data
                    List<Boolean[]> sonatina_symbols = scoreProc.getSonatinaNoteGroups();
                    Map<Rect, List<String>> canvasDrawings = new LinkedHashMap<>();

                    //TRAINING SETS FOR SYMBOLS
                    //load the training images and train symbol detection
                    addTrainingImages("training_set/g_clef", KnnLabels.G_CLEF);
                    addTrainingImages("training_set/f_clef", KnnLabels.F_CLEF);
                    addTrainingImages("training_set/brace", KnnLabels.BRACE);

                    addTrainingImages("training_set/time_four_four", KnnLabels.TIME_44);
                    addTrainingImages("training_set/time_three_four", KnnLabels.TIME_34);
                    addTrainingImages("training_set/time_six_eight", KnnLabels.TIME_68);
                    addTrainingImages("training_set/time_two_two", KnnLabels.TIME_22);
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


                    List<List<Integer>> knnResults = scoreProc.getKnnResults();
                    scoreProc.dotFilter();

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




                    int curLabel;
                    Boolean isTreble;
                    Measure curMeasure = new Measure();
                    for (int i = 0; i < staffObjects.size(); i++) {
                        Staff staff = new Staff(true);
                        score.addStaff(staff);
                        List<Rect> objects = staffObjects.get(i);

                        //Measure curMeasure = new Measure();
                        //staff.addMeasure(curMeasure);

                        boolean firstVertBar = false;
                        int numElemsInMeasure = 0;

                        Boolean[] isNoteGroup2 = sonatina_symbols.get(i);
                        Map<Integer, Rect> dotsInStaff = new LinkedHashMap<>();

                        for (int j = 0; j < objects.size(); j++) {
                            Rect obj = objects.get(j);
                            isTreble = scoreProc.inTrebleCleff(obj.top, obj.bottom, i);
                            curLabel = knnResults.get(i).get(j);
                            // TODO: add symbol detection here
                            if (curLabel == KnnLabels.BAR) {
                                if (!firstVertBar) {
                                    firstVertBar = true;
                                    // curMeasure = new Measure();
                                }
                                else {
                                    Log.d(TAG, "hit new bar");
                                    //dot integration and handling accidental/ties
                                    Log.d(TAG, String.format("Staff %d Measure %d with info %s",i,
                                            staff.getNumMeasures(), curMeasure.info()));
                                    curMeasure.checkNeighbours();
                                    staff.addMeasure(curMeasure);
                                    curMeasure = new Measure();
                                }
                            }
                            else if(curLabel == KnnLabels.G_CLEF){
                                curMeasure.addClef(ElementType.TrebleClef, obj);
                            }
                            else if (scoreProc.isNoteGroup(obj)){
//                                if ((i == 1 && j == 15) || (i == 3 && j == 32) || (i == 4 && j == 13)) {
//                                    int hello = 0;
//                                    int hello2 = hello*2;
//                                }

                                NoteGroup notegroup = scoreProc.classifyNoteGroup(objects.get(j), i, isTreble);
                                //TODO: figure out null notegroups
                                if(notegroup == null){
                                    Log.d(TAG, String.format("null notegroup on rect at %d,%d", i, j));
//                                    cnvs.drawRect(obj, paintB);
                                }
                                else{
                                    curMeasure.addNoteGroup(obj, notegroup, isTreble);
                                    String s = "[";

                                    for (Note note : notegroup.notes) {
                                        s += (note.pitch.toString() + Integer.toString(note.scale) + ",");
                                    }
                                    s += "]";
                                    canvasDrawings.put(staffObjects.get(i).get(j), new ArrayList<String>());
                                    canvasDrawings.get(staffObjects.get(i).get(j)).add(s);
//                                    cnvs.drawText(s, staffObjects.get(i).get(j).left, staffObjects.get(i).get(j).top, paintTxt);
                                    s = "[";
                                    for (Note note : notegroup.notes) {
                                        s += (Double.toString(note.weight) + ",");
                                    }

                                    s += "]";
                                    canvasDrawings.get(staffObjects.get(i).get(j)).add(s);
//                                    cnvs.drawText(s, staffObjects.get(i).get(j).left, staffObjects.get(i).get(j).bottom, paintTxt);
                                }

                            }
                            else {
                                // Ekteshaf: place your knn testing for each object here
                                //general symbols
//                                if (!scoreProc.isNoteGroup(obj) && isNoteGroup2[j]) {
////                                    boolean res = scoreProc.isNoteGroup(obj);
////                                    boolean hello = !res;
//                                    Log.d(TAG, String.format("NOT notegroup on rect at %d,%d", i, j));
//                                }
                                if(curLabel == KnnLabels.G_CLEF){
                                    curMeasure.addClef(ElementType.TrebleClef, obj);
                                }

                                else if(curLabel == KnnLabels.F_CLEF){
                                    curMeasure.addClef(ElementType.BassClef, obj);
                                }

                                else if(SymbolMapper.isRest(curLabel)){
                                    curMeasure.addRest(obj, SymbolMapper.classifyRest(knnResults.get(i).get(j), isTreble));
                                }
                                //accidentals
                                else if(curLabel == KnnLabels.FLAT_ACC){
                                    curMeasure.addToClefLists(isTreble, obj, ElementType.Flat);
                                    Log.d(TAG, String.format("Flat: added rounded yPos %.2f", scoreProc.getCenterYOfFlat(obj, ElementType.Flat, i)));
                                    curMeasure.addAccidentalCenter(obj, scoreProc.getCenterYOfFlat(obj, ElementType.Flat, i));
                                }
                                else if(curLabel == KnnLabels.SHARP_ACC){
                                    curMeasure.addToClefLists(isTreble, obj, ElementType.Sharp);
                                    curMeasure.addAccidentalCenter(obj, scoreProc.getCenterYOfFlat(obj, ElementType.Sharp, i));
                                }
                                else if(curLabel == KnnLabels.NATURAL_ACC){
                                    curMeasure.addToClefLists(isTreble, obj, ElementType.Natural);
                                    curMeasure.addAccidentalCenter(obj, scoreProc.getCenterYOfFlat(obj, ElementType.Natural, i));
                                }
                                //time sig
                                else if(SymbolMapper.isTimeSig(curLabel)){
                                    curMeasure.setTimeSig(SymbolMapper.getUpperTimeSig(curLabel), SymbolMapper.getLowerTimeSig(curLabel),
                                                            isTreble, obj);
                                }
                                //other symbols
                                else if(curLabel == KnnLabels.DOT){
                                    curMeasure.addToClefLists(isTreble, obj, ElementType.Dot);
                                    //save dot index for dot integration at the end
                                    dotsInStaff.put(j, obj);
                                    Log.d(TAG, String.format("Dot added at pos %d,%d",i,j));
                                }
                                else if(curLabel == KnnLabels.WHOLE_NOTE){
                                    Note whole = new Note();
                                    whole.weight = 1.0;
                                    whole.clef = isTreble ? 0 : 1;
                                    List<Note> wholeNoteList = new ArrayList<Note>();
                                    //add
                                    NoteGroup wNg = new NoteGroup(wholeNoteList);
                                    curMeasure.addNoteGroup(obj, wNg, isTreble);
                                    Log.d(TAG, String.format("Whole note added at pos %d,%d",i,j));
                                }
                                //TODO: wholenote2
                            }
                        }

                    }



//                    Bitmap testBmp = Bitmap.createBitmap(scoreProc.noStaffLinesImg.width(),scoreProc.noStaffLinesImg.height(),Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(scoreProc.noStaffLinesImg, testBmp);
//
//                    Canvas cnvs = new Canvas(testBmp);
//                    //...setting paint objects with brute force >_>

//                    mImageView.setImageBitmap(testBmp);

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

                    Bitmap finalBmp = Bitmap.createBitmap(scoreProc.colorFinalImg.width(),scoreProc.colorFinalImg.height(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(scoreProc.colorFinalImg, finalBmp);
                    Canvas cnvs = new Canvas(finalBmp);
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

                    for (Map.Entry<Rect, List<String>> entry : canvasDrawings.entrySet()) {
                        Rect rect = entry.getKey();
                        List<String> s = entry.getValue();
                        cnvs.drawText(s.get(0), rect.left, rect.top, paintTxt);
                        cnvs.drawText(s.get(1), rect.left, rect.bottom, paintTxt);
                    }

                    mImageView.setImageBitmap(finalBmp);
                    ImageUtils.saveImageToExternal(finalBmp, IMGNAME);
                    scoreProc.exportRects(getActivity());

                    ScoreImportToXmlParser parser = new ScoreImportToXmlParser();
                    parser.loadScore(score, appContext);
                    parser.parse();
                    parser.writeXml();
                }
                else {
                    mDebugView.setText("pfd or pdfRenderer not instantiated.");
                }
                break;
            }
        }
    }
}
