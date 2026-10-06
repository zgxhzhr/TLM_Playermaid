package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 静音饰品（mute_bauble）——人是狐玩家版（占位）。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code MuteBauble}：原版通过取消 {@code MaidPlaySoundEvent}（车万女仆专用事件）
 * 使女仆不发出声音；该事件只针对女仆，人是狐玩家无对应事件，玩家侧无效果。
 * 女仆侧原实例订阅在车万女仆初始化时已注册，覆盖绑定后依然生效，女仆不受影响。</p>
 */
public class PlayerMaidMuteBauble implements PlayerMaidBauble {

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无效果（玩家无女仆发声事件）
    }
}
