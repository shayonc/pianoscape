package piano.pianotrainer.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import java.io.File;
import android.os.FileObserver;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;

import java.io.IOException;
import piano.pianotrainer.R;
import piano.pianotrainer.adapters.ImageAdapter;
import piano.pianotrainer.model.MusicFile;
import piano.pianotrainer.parser.XMLMusicParser;
import java.util.ArrayList;
import java.util.Date;
import piano.pianotrainer.scoreImport.PDFHelper;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int PICKFILE_RESULT_CODE = 123;
    private static final int SCORE_IMPORT_RESULT_CODE = 1;

    static{
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG,"OpenCV not loaded");
        }
        else{
            Log.d(TAG,"OpenCV loaded");
        }
    }
    private String m_Text = "";
    private Context context;
    private XMLMusicParser xmlparser;
    private String filename = "";
    private static final String OUTPUT_FOLDER = "XMLFiles";
    private static final String ROOT_FOLDER = "Piano";
    private GridView gridview;
    private ArrayList<MusicFile> musicFileList = new ArrayList<>();
    private ImageAdapter imageAdapter;
    private SwipeRefreshLayout swiperefresh;

    // Variables for helping with evaluation
    private String directoryPath;
    private String xmlFilePath;
    private TextView mTextMessage;


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static FileObserver observer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        filename = GetFirstFilename();



        context = getApplicationContext();

        // initialize paths
        this.directoryPath = getSdCardPath() + ROOT_FOLDER;
        this.xmlFilePath = getSdCardPath() + ROOT_FOLDER + File.separator + OUTPUT_FOLDER + File.separator;
        //new sdk runtime permissions\

        if (isExternalStorageWritable()) {
            verifyStoragePermissions(HomeActivity.this);
            int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
                    Manifest.permission.WRITE_CALENDAR);
        }
        else{
            Log.d(TAG, "isExternalStorageWritable() returned false");
        }

//        Button button = (Button) findViewById(R.id.button);
//        button.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//
//                try {
//                    if (isExternalStorageWritable()) {
//                        verifyStoragePermissions(HomeActivity.this);
//                        int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
//                                Manifest.permission.WRITE_CALENDAR);
//
//                        xmlparser = new XMLMusicParser(filename, OUTPUT_FOLDER);
//                        xmlparser.parseMXL(); // parse the .mxl file
//                        xmlparser.parseXML(); // parse the .xml file
//
//                    }
//                    else  {
//                        CharSequence text = "External storage not available for read and write.";
//                        int duration = Toast.LENGTH_SHORT;
//                        Toast toast = Toast.makeText(context, text, duration);
//                        toast.show();
//                    }
//                }
//                catch (IOException ie) {
//                    ie.printStackTrace();
//                }
//            }
//        });

        loadMusicFileList(); // load music file list

        gridview = findViewById(R.id.gridview);
        imageAdapter = new ImageAdapter(this, musicFileList);
        gridview.setAdapter(imageAdapter);

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                MusicFile selectedItem = musicFileList.get(position);
                openMusicOptions(selectedItem.getFilename(), xmlFilePath); // pop up dialog
            }
        });

        swiperefresh = findViewById(R.id.swiperefresh);
        swiperefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshGrid();
                    }
                }
        );

        // check for file changes
        observer = new FileObserver(xmlFilePath) {
            @Override
            public void onEvent(int event, String file) {

                if (event == CREATE || event == DELETE || event == MODIFY || event == MOVED_TO) {
                    // refresh if activity
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshGrid();
                        }
                    });
                }
            }
        };
        observer.startWatching();

        FloatingActionButton importMxlFab = findViewById(R.id.importMxlFab);
        importMxlFab.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
