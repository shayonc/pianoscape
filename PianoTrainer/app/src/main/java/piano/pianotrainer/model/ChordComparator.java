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
    List<uk.co.dolphin_com.sscore.playdata.Note> expNotes = new ArrayList<>();

    public ChordComparator(List<uk.co.dolphin_com.sscore.playdata.Note> notes) {
        //add non tie off notes to expected list
        for(uk.co.dolphin_com.sscore.playdata.Note note: notes){
            if(!note.rest){
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
        for (uk.co.dolphin_com.sscore.playdata.Note expNote: expNotes) {
            if (expNote.midiPitch == note.getMidiData()){
                correctCount++;
                //cumCorrectCount++;
                //expNotes.remove(expNote);
                return noteCount - correctCount;
            }
        }
        //if one note played is incorrect then reset
        //cumIncorrectCount++;
        //This is where incorrect notes are detected
        this.clearCorrect();
        return noteCount - correctCount;
    }

    public void displayExpected(){
        for(uk.co.dolphin_com.sscore.playdata.Note note: expNotes){
            Log.d("chordComparitor", "Expected octave " + note.midiPitch / 12 + " step " + note.midiPitch % 12);
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
