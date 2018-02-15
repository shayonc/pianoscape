package piano.pianotrainer.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import java.io.File;

import android.os.FileObserver;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import java.io.IOException;
import piano.pianotrainer.R;
import piano.pianotrainer.adapters.ImageAdapter;
import piano.pianotrainer.model.MusicFile;
import piano.pianotrainer.parser.XMLMusicParser;
import java.util.ArrayList;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

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
    private String mxlFilePath;
    private String outputFolder;
    private String xmlFilePath;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static FileObserver observer;

    //Parsed Xml

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        filename = GetFirstFilename();
        context = getApplicationContext();

        // initialize paths
        this.directoryPath = getSdCardPath() + ROOT_FOLDER;
//        this.mxlFilePath = getSdCardPath() + ROOT_FOLDER + File.separator + filename + ".mxl";
//        this.outputFolder = getSdCardPath() + ROOT_FOLDER + File.separator + OUTPUT_FOLDER;
        this.xmlFilePath = getSdCardPath() + ROOT_FOLDER + File.separator + OUTPUT_FOLDER + File.separator;

        loadMusicFileList(); // load music file list

        gridview = findViewById(R.id.gridview);
        imageAdapter = new ImageAdapter(this, musicFileList);
        gridview.setAdapter(imageAdapter);

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // TODO: temp fix
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
        observer = new FileObserver(directoryPath) {
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
    }
    public void loadMusicFileList() {
        try {
            File file = new File(directoryPath);
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

    public String[] GetMxlFiles(){
        try {
            if (isExternalStorageWritable()) {
                verifyStoragePermissions(HomeActivity.this);
                int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                xmlparser = new XMLMusicParser(filename, ROOT_FOLDER, OUTPUT_FOLDER);
                //setup dropdown
                String mxlItems[] = xmlparser.getMxlFiles().toArray(new String[0]);
                return mxlItems;
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
}

