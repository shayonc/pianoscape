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

package midi.scope;

import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import piano.pianotrainer.fragments.MusicDialogFragment;
import piano.pianotrainer.model.ChordComparator;
import piano.pianotrainer.model.Note;
import uk.co.dolphin_com.seescoreandroid.MainActivity;
import uk.co.dolphin_com.seescoreandroid.Player;

/**
 * Convert incoming MIDI messages to a string and write them to a ScopeLogger.
 * Assume that messages have been aligned using a MidiFramer.
 */
public class NoteReceiver extends MidiReceiver {
    public static final String TAG = "MidiScope";
    private static final long NANOS_PER_MILLISECOND = 1000000L;
    private static final long NANOS_PER_SECOND = NANOS_PER_MILLISECOND * 1000L;
    private long mStartTime;
    private ScopeLogger mLogger;
    private long mLastTimeStamp = 0;
    private ArrayList<List<uk.co.dolphin_com.sscore.playdata.Note>> notes;
    private Lock compLock;
    private int curNote;
    private int incorrectCount;
    private int restCount = 0;
    private ChordComparator chordComparator;
    private boolean isChord = false;
    private boolean lastNote = false;
    private boolean songOver = false;
    private Player player;
    private int correctNotes=0;
    private int totalNotes=0;
    private int barIndex = 0;
    //private Set<Note> tieOnNotes = new HashSet<>();

    public NoteReceiver(ScopeLogger logger, ArrayList<List<uk.co.dolphin_com.sscore.playdata.Note>> notes, Lock compLock, int curNote, Player player) {
        mStartTime = System.nanoTime();
        mLogger = logger;
        this.notes = notes;
        this.compLock = compLock;
        this.curNote = curNote;
        this.player = player;
    }

    /*
     * @see android.media.midi.MidiReceiver#onSend(byte[], int, int, long)
     */
    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        Note note = new Note();
        int decode = data[0] & 0xF0;
        if(decode != 0x90 && decode != 0x80){
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (timestamp == 0) {
            sb.append(String.format("-----0----: "));
        } else {
            long monoTime = timestamp - mStartTime;
            long delayTimeNanos = timestamp - System.nanoTime();
            int delayTimeMillis = (int)(delayTimeNanos / NANOS_PER_MILLISECOND);
            double seconds = (double) monoTime / NANOS_PER_SECOND;
            // Mark timestamps that are out of order.
            sb.append((timestamp < mLastTimeStamp) ? "*" : " ");
            mLastTimeStamp = timestamp;
            sb.append(String.format("%10.3f (%2d): ", seconds, delayTimeMillis));
        }
        sb.append("Actual message: " + MidiPrinter.formatBytes(data, offset, count));

        sb.append(" Parsed: " + MidiPrinter.formatMessage(data, offset, count, timestamp, note));

        compLock.lock();

        /*for(List<uk.co.dolphin_com.sscore.playdata.Note> noteList: notes){
            try{
                Thread.sleep(1000);
            }catch(Exception e){

            }
            player.moveCursor(noteList);
        }*/

        Log.d("NoteReciever", "We made it here");
        //skip rests here, also skip on tie stop
        //ToDo add in way to alow user to play tieOff if they want
       /*while(notes.get(curNote).size() == 0 || isTieSkip(notes.get(curNote).get(0))){
            if(notes.get(curNote).size() == 0){
                Log.d("NoteReciever: ", "Skipped because of rest  curNote: " + curNote + " ");
            }else{
                Log.d("NoteReciever: ", "Skipped because of Tie  curNote: " + curNote + " ");
            }
            curNoteAdd(1);
            restCount++;
        }*/
        boolean skipping = true;
        while(skipping && !lastNote) {
            if (notes.get(curNote).size() <= 2) {
                if(notes.get(curNote).size() == 2 && notes.get(curNote).get(0).rest && notes.get(curNote).get(1).rest){
                    curNoteAdd(1);
                }
                else if(notes.get(curNote).get(0).rest){
                    curNoteAdd(1);
                }
                else{
                    skipping = false;
                }
            }
            else{
                skipping = false;
            }
        }

        //Compare notes here
        if(!note.getNoteOn() && isChord){
            correctNotes += chordComparator.getCorrectCount();
            //released cord too early, clear Chord Comparator
            chordComparator.clearCorrect();
            sb.append("A key was released before chord completed, chord has been reset\n");
            chordComparator.displayExpected();
        }

        if (note.getNoteOn() && !songOver) {
            totalNotes++;
            //chord detection
            if(!isChord && notes.get(curNote).size() > 1){
                chordComparator = new ChordComparator(notes.get(curNote));
                isChord = true;
                sb.append("chord detected!!!!\n");
            }
            //ToDo step compare will not work for flats, midi is only converted to sharp

            if(!isChord) {
                uk.co.dolphin_com.sscore.playdata.Note expNote = notes.get(curNote).get(0);
                if (expNote.midiPitch == note.getMidiData()) {
                    //Note is correct
                    sb.append("\nNote " + curNote + " was correct\n");
                    curNoteAdd(1);
                    //add to tie list
                    /*if(expNote.isTieStart()){
                        tieOnNotes.add(expNote);
                    }*/

                } else {
                    sb.append("\nNote " + curNote + " was incorrect\n");
                    sb.append("Expected octave " + expNote.midiPitch / 12 + " and step " + expNote.midiPitch % 12 + "\n");
                    sb.append("Given octave " + note.getOctave() + " and step " + note.getStep() + "\n");
                    //incorrectCount++;
                }
            }
            else {
                if(chordComparator.compareNotes(note) == 0){
                    isChord = false;
                    curNoteAdd(chordComparator.getNoteCount());
                    sb.append("chord completed successfully\n");
                }
            }
        }

        compLock.unlock();

        if(curNote >= notes.size()){
            Log.d("NoteReceiverEnd", "Song has been played");
        }

        String text = sb.toString();
        //String text = "event";
        mLogger.log(text);
        Log.i(TAG, text);
    }
    private void curNoteAdd(int n){
        curNote++;
        correctNotes+= n;

        if(curNote == notes.size() - 1){
            Log.d("NoteReceiverEnd", "Last note of song");
            lastNote = true;
        }
        else if(curNote == 100 || curNote >= notes.size()/* notes.size()*/){
            //end song
            songOver = true;
            Log.d("NoteReceiverEnd", "Should launch summary activity");
            ((MainActivity)mLogger).openSummaryPage(incorrectCount, notes.size() - restCount);
        }
        if(!songOver){
            List<uk.co.dolphin_com.sscore.playdata.Note> curArray = notes.get(curNote);

            if(curArray.size() == 2 && curArray.get(0).rest && curArray.get(1).rest)
            {
                player.moveCursor(notes.get(curNote + 1));
            }
            else if(curArray.size() == 1 && curArray.get(0).rest){
                player.moveCursor(notes.get(curNote + 1));
            }
            else{
                player.moveCursor(notes.get(curNote));
            }
        }
    }
    private boolean isTieSkip(Note note){
        if(note.isTieStop()){
            return true;
        }
        else if(note.isTieStart()){
            //tieOnNotes.add(note);
            return false;
        }
        return false;
    }

    public void setBarIndex(int barIndex) {
        this.barIndex = barIndex;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).get(0).startBarIndex == this.barIndex) {
                curNote = i;
                isChord = false;
                break;
            }
        }
    }
}
