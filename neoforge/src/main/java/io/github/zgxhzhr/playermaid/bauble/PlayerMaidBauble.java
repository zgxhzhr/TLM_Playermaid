package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 饰品偷天换日统一接口：作用对象放宽的饰品实现基类。
 *
 * <p>所有从万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）移植的
 * 饰品实现类均实现本接口：既作为车万女仆 {@link IMaidBauble} 绑定实现被女仆调用
 * （默认把 {@code onTick/onPutOn/onTakeOff} 转发到作用对象放宽的版本），
 * 也被人是狐玩家每 tick 驱动（见 {@link PlayerMaidBaubleRegistry#tickPlayerBaubles}）。</p>
 */
public interface PlayerMaidBauble extends IMaidBauble {

    /** 作用对象放宽的每 tick 效果入口（女仆与人狐玩家共用；实体为服务端侧）。 */
    void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick);

    /** 作用对象放宽的穿戴入口（可选覆盖）。 */
    default void onEquipLivingEntity(LivingEntity entity, ItemStack baubleItem) {
    }

    /** 作用对象放宽的卸下入口（可选覆盖）。 */
    default void onUnequipLivingEntity(LivingEntity entity, ItemStack baubleItem) {
    }

    @Override
    default void onTick(EntityMaid maid, ItemStack baubleItem) {
        tickLivingEntity(maid, baubleItem, maid.tickCount);
    }

    @Override
    default void onPutOn(EntityMaid maid, ItemStack baubleItem) {
        onEquipLivingEntity(maid, baubleItem);
    }

    @Override
    default void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
        onUnequipLivingEntity(maid, baubleItem);
    }
}
