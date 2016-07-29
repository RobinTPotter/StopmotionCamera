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
import android.widget.SeekBar;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by potterr on 28/07/2016.
 */
public class SquashedPreview extends View {

    File directory;
    private static String LOGTAG = "StopmotionCameraLog-SquashedPreview";
    Bitmap[] previewImages;
    String[] images;
    int currentImage = 0;
    SeekBar seekbar;

    public SquashedPreview(Context context) {
        super(context);
    }

    public SquashedPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquashedPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSeekbar(SeekBar s) {
        seekbar = s;
    }

    public void setImageNumber(int i) {
        if (i > previewImages.length - 1) i = previewImages.length - 1;
        if (i < 0) i = 0;
        currentImage = i;
        invalidate();
    }

    public int getNumberImages() {

        if (previewImages == null) return 0;

        return previewImages.length;

    }

    public void setDirectory(File d) {
        directory = d;

        images = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".jpg");
            }
        });

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    //Arrays.sort(images, Collections.reverseOrder());

                    Log.d(LOGTAG, "images " + images.length);

                    if (images.length > 0) {

                        previewImages = new Bitmap[images.length];
                        for (int ii = 0; (ii < images.length); ii++) {

                            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                            Log.d(LOGTAG, "loading " + images[ii]);
                            Bitmap pic = BitmapFactory.decodeFile(directory + "/" + images[ii], bmOptions);
                            Log.d(LOGTAG, pic.toString());
                            previewImages[ii] = Bitmap.createScaledBitmap(pic, pic.getWidth() / 10, pic.getHeight() / 10, false);
                            setImageNumber(ii);
                            seekbar.setMax(ii);
                        }
                    }
                } catch (Exception ex) {
                    Log.d(LOGTAG,ex.getMessage());
                }

            }
        });
        t.start();

    }

    public void draw(Canvas c) {

        super.draw(c);

        Paint p = new Paint();

        c.drawRect(new Rect(0, 0, getWidth(), getHeight()), p);

        if (previewImages != null) {

            int ww = getWidth();
            int wh = getHeight();

            if (previewImages[currentImage] != null) {

                int w = previewImages[currentImage].getWidth();
                int h = previewImages[currentImage].getHeight();

                float wr = (float) ww / wh;
                float r = (float) w / h;
                float r1 = 1 / r;

                int ht = (int) (r1 * ww);

                c.drawBitmap(previewImages[currentImage], new Rect(0, 0, w, h), new Rect(0, 0, ww, ht), p);

            }
        } else {
            c.drawText("Loading...", 20, 20, p);
        }
    }
}
