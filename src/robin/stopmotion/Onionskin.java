package robin.stopmotion;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Date;

/**
 * Created by potterr on 13/07/2016.
 */
public class Onionskin extends View {

    private static String LOGTAG = "StopmotionCameraLog-Onionskin";
    Bitmap bmp;
    String timeupdate = "not set";
    Bitmap bck;

    int opacity = 128;

    public int getOpacity() {
        return opacity;
    }

    public void increaseOpacity() {

        setOpacity(getOpacity() + 24);
        updateBackgound();
        Log.d(LOGTAG, "increaseOpacity");
    }

    public void decreaseOpacity() {

        setOpacity(getOpacity() - 24);
        updateBackgound();

        Log.d(LOGTAG, "decreaseOpacity");

    }

    public void setOpacity(int opacity) {
        if (opacity > 255) opacity = 255;
        if (opacity < 0) opacity = 0;
        this.opacity = opacity;

        Log.d(LOGTAG, "setOpactity " + opacity);
    }

    public void setOpacity() {
        setOpacity(opacity);
    }

    public void setBmp(Bitmap bmp) {
        this.bmp = bmp;
        this.timeupdate = (new Date()).toString();
        invalidate();
        Log.d(LOGTAG, "setBmp");
    }

    public Onionskin(Context context) {
        this(context, null);
    }

    public Onionskin(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.buttonStyle);
    }

    public Onionskin(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Log.d(LOGTAG, "creaed Onionskin");
    }


    public void updateBackgound() {

        setAlpha((float) (opacity) / 255);

        Log.d(LOGTAG, "updateBackground");
        //getBackground().setAlpha(opacity);  // 50% transparent
    }

    public void draw(Canvas c) {


        super.draw(c);

        Paint p = new Paint();
        p.setColor(Color.BLUE);
        // p.setStyle(Paint.Style.STROKE);
        //      p.setStrokeWidth(1.0f);


        if (bmp != null) {

            c.drawBitmap(bmp, new Rect(0, 0, bmp.getWidth(), bmp.getHeight()), new Rect(0, 0, getWidth(), getHeight()), null);

            c.drawText("Hello", 10, 10, p);
            c.drawText(String.valueOf(bmp.getWidth()) + "x" + String.valueOf(bmp.getHeight()), 10, 30, p);
            c.drawText(timeupdate, 10, 50, p);

        }


    }

}
