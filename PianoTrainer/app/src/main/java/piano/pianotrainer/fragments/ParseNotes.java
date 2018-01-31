package piano.pianotrainer.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import piano.pianotrainer.model.Note;
import piano.pianotrainer.parser.XMLMusicParser;

/**
 * Created by Matthew on 1/26/2018.
 */

public class ParseNotes {
    private ArrayList<List<Note>> correctSyncedNotes = new ArrayList<List<Note>>();
    private ArrayList<List<Note>> wrongSyncedNotes = new ArrayList<List<Note>>();
    private static final String ROOT_FOLDER = "Piano";
    private static final String WRONG_NOTES_FOLDER = "WrongPianoNotes";
    private static final String OUTPUT_FOLDER = "XMLFiles";
    private XMLMusicParser xmlparser;
    private XMLMusicParser rightxmlparser;
    private XMLMusicParser wrongxmlparser;
    private ComparisonSetup rightComparison;
    private ComparisonSetup wrongComparison;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public ArrayList<List<Note>> compareWrongNotes(String filename, Context context, Activity activity) {
        try {
            if (isExternalStorageWritable()) {
                correctSyncedNotes.clear();
                wrongSyncedNotes.clear();

                verifyStoragePermissions(activity);
                int permissionCheck = ContextCompat.checkSelfPermission(activity,
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

                verifyStoragePermissions(activity);
                int permissionCheck2 = ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                wrongxmlparser = null;
                wrongxmlparser = new XMLMusicParser(filename, WRONG_NOTES_FOLDER, OUTPUT_FOLDER);
                wrongxmlparser.parseMXL(); // parse the .mxl file
                List<Note> WrongParsedNotes = wrongxmlparser.parseXML(); // parse the .xml file
                Log.d("HomeActivity2", String.valueOf(WrongParsedNotes.size()));
                wrongComparison = new ComparisonSetup();
                wrongSyncedNotes = wrongComparison.SyncNotes(WrongParsedNotes);
                WrongParsedNotes.clear();
//                String WrongtoPrint = wrongComparison.CompareDebugPrintSync(correctSyncedNotes, wrongSyncedNotes);
//                buttonResult.setText(WrongtoPrint);
                return wrongSyncedNotes;
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
        return wrongSyncedNotes;
    }
    
    public List<Note> parseTheNotes(String filename, Context context, Activity activity) {
        Log.d("ParseNotes.java", filename);
        List<Note> parsedNotes = new ArrayList<>();
        ComparisonSetup comparison;

        try {
            if (isExternalStorageWritable()) {
                verifyStoragePermissions(activity);
                int permissionCheck = ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                xmlparser = new XMLMusicParser(filename, ROOT_FOLDER, OUTPUT_FOLDER);
                xmlparser.parseMXL(); // parse the .mxl file
                parsedNotes = xmlparser.parseXML(); // parse the .xml file
                comparison = new ComparisonSetup();
                comparison.SyncNotes(parsedNotes);
                return parsedNotes;
//                parsedNotes.clear();
//                buttonResult.setText(toPrint);

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
        return parsedNotes;
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
