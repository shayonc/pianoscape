package piano.pianotrainer.parser;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

public class XMLMusicParser {

    private static final String TAG = "XMLMusicParser";

    List<String> fileList;
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
            parseXML();
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

    public void parseXML() {
        try {
            File fXmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xmlDoc = dBuilder.parse(fXmlFile);
            xmlDoc.getDocumentElement().normalize();

            NodeList scorePart = xmlDoc.getElementsByTagName("score-partwise");
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
    }

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }

}