package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/23 13:25
 * @Filename：Target
 */
@ModuleInfo(
        name = "Target",
        description = "Prevent attack teammates",
        category = Category.MISC
)
public class Target extends Module {

    BooleanValue player = ValueBuilder.create(this, "Player").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue invisibles = ValueBuilder.create(this, "Invisibles").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue animals = ValueBuilder.create(this, "Animals").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue mobs = ValueBuilder.create(this, "Mobs").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue villager = ValueBuilder.create(this, "Villager").setDefaultBooleanValue(true).build().getBooleanValue();


    @Override
    public void onEnable() {
        this.setEnabled(false);
    }

    public boolean isTarget(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player) return false;
        if (entity instanceof PlayerEntity && entity.isSpectator()) return false;
        if (!entity.isAlive()) return false;

        if (entity.isInvisible() && !invisibles.getCurrentValue()) {
            return false;
        }

        if (entity instanceof PlayerEntity) {
            return player.getCurrentValue();
        }

        if (entity instanceof MerchantEntity) {
            return villager.getCurrentValue();
        }

        if (entity instanceof HostileEntity || entity instanceof SlimeEntity || entity instanceof EnderDragonEntity || entity instanceof EnderDragonPart) {
            return mobs.getCurrentValue();
        }

        if (entity instanceof AnimalEntity || entity instanceof AmbientEntity || entity instanceof WaterCreatureEntity || entity instanceof AbstractHorseEntity) {
            return animals.getCurrentValue();
        }

        return false;
    }
}