package piano.pianotrainer.fragments;

/**
 * Created by SL-1 on 04/03/2018.
 */

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import android.graphics.Rect;

import piano.pianotrainer.scoreModels.Accidental;
import piano.pianotrainer.scoreModels.Score;
import piano.pianotrainer.scoreModels.Measure;
import piano.pianotrainer.scoreModels.NoteGroup;
import piano.pianotrainer.scoreModels.Note;
import piano.pianotrainer.scoreModels.Rest;
import org.opencv.core.Point;

public class ScoreImportToXmlParser {
    Score score;
    StringBuilder xmlBuffer;
    static String XML_HEADER_PATH = "xml_parser_templates/musicxml_header.xml";
    static int divsPerBeat = 4;
    int upperTimeSig;
    int lowerTimeSig;


    //store appContext which is helpful for accessing internal file storage if we go that route
    private Context appContext;

    public ScoreImportToXmlParser() {
        score = null;
        appContext = null;
        xmlBuffer = new StringBuilder("Empty\n");
        upperTimeSig = 4;
        lowerTimeSig = 4;
    }

    public void loadScore(Score score, Context context){
        this.score = score;
        appContext = context;
    }

    public void parse() {
        // Start writing to XML buffer, first append header template
        xmlBuffer = new StringBuilder();
        try {
            InputStream json = appContext.getAssets().open(XML_HEADER_PATH);
            BufferedReader br = new BufferedReader(new InputStreamReader(json, "UTF-8"));
            String str;
            while ((str = br.readLine()) != null) {
                xmlBuffer.append(str);
            }
            xmlBuffer.append("\n");
            br.close();
        } catch (IOException e) {
            Log.d("ScoreImportToXmlParser", "IO Error when trying to access file for xml header.");
            e.printStackTrace();
        }

        //start parsing each measure
        int measureNum = 1;
        for (int i = 0; i < score.staffs.size(); i++) {
            for (int j = 0; j < score.staffs.get(i).measures.size(); j++) {
                parseMeasure(score.staffs.get(i).measures.get(j), measureNum);
                measureNum++;
            }
        }

        // Add end of file
        xmlBuffer.append("  </part>\n" +
                "</score-partwise>");
    }

    private void parseMeasure(Measure measure, int measureNum) {
        // If first measure, include some timing and grand staff information
        xmlBuffer.append("    <measure number=\"" + measureNum + "\">\n");
        if (measureNum == 1) {
            upperTimeSig = measure.upperTimeSig;
            lowerTimeSig = measure.lowerTimeSig;
            xmlBuffer.append("      <attributes>\n" +
                    "        <divisions>" + divsPerBeat + "</divisions>\n" +
                    "        <key>\n" +
                    "          <fifths>0</fifths>\n" +
                    "        </key>\n" +
                    "        <time>\n" +
                    "          <beats>" + measure.upperTimeSig + "</beats>\n" +
                    "          <beat-type>" + measure.lowerTimeSig + "</beat-type>\n" +
                    "        </time>\n" +
                    "        <staves>2</staves>\n" +
                    "        <clef number=\"1\">\n" +
                    "          <sign>G</sign>\n" +
                    "          <line>2</line>\n" +
                    "        </clef>\n" +
                    "        <clef number=\"2\">\n" +
                    "          <sign>F</sign>\n" +
                    "          <line>4</line>\n" +
                    "        </clef>\n" +
                    "      </attributes>\n");
        }

        // Split all elements by whether they're in treble or bass clef.
        // This is based on the assumption that the elements' maps are sorted

        List<NoteGroup> trebleNotes = new ArrayList<NoteGroup>();
        List<Rect> trebleNotesRects = new ArrayList<Rect>();
        List<Rest> trebleRests = new ArrayList<Rest>();
        List<Rect> trebleRestsRects = new ArrayList<Rect>();

        List<NoteGroup> bassNotes = new ArrayList<NoteGroup>();
        List<Rect> bassNotesRects = new ArrayList<Rect>();
        List<Rest> bassRests = new ArrayList<Rest>();
        List<Rect> bassRestsRects = new ArrayList<Rect>();

        int trebleCounter = 0;
        for (Map.Entry<Rect, NoteGroup> entry : measure.noteGroups.entrySet()) {
            if(entry.getValue().clef == 1) {
                trebleNotes.add(entry.getValue());
                trebleNotesRects.add(entry.getKey());
                Log.d("XMLParser", String.format("trebleNotes num %d, xPos: %d", trebleCounter, entry.getKey().left));
            } else {
                bassNotes.add(entry.getValue());
                bassNotesRects.add(entry.getKey());
                Log.d("XMLParser", String.format("bassNotes num %d, xPos: %d", trebleCounter, entry.getKey().left));
            }
            trebleCounter++;
        }

        int restCounter = 0;
        for (Map.Entry<Rect, Rest> entry : measure.rests.entrySet()) {
            if(entry.getValue().clef == 0) {
                trebleRests.add(entry.getValue());
                trebleRestsRects.add(entry.getKey());
                Log.d("XMLParser", String.format("trebleRests num %d, xPos: %d", restCounter, entry.getKey().left));
            } else {
                bassRests.add(entry.getValue());
                bassRestsRects.add(entry.getKey());
                Log.d("XMLParser", String.format("basseRests num %d, xPos: %d", restCounter, entry.getKey().left));
            }
            restCounter++;
        }

        //populate voices based on clefs and positions. First do treble, back up, then bass
        parseStaff(trebleNotes, trebleRests, trebleNotesRects, trebleRestsRects, 1, 2, measure);
        xmlBuffer.append("      <backup>\n" +
                "        <duration>" + divsPerBeat*upperTimeSig + "</duration>\n" +
                "      </backup>\n");
        parseStaff(bassNotes, bassRests, bassNotesRects, bassRestsRects, 6, 1, measure);

        // END OF MEASURE
        xmlBuffer.append("    </measure>\n");
    }

