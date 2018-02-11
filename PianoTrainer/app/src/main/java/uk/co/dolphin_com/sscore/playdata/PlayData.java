/**
 * SeeScore Android API
 * Dolphin Computing http://www.dolphin-com.co.uk
 */

package uk.co.dolphin_com.sscore.playdata;

import uk.co.dolphin_com.sscore.SScore;
import uk.co.dolphin_com.sscore.ex.ScoreException;

/**
 * Access to the midi-style play information for the score.<p>
 * iterator returns a BarIterator which steps to each bar in the correct play sequence.
 * The Bar returns each Part in the bar and also a virtual metronome part.<p>
 * Part.iterator returns a NoteIterator which steps to each note in the bar in that part.
 * The note returns a midi pitch and start time and duration in ms.
 */
public class PlayData implements Iterable<Bar> {

	/**
	 * an interface for the UI to determine playing parameters
	 */
	public interface PlayControls {

		/**
		 * return true if the part should be played
		 *
		 * @param partIndex the 0-based index of the part
		 * @return true if the part is to be played
		 */
		boolean getPartEnabled(int partIndex);

		/**
		 * return staff in part play status
		 * This allows it to play only 'left hand' or only 'right hand' in a piano part
		 *
		 * @param partIndex the 0-based index of the part
		 * @param staffIndex 0 for the top staff (right hand), 1 for the bottom staff (left hand) of a part with 2 staves
		 * @return true if the staff in the part should be played
		 */
		boolean getPartStaffEnabled(int partIndex, int staffIndex);

		/**
		 * return the program change value for the instrument track
		 *
		 * @param partIndex the 0-based index of the part
		 * @return the MIDI program change value to use for this part
		 */
		int getPartMIDIInstrument(int partIndex);

		/**
		 * return true if the metronome should be played (ie tick notes output into the MIDI file)
		 *
		 * @return true to output metronome tick notes into the MIDI file
		 */
		boolean getMetronomeEnabled();

		/**
		 * get the MIDI percussion instrument key (in channel 10, one of the values in "Appendix 1.5 - General MIDI Percussion Key Map" in MIDI spec. 1.1)
		 *
		 * @return the key [0, 35 .. 71], 0 to use the default value
		 */
		int getMidiKeyForMetronome();

		/**
		 * get the relative volume to use for the part
		 *
		 * @param partIndex the 0-based index of the part
		 * @return the volume [0.0 .. 1.0]
		 */
		float getPartVolume(int partIndex);

		/**
		 * get the relative volume to use for the metronome in the midi file
		 *
		 * @return the volume [0.0 .. 1.0]
		 */
		float getMetronomeVolume();
	}

	/**
	 * construct PlayData
	 *
	 * @param score     the score
	 * @param userTempo an implementation of the UserTempo interface allowing the user eg with a slider
	 *                  to define the tempo, or tempo scaling
	 * @throws ScoreException on error
	 */
	public PlayData(SScore score, UserTempo userTempo) throws ScoreException {
		this.score = score;
		this.numBars = score.numBars();
		this.userTempo = userTempo;
		this.nativePointer = getNativePointer(score, userTempo);
		this.loopStart = this.loopBack = -1;
		this.numRepeats = 0;
	}

	/**
	 * construct PlayData
	 *
	 * @param score      the score
	 * @param userTempo  an implementation of the UserTempo interface allowing the user eg with a slider
	 *                   to define the tempo, or tempo scaling
	 * @param loopStart  the index of the first bar to play in each loop
	 * @param loopBack   the index of the last bar to play in each loop
	 * @param numRepeats the number of times to repeat the loop
	 * @throws ScoreException on error
	 */
	public PlayData(SScore score, UserTempo userTempo, int loopStart, int loopBack, int numRepeats) throws ScoreException {
		this.score = score;
		this.numBars = score.numBars();
		this.userTempo = userTempo;
		this.nativePointer = getNativePointer(score, userTempo);
		this.loopStart = loopStart;
		this.loopBack = loopBack;
		this.numRepeats = numRepeats;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (Bar bar : this) {
			buffer.append(bar.toString());
			buffer.append('\n');
			for (int partIndex = 0; partIndex < numParts(); ++partIndex)
			{
				Part part = bar.part(partIndex);
				buffer.append(" ");
				buffer.append(part.toString());
				buffer.append('\n');
				for (Note note : part) {
					buffer.append('\t');
					buffer.append(note.toString());
					buffer.append('\n');
				}
			}
		}
		return buffer.toString();
	}

	/**
	 * get an iterator to the set of bars in the score.
	 * The iterator will start at the first bar and sequence through all
	 * bars in playing order accounting for repeats, DC.DS etc
	 * 
	 * @return the iterator
	 */
	public native BarIterator iterator();

	/**
	 * get the number of playing parts in the score
	 * 
	 * @return the number of playing parts
	 */
	public native int numParts();

    /**
     * the number of bars in the score;
     */
    public final int numBars;

	/**
	 * get the maximum value of any sound dynamic in any bar. This allows note dynamic values to be scaled accordingly
	 * 
	 * @return the maximum dynamic
	 */
	public native float maxSoundDynamic();
	
	/**
	 * is the first bar an 'up-beat' or anacrusis partial bar?
	 * 
	 * @return true if the first bar is missing the first beat (anacrusis)
	 */
	public native boolean firstBarAnacrusis();
	
	/**
	 * generate a MIDI file from the play data
	 *
	 * @param midiFilePath the full pathname of the MIDI file to create
	 * @return false if failed
	 */
	public native boolean createMIDIFile(String midiFilePath);
	
	/**
	 * generate a MIDI file from the play data with control of enabled parts
	 *
	 * @param midiFilePath the full pathname of the MIDI file to create
	 * @param controls define which parts should be output to the file
	 * @return false if failed
	 */
	public native boolean createMIDIFileWithControls(String midiFilePath, PlayControls controls);
	
	/**
	 * scale the tempo in the MIDI file by writing tempo-defining bytes into it
	 * NB This assumes that the file was written by createMIDIFile and by the same version of the SeeScoreLib
     *
	 * @param midiFilePath the full pathname of the MIDI file
	 * @param tempoScaling the scaling (1.0 is unscaled)
	 */
	public static native void scaleMIDIFileTempo(String midiFilePath, float tempoScaling);

	/**
	 * notification that the tempo is changed in the UI (eg slider moved)
	 */
	public native void updateTempo();
	
	protected native void finalize();
	private static native long getNativePointer(SScore score, UserTempo userTempo);
	private final SScore score;
	private final UserTempo userTempo;
	private final int loopStart;
	private final int loopBack;
	private final int numRepeats;
	private final long nativePointer;
}
