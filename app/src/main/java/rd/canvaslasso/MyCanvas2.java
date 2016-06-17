package rd.canvaslasso;

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
public class MyCanvas2 extends RelativeLayout {
    private String TAG = getClass().getSimpleName();

    public enum Mode {Brush, Lasso}

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint lassoLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
    private float sXLasso, sYLasso;

    private float preX, preY;

    private boolean isFirstDraw = true;

    public MyCanvas2(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(100);

        lassoLinePaint.setColor(Color.RED);
        lassoLinePaint.setStyle(Paint.Style.STROKE);
        lassoLinePaint.setStrokeWidth(1);

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
//        for (Path path : pathList)
//            canvas.drawPath(path, paint);
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
        lassoCanvas.drawLine(x1, y1, x2, y2, lassoLinePaint);
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
        drawLasso(preX, preY, sXLasso, sYLasso);
        lassoFrame = createFrame();
        setupLassoFrame(lassoFrame, lassoPath);

        Region region = new Region();
        region.setPath(lassoPath, new Region(0, 0, lassoCanvas.getWidth(), lassoCanvas.getHeight()));
        lassoRect = region.getBounds();

        drawingLassoBitmap.recycle();
        drawingLassoBitmap = null;
        stickerLassoBitmap = Bitmap.createBitmap(lassoRect.width(), lassoRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(stickerLassoBitmap);
        canvas.save();
        canvas.drawPaint(eraser);
        drawStickerLasso(canvas);
        canvas.restore();
        updateStickerImageView();

        buttonsBox = createButtonsBox();
        addView(buttonsBox);
        LayoutParams layoutParams = (LayoutParams)buttonsBox.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_LEFT, lassoFrame.getId());
        buttonsBox.setLayoutParams(layoutParams);
    }

    private void updateStickerImageView() {
        ((ImageView) lassoFrame.findViewById(R.id.imageView_lasso)).setImageBitmap(stickerLassoBitmap);
    }

    private void drawStickerLasso(Canvas canvas) {
        canvas.translate(-lassoRect.left, -lassoRect.top);
        canvas.drawPath(lassoPath, lassoLinePaint);
    }

    private void dismissLasso() {
        if (indexOfChild(lassoFrame) != -1) removeView(lassoFrame);
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
            sXLasso = preX;
            sYLasso = preY;
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

    public void cut() {
        if (!mode.equals(Mode.Lasso)) return;
        stickerLassoBitmap = copyLassoArea(mainBitmap, lassoPath);
        updateStickerImageView();
        mainCanvas.save();
        mainCanvas.clipPath(lassoPath);
        mainCanvas.drawPaint(eraser);
        mainCanvas.restore();
        invalidate();
    }

    private Bitmap copyLassoArea(Bitmap src, Path lasso) {
        Rect roi = computeLassoRect(src, lasso);
        Bitmap dst = Bitmap.createBitmap(roi.width(), roi.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(dst);
        c.drawPaint(eraser);
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
        cut();
    }

    private void onCopyButtonClick() {
        if (!mode.equals(Mode.Lasso)) return;
        stickerLassoBitmap = copyLassoArea(mainBitmap, lassoPath);
        updateStickerImageView();
    }

    private void onPasteButtonClick() {
        float x = lassoFrame.getX() + lassoFrame.findViewById(R.id.imageView_lasso).getX();
        float y = lassoFrame.getY() + lassoFrame.findViewById(R.id.imageView_lasso).getY();
        mainCanvas.save();
        mainCanvas.drawBitmap(stickerLassoBitmap, x, y, null);
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

    private RelativeLayout createFrame() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        RelativeLayout relativeLayout = (RelativeLayout) layoutInflater.inflate(R.layout.widget_lasso_frame, null);
        return relativeLayout;
    }

    private void setupLassoFrame(final RelativeLayout relativeLayout, final Path lasso) {
        int minWidthDp = 100;
        int minHeightDp = 100;
        addView(relativeLayout);
        final Rect rect = computeLassoRect(mainBitmap, lasso);
        LayoutParams layoutParams = (LayoutParams) relativeLayout.getLayoutParams();
        int dW = 0;
        int dH = 0;
        if (rect.width() < dpToPx(minWidthDp)) dW = dpToPx(minWidthDp) - rect.width();
        if (rect.height() < dpToPx(minHeightDp)) dH = dpToPx(minHeightDp) - rect.height();
        layoutParams.setMargins(rect.left - dW / 2, rect.top - dH / 2, 0, 0);
        layoutParams.width = rect.width() + dW;
        layoutParams.height = rect.height() + dH;

        relativeLayout.setOnTouchListener(new OnTouchListener() {
            float dX, dY;
            float startX, startY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = relativeLayout.getX() - event.getRawX();
                        dY = relativeLayout.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        relativeLayout.setX(event.getRawX() + dX);
                        relativeLayout.setY(event.getRawY() + dY);
                        break;
                    case MotionEvent.ACTION_UP:
                        Matrix matrix = new Matrix();
                        float x = startX - event.getRawX();
                        float y = startY - event.getRawY();
                        matrix.setTranslate(-x, -y);
                        lassoPath.transform(matrix);
                        break;
                }
                return true;
            }
        });
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