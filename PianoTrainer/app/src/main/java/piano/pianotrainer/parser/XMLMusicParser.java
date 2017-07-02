package piano.pianotrainer.parser;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

public class XMLMusicParser {

    private static final String TAG = "XMLMusicParser";

    List<String> fileList;
    private String filename;
    private String outputFolder;
    private ZipInputStream zis;
    private Context context;

    /*
        Constructor
     */
    public XMLMusicParser(String filename, String outputFolder) throws IOException {
        this.filename = getSdCardPath() + "Piano" + File.separator + filename;
        this.outputFolder = getSdCardPath() + "Piano" + File.separator +  outputFolder;
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

            FileInputStream fis = new FileInputStream(filename);
            zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

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
        //            File fXmlFile = new File(filename);
//            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//            Document xmlDoc = dBuilder.parse(fXmlFile);
//            xmlDoc.getDocumentElement().normalize();
//
//            NodeList scorePart = xmlDoc.getElementsByTagName("part");
    }

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }

}