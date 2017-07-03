package piano.pianotrainer.parser;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import piano.pianotrainer.model.Note;

public class XMLMusicParser {

    private static final String TAG = "XMLMusicParser";

    List<Note> NoteList = new ArrayList<>();
    private String mxlFilePath;
    private String xmlFilePath;
    private String outputFolder;
    private ZipInputStream zis;

    /*
        Constructor
     */
    public XMLMusicParser(String filename, String outputFolder) throws IOException {
        this.mxlFilePath = getSdCardPath() + "Piano" + File.separator + filename + ".mxl";
        this.outputFolder = getSdCardPath() + "Piano" + File.separator + outputFolder;
        this.xmlFilePath = getSdCardPath() + "Piano" + File.separator + "XMLfiles" + File.separator + filename + ".xml";
    }

    /*
        First method invocation
     */
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
            File fXmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xmlDoc = dBuilder.parse(fXmlFile);
            xmlDoc.getDocumentElement().normalize();

            Log.d(TAG, xmlDoc.getDocumentElement().getNodeName()); // remove this

            // Element names
            Node scorePartwise;
            Node part;
            Element mElement = null;
            NodeList measures;
            Node attributes;

            // temp variables for print and attributes
            String tPrint;
            int tDivision;
            int tFifths;
            String tMode;
            char tClef;
            String tSign;
            int tLine;

            scorePartwise  = xmlDoc.getElementsByTagName("score-partwise").item(0);
            // should only have 1 node
            if (scorePartwise.getNodeType() == Node.ELEMENT_NODE) {
                mElement = (Element) scorePartwise;
            }

            // only 1 part
            part = mElement.getElementsByTagName("part").item(0);

            Element temp = (Element) part;

            measures = temp.getElementsByTagName("measures");

            for (int j = 0; j < measures.getLength(); j++) {
                attributes = measures.item(j);
            }


        // TODO - </part> only 1 part plz
            // TODO - </measure>
                // TODO - </print> - this tells if page number or next line
                // TODO - </attributes>
                    // TODO - </division>
                    // TODO - </key>
                        // TODO - </fifths>
                        // TODO - </mode>
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
                    // TODO - </duration>
                    // TODO - </voice>
                    // TODO - </stem> which way the stick points
                    // TODO - </type>
                    // TODO - </accidental> sharps and flats
                    // TODO - </staff> treble 1 and bass 2
                    // TODO - </beam>
                    // TODO - </notation> - slurs, stacattos and stuff
                // TODO - </backup> -- coordinate multiple voices

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