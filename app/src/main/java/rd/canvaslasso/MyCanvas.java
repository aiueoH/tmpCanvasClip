package rd.canvaslasso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by Wei on 2016/6/14.
 */
public class MyCanvas extends RelativeLayout {
    public Activity activity;
    private String TAG = getClass().getSimpleName();

    public enum Mode {Brush, Lasso}

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG) {{
        setColor(Color.GREEN);
        setStyle(Paint.Style.STROKE);
        setStrokeWidth(100);
    }};

    private ArrayList<Path> pathList = new ArrayList<>();

    private Path currentPath;
    private Path lassoPath;
    private Mode mode = Mode.Brush;
    private Bitmap mainBitmap;

    private Canvas mainCanvas;
    private float preX, preY;

    private boolean isFirstDraw = true;

    private LassoController lassoController;

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        post(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        prePareMainBitmap(canvas);
        canvas.drawBitmap(mainBitmap, 0, 0, null);
        if (mode.equals(Mode.Lasso)) {
            lassoController.draw(canvas);
        }
    }

    private void prePareMainBitmap(Canvas canvas) {
        if (isFirstDraw) {
            isFirstDraw = false;
            mainBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mainCanvas = new Canvas(mainBitmap);
            mainCanvas.drawColor(Color.GRAY);
            Paint p = new Paint();
            p.setColor(Color.BLUE);
            p.setStrokeWidth(1);
            p.setStyle(Paint.Style.FILL_AND_STROKE);
            p.setTextSize(20);
            Paint pp = new Paint(p);
            pp.setColor(Color.RED);
            pp.setStrokeWidth(10);
            for (int x = 0; x < mainCanvas.getWidth(); x += 100) {
                for (int y = 0; y < mainCanvas.getHeight(); y += 100) {
                    String s = String.format("(%s, %s)", x, y);
                    mainCanvas.drawText(s, x, y, p);
                    mainCanvas.drawPoint(x, y, pp);
                }
            }


            lassoController = new LassoController(getContext(), mainBitmap, MyCanvas.this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (mode.equals(Mode.Lasso)) {
            lassoController.touchEvent(event);
            return true;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                onActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onActionMove(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_OUTSIDE:
                onActionUp(event);
                break;
        }
        invalidate();
        return true;
    }

    private void drawBitmap(float x1, float y1, float x2, float y2) {
        mainCanvas.drawLine(x1, y1, x2, y2, paint);
    }

    private void onActionUp(MotionEvent event) {
        if (mode.equals(Mode.Brush)) {
            currentPath = null;
        }
    }

    private void onActionMove(MotionEvent event) {
        float cX = event.getX();
        float cY = event.getY();
        if (mode.equals(Mode.Brush)) {
            currentPath.lineTo(cX, cY);
            drawBitmap(preX, preY, cX, cY);
        }
        preX = cX;
        preY = cY;
    }

    private void onActionDown(MotionEvent event) {
        preX = event.getX();
        preY = event.getY();
        if (mode.equals(Mode.Brush)) {
            currentPath = new Path();
            pathList.add(currentPath);
            currentPath.moveTo(event.getX(), event.getY());
            drawBitmap(preX, preY, preX, preY);
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    private void showBitmapDialog(Bitmap bitmap) {
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 2, bitmap.getHeight() * 2, false);
        ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(bitmap);
        imageView.setBackgroundColor(Color.BLACK);
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.addView(imageView);
        new AlertDialog.Builder(getContext())
                .setView(linearLayout)
                .show();
    }
}