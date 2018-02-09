package piano.pianotrainer.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by nick_ on 1/31/2018.
 */

public class ChordComparator {
    int noteCount;
    int correctCount;
    //int cumIncorrectCount;
    //int cumCorrectCount;
    boolean isFirstAttempt;
    List<Note> expNotes = new ArrayList<>();

    public ChordComparator(List<Note> notes) {
        //add non tie off notes to expected list
        for(Note note: notes){
            if(!note.isTieStop()){
                expNotes.add(note);
            }
        }
        noteCount = expNotes.size();
        correctCount = 0;
        //cumIncorrectCount = 0;
        //cumCorrectCount = 0;
        isFirstAttempt = true;
    }
    // returns remaining number of correct notes to wait for
    public int compareNotes(Note note){
        for (Note expNote: expNotes) {
            if (expNote.getOctave() == note.getOctave() && expNote.getStep().equals(note.getStep())) {
                correctCount++;
                //cumCorrectCount++;
                //expNotes.remove(expNote);
                return noteCount - correctCount;
            }
        }
        //if one note played is incorrect then reset
        //cumIncorrectCount++;
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
