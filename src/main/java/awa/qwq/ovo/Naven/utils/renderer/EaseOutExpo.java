package awa.qwq.ovo.Naven.utils.renderer;

public class EaseOutExpo implements Easing {
    @Override
    public double apply(double t) {
        t = clamp01(t);
        return t == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
    }
}