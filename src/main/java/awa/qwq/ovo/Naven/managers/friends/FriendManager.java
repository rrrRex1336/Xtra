package awa.qwq.ovo.Naven.managers.friends;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class FriendManager {
   private static final List<String> friends = new CopyOnWriteArrayList<>();

   public static boolean isFriend(Entity player) {
      return player instanceof PlayerEntity && isFriend(player.getName().getString());
   }

   public static boolean isFriend(String player) {
      return player != null && friends.contains(player);
   }

   public static void addFriend(PlayerEntity player) {
      friends.add(player.getName().getString());
   }

   public static void addFriend(String name) {
      friends.add(name);
   }

   public static void removeFriend(PlayerEntity player) {
      friends.remove(player.getName().getString());
   }

   public static List<String> getFriends() {
      return friends;
   }
}
