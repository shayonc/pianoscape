package piano.pianotrainer.scoreModels;

/**
 * Created by Shubho on 2018-03-03.
 */

public class Line {
    public double x1;
    public double y1;
    public double x2;
    public double y2;

    public Line(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double getSlope() {
        if (Math.abs(x1 - x2) < 2)
            return (double)Integer.MAX_VALUE;
        else
            return ((y2-y1)/(x2-x1));
    }

    public double getIntercept() {
        if (Math.abs(x1 - x2) < 2)
            return (double)Integer.MIN_VALUE;
        else
            return (y1-((this.getSlope())*x1));
    }

    public double getEuclidianDist() {
        return Math.sqrt(((x2-x1)*(x2-x1)) + ((y2-y1)*(y2-y1)));
    }

    public boolean existsInXRegion(double xMin, double xMax) {
        double curXMin = x1;
        double curXMax = x2;
        if (x1 > x2) {
            curXMin = x2;
            curXMax = x1;
        }

        if (curXMin <= xMin && curXMax >= xMax) return true;
        else if (curXMin <= xMin && curXMax <= xMax && curXMax >= xMin) return true;
        else if (curXMin >= xMin && curXMin <= xMax && curXMax >= xMax) return true;
        else return false;
    }
}
