package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.Island.*;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;

@ModuleInfo(
        name = "Island",
        description = "ISLAND~~~",
        category = Category.VISUAL
)
public class Island extends Module {
    private final IslandManager islandManager = new IslandManager();
    private final ModuleToggleContent moduleToggleContent = new ModuleToggleContent();
    private final ScaffoldContent scaffoldContent = new ScaffoldContent();
    private final CommandPaletteContent commandPaletteContent = new CommandPaletteContent();
    private final ErrorMessageContent errorMessageContent = new ErrorMessageContent();
    private final ChestContent chestContent = new ChestContent();
    private final PlayerListContent playerListContent = new PlayerListContent();

    public final FloatValue xOffset = ValueBuilder.create(this, "X Offset")
            .setMinFloatValue(-10000.0F)
            .setMaxFloatValue(10000.0F)
            .setDefaultFloatValue(0.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> false)
            .build()
            .getFloatValue();

    public final FloatValue yOffset = ValueBuilder.create(this, "Y Offset")
            .setMinFloatValue(-10000.0F)
            .setMaxFloatValue(10000.0F)
            .setDefaultFloatValue(0.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> false)
            .build()
            .getFloatValue();

    private final DragManager dragManager = new DragManager(this.xOffset, this.yOffset);

    public Island() {
        islandManager.addContent(moduleToggleContent);
        islandManager.addContent(scaffoldContent);
        islandManager.addContent(commandPaletteContent);
        islandManager.addContent(errorMessageContent);
        islandManager.addContent(chestContent);
        islandManager.addContent(playerListContent);
        commandPaletteContent.registerEvents();
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        islandManager.render(e.getGuiGraphics());
    }

    @EventTarget
    public void onShader(EventShader e) {
        islandManager.renderShader(e.getGraphics());
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public void notifyModuleToggled(Module module) {
        if (moduleToggleContent != null) {
            moduleToggleContent.onModuleToggled(module);
        }
    }

    public void updateDrag(float baseX, float baseY, float width, float height) {
        this.dragManager.update(baseX, baseY, width, height);
    }
}
