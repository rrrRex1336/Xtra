package awa.qwq.ovo.Naven.protocol.heypixel.network;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChannelRegistry {
   private final Set<String> channels = new HashSet<>();
   private int registerCount;

   public int getRegisterCount() {
      return this.registerCount;
   }

   public void incrementRegisterCount() {
      this.registerCount++;
   }

   public void parseAndAdd(byte[] data) {
      String content = new String(data == null ? new byte[0] : data, StandardCharsets.UTF_8);
      for (String channel : content.split("\0")) {
         if (!channel.isEmpty()) {
            this.channels.add(channel);
         }
      }
   }

   public void addChannels(Iterable<String> channels) {
      for (String channel : channels) {
         this.channels.add(channel);
      }
   }

   public byte[] serialize() {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      for (String channel : this.channels) {
         byte[] bytes = channel.getBytes(StandardCharsets.UTF_8);
         out.write(bytes, 0, bytes.length);
         out.write(0);
      }
      return out.toByteArray();
   }

   public Set<String> getChannels() {
      return Collections.unmodifiableSet(this.channels);
   }
}
