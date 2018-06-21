package piano.pianotrainer.scoreModels;

import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Shubho on 2017-11-19.
 */

public class NoteGroup {
    public List<Note> notes;
    // the slope of the beam. Can be negative or positive
    public int beamSlope;
    // location of beam. []
    // [none, top, bottom] : [0, 1, 2]
    public int beamLoc;
    public boolean hasBeam;
    public int clef;

    public NoteGroup(List<Note> notes, int beamSlope, int beamLoc) {
        this.notes = notes;
        this.beamSlope = beamSlope;
        this.beamLoc = beamLoc;
        this.hasBeam = true;
    }

    public NoteGroup(List<Note> notes){
        this.notes = notes;
        this.hasBeam = false;
    }

    public List<Integer> rightMostNotePositions(){
        int xDeviationThreshold = 10; //in pixels
        Log.d("NoteGroup",String.format("Notes size %d", notes.size()));
        int lastIndex = notes.size() - 1;
        double curX = notes.get(lastIndex).circleCenter.x;
        int curIndex = lastIndex;
        List<Integer> rightNotesIndicies = new ArrayList<>();
        while(Math.abs(curX - notes.get(curIndex).circleCenter.x) <= xDeviationThreshold && curIndex >= 0){
            rightNotesIndicies.add(curIndex);
            curIndex -= 1;
            if (curIndex < 0) break;
        }
        //guaranteed to have size of at least 1
        return rightNotesIndicies;
    }

    public boolean addDot(Rect notegroupRect, Rect dotRect){
        if(this.notes.size() == 0){
            return false;
        }
        List<Integer> rightMostNotesIndicies = rightMostNotePositions();
        Log.d("NoteGroupDot", String.format("adding a dot with # of notes %d and first center %.2f", rightMostNotesIndicies.size(),
                                                                            notes.get(rightMostNotesIndicies.get(0)).circleCenter.x));
        boolean hasAddedDots = true;
        for(int index : rightMostNotesIndicies){
            hasAddedDots &= notes.get(index).appendDot(notegroupRect, dotRect);
        }
        return hasAddedDots;
    }

    public void setTieStart(){
        if(notes.size() == 0){
            return;
        }
        int index = notes.size() - 1;
        Note rightMost = notes.get(index);
        rightMost.hasTieStart = true;
        notes.set(index, rightMost);
    }

    public void setTieEnd(){
        if(notes.size() == 0){
            return;
        }
        Note leftMost = notes.get(0);
        leftMost.hasTieEnd = true;
        notes.set(0, leftMost);
    }

    public boolean setAccidental(ElementType eleAcc){
        Accidental acc;
        if(eleAcc == ElementType.Flat){
            acc = Accidental.Flat;
        }
        else if(eleAcc == ElementType.Sharp){
            acc = Accidental.Sharp;
        }
        else if(eleAcc == ElementType.Natural){
            acc = Accidental.Natural;
        }
        else{
            return false;
        }
        Note leftMost = notes.get(0);
        leftMost.accidental = acc;
        notes.set(0, leftMost);
        return true;
    }

    public void sortCircles() {
        if (notes == null) return;
        if (notes.size() == 0) return;

        Collections.sort(notes, new Comparator<Note>() {
            @Override
            public int compare(Note n1, Note n2) {
                if (n1.circleCenter.x != n2.circleCenter.x) {
                    return Double.compare(n1.circleCenter.x, n2.circleCenter.x);
                }
                else {
                    return Double.compare(n1.circleCenter.y, n2.circleCenter.y);
                }
            }
        });
    }
}
