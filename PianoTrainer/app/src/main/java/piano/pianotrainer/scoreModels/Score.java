package piano.pianotrainer.scoreModels;

/**
 * Created by Shubho on 2017-11-19.
 * This class represents one page/sheet of the overall music score document
 */

import java.util.*;
import android.graphics.Point;


public class Score {
    String title;                   // score title
    public List<Staff> staffs;             // all staffs of the music score
    Map<Point, String> metaInfo;    // stores random text data across the sheet with its location (currently not used)

    public Score(String scoreTitle) {
        title = scoreTitle;
        staffs = new ArrayList<Staff>();
        metaInfo = new HashMap<Point, String>();
    }

    public void addStaff(Staff staff) {
        staffs.add(staff);
    }
}
