package rd.canvaslasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * Created by Wei on 2016/6/23.
 */
public class LassoHelper {

    private Path lasso;
    private Bitmap sticker, drawingLassoBitmap;
    private LassoBox lassoBox;
    private int canvasWidth, canvasHeight;

    private Context context;
    private RelativeLayout parent;

    private Paint lassoPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {{
        setColor(Color.RED);
        setStyle(Paint.Style.STROKE);
        setStrokeWidth(10);
        setPathEffect(new DashPathEffect(new float[] { 20, 30}, 0));
    }};

    public LassoHelper(Context context, int canvasWidth, int canvasHeight, RelativeLayout parent) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.context = context;
        this.parent = parent;
    }

    private void draw(Canvas canvas) {
        if (sticker != null) {
            canvas.save();
            canvas.rotate(lassoBox.getRotation(), lassoBox.getCenterX(), lassoBox.getCenterY());
            Path p = new Path(lasso);
            Utils.rotatePath(-lassoBox.getRotation(), lassoBox.getCenterX(), lassoBox.getCenterY(), p);
            Rect bounds = Utils.getPathBounds(p);
            canvas.drawBitmap(sticker, null, bounds, null);
            canvas.restore();
        }
        if (lasso != null) canvas.drawPath(lasso, lassoPaint);
    }

    private void touchEvent(MotionEvent event) {
        float cx = event.getX();
        float cy = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lasso = new Path();
                lasso.moveTo(cx, cy);
                newDrawingLassoBitmap();
                break;
            case MotionEvent.ACTION_MOVE:
                lasso.lineTo(cx, cy);
                break;
            case MotionEvent.ACTION_UP:
                lasso.close();
                onFinishDrawingLasso();
                break;
        }
    }

    private void onFinishDrawingLasso() {
        Rect bounds = Utils.getPathBounds(lasso);
        if (bounds.width() <= 0 && bounds.height() <= 0) return;
        freeDrawingLassoBitmap();
        lassoBox = new LassoBox(context, parent, bounds);
        lassoBox.setOnRotateListener(new LassoBox.OnRotateListener() {
            @Override
            public void onRotate(float degree, float px, float py) {
                onLassoBoxRotate(degree, px, py);
            }
        });
        lassoBox.setOnScaleListener(new LassoBox.OnScaleListener() {
            @Override
            public void onScale(float sx, float sy, float px, float py) {
                onLassoBoxScale(sx, sy, px, py);
            }
        });
        lassoBox.setOnTranslateListener(new LassoBox.OnTranslateListener() {
            @Override
            public void onTranslate(float dx, float dy) {
                onLassoBoxTranslate(dx, dy);
            }
        });
    }

    private void onLassoBoxRotate(float degree, float px, float py) {
        Utils.rotatePath(degree, px, py, lasso);
        invalidate();
    }

    private void onLassoBoxScale(float sx, float sy, float px, float py) {
        Utils.rotatePath(-lassoBox.getRotation(), px, py, lasso);
        Utils.scalePath(sx, sy, px, py, lasso);
        Utils.rotatePath(lassoBox.getRotation(), px, py, lasso);
        invalidate();
    }

    private void onLassoBoxTranslate(float dx, float dy) {
        Utils.translatePath(dx, dy, lasso);
        invalidate();
    }

    private void invalidate() {
        parent.invalidate();
    }

    private void freeDrawingLassoBitmap() {
        drawingLassoBitmap.recycle();
        drawingLassoBitmap = null;
    }

    private void newDrawingLassoBitmap() {
        if (drawingLassoBitmap != null)
            drawingLassoBitmap.recycle();
        drawingLassoBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
    }
}
