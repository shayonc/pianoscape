package piano.pianotrainer;

// Represents a key from a piano/keyboard
public class Key {
    public Note note;

    // Ranges from 0-8
    // On a 88-key piano, keys range from A0 to C8
    // Middle-C is C4
    public int octave;

    // If keyboard keys are pressure-sensitive, we can use this
    public int pressure;

    public Key(Note note, int octave, int pressure) {
        this.note = note;
        this.octave = octave;
        this.pressure = pressure;
    }
}
