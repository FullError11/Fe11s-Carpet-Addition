package feca.utils;

public class CompareUtils {
    public static int cmpNearZero(double a, double b) {
        int c = Double.compare(Math.abs(a), Math.abs(b));
        if (c != 0) return c;
        return Double.compare(b, a); // 距离相同：值大的（正数）在前
    }

    // 单点：先比距离，再逐坐标
    public static int cmpPoint(double x1, double y1, double z1,
                                double x2, double y2, double z2) {
        double d1 = x1 * x1 + y1 * y1 + z1 * z1; // 平方距离，等价于距离比较
        double d2 = x2 * x2 + y2 * y2 + z2 * z2;
        int c = Double.compare(d1, d2);
        if (c != 0) return c;
        if ((c = cmpNearZero(x1, x2)) != 0) return c;
        if ((c = cmpNearZero(y1, y2)) != 0) return c;
        return cmpNearZero(z1, z2);
    }
}
