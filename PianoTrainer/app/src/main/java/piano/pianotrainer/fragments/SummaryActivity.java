package piano.pianotrainer.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import piano.pianotrainer.R;

public class SummaryActivity extends AppCompatActivity {
    private static final String SUMMARY_PREFS = "SummaryPrefs";
    SharedPreferences sharedpreferences;
    private int correctNotes = 0;
    private int totalNotes = 0;
    private double accuracyRate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String filename = getIntent().getStringExtra("filename");

        // get shared preferences
        sharedpreferences = getSharedPreferences(SUMMARY_PREFS, MODE_PRIVATE);
        correctNotes = sharedpreferences.getInt(filename + "incorrectNotes", 0);
        totalNotes = sharedpreferences.getInt(filename + "totalNotes", 0) + correctNotes;

        TextView summaryTextView = (TextView) findViewById(R.id.summaryText);
        String summaryMessage = "";

            accuracyRate = ((double) correctNotes / (double) totalNotes) *100;
            summaryMessage = "Music File: " + filename +
                    System.getProperty("line.separator") +
                    "Notes correctly played: " + correctNotes + "/" + totalNotes +
                    System.getProperty("line.separator") +
                    "Accuracy: " + String.format("%.2f", accuracyRate) + "%";
        summaryTextView.setText(summaryMessage);
        summaryTextView.setTextSize(21);
    }
}
