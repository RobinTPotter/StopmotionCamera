package robin.stopmotion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FilenameFilter;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by potterr on 28/07/2016.
 */
public class SquashedImagePanel extends View {

    private static String LOGTAG = "StopmotionCameraLog-SquashedImagePanel";
    File directory;
    String[] images;
    Bitmap[] bmps;
    boolean[] selected;
    
    int cc = 3;
    int rr = 4;

    public SquashedImagePanel(Context context) {
        super(context);
    }

    public SquashedImagePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquashedImagePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void draw(Canvas c) {

        super.draw(c);
        Paint p = new Paint();
        p.setColor(Color.GREEN);
        c.drawRect(new Rect(0, 0, getWidth(), getHeight()), p);

        int ww = getWidth() / cc;
        int hh = getHeight() / rr;

        if (bmps == null) return;

        int ii = 0;

        for (int ccc = 0; ccc < cc; ccc++) {

            for (int rrr = 0; rrr < rr; rrr++) {

                if (ii < bmps.length) {
                    if (bmps[ii] != null)
                        c.drawBitmap(bmps[ii], new Rect(0, 0, bmps[ii].getWidth(), bmps[ii].getHeight()), new Rect(ccc * ww, rrr * hh, ccc * ww + ww, rrr * hh + hh), p);
                }
                ii++;

            }

        }


    }

    public void setDirectory(File d) {
        directory = d;
        images = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".jpg");
            }
        });

        Arrays.sort(images, Collections.reverseOrder());

        Log.d(LOGTAG, "images " + images.length);

        bmps = new Bitmap[images.length];
        for (int ii = 0; (ii < images.length) && ii<(rr*cc); ii++) {

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Log.d(LOGTAG, "loading " + images[ii]);
            Bitmap pic = BitmapFactory.decodeFile(directory + "/" + images[ii], bmOptions);
            Log.d(LOGTAG, pic.toString());
            bmps[ii] = Bitmap.createScaledBitmap(pic, pic.getWidth() / 10, pic.getHeight() / 10, false);
        }


    }

    public String[] getImages() {
        return images;
    }

}
