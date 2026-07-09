package awa.qwq.ovo.Naven.utils;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PotionResolver {
   private static final Logger log = LogManager.getLogger(PotionResolver.class);
   private static final Map<Integer, List<StatusEffect>> colorMap = new HashMap<>();

   public static List<StatusEffect> resolve(int color) {
      if (colorMap.containsKey(color)) {
         return colorMap.get(color);
      } else if (colorMap.containsKey(color + 1)) {
         return colorMap.get(color + 1);
      } else {
         return colorMap.containsKey(color - 1) ? colorMap.get(color - 1) : Collections.emptyList();
      }
   }

   static {
      InputStream stream = PotionResolver.class.getResourceAsStream("/assets/naven-modern/client/data/potion_effects.dat");
      if (stream != null) {
         try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(stream);

            for (String s : IOUtils.readLines(gzipInputStream)) {
               String[] split = s.split(":");
               if (split.length == 2) {
                  int color = Integer.parseInt(split[0]);
                  String data = split[1];
                  String[] potionIds = data.split("\\+");
                  List<StatusEffect> potions = Arrays.stream(potionIds)
                          .map(Integer::parseInt)
                          .map(id -> Registries.STATUS_EFFECT.get(id))
                          .filter(Objects::nonNull)
                          .collect(Collectors.toList());

                  colorMap.put(color, potions);
               }
            }
         } catch (Exception var9) {
            log.error("Failed to load potion effects", var9);
         }
      }
   }
}
