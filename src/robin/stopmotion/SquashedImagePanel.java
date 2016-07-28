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
public class SquashedImagePanel extends View {

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
        Paint p=new Paint();
        p.setColor(Color.GREEN);
        c.drawRect(new Rect(0,0,getWidth(),getHeight()),p);

    }

}
