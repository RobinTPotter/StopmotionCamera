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
    private static int OPACITY_INCREMENT = 64;
    private static int SKINS_MIN_ALPHA = 64;
    private static int numSkins = 3;
    Bitmap bmp;
    String timeupdate = "not set";
    Bitmap skins[];

    int opacity = 128;

    public int getOpacity() {
        return opacity;
    }

    @Override
    public void invalidate() {

        super.invalidate();
        Log.d(LOGTAG, "Invalidated Onionskin");
    }

    public void increaseOpacity() {

        setOpacity(getOpacity() + OPACITY_INCREMENT);
        updateBackgound();
        Log.d(LOGTAG, "increaseOpacity");
    }

    public void decreaseOpacity() {

        setOpacity(getOpacity() - OPACITY_INCREMENT);
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

        for (int ss = numSkins - 1; ss > 0; ss--) {
            skins[ss] = skins[ss - 1];
        }
        skins[0] = bmp;

        // skins[2] = skins[1];
        // skins[1] = skins[0];
        // skins[0] = bmp;

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
        skins = new Bitmap[numSkins];

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

        c.drawARGB(0, 0, 0, 0);

        int op = 255;
        int dec = (255 - SKINS_MIN_ALPHA) / numSkins;


        int skin = 0;

        for (int ss = numSkins - 1; ss >= 0; ss--) {

            Bitmap _bmp = skins[ss];
            skin++;
            Paint trans = new Paint();
            trans.setAlpha(op);
            op-=dec;
            Log.d(LOGTAG, "bmp with paint " + trans.getAlpha() + " " + trans.getColor());

            if (_bmp != null) {
                try {
                    c.drawBitmap(_bmp, new Rect(0, 0, _bmp.getWidth(), _bmp.getHeight()),
                            new Rect(0, 0, getWidth(), getHeight()), trans);
                    c.drawText(String.valueOf(skin), 30, 50 + (skin * 20), p);
                    c.drawText(String.valueOf(_bmp.getWidth()) + "x" +
                            String.valueOf(_bmp.getHeight()), 10, 30, p);
                    c.drawText(timeupdate, 10, 50, p);

                } catch (Exception ex) {
                    Toast.makeText(Onionskin.this.getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }
    }

}

