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
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;

import piano.pianotrainer.model.ChordComparator;
import piano.pianotrainer.model.Note;

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
    private List<Note> notes;
    private Lock compLock;
    private int curNote;
    private ChordComparator chordComparator;
    private boolean isChord = false;

    public NoteReceiver(ScopeLogger logger, List<Note> notes, Lock compLock, int curNote) {
        mStartTime = System.nanoTime();
        mLogger = logger;
        this.notes = notes;
        this.compLock = compLock;
        this.curNote = curNote;
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

        //Compare notes here

        compLock.lock();

        if(!note.getNoteOn() && isChord){
            //released cord too early, clear Chord Comparator
            chordComparator.clear();
            sb.append("A key was released before chord completed, chord has been reset\n");
            chordComparator.displayExpected();
        }
        if (note.getNoteOn()) {
            Note expNote = notes.get(curNote);

            //chord detection
            if(!isChord && notes.get(curNote + 1).isChord()){
                chordComparator = new ChordComparator(notes, curNote);
                isChord = true;
                sb.append("chord detected!!!!\n");
            }

            //ToDo step compare will not work for flats, midi is only converted to sharp

            if(!isChord) {
                if (expNote.getOctave() == note.getOctave() && expNote.getStep().equals(note.getStep())) {
                    //Note is correct
                    sb.append("\nNote " + curNote + " was correct\n");
                    curNote++;
                } else {
                    sb.append("\nNote " + curNote + " was incorrect\n");
                    sb.append("Expected octave " + expNote.getOctave() + " and step " + expNote.getStep() + "\n");
                    sb.append("Given octave " + note.getOctave() + " and step " + note.getStep() + "\n");
                }
            }
            else {
                if(chordComparator.compareNotes(note) == 0){
                    isChord = false;
                    curNote+= chordComparator.correctCount;
                    sb.append("chord completed successfully\n");
                }
            }
        }

        compLock.unlock();

        String text = sb.toString();
        //String text = "event";
        mLogger.log(text);
        Log.i(TAG, text);
    }

}
