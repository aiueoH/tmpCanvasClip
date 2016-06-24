package rd.canvaslasso;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LassoManipulationBox extends RelativeLayout {
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

    private OnTranslateListener onTranslateListener;
    private OnRotateListener onRotateListener;
    private OnScaleListener onScaleListener;

    private float canvasScreenDiffX;
    private float canvasScreenDiffY;
    private Rectangle rectangle;
    private int minWidth = dpToPx(MIN_WIDTH_DP);
    private int minHeight = dpToPx(MIN_HEIGHT_DP);
    private int paddingWidth = 0;
    private int paddingHeight = 0;

    public LassoManipulationBox(Context context, RelativeLayout parent, Rect lassoBounds) {
        super(context);
        parent.addView(this);
        initView();
        computeMinWidthHeight(lassoBounds);
        setupRectangle(lassoBounds, minWidth, minHeight, paddingWidth, paddingHeight);
        setSizeAndPosition();
        computeCanvasScreenDiffXY();
        setupTranslationProcessor();
        setupRotationProcessor();
        setupScalingProcessor();
    }

    private void initView() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.widget_lasso_manipulation_box, null);
        addView(view);
        ButterKnife.bind(this);
    }

    public void setSizeAndPosition() {
        setSize((int) rectangle.getWidth(), (int) rectangle.getHeight());
        setX(rectangle.getX());
        setY(rectangle.getY());
    }

    private void computeMinWidthHeight(Rect lassoBounds) {
        if (lassoBounds.width() < minWidth) paddingWidth = minWidth - lassoBounds.width();
        if (lassoBounds.height() < minHeight) paddingHeight = minHeight - lassoBounds.height();
    }

    private void computeCanvasScreenDiffXY() {
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

    private void setupRectangle(Rect lassoBounds, int minWidth, int minHeight, int paddingW, int paddingH) {
        float left = lassoBounds.left - paddingW / 2;
        float top = lassoBounds.top - paddingH / 2;
        float right = lassoBounds.right + paddingW / 2;
        float bottom = lassoBounds.bottom + paddingH / 2;
        PointF[] points = new PointF[] {
            new PointF(left, top),
            new PointF(left, bottom),
            new PointF(right, bottom),
            new PointF(right, top)
        };
        rectangle = new Rectangle(points);
        rectangle.setMinWidth(minWidth);
        rectangle.setMinHeight(minHeight);
    }

    private void setupTranslationProcessor() {
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

    private void setupScalingProcessor() {
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

    private void setupRotationProcessor() {
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

    private void onScale(float sx, float sy, float px, float py) {
        if (onScaleListener != null)
            onScaleListener.onScale(sx, sy, px, py);
    }

    private void onRotate(float dD, float px, float py) {
        if (onRotateListener != null)
            onRotateListener.onRotate(dD, px, py);
    }


    private void onTranslate(float dx, float dy) {
        if (onTranslateListener != null)
            onTranslateListener.onTranslate(dx, dy);
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

    private void setSize(int width, int height) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.width = width;
        lp.height = height;
        setLayoutParams(lp);
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public float getCenterX() {
        return rectangle.getCenterX();
    }

    public float getCenterY() {
        return rectangle.getCenterY();
    }

    public void setOnScaleListener(OnScaleListener onScaleListener) {
        this.onScaleListener = onScaleListener;
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

        public void rotate(float degrees, float px, float py) {
            Utils.rotatePoints(degrees, px, py, points[0], points[1], points[2], points[3]);
            this.degrees += degrees;
            this.degrees %= 360;
        }

        public void rotate(float degrees) {
            rotate(degrees, getCenterX(), getCenterY());
        }

        public void translate(float dx, float dy) {
            Utils.translatePoints(dx, dy, new float[][] {points[0], points[1], points[2], points[3]});
        }

        public void scaleByMovePoint(float nx, float ny, Point point) {
            if (point.equals(Point.RightBottom))
                scaleByMoveRightBottomPoint(nx, ny);
            else if (point.equals(Point.LeftTop))
                scaleByMoveLeftTopPoint(nx, ny);
        }

        private void scaleByMoveRightBottomPoint(float nx, float ny) {
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

        private void scaleByMoveLeftTopPoint(float nx, float ny) {
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
}
