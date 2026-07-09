package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.Naven;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;

public class ChatUtils {
   private static final String PREFIX = "§d" + Naven.CLIENT_DISPLAY_NAME + " §7>> ";

   public static void component(Text component) {
      ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
      chat.addMessage(component);
   }

   public static void addChatMessage(String message) {
      addChatMessage(true, message);
   }

   public static void addChatMessage(boolean prefix, String message) {
      component(Text.of((prefix ? PREFIX : "") + message));
   }
}
