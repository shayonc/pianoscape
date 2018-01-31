package piano.pianotrainer.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick_ on 1/31/2018.
 */

public class ChordComparator {
    int noteCount;
    public int correctCount;
    List<Note> expNotes = new ArrayList<>();

    public ChordComparator(List<Note> notes, int curNote) {
        noteCount = 1;
        correctCount = 0;
        Note nextNote = notes.get(curNote + noteCount);

        while(nextNote.isChord()){
            expNotes.add(nextNote);
            noteCount++;
            nextNote = notes.get(curNote + noteCount);
        }
    }
    // returns remaining number of correct notes to wait for
    public int compareNotes(Note note){
        for (Note expNote: expNotes) {
            if (expNote.getOctave() == note.getOctave() && expNote.getStep().equals(note.getStep())) {
                correctCount++;
                expNotes.remove(expNote);
                break;
            }
        }
        return noteCount - correctCount;
    }

    public void displayExpected(){
        for(Note note: expNotes){
            Log.d("chordComparitor", "Expected octave " + note.getOctave() + " step " + note.getStep());
        }
    }

    public void clear(){
        correctCount = 0;
    }
}
