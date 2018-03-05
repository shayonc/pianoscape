package piano.pianotrainer.scoreModels;

import java.util.Collection;
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

    public NoteGroup(List<Note> notes, int beamSlope, int beamLoc) {
        this.notes = notes;
        this.beamSlope = beamSlope;
        this.beamLoc = beamLoc;
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
