package robin.stopmotion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by potterr on 28/07/2016.
 */
public class SquashedPreview extends View {

    private File directory;
    private static String LOGTAG = "StopmotionCameraLog-SquashedPreview";
    Bitmap[] previewImages;

    private String[] thumbnailImages;
    private String[] mainImages;
    private int currentImage = 0;
    private SeekBar seekbar;
    private String main;
    private String thumb;

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

    public int getImageNumber() {
        return currentImage;
    }

    public String deleteImage(int imnum, boolean callRenumber) {
        if (imnum>=mainImages.length) return "Error";
        String me = mainImages[imnum];
        String th = thumbnailImages[imnum];
        Log.i(StopmotionCamera.LOGTAG,"found "+ me +  " exists "+(new File(me).exists()));
        Log.i(StopmotionCamera.LOGTAG,"found "+ th +  " exists "+(new File(th).exists()));
        boolean medone = (new File(me)).delete();
        boolean thdone = (new File(th)).delete();
        Log.i(StopmotionCamera.LOGTAG,"del "+ me +  " " + medone);
        Log.i(StopmotionCamera.LOGTAG,"del "+ th +  " " +thdone);
        if (medone && thdone) {
            if (callRenumber) renumber();
            return me;
        } else {
            return "Error";
        }
    }

    private void renumber() {
        int cnum = 0;
        for (int imnum=0;imnum<mainImages.length;imnum++) {
            File me = new File(mainImages[imnum]);
            File th = new File(thumbnailImages[imnum]);
            Log.d(StopmotionCamera.LOGTAG, "found " + me + " exists " + me.exists());
            Log.d(StopmotionCamera.LOGTAG, "found " + th + " exists " + th.exists());
            if (me.exists() && th.exists()) {
                String stamp = String.format(StopmotionCamera.IMAGE_NUMBER_FORMAT, cnum);
                me.renameTo(new File(me.getParent(), stamp + ".jpg"));
                th.renameTo(new File(th.getParent(), stamp + ".thumb.jpg"));
                cnum++;
            }
        }
        setDirectory();
    }

    public boolean deleteAll() {
        boolean ok = true;
        for (int imnum=0;imnum<mainImages.length;imnum++) {
            if (deleteImage(imnum,false).equals("Error")) ok=false;
        }
        return ok;

    }

    public void setImageNumber(int i) {
        if (previewImages == null) return;
        if (i > previewImages.length - 1) i = previewImages.length - 1;
        if (i < 0) i = 0;
        currentImage = i;
        seekbar.setProgress(currentImage);
        invalidate();
        //dammit
    }

    public int getNumberImages() {

        if (previewImages == null) return 0;

        return previewImages.length;

    }


    public void setDirectory() {
        if (this.main==null || this.thumb==null) return;
        setDirectory(this.main, this.thumb);
    }


    public void setDirectory(String main, String thumb) {

        this.thumb= thumb;
        this.main = main;

        directory = new File(main, thumb);

        String[] _thumbnailImages = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".thumb.jpg");
            }
        });

        String[] _mainImages = (new File(main)).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".jpg");
            }
        });

        mainImages = new String[_mainImages.length];
        thumbnailImages = new String[_thumbnailImages.length];

        for (int mm=0;mm<_mainImages.length;mm++) {
            mainImages[mm] = main + "/" + _mainImages[mm];
            Log.d(StopmotionCamera.LOGTAG, "main image now "+mainImages[mm]);
        }
        for (int mm=0;mm<_thumbnailImages.length;mm++) {
            thumbnailImages[mm] = main + "/" + thumb  + "/" +_thumbnailImages[mm];
            Log.d(StopmotionCamera.LOGTAG, "thumb image now "+thumbnailImages[mm]);
        }

        //Arrays.sort(images, Collections.reverseOrder());

        Log.d(LOGTAG, "images " + thumbnailImages.length);

        if (thumbnailImages.length > 0) {

            int to_be_used = StopmotionCamera.MAX_IMAGES;
            if (thumbnailImages.length < to_be_used) to_be_used = thumbnailImages.length;

            previewImages = new Bitmap[thumbnailImages.length];
            for (int ii = 0; (ii < to_be_used); ii++) {
                int imageindex = ii + thumbnailImages.length - to_be_used;
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Log.d(LOGTAG, "loading " + thumbnailImages[imageindex]);
                Bitmap pic = BitmapFactory.decodeFile( thumbnailImages[imageindex], bmOptions);
                Log.d(LOGTAG, pic.toString());
                previewImages[ii] = pic; //Bitmap.createScaledBitmap(pic, pic.getWidth() / 10, pic.getHeight() / 10, false);
                setImageNumber(ii);
                seekbar.setMax(ii);
            }
        } else {
            previewImages = null;
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
