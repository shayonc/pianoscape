package piano.pianotrainer.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
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
//        Collections.reverse(expNotes);
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

    public String displayExpected(){
        String correctNotes = "";
        for(int i = 0; i < expNotes.size(); i++){
            Log.d("chordComparitor", "Expected octave " + expNotes.get(i).midiPitch / 12 + " step " + expNotes.get(i).midiPitch % 12);
            correctNotes += Integer.toString(expNotes.get(i).midiPitch / 12) +  setStep(expNotes.get(i).midiPitch % 12) + System.getProperty("line.separator");
        }
        Log.d("chordComparitor", "End of expNotes");
        return correctNotes;
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

    public String setStep(int note) {
        String convStep;
        switch(note){
            case 0: convStep = "C";
                break;
            case 1: convStep = "C#/Db";
                break;
            case 2: convStep = "D";
                break;
            case 3: convStep = "D#/Eb";
                break;
            case 4: convStep = "E";
                break;
            case 5: convStep = "F";
                break;
            case 6: convStep = "F#/Gb";
                break;
            case 7: convStep = "G";
                break;
            case 8: convStep = "G#/Ab";
                break;
            case 9: convStep = "A";
                break;
            case 10: convStep = "A#/Bb";
                break;
            case 11: convStep = "B";
                break;
            default: convStep = "";
                break;
        }
        return convStep;
    }
}
