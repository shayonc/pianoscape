package piano.pianotrainer.scoreModels;

/**
 * Created by Shubho on 2017-11-19.
 */

import java.util.*;


public class Measure {
    Map<Double, NoteGroup> noteGroups;  // maps position % with a notegroup
    Map<Double, Rest> rests;            // maps position % with a rest
    int numPulses;                      // number of basic pulses that make up the measure's total weight
    double basicPulse;                  // the weight of a basic pulse in the measure
    Map<Pitch, Integer> keySigs;        // maps the pitch with its staff line/space
    List<List<NoteGroup>> ties;         // list of ties. Each tie is represented by a list of notegroups
    // the dynamics of the measure. The values are represented in the following way:
    // pp : -3
    // p  : -2
    // mp : -1
    // undefined : 0
    // mf : 1
    // f  : 2
    // ff : 3
    int dynamics;

    public Measure(int numPulses, double basicPulse) {
        this.numPulses = numPulses;
        this.basicPulse = basicPulse;
        this.dynamics = dynamics;
        noteGroups = new HashMap<Double, NoteGroup>();
        rests = new HashMap<Double, Rest>();
        keySigs = new HashMap<Pitch, Integer>();
        ties = new ArrayList<List<NoteGroup>>();
        dynamics = 0;
    }

    public void addKeySig(Pitch pitch, int adjustment) {
        keySigs.put(pitch, adjustment);
    }

    public void addNoteGroup(double position, NoteGroup notegroup) {
        noteGroups.put(position, notegroup);
    }

    public void addRest(double position, Rest rest) {
        rests.put(position, rest);
    }

    public void addTie(List<NoteGroup> tie) {
        ties.add(tie);
    }

    public void setDynamics(int dynamics) {
        this.dynamics = dynamics;
    }
}
