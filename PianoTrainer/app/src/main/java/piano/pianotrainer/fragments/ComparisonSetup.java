package piano.pianotrainer.fragments;


import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import piano.pianotrainer.model.Note;

/**
 * Created by SL-1 on 18/07/2017.
 */

public class ComparisonSetup {

    private int measureDivs;   // Time Divisions per measure
    private int maxVoices = 8;   // Maximum voice channels per measure
    private final ArrayList<List<Note>> syncedNotes;

    // Constructor
    public ComparisonSetup(){
        syncedNotes = new ArrayList<List<Note>>();
    }

    /*
     * Takes in list of notes after XML parsing and tries to sync them based on note durations.
     * Processing individual measures ensures integrity of timing.
     */
    public ArrayList<List<Note>> SyncNotes(List<Note> parsedNotes) {
        String toPrint = "";
        // Keeps track of special case for first measure
        boolean firstMeasure = true;

        //buffer for processing each measure separately
        List<Note> measureBuffer = new ArrayList();

        //loop through each note
        for (int ii = 0; ii < parsedNotes.size(); ii++) {
            measureBuffer.add(parsedNotes.get(ii));

            //check if last element OR next note is not in same measure. If so, process measure and flush buffer.
            if (ii == parsedNotes.size() - 1 || parsedNotes.get(ii).getMeasureNumber() != parsedNotes.get(ii + 1).getMeasureNumber()) {
                measureDivs = measureBuffer.get(0).getBeats()*measureBuffer.get(0).getDivisions();
                SyncMeasure(measureBuffer, firstMeasure);
                measureBuffer.clear();
                firstMeasure = false;
            }
        }
        return syncedNotes;
    }

    /*
     * Submodule for taking care of syncing notes in individual measures.
     * Processing individual measures ensures integrity of timing.
     */
    public void SyncMeasure(List<Note> MeasureBuffer, boolean isFirstMeasure){
        //initialize voicesPlace keeping track of position all voices
        int voicesPlace[] = new int[maxVoices];
        int lastPlace[] = new int[maxVoices];
        for (int ii = 0; ii < voicesPlace.length; ii++) {
            voicesPlace[ii] = 0;
        }

        List<List<Note>> playNotes = new ArrayList<List<Note>>();
        // playNotes[Position in measure][Voice Channel]
        // populate with number of measures
        for (int ii = 0; ii < measureDivs; ii++) {
            playNotes.add(new ArrayList<Note>());
        }

        //For every note in the measure...
        for (Note note : MeasureBuffer) {
            if (!note.isRest() && !note.isForward()) {  //if not rest or forward, plot note
                //if out of measure, revert voice position to latest that note duration can fit in.
                //if ((voicesPlace[note.getVoice()] + note.getDuration()) >= measureDivs && !note.isGrace()) {
                //    voicesPlace[note.getVoice()] = measureDivs - note.getDuration();
                //}
                // Add new note to voice channel
                if (!note.isChord()) {
                    try {
                        playNotes.get(voicesPlace[note.getVoice()]).add(note);
                    } catch(IndexOutOfBoundsException e) {
                        for (int ii = 0; ii < measureDivs; ii++) {
                            playNotes.add(new ArrayList<Note>());
                        }
                        playNotes.get(voicesPlace[note.getVoice()]).add(note);
                    }
                }
                else {
                    playNotes.get(lastPlace[note.getVoice()]).add(note);
                }
            }
            //if not grace note, change position.
            if (!note.isGrace() && !note.isChord()) {
                lastPlace = voicesPlace.clone();
                voicesPlace[note.getVoice()] += note.getDuration();
            }
        }

        // Handles special case of first measure,
        // specifically if there are gaps at end without a filled note.
        if (isFirstMeasure){
            //get the smallest shift possible to move all notes to end of measure without violating duration
            int shiftDivs = measureDivs;
            for (int ii = 0; ii < playNotes.size(); ii++){
                for (Note note: playNotes.get(ii)){
                    if ((measureDivs-note.getDuration()-ii) < shiftDivs){
                        shiftDivs = measureDivs-note.getDuration()-ii;
                    }
                }
            }
            //start shifting
            for (int ii = shiftDivs-1; ii >= 0; ii--) {
                playNotes.remove(measureDivs-1);
                playNotes.add(0, new ArrayList<Note>());
            }
        }

        // Setup returning the measure
        syncedNotes.addAll(playNotes);
    }

