package awa.qwq.ovo.Naven.modules;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventKey;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.exceptions.NoSuchModuleException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import awa.qwq.ovo.Naven.modules.impl.combat.*;
import awa.qwq.ovo.Naven.modules.impl.misc.*;
import awa.qwq.ovo.Naven.modules.impl.movement.*;
import awa.qwq.ovo.Naven.modules.impl.player.*;
import awa.qwq.ovo.Naven.modules.impl.visual.*;
import awa.qwq.ovo.Naven.modules.impl.world.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleManager {
   private static final Logger log = LogManager.getLogger(ModuleManager.class);
   private final List<Module> modules = new ArrayList<>();
   private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
   private final Map<String, Module> nameMap = new HashMap<>();

   public ModuleManager() {
      try {
         this.initModules();
         this.modules.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
      } catch (Exception var2) {
         log.error("Failed to initialize modules", var2);
         throw new RuntimeException(var2);
      }

      Naven.getInstance().getEventManager().register(this);
   }

   private void initModules() {
      this.registerModule(
         new KillAura(),
         new ExtraKB(),
         new LegitSpeed(),
         new AutoThrow(),
         new Piercing(),
         new ChestAura(),
         new ShieldBreaker(),
         new StopMove(),
         new AutoPlay(),
         new BackTrack(),
         new FastCobweb(),
         new KillerDetection(),
         new TNTWarning(),
         new AutoOffHand(),
         new TargetInfo(),
         new WaterMark(),
         new ModuleList(),
         new HotKeys(),
         new Interface(),
         new Velocity(),
         new NameTags(),
         new ContainerStealer(),
         new Timer(),
         new Surround(),
         new InventoryManager(),
         new Scaffold(),
         new Rotation(),
         new TargetStrafe(),
         new PearlInfo(),
         new KeepSprint(),
         new Flight(),
         new Speed(),
         new AntiBots(),
         new Sprint(),
         new ChestESP(),
         new Criticals(),
         new ClickGUIModule(),
         new EffectDisplay(),
         new Teams(),
         new OldHitting(),
         new Glow(),
         new LagRange(),
         new ItemTracker(),
         new NoFall(),
         new AutoMLG(),
         new SelfRescue(),
         new BedAura(),
         new InventoryMove(),
         new Island(),
         new ClientSpoofer(),
         new Protocol(),
         new ViaVersionFix(),
         new NoJumpDelay(),
         new FastPlace(),
         new AntiFireball(),
         new MiddlePearl(),
         new Stuck(),
         new ScoreboardSpoof(),
         new AutoTools(),
         new AutoHeal(),
         new NoRotate(),
         new ViewClip(),
         new Disabler(),
         new NoPush(),
         new Projectile(),
         new BedPlates(),
         new TimeChanger(),
         new FastMine(),
         new FullBright(),
         new NameProtect(),
         new NoHurtCam(),
         new NoFOV(),
         new AutoClicker(),
         new AntiBlindness(),
         new AntiNausea(),
         new FastPickup(),
         new LowFire(),
         new Scoreboard(),
         new Compass(),
         new ItemPhysics(),
         new Blink(),
         new PostProcess(),
         new CrystalAura(),
         new NoRender(),
         new ItemTags(),
         new SafeWalk(),
         new AimAssist(),
         new MotionBlur(),
         new Helper(),
         new Aura(),
         new NoSlow(),
         new LongJump()
      );
   }

   private void registerModule(Module... modules) {
      for (Module module : modules) {
         this.registerModule(module);
      }
   }

   private void registerModule(Module module) {
      module.initModule();
      this.modules.add(module);
      this.classMap.put((Class<? extends Module>)module.getClass(), module);
      this.nameMap.put(module.getName().toLowerCase(), module);
   }

   public List<Module> getModulesByCategory(Category category) {
      List<Module> modules = new ArrayList<>();

      for (Module module : this.modules) {
         if (module.getCategory() == category) {
            modules.add(module);
         }
      }

      return modules;
   }

   public Module getModule(Class<? extends Module> clazz) {
      Module module = this.classMap.get(clazz);
      if (module == null) {
         throw new NoSuchModuleException();
      } else {
         return module;
      }
   }

   public Module getModule(String name) {
      Module module = this.nameMap.get(name.toLowerCase());
      if (module == null) {
         throw new NoSuchModuleException();
      } else {
         return module;
      }
   }

   @EventTarget
   public void onKey(EventKey e) {
      if (e.isState() && MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.modules) {
            if (module.getKey() == e.getKey()) {
               module.toggle();
            }
         }
      }
   }

   @EventTarget
   public void onKey(EventMouseClick e) {
      if (!e.isState() && (e.getKey() == 3 || e.getKey() == 4)) {
         for (Module module : this.modules) {
            if (module.getKey() == -e.getKey()) {
               module.toggle();
            }
         }
      }
   }

   public List<Module> getModules() {
      return this.modules;
   }
}
