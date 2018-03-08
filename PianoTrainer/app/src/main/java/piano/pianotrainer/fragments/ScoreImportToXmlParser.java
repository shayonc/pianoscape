package piano.pianotrainer.fragments;

/**
 * Created by SL-1 on 04/03/2018.
 */

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import android.graphics.Rect;

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

    //store appContext which is helpful for accessing internal file storage if we go that route
    private Context appContext;

    public void loadScore(Score score, Context context){
        this.score = score;
        appContext = context;
    }

    public void Parse() {
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
        xmlBuffer.append("    <measure number=\"" + measureNum + ">\n");
        if (measureNum == 1) {
            xmlBuffer.append("      <attributes>\n" +
                    "        <divisions>" + divsPerBeat + "</divisions>\n" +
                    "        <key>\n" +
                    "          <fifths>0</fifths>\n" +
                    "        </key>\n" +
                    "        <time>\n" +
                    "          <beats>" + measure.upperSig + "</beats>\n" +
                    "          <beat-type>" + measure.lowerSig + "</beat-type>\n" +
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

        for (Map.Entry<Rect, NoteGroup> entry : measure.noteGroups.entrySet()) {
            if(entry.getValue().clef == 1) {
                trebleNotes.add(entry.getValue());
                trebleNotesRects.add(entry.getKey());
            } else {
                bassNotes.add(entry.getValue());
                bassNotesRects.add(entry.getKey());
            }
        }

        for (Map.Entry<Rect, Rest> entry : measure.rests.entrySet()) {
            if(entry.getValue().clef == 1) {
                trebleRests.add(entry.getValue());
                trebleRestsRects.add(entry.getKey());
            } else {
                bassRests.add(entry.getValue());
                bassRestsRects.add(entry.getKey());
            }
        }

        //populate voices based on clefs and positions. First do treble, back up, then bass
        parseStaff(trebleNotes, trebleRests, trebleNotesRects, trebleRestsRects, 1, 1);
        xmlBuffer.append("      <backup>\n" +
                "        <duration>" + divsPerBeat*measure.lowerSig + "</duration>\n" +
                "      </backup>\n");
        parseStaff(bassNotes, bassRests, bassNotesRects, bassRestsRects, 6, 2);

        // END OF MEASURE
        xmlBuffer.append("    </measure>\n");
    }

    /* Takes in lists of notes and rests with their positions and voice to start at.
     * Used for treble/bass staves. */
    private void parseStaff(List<NoteGroup> noteGroups, List<Rest> rests,
                            List<Rect> groupRects, List<Rect> restRects, int voice0, int staff){
        int restPos = 0;
        int notePos = 0;
        int[] voiceCounter = new int[5];    // keeps track of where to add forwards for voices.
        for(int voiceCount : voiceCounter) { voiceCount = 0;}
        while(restPos < rests.size() || notePos < noteGroups.size()) {
            // Check relative position of next rest and notegroup on lists, populate whichever one is more left.
            // Working with rest...
            if (notePos >= noteGroups.size() || restRects.get(restPos).left < groupRects.get(notePos).left) {
                // Assuming always uses voice0 (will require change here if increasing complexity)
                xmlBuffer.append("      <note>\n" +
                        "        <rest/>\n" +
                        "        <duration>" + (int)(rests.get(restPos).weight*divsPerBeat) + "</duration>\n" +
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
                    xmlBuffer.append("        <duration>" + note.weight*divsPerBeat + "</duration>\n" +
                            "        <voice>" + voice0 + "</voice>\n" +
                            "        <type>" + getNoteType(note.weight) + "</type>\n" +
                            "        <staff>" + staff + "</staff>\n" +
                            "      </note>\n");
                }
                notePos++;
            }
        }
    }

    private String getNoteType(double weight) {
        if (weight ==   4   ) {return "whole";}
        if (weight ==   2   ) {return "half";}
        if (weight ==   1   ) {return "quarter";}
        if (weight ==   0.5 ) {return "eighth";}
        if (weight ==   0.25) {return "16th";}
        if (weight ==   0.125) {return "32nd";}
        //else
        return "quarter";
    }

    // Write XML to new file
    public void writeXml() {
        String filename = "Converted_Xml_Output.xml";
        String fileContents = xmlBuffer.toString();
        FileOutputStream outputStream;

        try {
            outputStream = appContext.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (IOException e) {
            Log.d("ScoreImportToXmlParser", "IO Error when trying to access file to write for output xml.");
            e.printStackTrace();
        }
    }
}

class NoteOrRest{
    Note note;
    Rest rest;

}