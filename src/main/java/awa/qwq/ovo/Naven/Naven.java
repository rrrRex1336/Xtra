package awa.qwq.ovo.Naven;

import awa.qwq.ovo.Naven.commands.CommandManager;
import awa.qwq.ovo.Naven.events.api.EventManager;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventShutdown;
import awa.qwq.ovo.Naven.files.FileManager;
import awa.qwq.ovo.Naven.modules.ModuleManager;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.impl.visual.ClickGUIModule;
import awa.qwq.ovo.Naven.ui.notification.NotificationManager;
import awa.qwq.ovo.Naven.managers.theme.ThemeManager;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.PostProcessRenderer;
import awa.qwq.ovo.Naven.utils.renderer.Shaders;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.values.HasValueManager;
import awa.qwq.ovo.Naven.values.ValueManager;
import net.minecraft.client.MinecraftClient;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Naven {

   public static final String CLIENT_NAME = "Naven-Modern";
   public static final String CLIENT_DISPLAY_NAME = "Naven";
   private static Naven instance;
   public static boolean isReady;
   private final EventManager eventManager;
   private final EventWrapper eventWrapper;
   private final ValueManager valueManager;
   private final HasValueManager hasValueManager;
   private final RotationManager rotationManager;
   private final ThemeManager themeManager;
   public final ModuleManager moduleManager;
   private final CommandManager commandManager;
   private final FileManager fileManager;
   private final NotificationManager notificationManager;
   private final List<Module> pendingEnableModules = new ArrayList<>();
   public static float TICK_TIMER = 1.0F;
   public static Queue<Runnable> skipTasks = new ConcurrentLinkedQueue<>();
   public static int skipTicks = 0;
   public static MinecraftClient mc = MinecraftClient.getInstance();


   private Naven() {
      System.out.println("Naven Init");
      instance = this;
      this.eventManager = new EventManager();
      Shaders.init();
      PostProcessRenderer.init();

      try {
         Fonts.loadFonts();
      } catch (IOException | FontFormatException var2) {
         throw new RuntimeException(var2);
      }

      this.eventWrapper = new EventWrapper();
      this.valueManager = new ValueManager();
      this.hasValueManager = new HasValueManager();
      this.themeManager = new ThemeManager();
      this.moduleManager = new ModuleManager();
      this.rotationManager = new RotationManager();
      this.commandManager = new CommandManager();
      this.fileManager = new FileManager();
      this.notificationManager = new NotificationManager();
      this.fileManager.load();
      this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
      this.eventManager.register(getInstance());
      this.eventManager.register(this.eventWrapper);
      this.eventManager.register(new RotationManager());
      this.eventManager.register(new NetworkUtils());
      this.eventManager.register(MovementUtils.INSTANCE);
      this.eventManager.register(new ServerUtils());
      this.eventManager.register(new EntityWatcher());
      isReady = true;
   }

   public static boolean isReady() {
      return instance != null
              && Naven.instance.eventManager != null
              && isReady
              && mc != null
              && mc.player != null
              && mc.player.age > 5;
   }

   public static void modRegister() {


      try {
         mc = MinecraftClient.getInstance();
         Module.refreshMinecraft();
         new Naven();
         System.out.println();
         System.out.println("   ███╗   ██╗ █████╗ ██╗   ██╗███████╗███╗   ██╗");
         System.out.println("   ████╗  ██║██╔══██╗██║   ██║██╔════╝████╗  ██║");
         System.out.println("   ██╔██╗ ██║███████║██║   ██║█████╗  ██╔██╗ ██║");
         System.out.println("   ██║╚██╗██║██╔══██║╚██╗ ██╔╝██╔══╝  ██║╚██╗██║");
         System.out.println("   ██║ ╚████║██║  ██║ ╚████╔╝ ███████╗██║ ╚████║");
         System.out.println("   ╚═╝  ╚═══╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚═╝  ╚═══╝");
         System.out.println();
         System.out.println("五月的雨落下时，总是轻的、密的、带着草木破土的腥甜。有人说这是暮春的眼泪，我却觉得那是夏天在敲门——不急不缓，只为唤醒沉睡的种子。而总有阴云堆积的时刻。天低沉得像要塌下来，这时我就想起另一句誓言：愿做破云的那缕曦光。不是轰然的烈日，而是轻轻拨开厚重，一线、一丝，却足够让世界重新看见轮廓。林妍璃”——名字是咒语，也是方向。若五月无雨，我便成雨；若云层不散，我便成光。滋养与照亮，本就是同一颗心发出的两种温度。");

      } catch (Exception var1) {
         System.err.println("[Naven] Client load failed: " + var1.getMessage());
      }
   }

   public void queuePendingEnable(Module module) {
      if (module != null && !this.pendingEnableModules.contains(module)) {
         this.pendingEnableModules.add(module);
      }
   }

   public void enablePendingModulesIfReady() {
      if (mc == null || mc.player == null || mc.world == null || this.pendingEnableModules.isEmpty()) {
         return;
      }

      List<Module> modules = new ArrayList<>(this.pendingEnableModules);
      this.pendingEnableModules.clear();
      for (Module module : modules) {
         module.setEnabled(true);
      }
   }

   @EventTarget
   public void onShutdown(EventShutdown e) {
      this.fileManager.save();
      LogUtils.close();
   }

   @EventTarget(0)
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE) {
         TickTimeHelper.update();
      }
   }

   public static Naven getInstance() {
      return instance;
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public EventWrapper getEventWrapper() {
      return this.eventWrapper;
   }

   public ValueManager getValueManager() {
      return this.valueManager;
   }

   public HasValueManager getHasValueManager() {
      return this.hasValueManager;
   }

   public RotationManager getRotationManager() {
      return this.rotationManager;
   }

   public ThemeManager getThemeManager() {
      return this.themeManager;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public FileManager getFileManager() {
      return this.fileManager;
   }

   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }
}
