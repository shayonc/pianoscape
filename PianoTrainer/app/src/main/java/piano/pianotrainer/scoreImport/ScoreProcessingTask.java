package piano.pianotrainer.scoreImport;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.opencv.android.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import piano.pianotrainer.R;
import piano.pianotrainer.fragments.ScoreImportToXmlParser;
import piano.pianotrainer.scoreModels.Accidental;
import piano.pianotrainer.scoreModels.ElementType;
import piano.pianotrainer.scoreModels.Measure;
import piano.pianotrainer.scoreModels.Note;
import piano.pianotrainer.scoreModels.NoteGroup;
import piano.pianotrainer.scoreModels.Pitch;
import piano.pianotrainer.scoreModels.Score;
import piano.pianotrainer.scoreModels.Staff;

/**
 * Created by Shubho on 2018-03-19.
 */

public class ScoreProcessingTask extends AsyncTask<String, Integer, Integer> {
    private ProgressDialog dialog;
    private Activity activity;
    private ScoreProcessor scoreProc;
    private String TAG = "ScoreProcessingTask";

    public ScoreProcessingTask(Activity activity, ProgressDialog dialog) {
        this.dialog = dialog;
        this.activity = activity;
    }

    protected void onProgressUpdate(Integer... progress) {
        dialog.setProgress(progress[0]);
    }

    protected void onPostExecute(Integer result) {
        // do UI work here
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        String message = "";
        if (result == 1) message = "File cannot be imported. PDF is not in an appropriate format.";
        else if (result == 2) message = "File cannot be imported. Grand staffs were not found.";
        else message = "Score converted into Music XML format successfully.";

        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", message);
        activity.setResult(Activity.RESULT_OK ,returnIntent);
        activity.finish();
    }


    public boolean addTrainingImages( String fileDir, int label){
        Resources res = activity.getResources();
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
                    inputstream= activity.getApplicationContext().getAssets().open(fileDir + "/" +fileList[i]);
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


    protected Integer doInBackground(String... args) {
        // do background work here
        String SCORE_NAME = args[0];
        String realPath = args[1];
        String rawName = args[2];
        String IMGNAME = "final_" + SCORE_NAME + ".bmp";

        File file = new File(realPath);
        ParcelFileDescriptor pfd = null;
        PdfRenderer pdfRenderer = null;
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pfd);
        }
        catch(Exception e) {
            return 1;
        }

        publishProgress(1);

