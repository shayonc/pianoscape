package piano.pianotrainer.scoreModels;

import android.graphics.Rect;
import android.util.Log;

import org.opencv.core.Point;

/**
 * Created by Shubho on 2017-12-27.
 */

public class Note {
    public double weight;
    public boolean hasDot;
    public Accidental accidental;
    public Pitch pitch;
    public int scale;
    // 0 refers to treble clef, 1 refers to bass clef
    public int clef;
    // position of line w.r.t the note circle
    // [none, top-left, top-right, bottom-left, bottom-right] = [0,1,2,3,4]
    public int linePosition;
    public boolean hasStaccato;
    public Point circleCenter;
    public double circleRadius;
    public double blackRatio;
    public boolean hasTieStart;
    public boolean hasTieEnd;

    public Note(double weight, boolean hasDot, Accidental accidental, Pitch pitch, int clef, int linePosition, boolean hasStaccato) {
        this.weight = weight;
        this.hasDot = hasDot;
        this.accidental = accidental;
        this.pitch = pitch;
        this.clef = clef;
        this.linePosition = linePosition;
        this.hasStaccato = hasStaccato;
    }

    public boolean appendDot(Rect notegroupRect, Rect dot){
        double maxDeviationStaccato = 10;
        double maxDeviationDot = 20; //anything larger indicates an unhandled edge case
        int circPosX = notegroupRect.left + (int)circleCenter.x;
        int circPosY = notegroupRect.top + (int)circleCenter.y;
        Log.d("Note", String.format("Appending dot %d with note circle center of %d", dot.centerX(), circPosX));
        double xDistance = Math.abs(dot.centerX() - circPosX);
        if(xDistance <= maxDeviationStaccato){
            this.hasDot = true;
            this.hasStaccato = true;
            Log.d("Note", "Staccato");
        }
        else if(xDistance > maxDeviationStaccato && xDistance < maxDeviationDot){
            //diagonal to note-head - not a staccato
            this.hasDot = true;
            this.hasStaccato = false;
            Log.d("Note", "2nd case Dot");
        }
        else{
            Log.d("Note", String.format("dot append failed with xdistance of %.2f", xDistance));
            return false;
        }
        return true;
    }

    public Note() { }
}
