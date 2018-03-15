package piano.pianotrainer.scoreImport;

import org.opencv.core.Point;

import java.util.Map;

import piano.pianotrainer.scoreModels.ElementType;
import piano.pianotrainer.scoreModels.Rest;

/**
 * Created by Ekteshaf on 2018-03-05.
 */

public final class SymbolMapper {
    public static boolean isClef(int label){
        return label / 10 == 1;
    }

    //Rests
    public static Rest classifyRest(int label, boolean isTrebleClef){
        int clef;
        if(isTrebleClef){
            clef = 0;
        }
        else{
            clef = 1;
        }
        Rest r = new Rest(getRestWeight(label), clef);
        return r;
    }

    public static float getRestWeight(int label){
        switch (label){
            case KnnLabels.ONE_SIXTEENTH_REST:
                return (float)0.0625;
            case KnnLabels.EIGHTH_REST:
                return (float)0.125;
            case KnnLabels.QUARTER_REST:
                return (float)0.25;
            case KnnLabels.HALF_REST:
                return (float)0.5;
            case KnnLabels.WHOLE_REST:
                return (float) 1.0;
            default:
                return (float)-1;
        }
    }

    public static boolean isRest(int label){
        return label/10 == 1;
    }

    //Accidentals
    public static boolean isAcc(int label){
        return label/10 == 2;
    }

    //Timing

    public static boolean isTimeSig(int label){
        return label / 10 == 3;
    }

    public static int getUpperTimeSig(int label){
        switch (label){
            case KnnLabels.TIME_C:
                return 4;
            case KnnLabels.TIME_22:
                return 2;
            case KnnLabels.TIME_24:
                return 2;
            case KnnLabels.TIME_34:
                return 3;
            case KnnLabels.TIME_44:
                return 4;
            case KnnLabels.TIME_68:
                return 6;
            default:
                return -1;
        }
    }

    public static int getLowerTimeSig(int label){
        switch (label){
            case KnnLabels.TIME_C:
                return 4;
            case KnnLabels.TIME_22:
                return 2;
            case KnnLabels.TIME_24:
                return 4;
            case KnnLabels.TIME_34:
                return 4;
            case KnnLabels.TIME_44:
                return 4;
            case KnnLabels.TIME_68:
                return 8;
            default:
                return -1;
        }
    }




}
