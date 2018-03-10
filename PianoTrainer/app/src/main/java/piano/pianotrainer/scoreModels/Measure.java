package piano.pianotrainer.scoreModels;

/**
 * Created by Shubho on 2017-11-19.
 */

import android.graphics.Rect;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static android.content.ContentValues.TAG;


public class Measure {
    String TAG = "Measure";
    Map<Rect, NoteGroup> noteGroups;  // maps position % with a notegroup
    Map<Rect, Rest> rests;            // maps position % with a rest
    int upperTimeSig;                      // number of basic pulses that make up the measure's total weight
    double lowerTimeSig;                  // the weight of a basic pulse in the measure
    Map<Pitch, Integer> keySigs;        // maps the pitch with its staff line/space
    List<List<NoteGroup>> ties;         // list of ties. Each tie is represented by a list of notegroups
    Map<Rect, Double> accCenterPos;    //Map of center position of accidentals which is calculated with CV
    List<Double> keySigCentres;         //Store the centers which a seperate function can map to pitch and scale of accidental
    // the dynamics of the measure. The values are represented in the following way:
    // pp : -3
    // p  : -2
    // mp : -1
    // undefined : 0
    // mf : 1
    // f  : 2
    // ff : 3
    int dynamics;
    //sanity test - useful for testing and useful for finding neighbours for certain algorithms
    List<ElementType> trebleElementTypes;
    List<ElementType> bassElementTypes;
    List<Rect> trebleRects;
    List<Rect> bassRects;

    public Measure() {
        this.upperTimeSig = 0;
        this.lowerTimeSig = 0;
        this.dynamics = 0;
        noteGroups = new LinkedHashMap<>();
        rests = new LinkedHashMap<>();
        keySigs = new LinkedHashMap<Pitch, Integer>();
        ties = new ArrayList<List<NoteGroup>>();
        dynamics = 0;
        trebleElementTypes = new ArrayList<>();
        trebleRects = new ArrayList<>();
        bassElementTypes = new ArrayList<>();
        bassRects = new ArrayList<>();
        accCenterPos = new LinkedHashMap<>();
        keySigCentres = new ArrayList<Double>();
    }

    public String info(){
        String s = String.format("Size: te:%d, tr:%d, be:%d, br:%d", trebleElementTypes.size(), trebleRects.size(), bassElementTypes.size(), bassRects.size());
        int counterTreble = 0;
        int counterBass = 0;
        for(ElementType elementType : trebleElementTypes){
            if(elementType == ElementType.Dot){
                counterTreble++;
            }
        }
        for(ElementType elementType : bassElementTypes){
            if(elementType == ElementType.Dot){
                counterBass++;
            }
        }
        s += String.format("Total dots t,b: %d,%d", counterTreble, counterBass);
        return s;
    }

    public NoteGroup getNoteGroup(Rect r){
        return noteGroups.get(r);
    }



    public JSONObject toJSON(){
        JSONObject mainJO = new JSONObject();
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();

        try{
            for(Map.Entry<Rect, NoteGroup> noteEntry : noteGroups.entrySet()){
                for(Note n : noteEntry.getValue().notes){
                    jo.put("weight", n.weight);
                    jo.put("hasDot", n.hasDot);
                    jo.put("hasStaccato", n.hasStaccato);
                    jo.put("pitch", n.pitch);
                }
                ja.put(jo);
                mainJO.put("notes",ja);
            }
        }
        catch (Exception e){
            Log.d(TAG, e.getMessage());
        }

        return mainJO;
    }

    // upper sig, lower sig -> timing
    // key sigs(accidentals) -> always next to clef ... SC
    // regular acc -> populate Note field with enum
    // ties - figure out

    public void integrateDot(int dotIndex, List<Rect> clefRects){
        Rect dotRect = clefRects.get(dotIndex);
        Rect noteGroupRect = clefRects.get(dotIndex - 1);
        NoteGroup curNoteGroup = noteGroups.get(noteGroupRect);
        boolean added = curNoteGroup.addDot(dotRect);
        Log.d(TAG, String.format("integrated dot success? %b", added));
    }

    public boolean isEleTypeAccidental(ElementType elementType){
        return elementType == ElementType.Flat || elementType == ElementType.Sharp || elementType == ElementType.Natural;
    }

