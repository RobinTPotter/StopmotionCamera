package robin.stopmotion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by potterr on 28/07/2016.
 */
public class SqushedPreview extends View {
    public SqushedPreview(Context context) {
        super(context);
    }

    public SqushedPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SqushedPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void draw(Canvas c) {
        super.draw(c);
        Paint p=new Paint();
        p.setColor(Color.BLUE);
        c.drawRect(new Rect(0,0,getWidth(),getHeight()),p);

    }

}
