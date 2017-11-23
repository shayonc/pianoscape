package piano.pianotrainer.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import java.io.File;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;

import java.io.Console;
import java.io.IOException;

import piano.pianotrainer.R;
import piano.pianotrainer.adapters.ImageAdapter;
import piano.pianotrainer.db.DBHelper;
import piano.pianotrainer.parser.XMLMusicParser;
import piano.pianotrainer.fragments.ComparisonSetup;

//Temporary Imports
import piano.pianotrainer.model.Note;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    private Context context;
    private XMLMusicParser xmlparser;
    private XMLMusicParser rightxmlparser;
    private XMLMusicParser wrongxmlparser;
    private ComparisonSetup comparison;
    private ComparisonSetup rightComparison;
    private ComparisonSetup wrongComparison;
    private String filename = "";
    private static final String OUTPUT_FOLDER = "XMLFiles";
    private static final String ROOT_FOLDER = "Piano";
    private static final String WRONG_NOTES_FOLDER = "WrongPianoNotes";
    private ArrayList<List<Note>> correctSyncedNotes = new ArrayList<List<Note>>();
    private ArrayList<List<Note>> wrongSyncedNotes = new ArrayList<List<Note>>();

    // Variables for helping with evaluation
    private final String state = "";
    private int evalPosition;
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

    //Parsed Xml

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mTextMessage = (TextView) findViewById(R.id.message);

        final TextView buttonResult = (TextView)findViewById(R.id.simpleTextView);
        buttonResult.setText("Button not yet clicked");
        buttonResult.setMovementMethod(new ScrollingMovementMethod());
        filename = GetFirstFilename();
        context = getApplicationContext();
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase(); // get writable
        //Setup file Dropdown
        Spinner mxlDropdown = (Spinner)findViewById(R.id.mxlFileSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, GetMxlFiles());
        mxlDropdown.setAdapter(adapter);
        mxlDropdown.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                filename = parent.getItemAtPosition(pos).toString();
                Toast.makeText(parent.getContext(),parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
            }
            public void onNothingSelected(AdapterView<?> parent) {
                // Do Nothing
            }
        });

        Button printSyncButton = (Button) findViewById(R.id.printSyncButton);
        printSyncButton.setBackgroundColor(Color.rgb(0, 91, 170));
        printSyncButton.setTextColor(Color.WHITE);
        printSyncButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (isExternalStorageWritable()) {
                        verifyStoragePermissions(HomeActivity.this);
                        int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        xmlparser = new XMLMusicParser(filename, ROOT_FOLDER, OUTPUT_FOLDER);
                        xmlparser.parseMXL(); // parse the .mxl file
                        List<Note> parsedNotes = xmlparser.parseXML(); // parse the .xml file
                        comparison = new ComparisonSetup();
                        comparison.SyncNotes(parsedNotes);
                        String toPrint = comparison.DebugPrintSync();
                        parsedNotes.clear();
                        buttonResult.setText(toPrint);

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
            }
        });
//        Button startAtPaceEvalButton = (Button) findViewById(R.id.startAtPaceEvalButton);
//        startAtPaceEvalButton.setBackgroundColor(Color.rgb(0, 91, 170));
//        startAtPaceEvalButton.setTextColor(Color.WHITE);
//        startAtPaceEvalButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                try {
//                    if (isExternalStorageWritable()) {
//                        verifyStoragePermissions(HomeActivity.this);
//                        int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
//                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                        xmlparser = new XMLMusicParser(filename, ROOT_FOLDER, OUTPUT_FOLDER);
//                        xmlparser.parseMXL(); // parse the .mxl file
//                        List<Note> atPaceParsedNotes = xmlparser.parseXML(); // parse the .xml file
//                        comparison = new ComparisonSetup();
//                        comparison.SyncNotes(atPaceParsedNotes);
//                        atPaceParsedNotes.clear();
//                        buttonResult.setText("");
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

        Button compareNotes = (Button) findViewById(R.id.compareNotesButton);
        compareNotes.setBackgroundColor(Color.rgb(0, 91, 170));
        compareNotes.setTextColor(Color.WHITE);
        compareNotes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (isExternalStorageWritable()) {
                        correctSyncedNotes.clear();
                        wrongSyncedNotes.clear();

                        verifyStoragePermissions(HomeActivity.this);
                        int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        rightxmlparser = null;
                        rightxmlparser = new XMLMusicParser(filename, ROOT_FOLDER, OUTPUT_FOLDER);
                        rightxmlparser.parseMXL(); // parse the .mxl file
                        /**
                        *   if called again, the value of the reference will keep adding.
                         *   Needs to be cleared at end or else will keep adding to same mem space.
                        * */
                        List<Note> correctParsedNotes = xmlparser.parseXML(); // parse the .xml file
                        Log.d("HomeActivity1", String.valueOf(correctParsedNotes.size()));
                        rightComparison = new ComparisonSetup();
                        correctSyncedNotes = rightComparison.SyncNotes(correctParsedNotes);
                        correctParsedNotes.clear();

                        verifyStoragePermissions(HomeActivity.this);
                        int permissionCheck2 = ContextCompat.checkSelfPermission(HomeActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        wrongxmlparser = null;
                        wrongxmlparser = new XMLMusicParser(filename, WRONG_NOTES_FOLDER, OUTPUT_FOLDER);
                        wrongxmlparser.parseMXL(); // parse the .mxl file
                        List<Note> WrongParsedNotes = wrongxmlparser.parseXML(); // parse the .xml file
                        Log.d("HomeActivity2", String.valueOf(WrongParsedNotes.size()));
                        wrongComparison = new ComparisonSetup();
                        wrongSyncedNotes = wrongComparison.SyncNotes(WrongParsedNotes);
                        WrongParsedNotes.clear();
                        String WrongtoPrint = wrongComparison.CompareDebugPrintSync(correctSyncedNotes, wrongSyncedNotes);
                        buttonResult.setText(WrongtoPrint);

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
            }
        });
        /* TODO : List of files
         * put mxl files into array of objects
         * send mxl files and get (title, last updated) to ImageView to display
         * OnClick of each image show popup
        * */
        // initialize paths
        this.directoryPath = getSdCardPath() + ROOT_FOLDER;
//        this.mxlFilePath = getSdCardPath() + ROOT_FOLDER + File.separator + filename + ".mxl";
//        this.outputFolder = getSdCardPath() + ROOT_FOLDER + File.separator + OUTPUT_FOLDER;
//        this.xmlFilePath = getSdCardPath() + ROOT_FOLDER + File.separator + OUTPUT_FOLDER + File.separator + filename + ".xml";
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
                    Log.d("HomeActivity", name);
                    Date date = new Date(f.lastModified());
                    Log.d("HomeActivity", date.toString());
                }
            }

            GridView gridview = (GridView) findViewById(R.id.gridview);
            gridview.setAdapter(new ImageAdapter(this));

            gridview.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v,
                                        int position, long id) {
                    Toast.makeText(HomeActivity.this, "" + position,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }

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

