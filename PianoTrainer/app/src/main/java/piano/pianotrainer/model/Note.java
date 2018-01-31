package piano.pianotrainer.model;

import android.util.Log;

public class Note {

    String measureNumber;
    String print;
    int divisions;
    int fifths;
    String mode;
    int beats;
    int beattype;
    int staves;
    String sign;
    int line;
    boolean chord;
    boolean grace;
    String step;
    int alter;
    int octave;
    int voice;
    String stem;
    String type;
    String accidental;
    int staff;
    int duration;
    boolean rest;
    boolean forward;
    boolean tieStart;
    boolean tieStop;
    boolean noteOn;

    long startTime;
    long endTime;

    public Note() {

    }
    public boolean isTieStart() {
        return tieStart;
    }

    public void setTieStart(boolean tieStart) {
        this.tieStart = tieStart;
    }

    public boolean isTieStop() {
        return tieStop;
    }

    public void setTieStop(boolean tieStop) {
        this.tieStop = tieStop;
    }

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public boolean isRest() {
        return rest;
    }

    public void setRest(boolean rest) {
        this.rest = rest;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getMeasureNumber() {
        return measureNumber;
    }

    public void setMeasureNumber(String measureNumber) {
        this.measureNumber = measureNumber;
    }

    public String getPrint() {
        return print;
    }

    public void setPrint(String print) {
        this.print = print;
    }

    public int getDivisions() {
        return divisions;
    }

    public void setDivisions(int divisions) {
        this.divisions = divisions;
    }

    public int getFifths() {
        return fifths;
    }

    public void setFifths(int fifths) {
        this.fifths = fifths;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getBeats() {
        return beats;
    }

    public void setBeats(int beats) {
        this.beats = beats;
    }

    public int getBeattype() {
        return beattype;
    }

    public void setBeattype(int beattype) {
        this.beattype = beattype;
    }

    public int getStaves() {
        return staves;
    }

    public void setStaves(int staves) {
        this.staves = staves;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public boolean isChord() {
        return chord;
    }

    public void setChord(boolean chord) {
        this.chord = chord;
    }

    public boolean isGrace() {
        return grace;
    }

    public void setGrace(boolean grace) {
        this.grace = grace;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public void setStep(int note) {
        String convStep;
        switch(note){
            case 0: convStep = "C";
            break;
            case 1: convStep = "C#";
            break;
            case 2: convStep = "D";
                break;
            case 3: convStep = "D#";
                break;
            case 4: convStep = "E";
                break;
            case 5: convStep = "F";
                break;
            case 6: convStep = "F#";
                break;
            case 7: convStep = "G";
                break;
            case 8: convStep = "G#";
                break;
            case 9: convStep = "A";
                break;
            case 10: convStep = "A#";
                break;
            case 11: convStep = "B";
                break;
            default: convStep = "";
                break;
        }

        this.step = convStep;
    }

    public int getAlter() {
        return alter;
    }

    public void setAlter(int alter) {
        this.alter = alter;
    }

    public int getOctave() {
        return octave;
    }

    public void setOctave(int octave) {
        this.octave = octave;
    }

    public int getVoice() {
        return voice;
    }

    public void setVoice(int voice) {
        this.voice = voice;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccidental() {
        return accidental;
    }

    public void setAccidental(String accidental) {
        this.accidental = accidental;
    }

    public int getStaff() {
        return staff;
    }

    public void setStaff(int staff) {
        this.staff = staff;
    }

    public void setNoteOn(boolean on){this.noteOn = on;}

    public boolean getNoteOn() {return noteOn;}

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }


    public enum Key {
        C,
        Csharp,
        D,
        Eflat,
        E,
        F,
        Fsharp,
        G,
        Gsharp,
        A,
        Bflat,
        B
    }

}