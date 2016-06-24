package rd.canvaslasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.Timer;
import java.util.TimerTask;

public class LassoController {

    private Path lasso;
    private Bitmap sticker, drawingLassoBitmap;
    private LassoManipulationBox lassoManipulationBox;

    private Context context;
    private RelativeLayout parent;
    private LassoButtonBar lassoButtonBar;
    private Bitmap canvasBitmap;
    private Timer timer;

    private Paint lassoDisplayingPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {{
        setColor(Color.RED);
        setStyle(Paint.Style.STROKE);
        setStrokeWidth(5);
        setPathEffect(new DashPathEffect(new float[] { 20, 30}, 0));
    }};
    private Paint lassoDrawingPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {{
        setColor(Color.RED);
        setStyle(Style.STROKE);
        setStrokeWidth(5);
    }};
    private Paint eraser = new Paint() {{
        setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setColor(0xFFFFFFFF);
    }};
    private Paint lassoPaint = lassoDrawingPaint;


    public LassoController(Context context, Bitmap canvasBitmap, RelativeLayout parent) {
        this.context = context;
        this.parent = parent;
        this.canvasBitmap = canvasBitmap;
    }

    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            int t = 0;
            @Override
            public void run() {
                t = (t + 5) % 40;
                lassoDisplayingPaint.setPathEffect(new DashPathEffect(new float[] { 20, 20}, t));
                postInvalidate();
            }
        }, 50, 50);
    }

    private void stopTimer() {
        timer.cancel();
    }

    public void draw(Canvas canvas) {
        if (sticker != null) {
            canvas.save();
            canvas.rotate(lassoManipulationBox.getRotation(), lassoManipulationBox.getCenterX(), lassoManipulationBox.getCenterY());
            Path p = new Path(lasso);
            Utils.rotatePath(-lassoManipulationBox.getRotation(), lassoManipulationBox.getCenterX(), lassoManipulationBox.getCenterY(), p);
            Rect bounds = Utils.getPathBounds(p);
            canvas.drawBitmap(sticker, null, bounds, null);
            canvas.restore();
        }
        if (lasso != null) canvas.drawPath(lasso, lassoPaint);
    }

    public void touchEvent(MotionEvent event) {
        if (lassoManipulationBox != null) return;
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
        invalidate();
    }

    private void onFinishDrawingLasso() {
        lassoPaint = lassoDisplayingPaint;
        startTimer();
        Rect bounds = Utils.getPathBounds(lasso);
        if (bounds.width() <= 0 && bounds.height() <= 0) return;
        freeDrawingLassoBitmap();
        lassoManipulationBox = new LassoManipulationBox(context, parent, bounds);
        lassoManipulationBox.setOnRotateListener(new LassoManipulationBox.OnRotateListener() {
            @Override
            public void onRotate(float degree, float px, float py) {
                onLassoBoxRotate(degree, px, py);
            }
        });
        lassoManipulationBox.setOnScaleListener(new LassoManipulationBox.OnScaleListener() {
            @Override
            public void onScale(float sx, float sy, float px, float py) {
                onLassoBoxScale(sx, sy, px, py);
            }
        });
        lassoManipulationBox.setOnTranslateListener(new LassoManipulationBox.OnTranslateListener() {
            @Override
            public void onTranslate(float dx, float dy) {
                onLassoBoxTranslate(dx, dy);
            }
        });
        lassoManipulationBox.post(new Runnable() {
            @Override
            public void run() {
                setupButtonBar();
            }
        });
    }

    private void onLassoBoxRotate(float degree, float px, float py) {
        Utils.rotatePath(degree, px, py, lasso);
        updateButtonBarPosition();
        invalidate();
    }

    private void onLassoBoxScale(float sx, float sy, float px, float py) {
        Utils.rotatePath(-lassoManipulationBox.getRotation(), px, py, lasso);
        Utils.scalePath(sx, sy, px, py, lasso);
        Utils.rotatePath(lassoManipulationBox.getRotation(), px, py, lasso);
        updateButtonBarPosition();
        invalidate();
    }

    private void onLassoBoxTranslate(float dx, float dy) {
        Utils.translatePath(dx, dy, lasso);
        updateButtonBarPosition();
        invalidate();
    }

    private void postInvalidate() {
        parent.postInvalidate();
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
        drawingLassoBitmap = Bitmap.createBitmap(canvasBitmap.getWidth(), canvasBitmap.getHeight(), Bitmap.Config.ARGB_8888);
    }

    private void setupButtonBar() {
        lassoButtonBar = new LassoButtonBar(context);
        lassoButtonBar.setOnCancelButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });
        lassoButtonBar.setOnCutButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cut();
            }
        });
        lassoButtonBar.setOnCopyButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copy();
            }
        });
        lassoButtonBar.setOnPasteButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paste();
            }
        });
        parent.addView(lassoButtonBar);
        lassoButtonBar.post(new Runnable() {
            @Override
            public void run() {
                updateButtonBarPosition();
            }
        });
    }

    private void updateButtonBarPosition() {
        float x = lassoManipulationBox.getX() + lassoManipulationBox.getWidth() / 2 - lassoButtonBar.getWidth() / 2;
        float y = lassoManipulationBox.getY() + lassoManipulationBox.getHeight();
        lassoButtonBar.setX(x);
        lassoButtonBar.setY(y);
        float px = lassoButtonBar.getWidth() / 2;
        float py = -lassoManipulationBox.getHeight() / 2;
        lassoButtonBar.setPivotX(px);
        lassoButtonBar.setPivotY(py);
        lassoButtonBar.setRotation(lassoManipulationBox.getRotation());
    }

    private void copyLassoAreaToSticker() {
        Path p = new Path(lasso);
        Utils.rotatePath(-lassoManipulationBox.getRotation(), lassoManipulationBox.getCenterX(), lassoManipulationBox.getCenterY(), p);
        Rect r = Utils.getPathBounds(p);
        sticker = Bitmap.createBitmap(r.width(), r.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(sticker);
        c.translate(-r.left, -r.top);
        c.clipPath(p);
        c.rotate(-lassoManipulationBox.getRotation(), r.centerX(), r.centerY());
        c.drawBitmap(canvasBitmap, 0, 0, null);
    }

    public void cancel() {
        if (sticker != null) {
            sticker.recycle();
            sticker = null;
        }
        stopTimer();
        parent.removeView(lassoButtonBar);
        parent.removeView(lassoManipulationBox);
        lassoButtonBar = null;
        lassoManipulationBox = null;
        lasso = null;
    }

    private void cut() {
        copyLassoAreaToSticker();
        Canvas c = new Canvas(canvasBitmap);
        c.save();
        c.clipPath(lasso);
        c.drawPaint(eraser);
        c.restore();
    }

    private void copy() {
        copyLassoAreaToSticker();
    }

    private void paste() {
        float d = lassoManipulationBox.getRotation();
        float px = lassoManipulationBox.getCenterX();
        float py = lassoManipulationBox.getCenterY();
        Canvas c = new Canvas(canvasBitmap);
        c.save();
        c.rotate(d, px, py);
        Path p = new Path(lasso);
        Utils.rotatePath(-d, px, py, p);
        Rect r = Utils.getPathBounds(p);
        c.drawBitmap(sticker, null, r, null);
        c.restore();
        invalidate();
    }
}
