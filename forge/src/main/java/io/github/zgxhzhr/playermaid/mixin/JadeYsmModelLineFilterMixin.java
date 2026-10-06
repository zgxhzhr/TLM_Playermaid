package io.github.zgxhzhr.playermaid.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 从 Jade 面板移除第三方模型模组的「YSM模型」信息行。
 *
 * <p>第三方模型模组生产环境的类名经过混淆（开发环境为
 * {@code com.elfmcys.ysm.client.compat.jade.JadePlugin$YSMProvider}），
 * 因此按运行时混淆名定位其提供者类。该提供者实现了 Jade 的
 * {@code IEntityComponentProvider}，其 {@code appendTooltip} 方法名作为
 * 接口实现不会被混淆（已用 javap 对生产 jar 验证过签名），在此方法入口直接取消，
 * 即可让该行完全不进入面板。模型信息键对应本地化
 * {@code top.yes_steve_model.model_info.id}，语言文件由第三方模型模组定义。</p>
 *
 * <p>纯客户端处理：不修改任何网络包与服务端行为，因此不影响联机。
 * 第三方模型模组缺席或未来混淆名变化时，按 {@code defaultRequire=0} 配置
 * 该 Mixin 仅记录警告并跳过，不会影响游戏启动；Jade 缺席时同样自动跳过。</p>
 */
@Mixin(
        targets = "com.elfmcys.yesstevemodel.oOooo0Oo00000o0OooOo0OO0$Oo0Oo0o00O00Oo0OOoOOoooo",
        remap = false
)
public abstract class JadeYsmModelLineFilterMixin {

    @Inject(
            method = "appendTooltip(Lsnownee/jade/api/ITooltip;Lsnownee/jade/api/EntityAccessor;Lsnownee/jade/api/config/IPluginConfig;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void playermaid$dropYsmModelLine(ITooltip tooltip, EntityAccessor accessor,
                                             IPluginConfig config, CallbackInfo ci) {
        ci.cancel();
    }
}
