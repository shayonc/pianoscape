package piano.pianotrainer.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import piano.pianotrainer.model.Note;
import piano.pianotrainer.R;
import piano.pianotrainer.parser.XMLMusicParser;
import piano.pianotrainer.fragments.ComparisonSetup;

/**
 * Created by SL-1 on 05/02/2018.
 */

public class PreviewDebugActivity extends AppCompatActivity {
    private static final String SUMMARY_PREFS = "SummaryPrefs";
    SharedPreferences sharedpreferences;

    private Context context;
    private XMLMusicParser xmlparser;
    private ComparisonSetup comparison;
    private static final String OUTPUT_FOLDER = "XMLFiles";
    private static final String ROOT_FOLDER = "Piano";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_debug);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String filename = getIntent().getStringExtra("filename");

        // get shared preferences
        sharedpreferences = getSharedPreferences(SUMMARY_PREFS, MODE_PRIVATE);

        TextView previewText = (TextView) findViewById(R.id.notesPreviewText);
        String summaryMessage = "";
        try {
            xmlparser = new XMLMusicParser(filename, ROOT_FOLDER, OUTPUT_FOLDER);
        } catch (IOException e) {
            // Everything will fail
        }
        xmlparser.parseMXL(); // parse the .mxl file
        List<Note> parsedNotes = xmlparser.parseXML(); // parse the .xml file
        comparison = new ComparisonSetup();
        String toPrint = "";
        try {
            comparison.SyncNotes(parsedNotes);
            toPrint = comparison.DebugPrintSync();
        } catch (IndexOutOfBoundsException e) {
            toPrint = "MusicXML is formatted in a way that has its" +
                    " notes exceeding the measure divisions.\n" +
                    "This is likely because the the song is not meant" +
                    " for Piano or has wrong measure line placements.";
        }

        previewText.setMovementMethod(new ScrollingMovementMethod());
        previewText.setText(toPrint);
        previewText.setTextSize(16);
    }

    @Override
    public void onBackPressed() {
        Intent intentHome = new Intent(this , HomeActivity.class);
        PreviewDebugActivity.this.startActivity(intentHome);
        finish();
    }
}
