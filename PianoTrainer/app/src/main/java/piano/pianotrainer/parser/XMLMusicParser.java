package piano.pianotrainer.parser;

import android.os.Environment;
import android.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import piano.pianotrainer.model.Note;

public class XMLMusicParser {

    private static final String TAG = "XMLMusicParser";
    List<Note> NoteList = new ArrayList<>();
    private String mxlFilePath;
    private String xmlFilePath;
    private String outputFolder;
    private ZipInputStream zis;

    /* Constructor */
    public XMLMusicParser(String filename, String outputFolder) throws IOException {
        this.mxlFilePath = getSdCardPath() + "Piano" + File.separator + filename + ".mxl";
        this.outputFolder = getSdCardPath() + "Piano" + File.separator + outputFolder;
        this.xmlFilePath = getSdCardPath() + "Piano" + File.separator + "XMLfiles" + File.separator + filename + ".xml";
    }

    /* First method invocation */
    public void parseMXL() {
        try {
            unzipMXL();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void unzipMXL() {

        byte[] buffer = new byte[1024];

        try {
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }

            FileInputStream fis = new FileInputStream(mxlFilePath);
            zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String zeFileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + zeFileName);

                if (newFile.getAbsolutePath().lastIndexOf('.') == -1) {
                    newFile.mkdirs();
                }
                else {
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    public List<Note> parseXML() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {

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
                boolean chord = false;
                boolean grace = false;
                String step;
                int alter;
                int octave;
                int voice;
                String stem;
                String type;
                String accidental;
                int staff;

                boolean bmeasure = false;
                boolean bprint = false;
                boolean battributes = false;
                boolean bdivision = false;
                boolean bkey = false;
                boolean bfifths = false;
                boolean bmode = false;
                boolean btime = false;
                boolean bbeats = false;
                boolean bbeattype = false;
                boolean bstaves = false;
                boolean bclef = false;
                boolean bsign = false;
                boolean bline = false;
                boolean bnote = false;
                boolean bchord = false;
                boolean bgrace = false;
                boolean bpitch = false;
                boolean bstep = false;
                boolean balter = false;
                boolean boctave = false;
                boolean bvoice = false;
                boolean bstem = false;
                boolean btype = false;
                boolean baccidental = false;
                boolean bstaff = false;

                public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException {
                    if (qName.equalsIgnoreCase("measure")) {
                        bmeasure = true;
                        if (attributes.getQName(0).equals("number")) {
                           measureNumber = attributes.getValue(0);
                        }
                    }
                    if (qName.equalsIgnoreCase("print")) {
                        bprint = true;
                        if (attributes.getQName(0).equals("new-system") || attributes.getQName(0).equals("page-number")) {
                            print = attributes.getValue(0);
                        }
                    }
                    if (qName.equalsIgnoreCase("attributes")) {
                        battributes = true;
                    }
                    if (qName.equalsIgnoreCase("division")) {
                        bdivision = true;
                    }
                    if (qName.equalsIgnoreCase("key")) {
                        bkey = true;
                    }
                    if (qName.equalsIgnoreCase("fifths")) {
                        bfifths = true;
                    }
                    if (qName.equalsIgnoreCase("mode")) {
                        bmode = true;
                    }
                    if (qName.equalsIgnoreCase("time")) {
                        btime = true;
                    }
                    if (qName.equalsIgnoreCase("beats")) {
                        bbeats = true;
                    }
                    if (qName.equalsIgnoreCase("beat-type")) {
                        bbeattype = true;
                    }
                    if (qName.equalsIgnoreCase("staves")) {
                        bstaves = true;
                    }
                    if (qName.equalsIgnoreCase("clef")) {
                        bclef = true;
                    }
                    if (qName.equalsIgnoreCase("sign")) {
                        bsign = true;
                    }
                    if (qName.equalsIgnoreCase("line")) {
                        bline = true;
                    }
                    if (qName.equalsIgnoreCase("note")) {
                        bnote = true;
                    }
                    if (qName.equalsIgnoreCase("chord")) {
                        bchord = true;
                    }
                    if (qName.equalsIgnoreCase("grace")) {
                        bgrace = true;
                    }
                    if (qName.equalsIgnoreCase("pitch")) {
                        bpitch = true;
                    }
                    if (qName.equalsIgnoreCase("step")) {
                        bstep = true;
                    }
                    if (qName.equalsIgnoreCase("alter")) {
                        balter = true;
                    }
                    if (qName.equalsIgnoreCase("octave")) {
                        boctave = true;
                    }
                    if (qName.equalsIgnoreCase("voice")) {
                        bvoice = true;
                    }
                    if (qName.equalsIgnoreCase("stem")) {
                        bstem = true;
                    }
                    if (qName.equalsIgnoreCase("type")) {
                        btype = true;
                    }
                    if (qName.equalsIgnoreCase("accidental")) {
                        baccidental = true;
                    }
                    if (qName.equalsIgnoreCase("staff")) {
                        bstaff = true;
                    }
                }

                public void endElement(String uri, String localName,
                                       String qName) throws SAXException {
                    if (qName.equalsIgnoreCase("chord")) {
                        bchord = true;
                    }
                    if (qName.equalsIgnoreCase("grace")) {
                        bgrace = true;
                    }
                    if (qName.equalsIgnoreCase("note")) {
                        Note note = new Note();
                        note.setMeasureNumber(measureNumber);
                        note.setPrint(print);
                        note.setDivisions(divisions);
                        note.setFifths(fifths);
                        note.setMode(mode);
                        note.setBeats(beats);
                        note.setBeattype(beattype);
                        note.setStaves(staves);
                        note.setSign(sign);
                        note.setLine(line);
                        note.setChord(chord);
                        note.setGrace(grace);
                        note.setStep(step);
                        note.setAlter(alter);
                        note.setOctave(octave);
                        note.setVoice(voice);
                        note.setStem(stem);
                        note.setType(type);
                        note.setAccidental(accidental);
                        // TODO - if staff is 1 or 2 put in diff array
                        note.setStaff(staff);

                        chord = false; // set back to false after note object create
                        grace = false; // set back to false after note object create
                    }
                }

                public void characters(char ch[], int start, int length) throws SAXException {
                    if (bmeasure) {
                        bmeasure = false;
                    }
                    if (bprint) {
                        bprint = false;
                    }
                    if (battributes) {
                        battributes = false;
                    }
                    if (bdivision) {
                        divisions = Integer.parseInt(new String(ch, start, length));
                        bdivision = false;
                    }
                    if (bkey) {
                        bkey = false;
                    }
                    if (bfifths) {
                        fifths = Integer.parseInt(new String(ch, start, length));
                        bfifths = false;
                    }
                    if (bmode) {
                        mode = new String(ch, start, length);
                        bmode = false;
                    }
                    if (btime) {
                        btime = false;
                    }
                    if (bbeats) {
                        beats = Integer.parseInt(new String(ch, start, length));
                        bbeats = false;
                    }
                    if (bbeattype) {
                        beattype = Integer.parseInt(new String(ch, start, length));
                        bbeattype = false;
                    }
                    if (bstaves) {
                        staves = Integer.parseInt(new String(ch, start, length));
                        bstaves = false;
                    }
                    if (bclef) {
                        bclef = false;
                    }
                    if (bsign) {
                        sign = new String(ch, start, length);
                        bsign = false;
                    }
                    if (bline) {
                        line = Integer.parseInt(new String(ch, start, length));
                        bline = false;
                    }
                    if (bnote) {
                        bnote = false;
                    }
                    if (bchord) {
                        chord = true;
                        bchord = false;
                    }
                    if (bgrace) {
                        grace = true;
                        bgrace = false;
                    }
                    if (bpitch) {
                        bpitch = false;
                    }
                    if (bstep) {
                        step = new String(ch, start, length);
                        bstep = false;
                    }
                    if (balter) {
                        alter = Integer.parseInt(new String(ch, start, length));
                        balter = false;
                    }
                    if (boctave) {
                        octave = Integer.parseInt(new String(ch, start, length));
                        boctave = false;
                    }
                    if (bvoice) {
                        voice = Integer.parseInt(new String(ch, start, length));
                        bvoice = false;
                    }
                    if (bstem) {
                        stem = new String(ch, start, length);
                        bstem = false;
                    }
                    if (btype) {
                        type = new String(ch, start, length);
                        btype = false;
                    }
                    if (baccidental) {
                        accidental = new String(ch, start, length);
                        baccidental = false;
                    }
                    if (bstaff) {
                        staff = Integer.parseInt(new String(ch, start, length));
                        bstaff = false;
                    }
                }
            };

            saxParser.parse(xmlFilePath, handler);

        // TODO - </part> only 1 part plz
            // TODO - </measure>
                // TODO - </print> - this tells if page number or next line
                // TODO - </attributes>
                    // TODO - </division>
                    // TODO - </key>
                        // TODO - </fifths>
                        // TODO - </mode>
                    // TODO - </time>
                        // TODO - </beats>
                        // TODO - </beat-type>
                    // TODO - </staves>
                    // TODO - </clef>
                        // TODO - </sign>
                        // TODO - </line>
                // TODO - </note>
                    // TODO - <chord/>
                    // TODO - <grace/>
                    // TODO - </pitch>
                        // TODO - </step>
                        // TODO - </alter>
                        // TODO - </octave>

                    // TODO - </duration> watch out!!! nested inside something else

                    // TODO - </voice>
                    // TODO - </stem> which way the stick points
                    // TODO - </type>
                    // TODO - </accidental> sharps and flats
                    // TODO - </staff> treble 1 and bass 2

                    // TODO - </beam>
                    // TODO - </notation> - slurs, stacattos and stuff
                // TODO - </backup> -- coordinate multiple voices
                // TODO - </forward>

            return null;
        }
        catch (ParserConfigurationException pe) {
            pe.printStackTrace();
        }
        catch (SAXException se) {
            se.printStackTrace();
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }
}
