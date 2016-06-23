package rd.canvaslasso;

import android.graphics.Matrix;
import android.graphics.Path;

/**
 * Created by Wei on 2016/6/23.
 */
public class Utils {
    public static void rotatePath(float degrees, float px, float py, Path path) {
        Matrix m = createRotationMatrix(degrees, px, py);
        path.transform(m);
    }

    public static void scalePath(float sx, float sy, float px, float py, Path path) {
        Matrix m = createScaleMatrix(sx, sy, px, py);
        path.transform(m);
    }

    public static void rotatePoints(float degrees, float px, float py, float[] ... points) {
        Matrix m = createRotationMatrix(degrees, px, py);
        for (int i = 0; i < points.length; i++)
            m.mapPoints(points[i]);
    }

    public static void rotatePoint(float degrees, float px, float py, float[] dst, float[] src) {
        createRotationMatrix(degrees, px, py).mapPoints(dst, src);
    }

    public static Matrix createRotationMatrix(float degrees, float px, float py) {
        Matrix m = new Matrix();
        m.setRotate(degrees, px, py);
        return m;
    }

    public static Matrix createScaleMatrix(float sx, float sy, float px, float py) {
        Matrix m = new Matrix();
        m.setScale(sx, sy, px, py);
        return m;
    }
}