    /******************************************************************
     * DEBUG FUNCTIONS START
     ******************************************************************/
    /*
     * Makes string to print for checking timing of sync.
     */
    public String DebugPrintSync(){
        String toPrint = "";
        int divCounter = 0;
        String notePrint;
        String divPrint = "";
        String lastMeasureNumber = "";

        for (List<Note> posNotes: syncedNotes){

            for (Note note: posNotes){
                if (note != null){

                    notePrint = "_"+note.getStep()+note.getOctave()+"("+(note.getAlter()==-99?0:note.getAlter())+")";
                    if (note.isTieStart()&&note.isTieStop()){notePrint+="{sp}";}
                    else if (note.isTieStart()){notePrint+="{s}";}
                    else if (note.isTieStop()){notePrint+="{p}";}
                    else {notePrint+="{ }";}
                    lastMeasureNumber = note.getMeasureNumber();
                    // check for redundancy
                    if (!divPrint.contains(notePrint)){
                        divPrint+=notePrint;
                    }
                }
            }
            if (divCounter%measureDivs == 0){
                toPrint += lastMeasureNumber+"=================\n";
            }
            divCounter++;
            if (divPrint == ""){divPrint = "-";}
            toPrint += divPrint+"\n";
            divPrint = "";
        }
        return toPrint;
    }

    public String CompareDebugPrintSync(ArrayList<List<Note>> correctSyncedNotes, ArrayList<List<Note>> wrongSyncedNotes){
        String toPrint = "";
        int divCounter = 0;
        String notePrint;
        String divPrint = "";
        String lastMeasureNumber = "";

        Log.d("ComparisonSet1", String.valueOf(correctSyncedNotes.size()));
        Log.d("ComparisonSet2", String.valueOf(wrongSyncedNotes.size()));

        for (int i = 0; i < correctSyncedNotes.size(); i++) {
            for (int j = 0; j < correctSyncedNotes.get(i).size(); j++) {
                if (correctSyncedNotes.get(i).get(j) != null && wrongSyncedNotes.get(i).get(j) != null){

                    if (!correctSyncedNotes.get(i).get(j).getStep().equals(wrongSyncedNotes.get(i).get(j).getStep()) || correctSyncedNotes.get(i).get(j).getOctave() != wrongSyncedNotes.get(i).get(j).getOctave()) {
                        notePrint = " ✘ ";
                        //notePrint += wrongSyncedNotes.get(i).get(j).getStep() +  wrongSyncedNotes.get(i).get(j).getOctave();
                        notePrint += "_"+wrongSyncedNotes.get(i).get(j).getStep()+wrongSyncedNotes.get(i).get(j).getOctave()+"("+(wrongSyncedNotes.get(i).get(j).getAlter()==-99?0:wrongSyncedNotes.get(i).get(j).getAlter())+")";
                        if (wrongSyncedNotes.get(i).get(j).isTieStart()&&wrongSyncedNotes.get(i).get(j).isTieStop()){notePrint+="{sp}";}
                        else if (wrongSyncedNotes.get(i).get(j).isTieStart()){notePrint+="{s}";}
                        else if (wrongSyncedNotes.get(i).get(j).isTieStop()){notePrint+="{p}";}
                        else {notePrint+="{ }";}

                    }
                    else {
                        notePrint = " ✓";
                    }
                    notePrint += "_"+correctSyncedNotes.get(i).get(j).getStep()+correctSyncedNotes.get(i).get(j).getOctave()+"("+(correctSyncedNotes.get(i).get(j).getAlter()==-99?0:correctSyncedNotes.get(i).get(j).getAlter())+")";
//                    notePrint = "_"+correctSyncedNotes.get(i).get(j).getStep()+correctSyncedNotes.get(i).get(j).getOctave()+"("+(correctSyncedNotes.get(i).get(j).getAlter()==-99?0:correctSyncedNotes.get(i).get(j).getAlter())+")";
                    if (correctSyncedNotes.get(i).get(j).isTieStart()&&correctSyncedNotes.get(i).get(j).isTieStop()){notePrint+="{sp}";}
                    else if (correctSyncedNotes.get(i).get(j).isTieStart()){notePrint+="{s}";}
                    else if (correctSyncedNotes.get(i).get(j).isTieStop()){notePrint+="{p}";}
                    else {notePrint+="{ }";}
                    lastMeasureNumber = correctSyncedNotes.get(i).get(j).getMeasureNumber();
                    // check for redundancy
                    if (!divPrint.contains(notePrint)){
                        divPrint+=notePrint;
                    }
                }
            }
            if (divCounter%measureDivs == 0){
                toPrint += lastMeasureNumber+"=================\n";
            }
            divCounter++;
            if (divPrint == ""){divPrint = "-";}
            toPrint += divPrint+"\n";
            divPrint = "";
        }
        return toPrint;
    }

    /******************************************************************
     * DEBUG FUNCTIONS END
     ******************************************************************/


}
