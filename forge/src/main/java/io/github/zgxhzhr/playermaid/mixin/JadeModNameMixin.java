package io.github.zgxhzhr.playermaid.mixin;

import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.util.ModIdentification;

/**
 * Jade 面板底部模组名：处于人是狐状态的玩家，
 * 底部模组标识显示为 Touhou Little Maid（与真实女仆一致）。
 * 拦截 {@link ModIdentification#getModName(Entity)}（返回 String），Jade 缺席时自动跳过。
 */
@Mixin(value = ModIdentification.class, remap = false)
public abstract class JadeModNameMixin {

    @Inject(method = "getModName(Lnet/minecraft/world/entity/Entity;)Ljava/lang/String;",
            at = @At("HEAD"), cancellable = true)
    private static void playermaid$foxMaidModName(Entity entity,
                                                  CallbackInfoReturnable<String> cir) {
        if (entity instanceof Player player && ClientFoxState.isActive(player)) {
            cir.setReturnValue("Touhou Little Maid");
        }
    }
}
