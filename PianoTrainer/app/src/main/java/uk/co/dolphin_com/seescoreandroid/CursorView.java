package uk.co.dolphin_com.seescoreandroid;


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import uk.co.dolphin_com.sscore.CursorRect;
import uk.co.dolphin_com.sscore.SSystem;

public class CursorView extends View {

    /**
     * type of cursor
     */
    static enum CursorType
    {
        /** no cursor */
        none,

        /** vertical line cursor */
        line,

        /** rectangular cursor around bar */
        box
    }

    /**
     * interface to return the scroll offset from the ScrollView
     */
    public interface OffsetCalculator {
        public float getScrollY();
    }

    private static int kCursorColour = 0xFF0000;
    private static float kCursorAlpha = 0.5F;
    private static int kCursorLineWidth = 5;
    private static int kAnimateDuration = 150;

    private final Paint cursorPaint;
    private Context context;
    private OffsetCalculator offsetCalc;

    private ValueAnimator animator = new ValueAnimator();

    private float current_cursor_xpos = 0.0f;

    private int cursorBarIndex;
    private SSystem system;
    private float systemViewTop;
    private float previousXPos;
    private float destinationXPos;
    private CursorType cursorType = CursorType.none;

    private int viewHeight = 0;
    private int viewWidth = 0;

    /**
     * Creates a new cursor view corresponding to the given SeeScoreView.
     * @param context context
     * @param offsetCalc interface to get the current scroll offset
     */
    public CursorView(Context context, OffsetCalculator offsetCalc) {
        super(context);
        this.context = context;
        this.offsetCalc = offsetCalc;
        cursorPaint = new Paint();
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setColor(kCursorColour);
        cursorPaint.setAlpha((int)(kCursorAlpha*255));
        cursorPaint.setStrokeWidth(kCursorLineWidth);
    }

    /**
     * called by android to measure this view
     * @param widthMeasureSpec the view's width
     * @param heightMeasureSpec the view's height
     */
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {

        if (heightMeasureSpec > 0) {
            viewHeight = heightMeasureSpec;
            viewWidth = widthMeasureSpec;
        }
        setMeasuredDimension(viewWidth, viewHeight);
    }

    public void clear() {
        this.cursorType = CursorType.none;
        destinationXPos = 0;
        invalidate();
    }

    /**
     * called by SystemView to place the cursor at the beginning of the bar
     *
     * @param systemViewTop the top of the SystemView which the cursor is to be placed over
     * @param system the system which the cursor is to be placed over
     * @param cursorBarIndex the index of the bar where the cursor is to be shown
     * @param cursorType the type of the cursor
     */
    public void placeCursorAtBar (float systemViewTop, SSystem system, int cursorBarIndex, CursorType cursorType) {
        this.systemViewTop = systemViewTop;
        this.system = system;
        this.cursorBarIndex = cursorBarIndex;
        this.cursorType = cursorType;
        destinationXPos = 0;
        requestLayout();
        invalidate();
    }


    /**
     * animate the cursor from its current position to the given xpos
     * @param systemViewTop the top of the SystemView which the cursor is to be placed over
     * @param system the system which the cursor is to be placed over
     * @param cursorBarIndex the index of the bar where the cursor is to be shown
     * @param destinationXPos the x position where the cursor will arrive at completion of the animation
     */
    public void animateCursor(float systemViewTop, SSystem system, int cursorBarIndex, float destinationXPos) {
        this.systemViewTop = systemViewTop;
        this.system = system;
        this.cursorBarIndex = cursorBarIndex;
        this.destinationXPos = destinationXPos;
        if (previousXPos < destinationXPos)
            animator.setFloatValues(previousXPos, destinationXPos);
        else {
            animator.setFloatValues(100, destinationXPos);
        }
        animator.setDuration(kAnimateDuration);
        animator.removeAllUpdateListeners();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                current_cursor_xpos = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    /**
     * called by android to draw the View
     * @param canvas the canvas
     */
    @Override
    protected void onDraw(final Canvas canvas) {
        if (cursorType != CursorType.none) {
            float deltaY = systemViewTop - offsetCalc.getScrollY();
            CursorRect cursorRect = system.getCursorRect(canvas, cursorBarIndex);
            if (cursorType == CursorType.box) {
                canvas.drawRect(new RectF(cursorRect.rect.left,cursorRect.rect.top + deltaY, cursorRect.rect.right, cursorRect.rect.bottom + deltaY), cursorPaint);
            }
            else if (destinationXPos == 0) {
                canvas.drawLine(cursorRect.rect.left,cursorRect.rect.top + deltaY, cursorRect.rect.left, cursorRect.rect.bottom + deltaY, cursorPaint);
                previousXPos = cursorRect.rect.left;
            }
            else {
                canvas.drawLine(current_cursor_xpos, cursorRect.rect.top + deltaY, current_cursor_xpos, cursorRect.rect.bottom + deltaY, cursorPaint);
                previousXPos = destinationXPos;
            }
        }
    }

}
