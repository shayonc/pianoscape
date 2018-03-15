package piano.pianotrainer.scoreModels;

/**
 * Created by Shubho on 2017-11-19.
 */

import android.graphics.Rect;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;


public class Measure {
    String TAG = "Measure";
    public Map<Rect, NoteGroup> noteGroups;  // maps position % with a notegroup
    public Map<Rect, Rest> rests;            // maps position % with a rest
    public int upperTimeSig;                      // number of basic pulses that make up the measure's total weight
    public int lowerTimeSig;                  // the weight of a basic pulse in the measure
    public Map<Pitch, Accidental> keySigs;        // maps the pitch with its staff line/space
    List<List<NoteGroup>> ties;         // list of ties. Each tie is represented by a list of notegroups
    //accCenterPos - stores when we process an accidental and is used as a getter for keySigCenters
    Map<Rect, Double> accCenterPos;    //Map of center position of accidentals which is calculated with CV
    List<Double> keySigCentres;         //Store the centers which a seperate function can map to pitch and scale of accidental
    List<Boolean> isKeySigTreble;
    List<Accidental> keySigPitchAccList;
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
        keySigs = new LinkedHashMap<Pitch, Accidental>();
        ties = new ArrayList<List<NoteGroup>>();
        dynamics = 0;
        trebleElementTypes = new ArrayList<>();
        trebleRects = new ArrayList<>();
        bassElementTypes = new ArrayList<>();
        bassRects = new ArrayList<>();
        accCenterPos = new LinkedHashMap<>();
        keySigCentres = new ArrayList<Double>();
        isKeySigTreble = new ArrayList<Boolean>();
        keySigPitchAccList = new ArrayList<Accidental>();
    }

    public List<Accidental> getKeySigPitchAccList(){
        return keySigPitchAccList;
    }

    public String info(){
        String s = String.format("Size: te:%d, tr:%d, be:%d, br:%d", trebleElementTypes.size(), trebleRects.size(), bassElementTypes.size(), bassRects.size());
        int counterTreble = 0;
        int counterBass = 0;
        boolean hasTrebleClef = false;
        boolean hasBassClef = false;
        s += "TrebleElements: ";
        for(ElementType elementType : trebleElementTypes){
            if(elementType == ElementType.Dot){
                counterTreble++;
            }
            if(elementType == ElementType.TrebleClef){
                hasTrebleClef = true;
            }
            s += elementType.toString() + ",";
        }
        s += "BassElements: ";
        for(ElementType elementType : bassElementTypes){
            if(elementType == ElementType.Dot){
                counterBass++;
            }
            if(elementType == ElementType.BassClef){
                hasBassClef = true;
            }
            s += elementType.toString() + ",";
        }
        s += String.format("Treble Clef? %s Bass Clef? %s", hasTrebleClef ? "Yes" : "No", hasBassClef ? "Yes" : "No");
        s += String.format("Total dots t,b: %d,%d", counterTreble, counterBass);
        s += String.format("Time sig: %d / %d", upperTimeSig, lowerTimeSig);
        s += String.format("# Notegroups: %d, Rests: %d, Accs: %d, KeySigs: %d", noteGroups.size(),
                            rests.size(), accCenterPos.size(), keySigCentres.size());
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
        Log.d("MeasureDot", String.format("Integrating dot at %d with rect %d", dotRect.centerX(), noteGroupRect.centerX()));
        NoteGroup curNoteGroup = noteGroups.get(noteGroupRect);
        boolean added = curNoteGroup.addDot(noteGroupRect, dotRect);
        Log.d(TAG, String.format("integrated dot success? %b", added));
    }

    public boolean isEleTypeAccidental(ElementType elementType){
        return elementType == ElementType.Flat || elementType == ElementType.Sharp || elementType == ElementType.Natural;
    }

    public void addAccidentalCenter(Rect rect, double centerY){
        accCenterPos.put(rect, centerY);
    }

    public void setKeySigPitch(Map<Pitch,Accidental> keySigPitches){
        this.keySigs = keySigPitches;
    }

    public List<Boolean> getKeySigIsTreble(){
        return isKeySigTreble;
    }

    public List<Double> getKeySigCenters(){
        return keySigCentres;
    }

    public Accidental eleToAcc(ElementType e){
        if(e == ElementType.Flat){
            return Accidental.Flat;
        }
        else if(e == ElementType.Sharp){
            return Accidental.Sharp;
        }
        else if(e == ElementType.Natural){
            return Accidental.Natural;
        }
        else{
            return Accidental.None;
        }
    }

    //measure corrections
    public void checkNeighbours(){
        ElementType curType;
        Rect curRect;
        NoteGroup tmpNg;
        for(int i = 1; i < trebleElementTypes.size(); i++){
            curRect = trebleRects.get(i);
            curType = trebleElementTypes.get(i);
            if(curType == ElementType.Dot){
                if(trebleElementTypes.get(i - 1) == ElementType.NoteGroup){
                    Log.d("MeasureTreble", String.format("integrating dot with ng index %d",i));
                    integrateDot(i, trebleRects);
                }
                else{
                    Log.d("MeasureTreble", String.format("Dot detected at pos %d with prev ele type %s", i, trebleElementTypes.get(i - 1).toString()));
                }
                // ignore the rest - by exclusion unhandled dots are ignored
            }
            else if(isEleTypeAccidental(curType)){
                //adjacent accs are apparent of key sigs assumption
                if(i < trebleElementTypes.size() - 1 && trebleElementTypes.get(i + 1) == ElementType.NoteGroup && !isEleTypeAccidental(trebleElementTypes.get(i-1))){
                    //add logic here to tie notegroup/note to acc
                    tmpNg = noteGroups.get(trebleRects.get(i+1));
                    tmpNg.setAccidental(curType);
                    noteGroups.put(trebleRects.get(i+1), tmpNg);
                    Log.d("MeasureTreble", String.format("Set Acc of treble index %d to note on treble index ", i));
                }
                //by exclusion this case handles keysigs
                else{
                    Log.d("MeasureTreble", String.format("KEY SIG at treble index %d", i));
                    //adds the center value to the list which will be processed to map to pitch/scale
                    keySigCentres.add(accCenterPos.get(curRect));
                    isKeySigTreble.add(true);
                    keySigPitchAccList.add(eleToAcc(curType));
                    //add clef
                }
            }
            else if(curType == ElementType.Tie){
                //left neighbour
                if(trebleElementTypes.get(i - 1) == ElementType.NoteGroup){
                    //add tie boolean to left notegroup right most
                    tmpNg = noteGroups.get(trebleRects.get(i-1));
                    tmpNg.setTieStart();
                }
                //right neighbour
                if(i < trebleElementTypes.size() - 1 && trebleElementTypes.get(i + 1) == ElementType.NoteGroup){
                    //add tie boolean in right notegroups left most
                    tmpNg = noteGroups.get(trebleRects.get(i+1));
                    tmpNg.setTieEnd();
                }
            }
        }
        for(int j = 1; j < bassElementTypes.size(); j++){
            curRect = bassRects.get(j);
            curType = bassElementTypes.get(j);
            if(curType == ElementType.Dot){
                if(bassElementTypes.get(j - 1) == ElementType.NoteGroup){
                    Log.d("MeasureBass", String.format("integrating bass dot with ng index %d",j));
                    integrateDot(j, bassRects);
                }
                else{
                    Log.d("MeasureBass", String.format("Dot detected at bass pos %d with prev ele type %s", j, bassElementTypes.get(j - 1).toString()));
                }
                // ignore the rest - by exclusion unhandled dots are ignored
            }
            else if(isEleTypeAccidental(curType)){
                //adjacent accs are apparent of key sigs assumption
                if(j < bassElementTypes.size() - 1 && bassElementTypes.get(j + 1) == ElementType.NoteGroup && !isEleTypeAccidental(bassElementTypes.get(j-1))){
                    //add logic here to tie notegroup/note to acc
                    tmpNg = noteGroups.get(bassRects.get(j+1));
                    tmpNg.setAccidental(curType);
                    noteGroups.put(bassRects.get(j+1), tmpNg);
                    Log.d("MeasureBass", String.format("Set Acc of bass index %d to note on bass index ", j));
                }
                //by exclusion this case handles keysigs
                else{
                    Log.d("MeasureBass", String.format("KEY SIG at bass index %d", j));
                    //adds the center value to the list which will be processed to map to pitch/scale
                    keySigCentres.add(accCenterPos.get(curRect));
                    isKeySigTreble.add(false);
                    keySigPitchAccList.add(eleToAcc(curType));
                }
            }
            else if(curType == ElementType.Tie){
                //left neighbour
                if(bassElementTypes.get(j - 1) == ElementType.NoteGroup){
                    //add tie boolean to left notegroup right most
                    tmpNg = noteGroups.get(bassRects.get(j-1));
                    tmpNg.setTieStart();
                }
                //right neighbour
                if(j < bassElementTypes.size() - 1 && bassElementTypes.get(j + 1) == ElementType.NoteGroup){
                    //add tie boolean in right notegroups left most
                    tmpNg = noteGroups.get(bassRects.get(j+1));
                    tmpNg.setTieEnd();
                }
            }
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

    public void addKeySig(Pitch pitch, Accidental adjustment) {
        keySigs.put(pitch, adjustment);
    }

    public void addNoteGroup(Rect rect, NoteGroup notegroup, boolean isTreble) {
        notegroup.clef = isTreble ? 0 : 1;
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
