package piano.pianotrainer.fragments;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-10.
 */

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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
//import java.net.URI;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.*;

import piano.pianotrainer.R;
import piano.pianotrainer.scoreImport.ImageUtils;
import piano.pianotrainer.scoreImport.KnnLabels;
import piano.pianotrainer.scoreImport.PDFHelper;
import piano.pianotrainer.scoreImport.ScoreProcessingTask;
import piano.pianotrainer.scoreImport.ScoreProcessor;
import piano.pianotrainer.scoreImport.SymbolMapper;

import piano.pianotrainer.scoreModels.Accidental;
import piano.pianotrainer.scoreModels.ElementType;
import piano.pianotrainer.scoreModels.Measure;
import piano.pianotrainer.scoreModels.Note;
import piano.pianotrainer.scoreModels.NoteGroup;

import piano.pianotrainer.scoreModels.Pitch;
import piano.pianotrainer.scoreModels.Score;
import piano.pianotrainer.scoreModels.Staff;
import piano.pianotrainer.fragments.ScoreImportToXmlParser;


public class MusicScoreViewerFragment extends Fragment implements View.OnClickListener{
    /**
     * Key string for saving the state of current page index.
     */

    static final String TAG = "ScoreViewer";

    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";


    private static final String SCORE_NAME = "twelve_pieces";

//    private static final String FILENAME = SCORE_NAME + ".pdf";

    private static final String IMGNAME = "final_" + SCORE_NAME + ".bmp";

    private static final String TRAINING = "training_set";

    private ParcelFileDescriptor mFileDescriptor;
    private ImageView mImageView;
    private PDFHelper mPdfHelper;
    //used for loading saved page index from save states before we re-init PDF Helper object
    private int mPageIndexSaved;
    private String filename;
    private String rawName;
    private String path;
    private String realPath;
    Context context;

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
    private ProgressDialog dialog;

    public MusicScoreViewerFragment(String path, String filename) {
        this.filename = filename;
        this.path = path;
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
//        mDebugView = (TextView) view.findViewById(R.id.debug_view);
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

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Sets up a {@link android.graphics.pdf.PdfRenderer} and related resources.
     */
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        try {
            Uri uri= Uri.parse(path);
            realPath = getPath(getActivity(), uri);
            File file = new File(realPath);
            String fullFilename = realPath.substring(realPath.lastIndexOf("/")+1);
            this.rawName = fullFilename.substring(0, fullFilename.lastIndexOf('.'));

//        if (!file.exists()) {
//            // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
//            // the cache directory.
//            InputStream asset = getContentResolver().openInputStream(uri);
//            FileOutputStream output = new FileOutputStream(file);
//            final byte[] buffer = new byte[1024];
//            int size;
//            while ((size = asset.read(buffer)) != -1) {
//                output.write(buffer, 0, size);
//            }
//            asset.close();
//            output.close();
//        }

            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            // This is the PdfRenderer we use to render the PDF.
            if (mFileDescriptor != null) {
                //EC mPdfRenderer = new PdfRenderer(mFileDescriptor);
                mPdfHelper = new PDFHelper(mFileDescriptor);
                mPdfHelper.setCurPage(mPageIndexSaved);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void showObject(int index) {
        Bitmap bmpObject = scoreProc.getStaffObject(0, index);
        mImageView.setImageBitmap(bmpObject);
//        mDebugView.setText(String.format("Object -> Width: %d, Height: %d", bmpObject.getWidth(), bmpObject.getHeight()));
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
                dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Importing Score");
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setIndeterminate(false);
                dialog.setProgress(0);
                dialog.setMax(100);
                dialog.show();

                ScoreProcessingTask task = new ScoreProcessingTask(getActivity(), dialog);
                task.execute(SCORE_NAME, realPath, rawName);
            }
        }
    }
}
