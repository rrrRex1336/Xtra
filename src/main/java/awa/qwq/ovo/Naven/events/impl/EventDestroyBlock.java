package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class EventDestroyBlock implements Event {
   private final BlockPos pos;
   private final Direction face;

   public EventDestroyBlock(BlockPos pos, Direction face) {
      this.pos = pos;
      this.face = face;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public Direction getFace() {
      return this.face;
   }
}
