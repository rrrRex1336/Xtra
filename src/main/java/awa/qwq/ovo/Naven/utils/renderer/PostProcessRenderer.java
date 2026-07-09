package awa.qwq.ovo.Naven.utils.renderer;

import net.minecraft.client.util.math.MatrixStack;

public class PostProcessRenderer {
   private static Mesh mesh;

   public static void init() {
      mesh = new Mesh(DrawMode.Triangles, Mesh.Attrib.Vec2);
      mesh.begin();
      mesh.quad(mesh.vec2(-1.0, -1.0).next(), mesh.vec2(-1.0, 1.0).next(), mesh.vec2(1.0, 1.0).next(), mesh.vec2(1.0, -1.0).next());
      mesh.end();
   }

   public static void beginRender(MatrixStack stack) {
      mesh.beginRender(stack);
   }

   public static void render(MatrixStack stack) {
      mesh.render(stack);
   }

   public static void endRender() {
      mesh.endRender();
   }
}
