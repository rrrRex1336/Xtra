package awa.qwq.ovo.Naven.protocol.heypixel.utils;

import java.util.ArrayDeque;
import java.util.Queue;

public class ClickTracker {
   private static final int TIME_WINDOW_MS = 1000;
   private final Queue<Long> leftClickQueue = new ArrayDeque<>();
   private final Queue<Long> rightClickQueue = new ArrayDeque<>();

   public synchronized void recordLeftClick() {
      this.recordClick(this.leftClickQueue);
   }

   public synchronized void recordRightClick() {
      this.recordClick(this.rightClickQueue);
   }

   public void recordSwingArm() {
      this.recordLeftClick();
   }

   public void recordUseItem() {
      this.recordRightClick();
   }

   public synchronized int getLeftClickCount() {
      return this.getClickCount(this.leftClickQueue);
   }

   public synchronized int getRightClickCount() {
      return this.getClickCount(this.rightClickQueue);
   }

   public synchronized int getClickCount(boolean leftClick, int windowMs) {
      Queue<Long> queue = leftClick ? this.leftClickQueue : this.rightClickQueue;
      long threshold = System.currentTimeMillis() - windowMs;
      int count = 0;
      for (Long timestamp : queue) {
         if (timestamp >= threshold) {
            count++;
         }
      }
      return count;
   }

   private void recordClick(Queue<Long> queue) {
      queue.add(System.currentTimeMillis());
      cleanupQueue(queue);
   }

   private int getClickCount(Queue<Long> queue) {
      cleanupQueue(queue);
      return queue.size();
   }

   private static void cleanupQueue(Queue<Long> queue) {
      long threshold = System.currentTimeMillis() - TIME_WINDOW_MS;
      while (!queue.isEmpty() && queue.peek() < threshold) {
         queue.poll();
      }
   }
}
