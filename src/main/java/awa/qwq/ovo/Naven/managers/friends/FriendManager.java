package awa.qwq.ovo.Naven.managers.friends;

import java.util.List;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class FriendManager {
   private static final List<String> friends = new CopyOnWriteArrayList<>();
   private static final Set<String> ircFriends = ConcurrentHashMap.newKeySet();

   public static boolean isFriend(Entity player) {
      return player instanceof Player && isFriend(player.getName().getString());
   }

   public static boolean isFriend(String player) {
      return player != null && (friends.contains(player) || ircFriends.contains(player));
   }

   public static void addFriend(Player player) {
      friends.add(player.getName().getString());
   }

   public static void addFriend(String name) {
      friends.add(name);
   }

   public static void removeFriend(Player player) {
      friends.remove(player.getName().getString());
   }

   public static void setIrcFriends(Collection<String> names) {
      ircFriends.clear();
      if (names == null) {
         return;
      }

      for (String name : names) {
         if (name != null && !name.isEmpty()) {
            ircFriends.add(name);
         }
      }
   }

   public static boolean isIrcFriend(String player) {
      return player != null && ircFriends.contains(player);
   }

   public static List<String> getFriends() {
      return friends;
   }
}