    public void addAccidentalCenter(Rect rect, double centerY){
        accCenterPos.put(rect, centerY);
    }
    //measure corrections
    public void checkNeighbours(){
        ElementType curType;
        Rect curRect;

        for(int i = 1; i < trebleElementTypes.size(); i++){
            curRect = trebleRects.get(i);
            curType = trebleElementTypes.get(i);
            if(curType == ElementType.Dot){
                if(trebleElementTypes.get(i - 1) == ElementType.NoteGroup){
                    Log.d(TAG, "integrating dot...");
                    integrateDot(i, trebleRects);
                }
                else{
                    Log.d(TAG, String.format("Dot detected at pos %d with prev ele type %d", i, trebleElementTypes.get(i - 1).ordinal()));
                }
                // ignore the rest - by exclusion unhandled dots are ignored
            }
            else if(isEleTypeAccidental(curType)){
                if(trebleElementTypes.get(i - 1) == ElementType.NoteGroup){
                    //add logic here to tie notegroup/note to acc
                    Log.d(TAG, "Acc neighbour is a notegroup");
                }
                else if(trebleElementTypes.get(i - 1) == ElementType.TrebleClef){
                    if(isEleTypeAccidental(trebleElementTypes.get(i + 1))){
                        Log.d(TAG, String.format("KEY SIG at treble index %d", i));
                        //adds the center value to the list which will be processed to map to pitch/scale
                        keySigCentres.add(accCenterPos.get(curRect));
                        keySigCentres.add(accCenterPos.get(trebleRects.get(i+1)));
                    }
                }
            }
        }
        for(int j = 1; j < bassElementTypes.size(); j++){
            if(bassElementTypes.get(j) == ElementType.Dot){
                if(bassElementTypes.get(j - 1) == ElementType.NoteGroup){
                    integrateDot(j, bassRects);
                }
                else{
                    Log.d(TAG, String.format("Dot detected at pos %d with prev ele type %d", j, bassElementTypes.get(j - 1).ordinal()));
                }
                // ignore the rest - by exclusion unhandled dots are ignored
            }
//            else if(isEleTypeAccidental(trebleElementTypes.get(i))){
//                if(trebleElementTypes.get(i - 1) == ElementType.NoteGroup){
//                    //add logic here to tie notegroup/note to acc
//                }
//                else if(trebleElementTypes.get(i - 1) == ElementType.TrebleClef || trebleElementTypes.get(i - 1) == ElementType.BassClef){
//                    if(isEleTypeAccidental(trebleElementTypes.get(i + 1))){
//                        // set key signature since its inbetween a clef and another accidental (ignored edge case of varying accs for now)
//                    }
//                }
//            }
        }
    }

    public void setUpperTimeSig(int time) { this.upperTimeSig = time; }

    public void setLowerTimeSig(int time) { this.lowerTimeSig = time;}

    public void addToClefLists(boolean isTreble, Rect rect, ElementType eleType){
        if(isTreble){
            trebleRects.add(rect);
            trebleElementTypes.add(eleType);
        }
        else{
            bassRects.add(rect);
            bassElementTypes.add(eleType);
        }
    }

    public void setTimeSig(int upper, int lower, boolean isTreble, Rect r){
        setUpperTimeSig(upper);
        setLowerTimeSig(lower);
        addToClefLists(isTreble, r, ElementType.TimeSig);
    }

    public void addKeySig(Pitch pitch, int adjustment) {
        keySigs.put(pitch, adjustment);
    }

    public void addNoteGroup(Rect rect, NoteGroup notegroup, boolean isTreble) {
        noteGroups.put(rect, notegroup);
        addToClefLists(isTreble, rect, ElementType.NoteGroup);
    }

    public void addRest(Rect rect, Rest rest) {
        rests.put(rect, rest);
        addToClefLists(rest.clef == 0, rect, ElementType.Rest);
    }

    public void addClef(ElementType elementType, Rect r){
        addToClefLists(elementType == ElementType.TrebleClef, r, elementType);
    }

    public void addTie(List<NoteGroup> tie) {
        ties.add(tie);
    }

    public void setDynamics(int dynamics) {
        this.dynamics = dynamics;
    }
}
