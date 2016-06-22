package rd.canvaslasso;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LassoBox extends RelativeLayout {
    private static final int MIN_WIDTH_DP = 100;
    private static final int MIN_HEIGHT_DP = 100;

    @Bind(R.id.button_scale_left_top)
    Button scaleLeftTopButton;
    @Bind(R.id.button_rotate_right_top)
    Button rotateRightTopButton;
    @Bind(R.id.button_rotate_left_bottom)
    Button rotateLeftBottomButton;
    @Bind(R.id.button_scale_right_bottom)
    Button scaleRightBottomButton;

    public Activity activity;
    private OnTranslateListener onTranslateListener;
    private OnRotateListener onRotateListener;

    ////////////////////////////////////////
    private float canvasScreenDiffX;
    private float canvasScreenDiffY;
    private Rectangle rectangle;
    ////////////////////////////////////////
    private Path lasso;
    private Paint lassoPaint;

    public LassoBox(Context context, RelativeLayout parent, Path lasso) {
        super(context);
        this.lasso = lasso;
        parent.addView(this);
        initLassoPaint();
        initView();
        setBitmap();
        setupOnTouchListener();
        setRotate();
        setScale();
    }

    private void initLassoPaint() {
        lassoPaint = new Paint();
        lassoPaint.setColor(Color.RED);
        lassoPaint.setStyle(Paint.Style.STROKE);
        lassoPaint.setStrokeWidth(1);
    }

    private void setBitmap() {
        // create lass bitmap
        Region region = new Region();
        region.setPath(lasso, new Region(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE));
        Rect rect = region.getBounds();
        final Bitmap lassoBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(lassoBitmap);
        c.save();
        c.translate(-rect.left, -rect.top);
        c.drawPath(lasso, lassoPaint);
        c.restore();
        setSizeAndPosition(rect);
    }

    private void initView() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.widget_lasso_frame, null);
        addView(view);
        ButterKnife.bind(this);
    }

    public void setSizeAndPosition(Rect lassoRect) {
        int mW = dpToPx(MIN_WIDTH_DP);
        int mH = dpToPx(MIN_HEIGHT_DP);
        int paddingW = 0;
        int paddingH = 0;
        if (lassoRect.width() < mW) paddingW = mW - lassoRect.width();
        if (lassoRect.height() < mH) paddingH = mH - lassoRect.height();

        float left = lassoRect.left - paddingW / 2;
        float top = lassoRect.top - paddingH / 2;
        float right = lassoRect.right + paddingW / 2;
        float bottom = lassoRect.bottom + paddingH / 2;
        PointF[] points = new PointF[4];
        points[0] = new PointF(left, top);
        points[1] = new PointF(left, bottom);
        points[2] = new PointF(right, bottom);
        points[3] = new PointF(right, top);
        rectangle = new Rectangle(points);
        rectangle.setMinWidth(mW);
        rectangle.setMinHeight(mH);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        lp.width = (int) rectangle.getWidth();
        lp.height = (int) rectangle.getHeight();
        setX(rectangle.getX());
        setY(rectangle.getY());

        post(new Runnable() {
            @Override
            public void run() {
                int[] xy = new int[2];
                getLocationOnScreen(xy);
                canvasScreenDiffX = xy[0] - getX();
                canvasScreenDiffY = xy[1] - getY();
            }
        });
    }

    private void setupOnTouchListener() {
        setOnTouchListener(new OnTouchListener() {
            float cX, cY;
            float pX, pY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cX = event.getRawX() - canvasScreenDiffX;
                cY = event.getRawY() - canvasScreenDiffY;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pX = cX;
                        pY = cY;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dX = cX - pX;
                        float dY = cY - pY;
                        // Rectangle
                        rectangle.translate(dX, dY);
                        // Lasso
                        Matrix m = new Matrix();
                        m.setTranslate(dX, dY);
                        lasso.transform(m);
                        // LassoBox
                        setX(rectangle.getX());
                        setY(rectangle.getY());
                        pX = cX;
                        pY = cY;
                        invalidate();
                        ((ViewGroup) getParent()).invalidate();
                        if (onTranslateListener != null)
                            onTranslateListener.onTranslate(dX, dY);
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return true;
            }
        });
    }

    private void setScale() {
        OnTouchListener onTouchListener = new OnTouchListener() {
            float downX, downY;
            float dX, dY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float cX = event.getRawX() - canvasScreenDiffX;
                float cY = event.getRawY() - canvasScreenDiffY;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = cX;
                        downY = cY;
                        if (v == scaleRightBottomButton) {
                            dX = cX - rectangle.getX(Rectangle.Points.RightBottom);
                            dY = cY - rectangle.getY(Rectangle.Points.RightBottom);
                        } else if (v == scaleLeftTopButton) {
                            dX = cX - rectangle.getX(Rectangle.Points.LeftTop);
                            dY = cY - rectangle.getY(Rectangle.Points.LeftTop);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (v == scaleRightBottomButton) {
                            float beforeW = rectangle.getWidth();
                            float beforeH = rectangle.getHeight();
                            rectangle.scaleByMoveRightBottomPoint(cX - dX, cY - dY);
                            float sx = rectangle.getWidth() / beforeW;
                            float sy = rectangle.getHeight() / beforeH;

                            float px = rectangle.getX(Rectangle.Points.LeftTop);
                            float py = rectangle.getY(Rectangle.Points.LeftTop);
                            Matrix m = new Matrix();
                            m.setRotate(-rectangle.getRotation(), px, py);
                            lasso.transform(m);
                            m.setScale(sx, sy, px, py);
                            lasso.transform(m);
                            m.setRotate(rectangle.getRotation(), px, py);
                            lasso.transform(m);
                        }
                        else if (v == scaleLeftTopButton) {
                            float beforeW = rectangle.getWidth();
                            float beforeH = rectangle.getHeight();
                            rectangle.scaleByMoveLeftTopPoint(cX - dX, cY - dY);
                            float sx = rectangle.getWidth() / beforeW;
                            float sy = rectangle.getHeight() / beforeH;

                            float px = rectangle.getX(Rectangle.Points.RightBottom);
                            float py = rectangle.getY(Rectangle.Points.RightBottom);
                            Matrix m = new Matrix();
                            m.setRotate(-rectangle.getRotation(), px, py);
                            lasso.transform(m);
                            m.setScale(sx, sy, px, py);
                            lasso.transform(m);
                            m.setRotate(rectangle.getRotation(), px, py);
                            lasso.transform(m);
                        }
                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
                        lp.width = (int) rectangle.getWidth();
                        lp.height = (int) rectangle.getHeight();
                        setLayoutParams(lp);
                        setX(rectangle.getX());
                        setY(rectangle.getY());
                        invalidate();
                        ((ViewGroup) getParent()).invalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return false;
            }
        };
        scaleLeftTopButton.setOnTouchListener(onTouchListener);
        scaleRightBottomButton.setOnTouchListener(onTouchListener);
    }



    private void setRotate() {
        OnTouchListener onTouchListener = new OnTouchListener() {
            float cX, cY;
            float cD;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cX = event.getRawX() - canvasScreenDiffX;
                cY = event.getRawY() - canvasScreenDiffY;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        cD = (float) Math.toDegrees(Math.atan((cY - rectangle.getCenterY()) / (cX - rectangle.getCenterX())));
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float d = (float) Math.toDegrees(Math.atan((cY - rectangle.getCenterY()) / (cX - rectangle.getCenterX())));
                        float dD = d - cD;
                        rectangle.rotate(dD);
                        Matrix m = new Matrix();
                        m.setRotate(dD, rectangle.getCenterX(), rectangle.getCenterY());
                        lasso.transform(m);
                        setRotation(rectangle.getRotation());
                        cD = d;
                        invalidate();
                        ((ViewGroup) getParent()).invalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return true;
            }
        };
        rotateRightTopButton.setOnTouchListener(onTouchListener);
        rotateLeftBottomButton.setOnTouchListener(onTouchListener);
    }

    private void onTranslate(float x, float y) {
        if (onTranslateListener != null)
            onTranslateListener.onTranslate(x, y);
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public float getLassoCenterX() {
        return rectangle.getCenterX();
    }

    public float getLassoCenterY() {
        return rectangle.getCenterY();
    }

    public void setOnTranslateListener(OnTranslateListener onTranslateListener) {
        this.onTranslateListener = onTranslateListener;
    }

    public void setOnRotateListener(OnRotateListener onRotateListener) {
        this.onRotateListener = onRotateListener;
    }

    public interface OnTranslateListener {
        void onTranslate(float x, float y);
    }

    public interface OnRotateListener {
        void onRotate(float degree);
    }

    List<Bitmap> bitmapList = new ArrayList<>();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Bitmap bitmap : bitmapList) {
            canvas.drawBitmap(bitmap, 0, 0, null);
            bitmap.recycle();
        }
        bitmapList.clear();
    }

    /**
     * Use to compute scale, rotation result.
     *
     * <pre>
     * 0---------3
     * |             |
     * |             |
     * |             |
     * 1---------2
     * </pre>
     */
    static class Rectangle {
        public enum Points {
            LeftTop(0), LeftBottom(1), RightBottom(2), RightTop(3);
            private int value;
            Points(int value) { this.value = value; }
        };
        private float[][] points = new float[4][2];
        private float degrees = 0;
        private int minWidth = 0;
        private int minHeight = 0;

        /**
         * @param points lef-top, left-bottom, right-bottom, right-top.
         */
        public Rectangle(PointF[] points) {
            for (int i = 0; i < 4; i++) {
                this.points[i][0] = points[i].x;
                this.points[i][1] = points[i].y;
            }
        }

        public void setMinWidth(int minWidth) {
            this.minWidth = minWidth;
        }

        public void setMinHeight(int minHeight) {
            this.minHeight = minHeight;
        }

        public Path getPath() {
            Path p = new Path();
            p.moveTo(points[0][0], points[0][1]);
            for (int i = 1; i < 4; i++)
                p.lineTo(points[i][0], points[i][1]);
            p.close();
            return p;
        }

        public void rotate(float degrees, float px, float py) {
            Matrix m = new Matrix();
            m.setRotate(degrees, px, py);
            mapPoints(m);
            this.degrees += degrees;
            this.degrees %= 360;
        }

        public void rotate(float degrees) {
            rotate(degrees, getCenterX(), getCenterY());
        }

        public void translate(float dx, float dy) {
            Matrix m = new Matrix();
            m.setTranslate(dx, dy);
            mapPoints(m);
        }

        public void scaleByMoveRightBottomPoint(float nx, float ny) {
            // Let left-top point as pivot
            points[2][0] = nx;
            points[2][1] = ny;
            Matrix m = new Matrix();
            m.setRotate(-degrees, points[0][0], points[0][1]);
            m.mapPoints(points[2]);
            if (points[2][0] - points[0][0] < minWidth) points[2][0] = points[0][0] + minWidth;
            if (points[2][1] - points[0][1] < minHeight) points[2][1] = points[0][1] + minHeight;
            points[1][0] = points[0][0];
            points[1][1] = points[2][1];
            points[3][0] = points[2][0];
            points[3][1] = points[0][1];
            m.setRotate(degrees, points[0][0], points[0][1]);
            m.mapPoints(points[1]);
            m.mapPoints(points[2]);
            m.mapPoints(points[3]);
        }

        public void scaleByMoveLeftTopPoint(float nx, float ny) {
            // Let right-bottom point as pivot
            points[0][0] = nx;
            points[0][1] = ny;
            Matrix m = new Matrix();
            m.setRotate(-degrees, points[2][0], points[2][1]);
            m.mapPoints(points[0]);
            if (points[2][0] - points[0][0] < minWidth) points[0][0] = points[2][0] - minWidth;
            if (points[2][1] - points[0][1] < minHeight) points[0][1] = points[2][1] - minHeight;
            points[1][0] = points[0][0];
            points[1][1] = points[2][1];
            points[3][0] = points[2][0];
            points[3][1] = points[0][1];
            m.setRotate(degrees, points[2][0], points[2][1]);
            m.mapPoints(points[0]);
            m.mapPoints(points[1]);
            m.mapPoints(points[3]);
        }

        private float getX(Points points) {
            return this.points[points.value][0];
        }

        private float getY(Points points) {
            return this.points[points.value][1];
        }

        private void mapPoints(Matrix m) {
            for (int i = 0; i < 4; i++)
                m.mapPoints(points[i]);
        }

        public float getCenterX() {
            return (points[0][0] + points[2][0]) / 2;
        }

        public float getCenterY() {
            return (points[0][1] + points[2][1]) / 2;
        }

        public float getWidth() {
            return (float) Math.sqrt(Math.pow(points[0][0] - points[3][0], 2) + Math.pow(points[0][1] - points[3][1], 2));
        }

        public float getHeight() {
            return (float) Math.sqrt(Math.pow(points[0][0] - points[1][0], 2) + Math.pow(points[0][1] - points[1][1], 2));
        }

        public float getRotation() {
            return degrees;
        }

        public float getX() {
            Matrix m = new Matrix();
            m.setRotate(-degrees, getCenterX(), getCenterY());
            float[] f = new float[2];
            m.mapPoints(f, points[0]);
            return f[0];
        }

        public float getY() {
            Matrix m = new Matrix();
            m.setRotate(-degrees, getCenterX(), getCenterY());
            float[] f = new float[2];
            m.mapPoints(f, points[0]);
            return f[1];
        }
    }

    class TopView extends View {
        public TopView(Context context) {
            super(context);
            setWillNotDraw(false);
            pPaint = new Paint();
            pPaint.setColor(Color.RED);
            pPaint.setStyle(Paint.Style.STROKE);
            pPaint.setStrokeWidth(10);
            cPaint = new Paint(pPaint);
            cPaint.setColor(Color.BLUE);
            lPaint = new Paint(pPaint);
            lPaint.setColor(Color.GREEN);
            lPaint.setStrokeWidth(1);
            rPaint = new Paint(lPaint);
            rPaint.setColor(Color.YELLOW);
            rPaint.setStrokeWidth(3);
        }

        float cX, cY;
        float pX, pY;
        float ppX, ppY;
        Rect r;

        Paint pPaint, cPaint, lPaint, rPaint;

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPoint(cX, cY, cPaint);
            canvas.drawPoint(pX, pY, pPaint);
            canvas.drawLine(cX, cY, pX, pY, lPaint);
            Paint t = new Paint(cPaint);
            t.setColor(Color.CYAN);
            t.setStrokeWidth(20);
//            canvas.drawPoint(ppX, ppY, t);
        }
    }
}
