package rd.canvaslasso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.security.acl.LastOwnerException;
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
        lassoPaint.setStrokeWidth(10);
        lassoPaint.setPathEffect(new DashPathEffect(new float[] { 20, 30}, 0));

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
            mainCanvas.drawColor(Color.GRAY);
            Paint p = new Paint();
            p.setColor(Color.BLUE);
            p.setStrokeWidth(1);
            p.setStyle(Paint.Style.FILL_AND_STROKE);
            p.setTextSize(20);
            Paint pp = new Paint(p);
            pp.setColor(Color.RED);
            pp.setStrokeWidth(10);
            for (int i = 0 ; i < mainCanvas.getWidth(); i += 100) {
//                mainCanvas.drawLine(i, 0, i, mainCanvas.getHeight(), p);
            }
            for (int i = 0; i < mainCanvas.getHeight(); i += 100) {
//                mainCanvas.drawLine(0, i, mainCanvas.getWidth(), i, p);
            }
            for (int x = 0; x < mainCanvas.getWidth(); x += 100) {
                for (int y = 0; y < mainCanvas.getHeight(); y += 100) {
                    String s = String.format("(%s, %s)", x, y);
                    mainCanvas.drawText(s, x, y, p);
                    mainCanvas.drawPoint(x, y, pp);
                }
            }
        }
        canvas.drawBitmap(mainBitmap, 0, 0, null);
        if (mode.equals(Mode.Lasso) && drawingLassoBitmap != null) {
            canvas.drawBitmap(drawingLassoBitmap, 0, 0, null);
        }
        if (mode.equals(Mode.Lasso)) {
            if (stickerLassoBitmap != null) {
                canvas.save();
                canvas.rotate(lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY());

                Path p = new Path(lassoPath);
                Matrix m = new Matrix();
                m.setRotate(-lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY());
                p.transform(m);
                Rect rect = computeLassoRect(mainBitmap, p);

                canvas.drawBitmap(stickerLassoBitmap, null, rect, null);
                canvas.restore();
            }
            if (lassoPath != null) canvas.drawPath(lassoPath, lassoPaint);
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
//        Rect rect = computeLassoRect(mainBitmap, lassoPath);
//        final Bitmap lassoBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
//        lassoPath.offset(-rect.left, -rect.top);
//        Canvas c = new Canvas(lassoBitmap);
//        c.save();
//        c.translate(-rect.left, -rect.top);
//        c.drawPath(lassoPath, lassoPaint);
//        c.restore();

        // create lassBox
        lassoBox = new LassoBox(getContext(), this, lassoPath);
        lassoBox.activity = activity;

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

    LassoBox.OnRotateListener onRotateListener = new LassoBox.OnRotateListener() {
        @Override
        public void onRotate(float degree) {
            Matrix m = new Matrix();
            m.setRotate(degree, lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY());
            lassoPath.transform(m);
        }
    };

    public Mode getMode() {
        return mode;
    }

    private Bitmap copyLassoArea(Bitmap src, Path lasso) {
        // 轉回來的 lasso
        lasso = new Path(lasso);
        Matrix m = new Matrix();
        m.setRotate(-lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY());
        lasso.transform(m);

        Rect roi = computeLassoRect(src, lasso);
        Bitmap dst = Bitmap.createBitmap(roi.width(), roi.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(dst);
        c.translate(-roi.left, -roi.top);
        c.clipPath(lasso);
        c.rotate(-lassoBox.getRotation(), roi.centerX(), roi.centerY());
        c.drawBitmap(src, 0, 0, null);

        showBitmapDialog(dst);
        return dst;
    }

    private Rect computeLassoRect(Bitmap bitmap, Path lasso) {
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
        lassoBox.setStickerBitmap(stickerLassoBitmap);
        Rect r = computeLassoRect(mainBitmap, lassoPath);
        mainCanvas.save();
//        mainCanvas.translate(lassoBox.getLassoX(), lassoBox.getLassoY());
//        mainCanvas.rotate(lassoBox.getRotation(), r.centerX(), r.centerY());
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
//        Rect r = computeLassoRect(mainBitmap, lassoPath);
//        mainCanvas.save();
//        mainCanvas.translate(lassoBox.getLassoX(), lassoBox.getLassoY());
//        mainCanvas.rotate(lassoBox.getRotation(), r.centerX(), r.centerY());
//        mainCanvas.drawBitmap(stickerLassoBitmap, 0, 0, null);
//        mainCanvas.restore();
        mainCanvas.save();
        mainCanvas.rotate(lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY());

        Path p = new Path(lassoPath);
        Matrix m = new Matrix();
        m.setRotate(-lassoBox.getRotation(), lassoBox.getLassoCenterX(), lassoBox.getLassoCenterY());
        p.transform(m);
        Rect rect = computeLassoRect(mainBitmap, p);

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

    private int dpToPx(float dp) {
        return (int) (dp * getDensity());
    }

    private float pxToDp(float px) {
        return px / getDensity();
    }

    private float getDensity() {
        return getResources().getDisplayMetrics().density;
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