    /* Takes in lists of notes and rests with their positions and voice to start at.
     * Used for treble/bass staves. */
    private void parseStaff(List<NoteGroup> noteGroups, List<Rest> rests,
                            List<Rect> groupRects, List<Rect> restRects, int voice0, int staff, Measure measure){
        int restPos = 0;
        int notePos = 0;
        int[] voiceCounter = new int[5];    // keeps track of where to add forwards for voices.
        for(int voiceCount : voiceCounter) { voiceCount = 0;}
        while(restPos < rests.size() || notePos < noteGroups.size()) {
            // Check relative position of next rest and notegroup on lists, populate whichever one is more left.
            // Working with rest...
            if (restPos < rests.size() && (notePos >= noteGroups.size() ||
                    restRects.get(restPos).left < groupRects.get(notePos).left)) {
                // Assuming always uses voice0 (will require change here if increasing complexity)
                xmlBuffer.append("      <note>\n" +
                        "        <rest/>\n" +
                        "        <duration>" + (int)(rests.get(restPos).weight*divsPerBeat*lowerTimeSig) + "</duration>\n" +
                        "        <voice>" + voice0 + "</voice>\n" +
                        "        <type>" + getNoteType(rests.get(restPos).weight) + "</type>\n" +
                        "        <staff>" + staff + "</staff>\n" +
                        "      </note>\n");
                // increment all other voice numbers aside from 1.
                for (int i = 1; i < voiceCounter.length; i++){voiceCounter[i]++;}
                restPos++;
            } else {    // Else working with notegroup
                double previousX = -100;
                double maxOffset = 1;
                for(Note note : noteGroups.get(notePos).notes) {
                    xmlBuffer.append("      <note>\n");

                    //If X pos really close to previous, add as chord.
                    if (note.circleCenter.x < previousX + maxOffset) {
                        xmlBuffer.append("        <chord/>\n");
                    }
                    // Pitch and octave
                    xmlBuffer.append("        <pitch>\n" +
                            "          <step>" + note.pitch + "</step>\n" +
                            "          <octave>" + note.scale + "</octave>\n");
                    // alters
                    int alter = 0;
                    String accidental = "";
                    if (measure.keySigs.get(note.pitch) == Accidental.Sharp) {
                        alter = 1;
                    } else if (measure.keySigs.get(note.pitch) == Accidental.Flat) {
                        alter = -1;
                    }
                    if (note.accidental == Accidental.Sharp) {
                        alter++;
                        accidental = "sharp";
                    } else if (note.accidental == Accidental.Flat) {
                        alter--;
                        accidental = "flat";
                    } else if (note.accidental == Accidental.Natural) {
                        alter = 0;
                        accidental = "natural";
                    }
                    if (alter != 0) {
                        xmlBuffer.append("          <alter>" + alter + "</alter>\n");
                    }
                    xmlBuffer.append("        </pitch>\n");
                    // Duration
                    xmlBuffer.append("        <duration>" + (int)(note.weight*divsPerBeat*lowerTimeSig) + "</duration>\n");
                    // Ties
                    if (note.hasTieStart) {
                        xmlBuffer.append("        <tie type=\"start\"/>\n");
                    }
                    if (note.hasTieEnd) {
                        xmlBuffer.append("        <tie type=\"stop\"/>\n");
                    }
                    // Voice and notetype
                    xmlBuffer.append("        <voice>" + voice0 + "</voice>\n");
                    xmlBuffer.append("        <type>" + getNoteType(note.weight) + "</type>\n");
                    // dots
                    if (note.hasDot) {
                        xmlBuffer.append("        <dot/>\n");
                    }
                    // Accidental notation
                    if (accidental != "") {
                        xmlBuffer.append("        <accidental>" + accidental + "</accidental>\n");
                    }
                    // Other Notations
                    xmlBuffer.append("        <notations>\n");
                    if (note.hasTieStart) {     // Ties
                        xmlBuffer.append("          <tied type=\"start\"/>\n");
                    }
                    if (note.hasTieEnd) {
                        xmlBuffer.append("          <tied type=\"stop\"/>\n");
                    }
                    if (note.hasStaccato) {     // Staccato
                        xmlBuffer.append("        <articulations>\n");
                        xmlBuffer.append("          <staccato/>\n");
                        xmlBuffer.append("        </articulations>\n");
                    }
                    xmlBuffer.append("        </notations>\n");
                    xmlBuffer.append("        <staff>" + staff + "</staff>\n" +
                            "      </note>\n");
                }
                notePos++;
            }
        }
    }

    private String getNoteType(double weight) {
        if (weight ==   1   ) {return "whole";}
        if (weight ==   0.5   ) {return "half";}
        if (weight ==   0.25   ) {return "quarter";}
        if (weight ==   0.125 ) {return "eighth";}
        if (weight ==   0.0625) {return "16th";}
        if (weight ==   0.03125) {return "32nd";}
        //else
        return "quarter";
    }

    // Write XML to new file
    public void writeXml() {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e("ScoreImportToXmlParser", "No write permission for xml file!");
        }
        String fileContents = xmlBuffer.toString();

        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + File.separator + "Piano" +  File.separator + "XML_Output");
        dir.mkdirs();
        File file = new File(dir, "Converted_XML.xml");
        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.println(fileContents);
            pw.flush();
            pw.close();
            f.close();
            Log.i("ScoreImportToXmlParser", fileContents);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("ScoreImportToXmlParser", "File not found.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}