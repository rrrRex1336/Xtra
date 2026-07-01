package tech.naven;

import awa.qwq.ovo.Naven.viaversionfix.items.ModItems;
import net.fabricmc.api.ModInitializer;

public class NavenModLoader implements ModInitializer {
    @Override
    public void onInitialize() {
        ModItems.init();
    }
}
