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
        if (mainImages == null) return true;
        boolean ok = true;
        for (int imnum=0;imnum<mainImages.length;imnum++) {
            if (deleteImage(imnum,false).equals("Error")) ok=false;
        }
        return ok;

    }

    public void setImageNumber(int i) {
        if (previewImages == null || previewImages.length == 0) return;
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
        if (_thumbnailImages == null) _thumbnailImages = new String[0];
        java.util.Arrays.sort(_thumbnailImages);

        String[] _mainImages = (new File(main)).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".jpg");
            }
        });
        if (_mainImages == null) _mainImages = new String[0];
        java.util.Arrays.sort(_mainImages);

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

        Log.d(LOGTAG, "images " + thumbnailImages.length);

        if (thumbnailImages.length > 0) {

            int to_be_used = StopmotionCamera.MAX_IMAGES;
            if (thumbnailImages.length < to_be_used) to_be_used = thumbnailImages.length;

            previewImages = new Bitmap[to_be_used];
            for (int ii = 0; (ii < to_be_used); ii++) {
                int imageindex = ii + thumbnailImages.length - to_be_used;
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Log.d(LOGTAG, "loading " + thumbnailImages[imageindex]);
                Bitmap pic = BitmapFactory.decodeFile( thumbnailImages[imageindex], bmOptions);
                if (pic != null) {
                    Log.d(LOGTAG, pic.toString());
                }
                previewImages[ii] = pic;
                setImageNumber(ii);
                seekbar.setMax(ii);
            }
        } else {
            previewImages = null;
            seekbar.setMax(0);
        }

        seekbar.setProgress(0);
        invalidate();

    }

    public void draw(Canvas c) {

        super.draw(c);
        Paint p = new Paint();
        c.drawRect(new Rect(0, 0, getWidth(), getHeight()), p);
        if (previewImages != null && currentImage >= 0 && currentImage < previewImages.length) {

            int ww = getWidth();
            int wh = getHeight();

            if (previewImages[currentImage] != null && ww > 0 && wh > 0) {

                int w = previewImages[currentImage].getWidth();
                int h = previewImages[currentImage].getHeight();

                if (w > 0 && h > 0) {
                    float r = (float) w / h;
                    int drawW = ww;
                    int drawH = (int) (ww / r);
                    if (drawH > wh) {
                        drawH = wh;
                        drawW = (int) (wh * r);
                    }
                    int left = (ww - drawW) / 2;
                    int top = (wh - drawH) / 2;
                    c.drawBitmap(previewImages[currentImage], new Rect(0, 0, w, h),
                            new Rect(left, top, left + drawW, top + drawH), p);
                }

            }
        } else {
            p.setColor(android.graphics.Color.WHITE);
            p.setTextSize(32f);
            c.drawText("No frames captured yet", 40, 60, p);
        }
    }
}
