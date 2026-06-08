package awa.qwq.ovo.Naven.utils;

public class Easing {
    public static final EasingFunction EASE_OUT_ELASTIC = x -> {
        float c4 = (float) ((2 * Math.PI) / 3);
        return x == 0 ? 0 : x == 1 ? 1 : (float) (Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1);
    };

    public static final EasingFunction EASE_IN_BACK = x -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return c3 * x * x * x - c1 * x * x;
    };

    public static final EasingFunction EASE_OUT_SINE = x -> (float) Math.sin((x * Math.PI) / 2);

    public static final EasingFunction EASE_OUT_QUINT = x -> (float) (1 - Math.pow(1 - x, 5));

    public static final EasingFunction LINEAR = x -> x;
}