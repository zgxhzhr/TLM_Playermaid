package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 岩心水晶（rock_crystal）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code RockCrystalBauble}：为佩戴者提供击退抗性（ADDITION +8，镜像
 * Config.rockCrystalKnockbackResistance 默认值），作用对象由女仆放宽为通用活体。
 * 每 10 tick 以永久修饰符形式补挂（幂等），卸下时移除。</p>
 */
public class PlayerMaidRockCrystalBauble implements PlayerMaidBauble {

    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("8b5c7a26-3e4f-4c45-a7b3-2f8d9e1a5c47");
    /** 击退抗性加成（镜像万法皆通 Config.rockCrystalKnockbackResistance 默认值 8）。 */
    private static final double KNOCKBACK_RESISTANCE = 8.0D;
    private static final String MODIFIER_NAME = "Rock Crystal Knockback Resistance";

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (tick % 10 != 0) {
            return;
        }
        AttributeInstance knockbackResistance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance == null) {
            return;
        }
        if (knockbackResistance.getModifier(KNOCKBACK_RESISTANCE_UUID) == null) {
            knockbackResistance.addPermanentModifier(
                    new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, MODIFIER_NAME,
                            KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public void onUnequipLivingEntity(LivingEntity entity, ItemStack baubleItem) {
        AttributeInstance knockbackResistance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance == null) {
            return;
        }
        knockbackResistance.removeModifier(KNOCKBACK_RESISTANCE_UUID);
    }
}
