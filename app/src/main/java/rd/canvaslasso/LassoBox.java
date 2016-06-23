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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
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
    private OnScaleListener onScaleListener;

    public void setOnScaleListener(OnScaleListener onScaleListener) {
        this.onScaleListener = onScaleListener;
    }


    ////////////////////////////////////////
    private float canvasScreenDiffX;
    private float canvasScreenDiffY;
    private Rectangle rectangle;
    ////////////////////////////////////////
    private Path lasso;

    public LassoBox(Context context, RelativeLayout parent, Path lasso) {
        super(context);
        this.lasso = lasso;
        parent.addView(this);
        initView();
        setSizeAndPosition(Utils.getPathBounds(lasso));
        setupOnTouchListener();
        setRotate();
        setScale();
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
            float cx, cy;
            float px, py;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cx = event.getRawX() - canvasScreenDiffX;
                cy = event.getRawY() - canvasScreenDiffY;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        px = cx;
                        py = cy;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dX = cx - px;
                        float dY = cy - py;
                        px = cx;
                        py = cy;
                        rectangle.translate(dX, dY);
                        setX(rectangle.getX());
                        setY(rectangle.getY());
                        onTranslate(dX, dY);
                        break;
                }
                return true;
            }
        });
    }

    private void setScale() {
        OnTouchListener onTouchListener = new OnTouchListener() {
            float downX, downY;
            float dx, dy;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float cX = event.getRawX() - canvasScreenDiffX;
                float cY = event.getRawY() - canvasScreenDiffY;
                Rectangle.Point movePoint = v == scaleRightBottomButton ? Rectangle.Point.RightBottom : Rectangle.Point.LeftTop;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = cX;
                        downY = cY;
                        dx = cX - rectangle.getX(movePoint);
                        dy = cY - rectangle.getY(movePoint);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float beforeW = rectangle.getWidth();
                        float beforeH = rectangle.getHeight();
                        rectangle.scaleByMovePoint(cX - dx, cY - dy, movePoint);
                        float sx = rectangle.getWidth() / beforeW;
                        float sy = rectangle.getHeight() / beforeH;
                        Rectangle.Point pivot = v == scaleRightBottomButton ? Rectangle.Point.LeftTop : Rectangle.Point.RightBottom;
                        float px = rectangle.getX(pivot);
                        float py = rectangle.getY(pivot);
                        setSize((int) rectangle.getWidth(), (int) rectangle.getHeight());
                        setX(rectangle.getX());
                        setY(rectangle.getY());
                        onScale(sx, sy, px, py);
                        break;
                }
                return true;
            }
        };
        scaleLeftTopButton.setOnTouchListener(onTouchListener);
        scaleRightBottomButton.setOnTouchListener(onTouchListener);
    }

    private void setSize(int width, int height) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.width = width;
        lp.height = height;
        setLayoutParams(lp);
    }

    private void onScale(float sx, float sy, float px, float py) {
        if (onScaleListener != null)
            onScaleListener.onScale(sx, sy, px, py);
    }


    private void setRotate() {
        OnTouchListener onTouchListener = new OnTouchListener() {
            float cx, c;
            float cd;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cx = event.getRawX() - canvasScreenDiffX;
                c = event.getRawY() - canvasScreenDiffY;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        cd = computeDegrees(rectangle.getCenterX(), rectangle.getCenterY(), cx, c);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float d = computeDegrees(rectangle.getCenterX(), rectangle.getCenterY(), cx, c);
                        float dD = d - cd;
                        cd = d;
                        rectangle.rotate(dD);
                        setRotation(rectangle.getRotation());
                        onRotate(dD, rectangle.getCenterX(), rectangle.getCenterY());
                        break;
                }
                return true;
            }
        };
        rotateRightTopButton.setOnTouchListener(onTouchListener);
        rotateLeftBottomButton.setOnTouchListener(onTouchListener);
    }

    private void onRotate(float dD, float px, float py) {
        if (onRotateListener != null)
            onRotateListener.onRotate(dD, px, py);
    }

    private float computeDegrees(float x1, float y1, float x2, float y2) {
        // y axis
        if (x1 == x2) return y2 > y1 ? 90 : -90;
        double d = Math.toDegrees(Math.atan((y2 - y1) / (x2 - x1)));
        // x axis, second quadrant, third quadrant
        if (x2 < x1) return y2 < y1 ? -(180 - (float) d) : 180 - (float) Math.abs(d);
        // first quadrant, fourth quadrant
        return (float) d;
    }

    private void onTranslate(float dx, float dy) {
        if (onTranslateListener != null)
            onTranslateListener.onTranslate(dx, dy);
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
        void onTranslate(float dx, float dy);
    }

    public interface OnRotateListener {
        void onRotate(float degree, float px, float py);
    }

    public interface OnScaleListener {
        void onScale(float sx, float sy, float px, float py);
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
        public enum Point {
            LeftTop(0), LeftBottom(1), RightBottom(2), RightTop(3);
            private int value;
            Point(int value) { this.value = value; }
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
            Utils.rotatePoints(degrees, px, py, points[0], points[1], points[2], points[3]);
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

        public void scaleByMovePoint(float nx, float ny, Point point) {
            if (point.equals(Point.RightBottom))
                scaleByMoveRightBottomPoint(nx, ny);
            else if (point.equals(Point.LeftTop))
                scaleByMoveLeftTopPoint(nx, ny);
        }

        public void scaleByMoveRightBottomPoint(float nx, float ny) {
            // Let left-top point as pivot
            points[2][0] = nx;
            points[2][1] = ny;
            Utils.rotatePoints(-degrees, points[0][0], points[0][1], points[2]);
            if (points[2][0] - points[0][0] < minWidth) points[2][0] = points[0][0] + minWidth;
            if (points[2][1] - points[0][1] < minHeight) points[2][1] = points[0][1] + minHeight;
            points[1][0] = points[0][0];
            points[1][1] = points[2][1];
            points[3][0] = points[2][0];
            points[3][1] = points[0][1];
            Utils.rotatePoints(degrees, points[0][0], points[0][1], points[1], points[2], points[3]);
        }

        public void scaleByMoveLeftTopPoint(float nx, float ny) {
            // Let right-bottom point as pivot
            points[0][0] = nx;
            points[0][1] = ny;
            Utils.rotatePoints(-degrees, points[2][0], points[2][1], points[0]);
            if (points[2][0] - points[0][0] < minWidth) points[0][0] = points[2][0] - minWidth;
            if (points[2][1] - points[0][1] < minHeight) points[0][1] = points[2][1] - minHeight;
            points[1][0] = points[0][0];
            points[1][1] = points[2][1];
            points[3][0] = points[2][0];
            points[3][1] = points[0][1];
            Utils.rotatePoints(degrees, points[2][0], points[2][1], points[0], points[1], points[3]);
        }

        private float getX(Point point) {
            return this.points[point.value][0];
        }

        private float getY(Point point) {
            return this.points[point.value][1];
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
            float[] f = new float[2];
            Utils.rotatePoint(-degrees, getCenterX(), getCenterY(), f, points[0]);
            return f[0];
        }

        public float getY() {
            float[] f = new float[2];
            Utils.rotatePoint(-degrees, getCenterX(), getCenterY(), f, points[0]);
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
        }
    }
}
