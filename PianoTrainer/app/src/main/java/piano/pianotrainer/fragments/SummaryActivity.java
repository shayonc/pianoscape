package piano.pianotrainer.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import piano.pianotrainer.R;

public class SummaryActivity extends AppCompatActivity {
    private static final String SUMMARY_PREFS = "SummaryPrefs";
    SharedPreferences sharedpreferences;
    private int incorrectNotes = 0;
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
        incorrectNotes = sharedpreferences.getInt(filename + "incorrectNotes", 0);
        correctNotes = sharedpreferences.getInt(filename + "totalNotes", 0);
        totalNotes = correctNotes + incorrectNotes;

        TextView summaryTextView = (TextView) findViewById(R.id.summaryText);
        String summaryMessage = "";

            accuracyRate = ((double) correctNotes / (double) totalNotes) *100;
            summaryMessage = getString(R.string.music_file) + filename +
                    System.getProperty("line.separator") +
                    getString(R.string.correct_notes_played) + correctNotes + "/" + totalNotes +
                    System.getProperty("line.separator") +
                    getString(R.string.accuracy_text) + String.format("%.2f", accuracyRate) + "%";
        summaryTextView.setText(summaryMessage);
        summaryTextView.setTextSize(21);
    }

    @Override
    public void onBackPressed() {
        Intent intentHome = new Intent(this , HomeActivity.class);
        SummaryActivity.this.startActivity(intentHome);
        finish();
    }
}
