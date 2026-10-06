package io.github.zgxhzhr.playermaid.dimension;

import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/**
 * 「魂符收容」维度。
 *
 * <p>维度类型（dimension_type）与维度茎（level_stem）通过数据包 JSON 注册
 * （{@code data/playermaid/dimension_type/containment.json} 与
 * {@code data/playermaid/dimension/containment.json}），服务端启动时会为数据包里
 * 的每个维度创建世界。区块生成器类型则在 {@link Registries#CHUNK_GENERATOR}
 * 注册表中登记，供 JSON 引用。维度内无自然生成（虚空生物群系没有刷怪设置）、
 * 光照恒亮（ambient_light = 1.0，光照贴图恒为满亮）。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ContainmentDimensions {

    /** 收容维度键。 */
    public static final ResourceKey<Level> CONTAINMENT =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(Constants.MOD_ID, "containment"));

    /** 收容房间中心出生点（房间内地面上方）。 */
    public static final BlockPos CONTAINMENT_SPAWN = new BlockPos(0, 3, 0);

    private ContainmentDimensions() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        // 仅区块生成器类型需要代码注册；维度类型/维度茎由数据包 JSON 提供
        if (event.getRegistryKey().equals(Registries.CHUNK_GENERATOR)) {
            event.register(Registries.CHUNK_GENERATOR, ContainmentChunkGenerator.ID,
                    () -> ContainmentChunkGenerator.CODEC);
        }
    }

    /** 当前是否位于收容维度。 */
    public static boolean isContainment(LevelAccessor level) {
        return level instanceof Level l && l.dimension().equals(CONTAINMENT);
    }

    /** 把玩家传送进收容维度。 */
    public static void teleportInto(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(CONTAINMENT);
        if (level == null) {
            return;
        }
        player.teleportTo(level,
                CONTAINMENT_SPAWN.getX() + 0.5, CONTAINMENT_SPAWN.getY(), CONTAINMENT_SPAWN.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }
}
