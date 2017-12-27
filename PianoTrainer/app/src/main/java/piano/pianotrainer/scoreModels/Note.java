package piano.pianotrainer.scoreModels;

/**
 * Created by Shubho on 2017-12-27.
 */

public class Note {
    double weight;
    boolean hasDot;
    Accidental accidental;
    Pitch pitch;
    // 0 refers to treble clef, 1 refers to bass clef
    int clef;
    // position of line w.r.t the note circle
    // [none, top-left, top-right, bottom-left, bottom-right] = [0,1,2,3,4]
    int linePosition;
    boolean hasStaccato;

    public Note(double weight, boolean hasDot, Accidental accidental, Pitch pitch, int clef, int linePosition, boolean hasStaccato) {
        this.weight = weight;
        this.hasDot = hasDot;
        this.accidental = accidental;
        this.pitch = pitch;
        this.clef = clef;
        this.linePosition = linePosition;
        this.hasStaccato = hasStaccato;
    }
}
