/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package piano.pianotrainer.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import midi.scope.MidiPrinter;
import midi.scope.MidiScope;
import midi.scope.NoteReceiver;
import piano.pianotrainer.R;
import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiOutputPortSelector;
import com.mobileer.miditools.MidiPortWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import midi.scope.LoggingReceiver;
import midi.scope.ScopeLogger;
import piano.pianotrainer.model.Note;

/*
 * Print incoming MIDI messages to the screen.
 */
public class MainActivity extends AppCompatActivity implements ScopeLogger {
    private static final String TAG = "MidiScope";
    private static final String SUMMARY_PREFS = "SummaryPrefs";

    private TextView mLog;
    private ScrollView mScroller;
    private LinkedList<String> logLines = new LinkedList<String>();
    private static final int MAX_LINES = 100;
    private MidiOutputPortSelector mLogSenderSelector;
    private MidiManager mMidiManager;
    private MidiReceiver mNoteReceiver;
    private MidiFramer mConnectFramer;
    private MyDirectReceiver mDirectReceiver;
    private boolean mShowRaw;
    private Context context;
    private List<Note> notesArray = new ArrayList<>();;
    private Lock compLock = new ReentrantLock();
    private int curNote = 170;
    private String filename = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(piano.pianotrainer.R.layout.main);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        filename = getIntent().getStringExtra("filename");

        ParseNotes parseNotes = new ParseNotes();

        notesArray = parseNotes.parseTheNotes(filename, context, MainActivity.this);

        Log.d("MainActivity size: ", Integer.toString(notesArray.size()));

        if(notesArray.size() == 0 ){
            throw new java.lang.RuntimeException("Parsing failed");
        }
        context = getApplicationContext();

        mLog = (TextView) findViewById(R.id.log);
        mScroller = (ScrollView) findViewById(R.id.scroll);

        // Setup MIDI
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        // Receiver that prints the messages.
        mNoteReceiver = new NoteReceiver(this, notesArray, compLock, curNote);

        // Receivers that parses raw data into complete messages.
        mConnectFramer = new MidiFramer(mNoteReceiver);

        // Setup a menu to select an input source.
        mLogSenderSelector = new MidiOutputPortSelector(mMidiManager, this,
                R.id.spinner_senders) {

            @Override
            public void onPortSelected(final MidiPortWrapper wrapper) {
                super.onPortSelected(wrapper);
                if (wrapper != null) {
                    log(MidiPrinter.formatDeviceInfo(wrapper.getDeviceInfo()));
                }
            }
        };

        mDirectReceiver = new MyDirectReceiver();
        mLogSenderSelector.getSender().connect(mDirectReceiver);

        // Tell the virtual device to log its messages here..
        MidiScope.setScopeLogger(this, notesArray, compLock, curNote);
    }

    @Override

    public void onDestroy() {
        mLogSenderSelector.onClose();
        // The scope will live on as
        // a service so we need to tell it to stop
        // writing log messages to this Activity.
        MidiScope.setScopeLogger(null, null, null, 0);
        super.onDestroy();
    }

    public void onToggleScreenLock(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        if (checked) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
    public void onToggleShowRaw(View view) {
        mShowRaw = ((CheckBox) view).isChecked();
    }

    public void onClearLog(@SuppressWarnings("unused") View view) {
        logLines.clear();
        logFromUiThread("");
    }

    class MyDirectReceiver extends MidiReceiver {
        @Override
        public void onSend(byte[] data, int offset, int count,
                long timestamp) throws IOException {
            if (mShowRaw) {
                String prefix = String.format("0x%08X, ", timestamp);
                logByteArray(prefix, data, offset, count);
            }
            // Send raw data to be parsed into discrete messages.
            mConnectFramer.send(data, offset, count, timestamp);
        }
    }

    /**
     * @param string
     */
    @Override
    public void log(final String string) {
        runOnUiThread(new Runnable() {
                @Override
            public void run() {
                logFromUiThread(string);
            }
        });
    }

    // Log a message to our TextView.
    // Must run on UI thread.
    private void logFromUiThread(String s) {
        logLines.add(s);
        if (logLines.size() > MAX_LINES) {
            logLines.removeFirst();
        }
        // Render line buffer to one String.
        StringBuilder sb = new StringBuilder();
        for (String line : logLines) {
            sb.append(line).append('\n');
        }
        mLog.setText(sb.toString());
        mScroller.fullScroll(View.FOCUS_DOWN);
    }

    private void logByteArray(String prefix, byte[] value, int offset, int count) {
        StringBuilder builder = new StringBuilder(prefix);
        for (int i = 0; i < count; i++) {
            builder.append(String.format("0x%02X", value[offset + i]));
            if (i != count - 1) {
                builder.append(", ");
            }
        }
        log(builder.toString());
    }

    public void openSummaryPage(int incorrectNotes, int notesCount) {
        Log.d("openSummaryPage", "Should open summary");
        Log.d("openSummaryPage", "incorrect notes: " + incorrectNotes + " totalNotes: " + notesCount);
        Intent intentMain = new Intent(this , SummaryActivity.class);
        intentMain.putExtra("filename", filename);
        SharedPreferences.Editor sharedPreferences = getSharedPreferences(SUMMARY_PREFS, MODE_PRIVATE).edit();

        sharedPreferences.putInt(filename + "incorrectNotes", incorrectNotes);
        sharedPreferences.putInt(filename + "totalNotes", notesCount);
        sharedPreferences.apply();
        MainActivity.this.startActivity(intentMain);
        finish();
    }
}
