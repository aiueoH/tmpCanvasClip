package rd.canvaslasso;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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
    private Paint lassoPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {{
        setColor(Color.RED);
        setStyle(Paint.Style.STROKE);
        setStrokeWidth(10);
        setPathEffect(new DashPathEffect(new float[] { 20, 30}, 0));
    }};
    private Paint eraser = new Paint() {{
        setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setColor(0xFFFFFFFF);
    }};

    private LassoBox.OnRotateListener onRotateListener = new LassoBox.OnRotateListener() {
        @Override
        public void onRotate(float degree, float px, float py) {
            Utils.rotatePath(degree, px, py, lassoPath);
            invalidate();
        }
    };
    private LassoBox.OnScaleListener onScaleListener = new LassoBox.OnScaleListener() {
        @Override
        public void onScale(float sx, float sy, float px, float py) {
            Utils.rotatePath(-lassoBox.getRotation(), px, py, lassoPath);
            Utils.scalePath(sx, sy, px, py, lassoPath);
            Utils.rotatePath(lassoBox.getRotation(), px, py, lassoPath);
            invalidate();
        }
    };
    private LassoBox.OnTranslateListener onTranslateListener = new LassoBox.OnTranslateListener() {
        @Override
        public void onTranslate(float dx, float dy) {
            Utils.translatePath(dx, dy, lassoPath);
            invalidate();
        }
    };

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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        prePareMainBitmap(canvas);
        canvas.drawBitmap(mainBitmap, 0, 0, null);
        if (mode.equals(Mode.Lasso) && drawingLassoBitmap != null) {
            canvas.drawBitmap(drawingLassoBitmap, 0, 0, null);
        }
        if (mode.equals(Mode.Lasso)) {
            if (stickerLassoBitmap != null) {
                canvas.save();
                canvas.rotate(lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY());
                Path p = new Path(lassoPath);
                Utils.rotatePath(-lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY(), p);
                Rect bounds = computeLassoBounds(p);
                canvas.drawBitmap(stickerLassoBitmap, null, bounds, null);
                canvas.restore();
            }
            if (lassoPath != null) canvas.drawPath(lassoPath, lassoPaint);
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
            Rect rect = computeLassoBounds(lassoPath);
            if (rect.width() > 0 && rect.height() > 0)
                onFinishLasso();
        }
    }

    private View buttonsBox;
    private void onFinishLasso() {
        // clear drawing lasso
        drawingLassoBitmap.recycle();
        drawingLassoBitmap = null;
        // create lassBox
        lassoBox = new LassoBox(getContext(), this, lassoPath);
        lassoBox.activity = activity;
        lassoBox.setOnRotateListener(onRotateListener);
        lassoBox.setOnScaleListener(onScaleListener);
        lassoBox.setOnTranslateListener(onTranslateListener);

        buttonsBox = createButtonsBox();
        addView(buttonsBox);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)buttonsBox.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_LEFT, lassoBox.getId());
        buttonsBox.setLayoutParams(layoutParams);
    }

    private void dismissLasso() {
        if (indexOfChild(lassoBox) != -1) removeView(lassoBox);
        if (indexOfChild(buttonsBox) != -1) removeView(buttonsBox);
        if (stickerLassoBitmap != null) {
            stickerLassoBitmap.recycle();
            stickerLassoBitmap = null;
        }
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
        lasso = new Path(lasso);
        Utils.rotatePath(-lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY(), lasso);
        Rect bounds = computeLassoBounds(lasso);
        Bitmap dst = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(dst);
        c.translate(-bounds.left, -bounds.top);
        c.clipPath(lasso);
        c.rotate(-lassoBox.getRotation(), bounds.centerX(), bounds.centerY());
        c.drawBitmap(src, 0, 0, null);
        return dst;
    }

    private Rect computeLassoBounds(Path lasso) {
        Region region = new Region();
        region.setPath(lasso, new Region(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        return region.getBounds();
    }

    private void onCancelButtonClick() {
        dismissLasso();
    }

    private void onCutButtonClick() {
        if (!mode.equals(Mode.Lasso)) return;
        stickerLassoBitmap = copyLassoArea(mainBitmap, lassoPath);
        Rect r = computeLassoBounds(lassoPath);
        mainCanvas.save();
        mainCanvas.clipPath(lassoPath);
        mainCanvas.drawPaint(eraser);
        mainCanvas.restore();
        invalidate();
    }

    private void onCopyButtonClick() {
        if (!mode.equals(Mode.Lasso)) return;
        stickerLassoBitmap = copyLassoArea(mainBitmap, lassoPath);
    }

    private void onPasteButtonClick() {
        mainCanvas.save();
        mainCanvas.rotate(lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY());

        Path p = new Path(lassoPath);
        Utils.rotatePath(-lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY(), p);
        Rect rect = computeLassoBounds(p);

        mainCanvas.drawBitmap(stickerLassoBitmap, null, rect, null);
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