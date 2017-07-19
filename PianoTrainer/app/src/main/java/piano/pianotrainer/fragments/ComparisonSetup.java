package piano.pianotrainer.fragments;


import java.util.ArrayList;
import java.util.List;

import piano.pianotrainer.model.Note;

/**
 * Created by SL-1 on 18/07/2017.
 */

public class ComparisonSetup {

    private int measureDivs = 16;   // Time Divisions per measure
    private int maxVoices = 8;   // Maximum voice channels per measure
    private final List<Note[]> syncedNotes;

    // Constructor
    public ComparisonSetup(){
        syncedNotes = new ArrayList<Note[]>();
    }

    /*
     * Takes in list of notes after XML parsing and tries to sync them based on note durations.
     * Processing individual measures ensures integrity of timing.
     */
    public List<Note[]> SyncNotes(List<Note> parsedNotes) {
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
        for (int jj = 0; jj < voicesPlace.length; jj++) {
            voicesPlace[jj] = 0;
        }

        Note playNotes[][] = new Note[measureDivs][maxVoices];
        // playNotes[Position in measure][Voice Channel]

        //For every note in the measure...
        for (Note note : MeasureBuffer) {
            if (!note.isRest() && !note.isForward()) {
                //if out of measure, revert voice position to latest that note duration can fit in.
                if ((voicesPlace[note.getVoice()] + note.getDuration()) >= measureDivs) {
                    voicesPlace[note.getVoice()] = measureDivs - note.getDuration();
                }
                // Add new note to voice channel
                playNotes[voicesPlace[note.getVoice()]][note.getVoice()] = note;
            }
            voicesPlace[note.getVoice()] += note.getDuration();
        }

        // Handles special case of first measure,
        // specifically if there are gaps at end without a filled note.
        if (isFirstMeasure){
            //get the smallest shift possible to move all notes to end of measure without violating duration
            int shiftDivs = measureDivs;
            for (int ii = 0; ii < playNotes.length; ii++){
                for (int jj = 0; jj < playNotes[ii].length; jj++){
                    if ((playNotes[ii][jj] != null) && ((measureDivs-playNotes[ii][jj].getDuration()-ii) < measureDivs)){
                        shiftDivs = measureDivs-playNotes[ii][jj].getDuration()-ii;
                    }
                }
            }
            //start shifting
            for (int ii = measureDivs-shiftDivs-1; ii >= 0; ii--) {
                playNotes[shiftDivs+ii] = playNotes[ii].clone();
                for(int jj = 0; jj < playNotes[ii].length; jj++){
                    playNotes[ii][jj] = null;
                }
            }
        }

        // Setup returning the measure
        for (int ii = 0; ii < playNotes.length; ii++) { //for each position
            //append notes array
            syncedNotes.add(playNotes[ii]);
        }
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

        for (Note[] posNotes: syncedNotes){

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

    /******************************************************************
     * DEBUG FUNCTIONS END
     ******************************************************************/


}
