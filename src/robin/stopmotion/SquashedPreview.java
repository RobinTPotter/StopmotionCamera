package robin.stopmotion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by potterr on 28/07/2016.
 */
public class SquashedPreview extends View {

    private static String LOGTAG = "StopmotionCameraLog-SquashedPreview";
    Bitmap[] previewImages;
    int currentImage = 0;

    public SquashedPreview(Context context) {
        super(context);
    }

    public SquashedPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquashedPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImageNumber(int i) {
        if (i > previewImages.length - 1) i = previewImages.length - 1;
        if (i < 0) i = 0;
        currentImage = i;
    }

    public void loadImages(String[] ims) {

        previewImages = new Bitmap[ims.length];

        for (int ii = 0; ii < ims.length; ii++) {

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap pic = BitmapFactory.decodeFile(ims[ii], bmOptions);

            previewImages[ii] = Bitmap.createScaledBitmap(pic, pic.getWidth() / 2, pic.getHeight() / 2, false);
        }
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
        }
    }
}
