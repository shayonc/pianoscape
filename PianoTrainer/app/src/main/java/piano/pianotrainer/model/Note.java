package piano.pianotrainer.model;

public class Note {

    // pitch
    private String step;
    private int alter;
    private int octave;

    // treble = 1; bass = 2;
    private int staff;

    // measure number
    private int measure;

    private int line;
    private int duration;
    private String type;

    public enum Key {
        A,
        Bflat,
        B,
        C,
        Csharp,
        D,
        Eflat,
        E,
        F,
        Fsharp,
        G,
        Gsharp
    }

}
