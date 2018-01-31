package piano.pianotrainer.parser;

import android.os.Environment;
import android.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Dictionary;
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
    private String directoryPath;
    private String mxlFilePath;
    private String xmlFilePath;
    private String outputFolder;
    private ZipInputStream zis;

    /* Constructor */
    public XMLMusicParser(String filename, String rootFolder, String outputFolder) throws IOException {
        this.directoryPath = getSdCardPath() + rootFolder;
        this.mxlFilePath = getSdCardPath() + rootFolder + File.separator + filename + ".mxl";
        this.outputFolder = getSdCardPath() + rootFolder + File.separator + outputFolder;
        this.xmlFilePath = getSdCardPath() + rootFolder + File.separator + outputFolder + File.separator + filename + ".xml";

    }

    public List<String> getMxlFiles(){
        List<String> mxlFiles = new ArrayList<String>();
        try {
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdir();
            }
            File[] files = directory.listFiles();
            for (File file: files){
                if (file.isFile() && file.getPath().endsWith(".mxl")) {
                    mxlFiles.add(file.getName().substring(0, file.getName().lastIndexOf(".")));
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return mxlFiles;
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
        NoteList.clear();
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

                String measureNumber = "";
                String print = "";
                int divisions = 1;
                int fifths = -1;
                String mode = "";
                int beats = 4;
                int beattype = 4;
                int staves = -1;
                String sign = "";
                int line = -1;
                boolean chord = false;
                boolean grace = false;
                String step = "";
                int alter = 0;
                int octave = -25;
                int voice = -1;
                String stem = "";
                String type = "";
                String accidental = "";
                int staff = -1;
                int duration = -1;
                boolean rest = false;
                boolean forward = false;
                boolean tieStart = false;
                boolean tieStop = false;

                boolean bmeasure = false;
                boolean bprint = false;
                boolean battributes = false;
                boolean bdivisions = false;
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
                boolean bduration = false;
                boolean brest = false;
                boolean bforward = false;
                boolean btieStart = false;
                boolean btieStop = false;

                public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException {
                    if (qName.equalsIgnoreCase("measure")) {
                        bmeasure = true;
                        for (int ii = 0; ii < attributes.getLength(); ii++) {
                            if (attributes.getQName(ii).equals("number")) {
                                measureNumber = attributes.getValue(ii);
                            }
                        }
                    }
                    if (qName.equalsIgnoreCase("print")) {
                        bprint = true;
                        for (int ii = 0; ii < attributes.getLength(); ii++) {
                            if (attributes.getQName(ii).equals("new-system") || attributes.getQName(ii).equals("page-number")) {
                                print = attributes.getValue(ii);
                            }
                        }
                    }
                    if (qName.equalsIgnoreCase("tie")) {
                        for (int ii = 0; ii < attributes.getLength(); ii++) {
                            if (attributes.getQName(ii).equals("type")) {
                                if (attributes.getValue(ii).equals("start")){tieStart = true;}
                                if (attributes.getValue(ii).equals("stop")){tieStop = true;}
                            }
                        }
                    }
                    if (qName.equalsIgnoreCase("attributes")) {
                        battributes = true;
                    }
                    if (qName.equalsIgnoreCase("divisions")) {
                        bdivisions = true;
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
                    if (qName.equalsIgnoreCase("duration")) {
                        bduration = true;
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
                    if (qName.equalsIgnoreCase("rest")) {
                        brest = true;
                    }

                    if (qName.equalsIgnoreCase("note")||qName.equalsIgnoreCase("forward")) {
                        Note note = new Note();

                        //check own type, forward counts as note (invisible rest)
                        if (qName.equalsIgnoreCase("forward")) {
                            forward = true;
                        }

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
                        note.setStaff(staff);
                        note.setDuration(duration);
                        note.setRest(rest);
                        note.setForward(forward);
                        note.setTieStart(tieStart);
                        note.setTieStop(tieStop);

                        NoteList.add(note);
                        // reset note specific attributes
                        chord = false; // set back to false after note object create
                        grace = false; // set back to false after note object create
                        rest = false;
                        forward = false;
                        tieStart = false;
                        tieStop = false;
                        step = "";
                        alter = -99;
                        octave = -99;
                        voice = -99;
                        stem = "";
                        type = "";
                        accidental = "";
                        staff = -99;
                        duration = -99;
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
                    if (bdivisions) {
                        divisions = Integer.parseInt(new String(ch, start, length));
                        bdivisions = false;
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
                    if (bduration) {
                        duration = Integer.parseInt(new String(ch, start, length));
                        bduration = false;
                    }
                    if (brest) {
                        rest = true;
                        brest = false;
                    }
                    if (bforward){
                        forward=true;
                        bforward=false;
                    }
                    if (btieStart){
                        tieStart = true;
                        btieStart = false;
                    }
                    if (btieStop){
                        tieStop = true;
                        btieStop = false;
                    }
                }
            };

            File file = new File(xmlFilePath);
            InputStream inputStream= new FileInputStream(file);
            Reader reader = new InputStreamReader(inputStream,"UTF-8");

            InputSource is = new InputSource(reader);
            is.setEncoding("UTF-8");
            Log.d(TAG, String.valueOf(NoteList.size()));
            saxParser.parse(is, handler);

            return NoteList;
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
        return NoteList;
    }

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }
}