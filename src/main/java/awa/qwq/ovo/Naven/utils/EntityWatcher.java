package awa.qwq.ovo.Naven.utils;

import org.mixin.accessors.LivingEntityAccessor;
import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventRespawn;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import org.antlr.v4.runtime.misc.OrderedHashSet;

public class EntityWatcher {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Map<Entity, Set<String>> tags = new ConcurrentHashMap<>();
   private static final Map<String, SharedESPData> sharedESPData = new ConcurrentHashMap<>();

   public static Set<String> getEntityTags(AbstractClientPlayerEntity player) {
      List<StatusEffect> effects = PotionResolver.resolve((Integer)player.getDataTracker().get(LivingEntityAccessor.getEffectColorId()));
      effects.remove(StatusEffects.ABSORPTION);
      Set<String> currentPlayerTags = new OrderedHashSet();
      if (tags.containsKey(player)) {
         currentPlayerTags.addAll(tags.get(player));
      }

      Set<String> collect = effects.stream()
         .map(effect -> "effect.minecraft." + Registries.STATUS_EFFECT.getId(effect).getPath())
         .collect(Collectors.toSet());
      currentPlayerTags.addAll(collect);
      return currentPlayerTags;
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      tags.clear();
      sharedESPData.clear();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && mc.world != null) {
         getSharedESPData().forEach((ign, data) -> {
            if (System.currentTimeMillis() - data.getUpdateTime() > 500L) {
               getSharedESPData().remove(ign);
            }
         });

         for (AbstractClientPlayerEntity player : new ArrayList<>(mc.world.getPlayers())) {
            if (player != mc.player) {
               if (!tags.containsKey(player)) {
                  tags.put(player, new HashSet<>());
               }

               Set<String> playerTags = tags.get(player);
               if ((InventoryUtils.isGodAxe(player.getMainHandStack()) || InventoryUtils.isGodAxe(player.getOffHandStack())) && !playerTags.contains("God Axe")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding god axe!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("God Axe");
               }

               if ((InventoryUtils.isEnchantedGApple(player.getMainHandStack()) || InventoryUtils.isEnchantedGApple(player.getOffHandStack()))
                  && !playerTags.contains("Enchanted Golden Apple")) {
                  Notification notification = new Notification(
                     NotificationLevel.WARNING, player.getName().getString() + " is holding enchanted golden apple!", 3000L
                  );
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("Enchanted Golden Apple");
               }

               if ((InventoryUtils.isEndCrystal(player.getMainHandStack()) || InventoryUtils.isEndCrystal(player.getOffHandStack()))
                  && !playerTags.contains("End Crystal")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding end crystal!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("End Crystal");
               }

               if ((InventoryUtils.isKBBall(player.getMainHandStack()) || InventoryUtils.isKBBall(player.getOffHandStack())) && !playerTags.contains("KB Ball")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding KB Ball!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("KB Ball");
               }

               if ((InventoryUtils.isKBStick(player.getMainHandStack()) || InventoryUtils.isKBStick(player.getOffHandStack()))
                  && !playerTags.contains("KB Stick")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding KB Stick!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("KB Stick");
               }

               if ((InventoryUtils.getPunchLevel(player.getMainHandStack()) > 2 || InventoryUtils.getPunchLevel(player.getOffHandStack()) > 2)
                  && !playerTags.contains("Punch Bow")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding Punch Bow!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("Punch Bow");
               }

               if ((InventoryUtils.getPowerLevel(player.getMainHandStack()) > 3 || InventoryUtils.getPowerLevel(player.getOffHandStack()) > 3)
                  && !playerTags.contains("Power Bow")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding Power Bow!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("Power Bow");
               }
            }
         }
      }
   }

   public static Map<String, SharedESPData> getSharedESPData() {
      return sharedESPData;
   }
}
