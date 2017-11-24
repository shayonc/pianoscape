package piano.pianotrainer.model;

import java.util.Date;

/**
 * Created by Matthew on 11/23/2017.
 */

public class MusicFile {
    private String filename;
    private Date dateModified;
    private Integer thumbnail;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Date getDateModified() {
        return dateModified;
    }

    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    public Integer getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Integer thumbnail) {
        this.thumbnail = thumbnail;
    }
}
