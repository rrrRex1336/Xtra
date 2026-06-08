package awa.qwq.ovo.Naven.utils.animation;

import awa.qwq.ovo.Naven.utils.EasingFunction;

public class AnimationUtils {
   public static int delta;
   private long startTime;
   private long duration;
   private float startValue;
   private float targetValue;
   private float currentValue;
   private boolean running;
   private EasingFunction easing;

   public AnimationUtils(EasingFunction easeOutElastic, int i) {
   }

   public static float getAnimationState(float animation, float finalState, float speed) {
      float add = (float)delta * (speed / 1000.0F);
      if (animation < finalState) {
         if (animation + add < finalState) {
            animation += add;
         } else {
            animation = finalState;
         }
      } else if (animation - add > finalState) {
         animation -= add;
      } else {
         animation = finalState;
      }

      return animation;
   }

    public static float smooth(float current, float target, float speed) {
       long deltaTime = AnimationUtils.delta;

       speed = Math.abs(target - current) * speed;

       if (deltaTime < 1L) {
          deltaTime = 1L;
       }

       final float difference = current - target;
       final float smoothing = Math.max(speed * (deltaTime / 16F), .15F);

       if (difference > speed) {
          current = Math.max(current - smoothing, target);
       } else if (difference < -speed) {
          current = Math.min(current + smoothing, target);
       } else {
          current = target;
       }

       return current;
    }

    public static double smooth(double current, double target, double speed) {
       long deltaTime = AnimationUtils.delta;

       speed = Math.abs(target - current) * speed;

       if (deltaTime < 1L) {
          deltaTime = 1L;
       }

       final double difference = current - target;
       final double smoothing = Math.max(speed * (deltaTime / 16F), .15F);

       if (difference > speed) {
          current = Math.max(current - smoothing, target);
       } else if (difference < -speed) {
          current = Math.min(current + smoothing, target);
       } else {
          current = target;
       }

       return current;
    }

    public void Animation(EasingFunction easing, long duration) {
      this.easing = easing;
      this.duration = duration;
      this.currentValue = 0;
   }

   public void run(float target) {
      this.startValue = this.currentValue;
      this.targetValue = target;
      this.startTime = System.currentTimeMillis();
      this.running = true;
   }

   public void update() {
      if (!running) return;

      long elapsed = System.currentTimeMillis() - startTime;
      float progress = Math.min(1.0f, (float) elapsed / duration);

      if (progress >= 1.0f) {
         currentValue = targetValue;
         running = false;
         return;
      }

      currentValue = startValue + (targetValue - startValue) * easing.apply(progress);
   }

   public float getValue() {
      return currentValue;
   }

   public void setDuration(long duration) {
      this.duration = duration;
   }

   public void setEasing(EasingFunction easing) {
      this.easing = easing;
   }

   public boolean isRunning() {
      return running;
   }
}


