package piano.pianotrainer.scoreModels;

/**
 * Created by Shubho on 2017-11-19.
 * This class represents a single staff (may or may not be a grand staff).
 */

import android.util.Pair;
import java.util.*;


public class Staff {
    // all measures in that staff
    public List<Measure> measures;
    // indicator to see if the staff contains both clefs or not (currently only grand staffs are supported).
    boolean grandStaff;
    // maps volume dynamics with its position pair (start x, end x). Positions are a % relative to the staff.
    // Crescendo: true
    // Diminuendo: false
    Map<Pair<Float, Float>, Boolean> volumeDynamics;

    public Staff(boolean grand) {
        measures = new ArrayList<Measure>();
        grandStaff = grand;
        volumeDynamics = new HashMap<Pair<Float, Float>, Boolean>();
    }

    public void addMeasure(Measure measure) {
        measures.add(measure);
    }
}
