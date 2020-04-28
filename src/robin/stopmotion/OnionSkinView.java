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
public class OnionSkinView extends View {

    private static String LOGTAG = "StopmotionCameraLog-Onionskin";

    private static int OPACITY_INCREMENT = 32;
    private static int SKINS_MIN_ALPHA = 40;

    private int numSkins = 3;
    private String timeupdate = ""; //"not set";
    private Bitmap _onionSkins[];

    /**
     *  opacity of the entire drawing, can be set in UI
     */
    private int opacity = 128;

    /**
     * controls whether skins should be drawn or not
     */
    private boolean activated = true;




    public OnionSkinView(Context context) {
        this(context, null);
    }

    public OnionSkinView(Context context, int skinsnum) {
        this(context, null);
    }

    public OnionSkinView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.buttonStyle);
    }

    public OnionSkinView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Log.d(LOGTAG, "created Onionskin");
    }





    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public void setActivated(boolean activated) {
        if (!activated) Log.d(LOGTAG, "INACTIVE");
        this.activated = activated;
        if (activated) Log.d(LOGTAG, "ACTIVE");
    }


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

        if (_onionSkins == null) return;

        this.timeupdate = (new Date()).toString() + " | " + bmp.getWidth() + " x " + bmp.getHeight();

        for (int ss = numSkins - 1; ss > 0; ss--) {
            _onionSkins[ss] = _onionSkins[ss - 1];
        }

        _onionSkins[0] = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 2, bmp.getHeight() / 2, false);

        invalidate();
        Log.d(LOGTAG, "setBmp");
    }

    public void skinsInc() {
        Log.d(LOGTAG,"skins inc");
        setOnionSkins(this.numSkins+1);
    }
    public void skinsDec() {
        Log.d(LOGTAG,"skins dec");
        if (this.numSkins > 1) {
            setOnionSkins(this.numSkins-1);
        }
    }
    public void skinsClear() {
        Log.d(LOGTAG,"skins clear");
        initSkins();
        setOpacity();
        updateBackgound();
        invalidate();
    }

    public void setOnionSkins(int skinsnum) {

        if (skinsnum == this.numSkins) return;

        this.numSkins = skinsnum;

        if (_onionSkins == null || _onionSkins.length!=this.numSkins) {
            initSkins();
        }


        Bitmap[] tmpskins = new Bitmap[this.numSkins];
        for (int bb = 0; bb < numSkins; bb++) {
            Log.d("size of temp is "+tmpskins.length+" index:"+bb+ " size of onionskins "+_onionSkins.length,LOGTAG);
            tmpskins[bb] = _onionSkins[bb];
        }

        this.numSkins = skinsnum;

        _onionSkins = new Bitmap[skinsnum];
        for (int bb = 0; bb < _onionSkins.length; bb++) {
            if (bb < tmpskins.length) _onionSkins[bb] = tmpskins[bb];
        }

        Log.d(LOGTAG, "Number of _onionSkins is " + this.numSkins);
        invalidate();
    }

    public void updateBackgound() {

        setAlpha((float) (opacity) / 255);

        Log.d(LOGTAG, "updateBackground");
        //getBackground().setAlpha(opacity);  // 50% transparent
    }

    public void draw(Canvas canvas) {

        if (!activated) return;

        super.draw(canvas);

        //text paint
        Paint overDetailPaint = new Paint();
        overDetailPaint.setColor(Color.BLUE);
        overDetailPaint.setTextSize(30.0f);

        Paint underDetailPaint = new Paint();
        underDetailPaint.setColor(Color.WHITE);
        underDetailPaint.setTextSize(30.0f);
        underDetailPaint.setStrokeWidth(6.0f);

        int offset_skin_number = 36;

        //skin number text paint
        Paint overSkinPaint = new Paint();
        overSkinPaint.setColor(Color.BLACK);
        overSkinPaint.setTextSize(42.0f);
        overSkinPaint.setStrokeWidth(8.0f);

        //skin number text paint
        Paint underSkinPaint = new Paint();
        underSkinPaint.setColor(Color.WHITE);
        underSkinPaint.setTextSize(42.0f);
        underSkinPaint.setStrokeWidth(8.0f);

        canvas.drawARGB(0, 0, 0, 0);

        int opacity = 255;
        int opacityDecrease = (opacity - SKINS_MIN_ALPHA) / (numSkins);

        int skin = 0;

        if (_onionSkins != null) {

            for (int ss = numSkins - 1; ss >= 0; ss--) {

                Bitmap _bmp = _onionSkins[ss];
                skin++;
                Paint trans = new Paint();
                trans.setAlpha(opacity);
                opacity -= opacityDecrease;
                Log.d(LOGTAG, "bmp with paint " + trans.getAlpha() + " " + trans.getColor());

                if (_bmp != null) {
                    try {

                        canvas.drawBitmap(_bmp, new Rect(0, 0, _bmp.getWidth(), _bmp.getHeight()),
                                new Rect(0, 0, getWidth(), getHeight()), trans);

                        canvas.drawText(String.valueOf(skin), 42, 42 + (skin * offset_skin_number), underSkinPaint);
                        canvas.drawText(String.valueOf(skin), 40, 40 + (skin * offset_skin_number), overSkinPaint);

                    } catch (Exception ex) {
                        Toast.makeText(OnionSkinView.this.getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                    }

                }
            }

            canvas.drawText(timeupdate, 11, 32, underDetailPaint);
            canvas.drawText(timeupdate, 10, 30, overDetailPaint);

        } else {
            canvas.drawText("null array", 10, 50, overDetailPaint);
            Log.i("onion skins are null", LOGTAG);
            initSkins();
        }
    }

    private void initSkins() {
        _onionSkins = new Bitmap[numSkins];
    }

    public int getNumSkins() {
        return numSkins;
    }
}

