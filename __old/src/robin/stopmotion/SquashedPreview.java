package robin.stopmotion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by potterr on 28/07/2016.
 */
public class SquashedPreview extends View {

    private File directory;
    private static String LOGTAG = "StopmotionCameraLog-SquashedPreview";
    Bitmap[] previewImages;
    private String[] images;
    private int currentImage = 0;
    private SeekBar seekbar;
    int MAX_IMAGES = 100;

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
        if (previewImages == null) return;
        if (i > previewImages.length - 1) i = previewImages.length - 1;
        if (i < 0) i = 0;
        currentImage = i;
        invalidate();
        //dammit
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
                return filename.endsWith(".thumb.jpg");
            }
        });

        //Arrays.sort(images, Collections.reverseOrder());

        Log.d(LOGTAG, "images " + images.length);

        if (images.length > 0) {

            int to_be_used = MAX_IMAGES;
            if (images.length < to_be_used) to_be_used = images.length;

            previewImages = new Bitmap[images.length];
            for (int ii = 0; (ii < to_be_used); ii++) {
                int imageindex = ii + images.length - to_be_used;
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Log.d(LOGTAG, "loading " + images[imageindex]);
                Bitmap pic = BitmapFactory.decodeFile(directory + "/" + images[imageindex], bmOptions);
                Log.d(LOGTAG, pic.toString());
                previewImages[ii] = pic; //Bitmap.createScaledBitmap(pic, pic.getWidth() / 10, pic.getHeight() / 10, false);
                setImageNumber(ii);
                seekbar.setMax(ii);
            }
        }

        seekbar.setProgress(0);

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

                if (previewImages[currentImage] != null)
                    c.drawBitmap(previewImages[currentImage], new Rect(0, 0, w, h), new Rect(0, 0, ww, ht), p);

            }
        } else {
            c.drawText("Loading...", 20, 20, p);
        }
    }
}
