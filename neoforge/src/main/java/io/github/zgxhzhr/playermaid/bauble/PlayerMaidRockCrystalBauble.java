package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * 岩心水晶（rock_crystal）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code RockCrystalBauble}：为佩戴者提供击退抗性（ADDITION +8，镜像
 * Config.rockCrystalKnockbackResistance 默认值），作用对象由女仆放宽为通用活体。
 * 每 10 tick 以永久修饰符形式补挂（幂等），卸下时移除。</p>
 */
public class PlayerMaidRockCrystalBauble implements PlayerMaidBauble {

    /**
     * 修饰符标识。
     *
     * <p>1.21 适配：AttributeModifier 改为 record，只接受 (id, amount, operation)，
     * 原来的 UUID 常量与名称参数合并成 ResourceLocation（命名空间 playermaid，
     * path 沿用原修饰符名语义）。</p>
     */
    private static final ResourceLocation KNOCKBACK_RESISTANCE_ID =
            ResourceLocation.fromNamespaceAndPath("playermaid", "rock_crystal_knockback_resistance");
    /** 击退抗性加成（镜像万法皆通 Config.rockCrystalKnockbackResistance 默认值 8）。 */
    private static final double KNOCKBACK_RESISTANCE = 8.0D;

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
        if (knockbackResistance.getModifier(KNOCKBACK_RESISTANCE_ID) == null) {
            knockbackResistance.addPermanentModifier(
                    new AttributeModifier(KNOCKBACK_RESISTANCE_ID,
                            KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void onUnequipLivingEntity(LivingEntity entity, ItemStack baubleItem) {
        AttributeInstance knockbackResistance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance == null) {
            return;
        }
        knockbackResistance.removeModifier(KNOCKBACK_RESISTANCE_ID);
    }
}
