package piano.pianotrainer.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Matthew on 6/18/2017.
 */

public class XMLMusicParser {
    public XMLMusicParser(String filename) throws IOException {
        File input = new File(filename);

        InputStream is = new FileInputStream(filename);
    }
}