        if (pfd != null && pdfRenderer != null) {
            PDFHelper pdfHelper = new PDFHelper(pfd);
            pdfHelper.setCurPage(0);

            //Bitmap curPageBitmap = pdfHelper.toBinImg(pdfHelper.getCurPage().getIndex());
            PdfRenderer.Page curPage = pdfRenderer.openPage(pdfHelper.getCurPage().getIndex());

//            mDebugView.setText(String.format("width: %d, height: %d", curPage.getWidth(), curPage.getHeight()));
            Bitmap bitmap = Bitmap.createBitmap(curPage.getWidth()*4, curPage.getHeight()*4,
                    Bitmap.Config.ARGB_8888);
            curPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            publishProgress(5);

            //image processing
            //loads the Mat object of the image
            // TODO: get from user
            String scoreTitle = "Twinkle Twinkle Little Star";
            scoreProc = new ScoreProcessor(bitmap);
            Score score = new Score(scoreTitle);

            //Threshold so Mat will only have 0s and 255s
            scoreProc.binarize();
            publishProgress(10);
            scoreProc.removeStaffLines();
            publishProgress(15);
            scoreProc.refineStaffLines();
            publishProgress(20);

            boolean grandStaff = scoreProc.isGrandStaff();
            if (!grandStaff) {
                // TODO: return error to user in a dialog
                return 2;
            }

            int numPulses = 0;
            double basicPulse = 0;
            List<List<Rect>> staffObjects = scoreProc.detectObjects();
            publishProgress(40);

            // Generating hard-coded symbols data
//            List<Boolean[]> sonatina_symbols = scoreProc.getSonatinaNoteGroups();
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
            addTrainingImages("training_set/notesteminv", KnnLabels.NOTE_STEM_INV);
            publishProgress(45);

            //Train
            scoreProc.trainKnn();
            publishProgress(50);
            //test all
            scoreProc.testMusicObjects();
            publishProgress(60);

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

            int objCount = 0;
            for (List<Rect> list : staffObjects) {
                objCount += list.size();
            }
            double objProgAmount = 36.0 / objCount;
            double progress = 60.0;
            int intProgress = 60;

            int curLabel;
            Boolean isTreble;
            Measure curMeasure = new Measure();
            Map<Pitch, Accidental> keySigFirstMeasure = new HashMap<Pitch, Accidental>();

            for (int i = 0; i < staffObjects.size(); i++) {
                Staff staff = new Staff(true);
                score.addStaff(staff);
                List<Rect> objects = staffObjects.get(i);

                boolean firstVertBar = false;
//                Boolean[] isNoteGroup2 = sonatina_symbols.get(i);
                Map<Integer, Rect> dotsInStaff = new LinkedHashMap<>();

                for (int j = 0; j < objects.size(); j++) {
                    Rect obj = objects.get(j);
                    isTreble = scoreProc.inTrebleCleff(obj.top, obj.bottom, i);
                    curLabel = knnResults.get(i).get(j);
                    if(!firstVertBar){
                        if(curLabel == KnnLabels.BAR){
                            firstVertBar = true;
                        }
                    }
                    else{
                        // TODO: add symbol detection here
                        if (curLabel == KnnLabels.BAR) {
                            if (!firstVertBar) {
                                firstVertBar = true;
                            }
                            else {
                                Log.d(TAG, String.format("hit new bar on staff %d measure %d",i, staff.getNumMeasures()));
                                //dot integration and handling accidental/ties
                                curMeasure.checkNeighbours();

                                Map<Pitch, Integer> keySigPitchScaleMap = scoreProc.getPitchScaleFromKeySig(curMeasure.getKeySigCenters(),
                                        curMeasure.getKeySigIsTreble(), i);
                                Map<Pitch, Accidental> keySigsPitchAccMap = new HashMap<Pitch, Accidental>();
                                int accCounter = 0;
                                for(Pitch keyPitch : keySigPitchScaleMap.keySet()){
                                    keySigsPitchAccMap.put(keyPitch, curMeasure.getKeySigPitchAccList().get(accCounter));
                                    accCounter++;
                                }
                                curMeasure.setKeySigPitch(keySigsPitchAccMap);
                                if(staff.getNumMeasures() == 0){
                                    keySigFirstMeasure = curMeasure.keySigs;
                                }
                                else{
                                    curMeasure.keySigs = keySigFirstMeasure;
                                }
                                Log.d(TAG, String.format("Staff %d Measure %d with info %s",i,
                                        staff.getNumMeasures(), curMeasure.info()));
                                staff.addMeasure(curMeasure);
                                curMeasure = new Measure();
                            }
                        }

                        else if(curLabel == KnnLabels.G_CLEF){
                            Log.d(TAG, "Frag found treble clef");
                            curMeasure.addClef(ElementType.TrebleClef, obj);
                        }
                        //time sig
                        else if(SymbolMapper.isTimeSig(curLabel) && i == 0 && staff.getNumMeasures() == 0){
                            curMeasure.setTimeSig(SymbolMapper.getUpperTimeSig(curLabel), SymbolMapper.getLowerTimeSig(curLabel),
                                    isTreble, obj);
                        }
                        else if (scoreProc.isNoteGroup(obj)){
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
//                                        cnvs.drawText(s, staffObjects.get(i).get(j).left, staffObjects.get(i).get(j).bottom, paintTxt);
                            }

                        }
                        else {
                            // Ekteshaf: place your knn testing for each object here
                            //general symbols not usually confused with clefs
                            if(curLabel == KnnLabels.F_CLEF){
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
                            //other symbols
                            else if(curLabel == KnnLabels.DOT){
                                curMeasure.addToClefLists(isTreble, obj, ElementType.Dot);
                                //save dot index for dot integration at the end
                                dotsInStaff.put(j, obj);
                                Log.d(TAG, String.format("Dot added at pos loop ij %d,%d",i,j));
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
                            else if(curLabel == KnnLabels.TIE){
                                curMeasure.addToClefLists(isTreble, obj, ElementType.Tie);
                            }
                            //TODO: wholenote2
                        }
                    }

                    progress += objProgAmount;
                    if ((int)progress > intProgress) {
                        intProgress = (int)progress;
                    }
                    if (intProgress >= 96) {
                        intProgress = 96;
                    }
                    publishProgress(intProgress);
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

//            for(int i = 0; i < staffObjects.size(); i++){
//                for(int j = 0; j < staffObjects.get(i).size(); j++){
//                    if(knnResults.get(i).get(j)/10 == 0){
//                        cnvs.drawRect(staffObjects.get(i).get(j), paintR);
//                    }
//                    if(knnResults.get(i).get(j)/10 == 1){
//                        cnvs.drawRect(staffObjects.get(i).get(j), paintB);
//                    }
//                    if(knnResults.get(i).get(j)/10 == 2){
//                        cnvs.drawRect(staffObjects.get(i).get(j), paintG);
//                    }
//                    if(knnResults.get(i).get(j)/10 == 3){
//                        cnvs.drawRect(staffObjects.get(i).get(j), paintM);
//                    }
//                    if(knnResults.get(i).get(j)/10 == 4){
//                        cnvs.drawRect(staffObjects.get(i).get(j), paintC);
//                    }
//                    cnvs.drawText(knnResults.get(i).get(j).toString(),
//                            staffObjects.get(i).get(j).left, staffObjects.get(i).get(j).top, paintTxt);
//                }
//            }

            for (Map.Entry<Rect, List<String>> entry : canvasDrawings.entrySet()) {
                Rect rect = entry.getKey();
                List<String> s = entry.getValue();
                cnvs.drawText(s.get(0), rect.left, rect.top, paintTxt);
                cnvs.drawText(s.get(1), rect.left, rect.bottom, paintTxt);
            }

            ImageUtils.saveImageToExternal(finalBmp, IMGNAME);
            scoreProc.exportRects(this.activity);

            ScoreImportToXmlParser parser = new ScoreImportToXmlParser(rawName);
            parser.loadScore(score, activity.getApplicationContext());
            parser.parse();
            publishProgress(98);
            parser.writeXml();
            publishProgress(100);
            return 0;
        }
        else {
            return 1;
        }
    }
}
