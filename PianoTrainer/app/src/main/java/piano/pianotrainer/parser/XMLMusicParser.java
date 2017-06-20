package piano.pianotrainer.parser;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

/**
 * Created by Matthew on 6/18/2017.
 */

public class XMLMusicParser {

    List<String> fileList;
    private String filename;
    public XMLMusicParser(String filename) throws IOException {
        this.filename = filename;
    }

    public String[] parseMXL() {
        try {
            File fXmlFile = new File(filename);     // path to directory
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
