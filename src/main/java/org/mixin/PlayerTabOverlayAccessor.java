package org.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHud.class)
public interface PlayerTabOverlayAccessor {
   @Accessor("header")
   Text getHeader();

   @Accessor("footer")
   Text getFooter();
}
