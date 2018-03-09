package piano.pianotrainer.scoreModels;

import android.graphics.Rect;

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
        int lastIndex = notes.size() - 1;
        double curX = notes.get(lastIndex).circleCenter.x;
        int curIndex = lastIndex;
        List<Integer> rightNotesIndicies = new ArrayList<>();
        while(Math.abs(curX - notes.get(curIndex).circleCenter.x) <= xDeviationThreshold && curIndex >= 0){
            rightNotesIndicies.add(curIndex);
            curIndex -= 1;
        }
        //guaranteed to have size of at least 1
        return rightNotesIndicies;
    }

    public boolean addDot(Rect dotRect){
        List<Integer> rightMostNotesIndicies = rightMostNotePositions();
        boolean hasAddedDots = true;
        for(int index : rightMostNotesIndicies){
            hasAddedDots &= notes.get(index).appendDot(dotRect);
        }
        return hasAddedDots;
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
