package piano.pianotrainer.scoreImport;

/**
 * Created by Ekteshaf on 2018-02-22.
 */

public final class KnnLabels {
    //clefs
    public static final int G_CLEF = 1;
    public static final int F_CLEF = 2;
    //rests and notes
    //minor differences between whole/half rest - need additional criteria after
    public static final int WHOLE_HALF_REST = 10;
    public static final int QUARTER_REST = 11;
    public static final int EIGHTH_REST  = 12;
    public static final int ONE_SIXTEENTH_REST  = 13;
    //after heuristic classifier
    public static final int WHOLE_REST  = 14;
    public static final int HALF_REST  = 15;

    //accidentals
    public static final int FLAT_ACC = 20;
    public static final int SHARP_ACC = 21;
    public static final int NATURAL_ACC = 22;
    //time signatures
    public static final int TIME_C = 30;
    public static final int TIME_22 = 31;
    public static final int TIME_24 = 32;
    public static final int TIME_34 = 33;
    public static final int TIME_44 = 34;
    public static final int TIME_68 = 35;


    //no samples of time 34 - omit for now
    //others
    public static final int TIE = 40;
    public static final int DYNAMICS_F = 41;
    public static final int DOT = 42;
    public static final int BRACE = 43;
    public static final int BAR = 44;
    public static final int WHOLE_NOTE  = 45;
    //two whole notes positioned vertically
    public static final int WHOLE_NOTE_2  = 46;





}