//            try {
//                PackageManager pm = context.getPackageManager();
//                boolean installed = isPackageInstalled("com.asus.filemanager", pm);
//                Intent LaunchIntent;
//                if (installed) {
//                    LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.asus.filemanager");
//
//                } else {
//                    LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.estrongs.android.pop");
//                }
//                startActivity(LaunchIntent);
//                Toast toast;
//                toast = Toast.makeText(getApplicationContext(), R.string.folder_info ,Toast.LENGTH_LONG);
//                toast.show();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
                try {
                    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                    chooseFile.setType("application/pdf");
                    chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                    startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

//        Button buttonImport = (Button) findViewById(R.id.button_import);
//        buttonImport.setOnClickListener(new View.OnClickListener() {
//            public void onClick(final View v) {
//
//                try {
//                    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
//                    chooseFile.setType("application/pdf");
//                    chooseFile = Intent.createChooser(chooseFile, "Choose a file");
//                    startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
////                    AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
////                    builder.setTitle("Enter filename ex. mario_starman");
////
////// Set up the input
////                    final EditText input = new EditText(HomeActivity.this);
////// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
////                    input.setInputType(InputType.TYPE_CLASS_TEXT);
////                    builder.setView(input);
////
////// Set up the buttons
////                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
////                        @Override
////                        public void onClick(DialogInterface dialog, int which) {
////                            m_Text = input.getText().toString();
////                            importMusicScore(v, m_Text);
////
////                        }
////                    });
////                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
////                        @Override
////                        public void onClick(DialogInterface dialog, int which) {
////                            dialog.cancel();
////                        }
////                    });
////
////                    builder.show();
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }
    public void loadMusicFileList() {
        try {
            File file = new File(xmlFilePath);
            //if Piano folder doesn't exist then create one
            if (!file.exists()) {
                file.mkdir();
            }
            //get list of files, no recursive
            File[] list = file.listFiles();
            for (File f: list){
                String name = f.getName();
                if (name.endsWith(".mxl") || name.endsWith(".xml")) {
                    MusicFile musicFile = new MusicFile();
                    name = name.substring(0, name.lastIndexOf("."));
                    musicFile.setFilename(name);

                    // get modified date of xml
                    File xmlFile = new File(xmlFilePath + name + ".xml");
                    if (xmlFile.exists()) {
                        Date date = new Date(xmlFile.lastModified());
                        musicFile.setDateModified(date);
                    }
                    musicFile.setThumbnail(R.drawable.music_sheet);
                    //add to list
                    musicFileList.add(musicFile);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openMusicOptions(String filename, String xmlFilePath) {
        DialogFragment dialogFragment = new MusicDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("filename", filename);
        bundle.putString("xmlFilePath", xmlFilePath);
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(), "musicOptions");
    }

    public String GetFirstFilename() {
        try {
            if (isExternalStorageWritable()) {
                verifyStoragePermissions(HomeActivity.this);
                int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                xmlparser = new XMLMusicParser(filename, ROOT_FOLDER, OUTPUT_FOLDER);
                //setup dropdown
                String mxlItems[] = xmlparser.getMxlFiles().toArray(new String[0]);
                if (mxlItems.length > 0) {
                    filename = mxlItems[0];
                }
                return filename;
            }
            else  {
                CharSequence text = "External storage not available for read and write.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }
        return null;

    }

    /** Called when the user taps the Send button */
    public void importMusicScore(String path, String filename) {
        Intent intent = new Intent(this, MusicScoreImportActivity.class);
        intent.putExtra("filename", filename);
        intent.putExtra("path", path);
        startActivityForResult(intent, SCORE_IMPORT_RESULT_CODE);
//        startActivity(intent);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }

    public void refreshGrid() {
        musicFileList.clear();
        loadMusicFileList();
        imageAdapter.notifyDataSetChanged();
        gridview.setAdapter(imageAdapter);
        swiperefresh.setRefreshing(false);
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

    @Override
    public void onBackPressed() {
        finish();
    }

    private boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKFILE_RESULT_CODE && resultCode == Activity.RESULT_OK){
            Uri content_describer = data.getData();
            File myFile = new File(content_describer.getPath());
            String src = myFile.getPath();
//            String filename = content_describer.getLastPathSegment();
            importMusicScore(content_describer.toString(), myFile.getName());

        }
        else if (requestCode == SCORE_IMPORT_RESULT_CODE) {
            if(resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                Snackbar popup = Snackbar.make(findViewById(android.R.id.content), result, Snackbar.LENGTH_LONG);
                popup.show();
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                Snackbar popup = Snackbar.make(findViewById(android.R.id.content), R.string.score_cancelled, Snackbar.LENGTH_LONG);
                popup.show();
            }
        }
    }
}
