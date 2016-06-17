package rd.canvaslasso;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by Wei on 2016/6/14.
 */
public class MyCanvas extends RelativeLayout {
    public Activity activity;
    private String TAG = getClass().getSimpleName();

    public enum Mode {Brush, Lasso}

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint lassoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint eraser;
    private ArrayList<Path> pathList = new ArrayList<>();
    private Path currentPath;
    private Path lassoPath;
    private Mode mode = Mode.Brush;

    private Bitmap mainBitmap;
    private Bitmap drawingLassoBitmap;
    private Bitmap stickerLassoBitmap;
    private Canvas mainCanvas;
    private Canvas lassoCanvas;

    private float preX, preY;

    private boolean isFirstDraw = true;

    private LassoBox lassoBox;

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(100);

        lassoPaint.setColor(Color.RED);
        lassoPaint.setStyle(Paint.Style.STROKE);
        lassoPaint.setStrokeWidth(1);

        eraser = new Paint();
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraser.setFlags(Paint.ANTI_ALIAS_FLAG);
        eraser.setColor(0xFFFFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isFirstDraw) {
            isFirstDraw = false;
            mainBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mainCanvas = new Canvas(mainBitmap);
        }
        canvas.drawBitmap(mainBitmap, 0, 0, null);
        if (mode.equals(Mode.Lasso) && drawingLassoBitmap != null) {
            canvas.drawBitmap(drawingLassoBitmap, 0, 0, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
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

    private void drawLasso(float x1, float y1, float x2, float y2) {
        lassoCanvas.drawLine(x1, y1, x2, y2, lassoPaint);
    }

    private void onActionUp(MotionEvent event) {
        if (mode.equals(Mode.Brush)) {
            currentPath = null;
        }
        if (mode.equals(Mode.Lasso)) {
            lassoPath.close();
            PathMeasure pathMeasure = new PathMeasure(lassoPath, false);
            Rect rect = computeLassoRect(mainBitmap, lassoPath);
            if (rect.width() > 0 && rect.height() > 0)
                onFinishLasso();
//            TODO:
//            else
//                cancelLasso
        }
    }

    private Rect lassoRect;
    private RelativeLayout lassoFrame;
    private View buttonsBox;
    private void onFinishLasso() {
        // clear drawing lasso
        drawingLassoBitmap.recycle();
        drawingLassoBitmap = null;
        // create lass bitmap
        Rect rect = computeLassoRect(mainBitmap, lassoPath);
        final Bitmap lassoBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(lassoBitmap);
        c.save();
        c.translate(-rect.left, -rect.top);
        c.drawPath(lassoPath, lassoPaint);
        c.restore();

        // create lassBox
        lassoBox = new LassoBox(getContext());
        lassoBox.activity = activity;
        addView(lassoBox);
        lassoBox.setLassoBitmap(lassoBitmap);
        lassoBox.setSizeAndPosition(rect);
        lassoBox.setOnTranslateListener(new LassoBox.OnTranslateListener() {
            @Override
            public void onTranslate(float x, float y) {
                Matrix matrix = new Matrix();
                matrix.setTranslate(-x, -y);
                lassoPath.transform(matrix);
            }
        });
        lassoBox.setOnRotateListener(new LassoBox.OnRotateListener() {
            @Override
            public void onRotate(float degree) {
                Matrix matrix = new Matrix();
                matrix.setRotate(degree);
//                lassoPath.transform(matrix);
            }
        });

        buttonsBox = createButtonsBox();
        addView(buttonsBox);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)buttonsBox.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_LEFT, lassoBox.getId());
        buttonsBox.setLayoutParams(layoutParams);
    }

    private void dismissLasso() {
        if (indexOfChild(lassoBox) != -1) removeView(lassoBox);
        if (indexOfChild(buttonsBox) != -1) removeView(buttonsBox);
    }

    private void onActionMove(MotionEvent event) {
        float cX = event.getX();
        float cY = event.getY();
        if (mode.equals(Mode.Brush)) {
            currentPath.lineTo(cX, cY);
            drawBitmap(preX, preY, cX, cY);
        }
        if (mode.equals(Mode.Lasso)) {
            lassoPath.lineTo(cX, cY);
            drawLasso(preX, preY, cX, cY);
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
        if (mode.equals(Mode.Lasso)) {
            dismissLasso();
            lassoPath = new Path();
            lassoPath.moveTo(preX, preY);
            drawingLassoBitmap = Bitmap.createBitmap(mainBitmap.getWidth(), mainBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            lassoCanvas = new Canvas(drawingLassoBitmap);
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        dismissLasso();
    }

    public Mode getMode() {
        return mode;
    }

    private Bitmap copyLassoArea(Bitmap src, Path lasso) {
        Rect roi = computeLassoRect(src, lasso);
        Bitmap dst = Bitmap.createBitmap(roi.width(), roi.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(dst);
        c.translate(-roi.left, -roi.top);
        c.clipPath(lasso);
        c.drawBitmap(src, 0, 0, null);
        return dst;
    }

    private Rect computeLassoRect(Bitmap bitmap, Path lasso) {
        Region region = new Region();
        region.setPath(lasso, new Region(0, 0, bitmap.getWidth(), bitmap.getHeight()));
        return region.getBounds();
    }

    private void onCancelButtonClick() {
        dismissLasso();
    }

    private void onCutButtonClick() {
        if (!mode.equals(Mode.Lasso)) return;
        stickerLassoBitmap = copyLassoArea(mainBitmap, lassoPath);
        lassoBox.setStickerBitmap(stickerLassoBitmap);
        mainCanvas.save();
        mainCanvas.clipPath(lassoPath);
        mainCanvas.drawPaint(eraser);
        mainCanvas.restore();
        invalidate();
    }

    private void onCopyButtonClick() {
        if (!mode.equals(Mode.Lasso)) return;
        stickerLassoBitmap = copyLassoArea(mainBitmap, lassoPath);
        lassoBox.setStickerBitmap(stickerLassoBitmap);
    }

    private void onPasteButtonClick() {
        float x = lassoBox.getLassoX();
        float y = lassoBox.getLassoY();
        mainCanvas.save();
        mainCanvas.rotate(lassoBox.getRotation());
        mainCanvas.drawBitmap(stickerLassoBitmap, x, y, null);
//        mainCanvas.drawPath(lassoPath, lassoPaint);
        mainCanvas.restore();
        invalidate();
    }

    private View createButtonsBox() {
        View view =  LayoutInflater.from(getContext()).inflate(R.layout.layout_lasso_buttons, null);
        view.findViewById(R.id.button_cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelButtonClick();
            }
        });
        view.findViewById(R.id.button_cut).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCutButtonClick();
            }
        });
        view.findViewById(R.id.button_copy).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCopyButtonClick();
            }
        });
        view.findViewById(R.id.button_paste).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPasteButtonClick();
            }
        });
        return view;
    }

    private int dpToPx(float dp) {
        return (int) (dp * getDensity());
    }

    private float pxToDp(float px) {
        return px / getDensity();
    }

    private float getDensity() {
        return getResources().getDisplayMetrics().density;
    }
}