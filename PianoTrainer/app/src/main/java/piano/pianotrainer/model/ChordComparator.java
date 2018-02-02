package piano.pianotrainer.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick_ on 1/31/2018.
 */

public class ChordComparator {
    int noteCount;
    int correctCount;
    boolean isFirstAttempt;
    List<Note> expNotes = new ArrayList<>();

    public ChordComparator(List<Note> notes, int curNote) {
        noteCount = 1;
        correctCount = 0;
        isFirstAttempt = true;
        expNotes.add(notes.get(curNote));

        Note nextNote = notes.get(curNote + noteCount);

        while(nextNote.isChord()){
            expNotes.add(nextNote);
            noteCount++;
            if (notes.size() < curNote + noteCount) {
                nextNote = notes.get(curNote + noteCount);
            }
            else{
                break;
            }
        }
    }
    // returns remaining number of correct notes to wait for
    public int compareNotes(Note note){
        for (Note expNote: expNotes) {
            if (expNote.getOctave() == note.getOctave() && expNote.getStep().equals(note.getStep())) {
                correctCount++;
                //expNotes.remove(expNote);
                return noteCount - correctCount;
            }
        }
        //if one note played is incorrect then reset
        this.clearCorrect();
        return noteCount - correctCount;
    }

    public void displayExpected(){
        for(Note note: expNotes){
            Log.d("chordComparitor", "Expected octave " + note.getOctave() + " step " + note.getStep());
        }
        Log.d("chordComparitor", "End of expNotes");
    }

    public void clearCorrect(){
        correctCount = 0;
    }

    public int getCorrectCount(){
        return correctCount;
    }

    public int getNoteCount(){
        return  noteCount;
    }

    public boolean isFirstAttempt(){
        boolean ret = isFirstAttempt;
        isFirstAttempt = false;
        return ret;
    }
}
