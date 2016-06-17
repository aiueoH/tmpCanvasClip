package rd.canvaslasso;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LassoBox extends RelativeLayout {
    private static final int MIN_WIDTH_DP = 100;
    private static final int MIN_HEIGHT_DP = 100;

    @Bind(R.id.imageView_lasso)
    ImageView lassoImageView;
    @Bind(R.id.imageView_sticker)
    ImageView stickerImageView;
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

    private float rawCenterX;
    private float rawCenterY;

    public LassoBox(Context context) {
        super(context);
        initView();
        setupOnTouchListener();
    }

    private void initView() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.widget_lasso_frame, null);
        addView(view);
        ButterKnife.bind(this);
        setRoate();
    }

    public void setSizeAndPosition(Rect lassoRect) {
        int mW = dpToPx(MIN_WIDTH_DP);
        int mH = dpToPx(MIN_HEIGHT_DP);
        int paddingW = 0;
        int paddingH = 0;
        if (lassoRect.width() < mW) paddingW = mW - lassoRect.width();
        if (lassoRect.height() < mH) paddingH = mH - lassoRect.height();
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        lp.setMargins(lassoRect.left - paddingW / 2, lassoRect.top - paddingH / 2, 0 ,0);
        lp.width = lassoRect.width() + paddingW;
        lp.height = lassoRect.height() + paddingH;
        setLayoutParams(lp);
        post(new Runnable() {
            @Override
            public void run() {
                updateRawCenterXY();
            }
        });
    }

    private void updateRawCenterXY() {
        if (getRotation() != 0) return;
        int[] location = new int[2];
        getLocationOnScreen(location);
        rawCenterX = location[0] + getWidth() / 2;
        rawCenterY = location[1] + getHeight() / 2;
    }

    private void offsetRawCenterXY(float dX, float dY) {
        rawCenterX += dX;
        rawCenterY += dY;
    }

    private void setupOnTouchListener() {
        setOnTouchListener(new OnTouchListener() {
            float dX, dY, startX, startY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = getX() - event.getRawX();
                        dY = getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        setX(event.getRawX() + dX);
                        setY(event.getRawY() + dY);
                        break;
                    case MotionEvent.ACTION_UP:
                        onTranslate(startX - event.getRawX(), startY - event.getRawY());
                        offsetRawCenterXY(event.getRawX() - startX, event.getRawY() - startY);
                        break;
                }
                return true;
            }
        });
    }
    public Activity activity;
    private void setRoate() {
        rotateRightTopButton.setOnTouchListener(new OnTouchListener() {
            float startDegree;
            float originalRotation;
            float dX, dY;
            TopView topView = new TopView(getContext());
            ViewGroup getRoot() {
                ViewParent viewParent = (ViewGroup) activity.findViewById(android.R.id.content).getRootView();
                return (ViewGroup) viewParent;
            }
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        getRoot().addView(topView);
                        dX = event.getRawX() - rawCenterX;
                        dY = event.getRawY() - rawCenterY;
                        startDegree = (float) Math.toDegrees(Math.atan((dY / dX)));
                        originalRotation = getRotation();
                        topView.cX = rawCenterX;
                        topView.cY = rawCenterY;
                        topView.pX = event.getRawX();
                        topView.pY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        dX = event.getRawX() - rawCenterX;
                        dY = event.getRawY() - rawCenterY;
                        float currentDegree = (float) Math.toDegrees(Math.atan(dY / dX));
                        setRotation(originalRotation + currentDegree - startDegree);
                        invalidate();
                        topView.pX = event.getRawX();
                        topView.pY = event.getRawY();
                        topView.invalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        getRoot().removeView(topView);
                        if (onRotateListener != null)
                            onRotateListener.onRotate(getRotation());
                        break;
                }
                return true;
            }
        });
    }

    private void onTranslate(float x, float y) {
        if (onTranslateListener != null)
            onTranslateListener.onTranslate(x, y);
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public float getLassoX() {
        return getX() + lassoImageView.getX();
    }

    public float getLassoY() {
        return getY() + lassoImageView.getY();
    }

    public void setOnTranslateListener(OnTranslateListener onTranslateListener) {
        this.onTranslateListener = onTranslateListener;
    }

    public void setOnRotateListener(OnRotateListener onRotateListener) {
        this.onRotateListener = onRotateListener;
    }

    public void setLassoBitmap(Bitmap bitmap) {
        lassoImageView.setImageBitmap(bitmap);
    }

    public void setStickerBitmap(Bitmap bitmap) {
        stickerImageView.setImageBitmap(bitmap);
    }

    public interface OnTranslateListener {
        void onTranslate(float x, float y);
    }

    public interface OnRotateListener {
        void onRotate(float degree);
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
        Rect r;

        Paint pPaint, cPaint, lPaint, rPaint;

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPoint(cX, cY, cPaint);
            canvas.drawPoint(pX, pY, pPaint);
            canvas.drawLine(cX, cY, pX, pY, lPaint);
        }
    }
}
