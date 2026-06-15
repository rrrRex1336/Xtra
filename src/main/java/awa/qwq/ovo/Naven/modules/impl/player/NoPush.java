package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;

@ModuleInfo(
        name = "NoPush",
        category = Category.PLAYER,
        description = "Prevents being pushed by entities"
)
public class NoPush extends Module {

    public BooleanValue players = ValueBuilder.create(this, "Players")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue mobs = ValueBuilder.create(this, "Mobs")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue items = ValueBuilder.create(this, "Items")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public BooleanValue vehicles = ValueBuilder.create(this, "Vehicles")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue projectiles = ValueBuilder.create(this, "Projectiles")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
}