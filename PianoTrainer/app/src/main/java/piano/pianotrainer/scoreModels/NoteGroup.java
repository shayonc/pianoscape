package piano.pianotrainer.scoreModels;

import java.util.List;

/**
 * Created by Shubho on 2017-11-19.
 */

public class NoteGroup {
    List<Note> notes;
    // the slope of the beam. Can be negative or positive
    int beamSlope;
    // location of beam. []
    // [none, top, bottom] : [0, 1, 2]
    int beamLoc;

    public NoteGroup(List<Note> notes, int beamSlope, int beamLoc) {
        this.notes = notes;
        this.beamSlope = beamSlope;
        this.beamLoc = beamLoc;
    }
    
}
