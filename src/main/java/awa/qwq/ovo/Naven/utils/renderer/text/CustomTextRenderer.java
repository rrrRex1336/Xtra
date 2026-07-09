package awa.qwq.ovo.Naven.utils.renderer.text;

import awa.qwq.ovo.Naven.utils.renderer.*;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;

public class CustomTextRenderer {
   private static final Logger log = LogManager.getLogger(CustomTextRenderer.class);
   private static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);
   public final Mesh mesh = new ShaderMesh(Shaders.TEXT, DrawMode.Triangles, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color);
   private final Font font;

   public CustomTextRenderer(String name, int size, int from, int to, int textureSize) {
      InputStream in = this.getClass().getResourceAsStream("/assets/naven-modern/client/fonts/" + name + ".ttf");
      if (in == null) {
         throw new RuntimeException("Font not found: " + name);
      } else {
         byte[] bytes;
         try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            int len;
            while ((len = in.read(buffer)) != -1) {
               out.write(buffer, 0, len);
            }

            bytes = out.toByteArray();
         } catch (IOException var11) {
            throw new RuntimeException("Failed to read font: " + name, var11);
         }

         ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);
         ((Buffer)buffer).flip();
         long startTime = System.currentTimeMillis();
         this.font = new Font(buffer, size, from, to, textureSize);
         log.info("Loaded font {} in {}ms", name, System.currentTimeMillis() - startTime);
      }
   }

   public void setAlpha(float alpha) {
      this.mesh.alpha = alpha;
   }

   public float getWidth(String text, double scale) {
      return (float)this.getWidth(text, false, scale);
   }

   public double getWidth(String text, boolean shadow, double scale) {
      return (this.font.getWidth(text) + (shadow ? 0.5F : 0.0F)) * scale;
   }
   public double drawString(MatrixStack stack, String text, double x, double y, Color color, boolean shadow, double scale) {
      Color currentColor = color;
      double currentX = x;
      double totalWidth = 0;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);

         if (c == '\u00A7' && i + 1 < text.length()) {
            char ctrl = text.charAt(i + 1);
            Formatting byCode = Formatting.byCode(ctrl);
            if (byCode != null && byCode.isColor()) {
               currentColor = new Color(byCode.getColorValue());
            } else if (byCode == Formatting.RESET) {
               currentColor = color;
            }

            i++;
            continue;
         }


            String charStr = String.valueOf(c);
            this.mesh.begin();
            double width;
            if (shadow) {
               width = this.font.render(this.mesh, charStr, currentX + 0.5, y + 0.5, SHADOW_COLOR, scale, true);
               this.font.render(this.mesh, charStr, currentX, y, currentColor, scale, false);
            } else {
               width = this.font.render(this.mesh, charStr, currentX, y, currentColor, scale, false);
            }
            this.mesh.end();
            GL.bindTexture(this.font.texture.getGlId());
            this.mesh.render(stack);
            totalWidth += width;
            currentX += this.font.getWidth(charStr) * scale;
         }

      return totalWidth;
   }
   public double getHeight(boolean shadow, double scale) {
      return (this.font.getHeight() + (shadow ? 0.5F : 0.0F)) * scale;
   }

   public double render(MatrixStack stack, String text, double x, double y, Color color, boolean shadow, double scale) {
      this.mesh.begin();
      double width;
      if (shadow) {
         width = this.font.render(this.mesh, text, x + 0.5, y + 0.5, SHADOW_COLOR, scale, true);
         this.font.render(this.mesh, text, x, y, color, scale, false);
      } else {
         width = this.font.render(this.mesh, text, x, y, color, scale, false);
      }

      this.mesh.end();
      GL.bindTexture(this.font.texture.getGlId());
      this.mesh.render(stack);
      return width;
   }
}
