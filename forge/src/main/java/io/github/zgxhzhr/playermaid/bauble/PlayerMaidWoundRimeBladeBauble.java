package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 破愈咒锋（wound_rime_blade）饰品逻辑——人是狐玩家版（降级）。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code WoundRimeBladeBauble}：佩戴者直接攻击时记录目标生命，之后目标的治疗效果
 * 会被阻止（禁疗，记录时长 15 tick，镜像 Config.woundRimeBladeRecordDuration 默认值）。</p>
 *
 * <p><b>降级说明</b>：原版在其 LivingEntityMixin 拦截 setHealth 时同步取消治疗；
 * playermaid 无该注入点，降级为「每 tick 巡检已记录目标，若生命高于记录值则回退到记录值」，
 * 效果与原版等价但存在至多 1 tick 的延迟。因此本件不覆盖车万女仆绑定，
 * 女仆佩戴仍走万法皆通原版逻辑。</p>
 */
public class PlayerMaidWoundRimeBladeBauble implements PlayerMaidBauble {

    /** 禁疗记录时长 tick（镜像 Config.woundRimeBladeRecordDuration 默认值 15）。 */
    private static final int RECORD_DURATION = 15;

    /** 玩家 UUID → (目标记录键 → 目标记录生命)。 */
    private static final Map<UUID, Map<TargetKey, TargetRecord>> RECORDS = new ConcurrentHashMap<>();

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide()) {
            return;
        }
        UUID playerId = entity.getUUID();
        Map<TargetKey, TargetRecord> targets = RECORDS.get(playerId);
        if (targets == null || targets.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<TargetKey, TargetRecord>> it = targets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<TargetKey, TargetRecord> entry = it.next();
            TargetRecord record = entry.getValue();
            if (record.expireTick <= tick) {
                it.remove();
                continue;
            }
            LivingEntity target = resolveTarget(entity, entry.getKey());
            if (target == null || !target.isAlive()) {
                it.remove();
                continue;
            }
            // 目标生命高于记录值（被治疗/回血）→ 回退到记录值（降级版禁疗）
            if (target.getHealth() > record.recordHealth) {
                target.setHealth(record.recordHealth);
            } else {
                // 自然下降则跟随，保持记录为"受伤后的最低生命"
                record.recordHealth = target.getHealth();
            }
        }
        if (targets.isEmpty()) {
            RECORDS.remove(playerId, targets);
        }
    }

    /** 攻击命中时记录目标生命（由 {@link PlayerMaidBaubleEvents#onLivingHurt} 调用）。 */
    public static void recordHit(LivingEntity player, LivingEntity target, int tick) {
        if (target == null || target instanceof Player || target instanceof EntityMaid) {
            return;
        }
        Map<TargetKey, TargetRecord> targets = RECORDS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        TargetKey key = TargetKey.of(target);
        targets.put(key, new TargetRecord(target.getHealth(), tick + RECORD_DURATION));
    }

    @Override
    public void onUnequipLivingEntity(LivingEntity entity, ItemStack baubleItem) {
        RECORDS.remove(entity.getUUID());
    }

    private static LivingEntity resolveTarget(LivingEntity player, TargetKey key) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerLevel level = serverLevel.getServer().getLevel(key.dimension);
        if (level == null) {
            return null;
        }
        if (level.getEntity(key.entityId) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private record TargetKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                             UUID entityId) {
        private static TargetKey of(LivingEntity entity) {
            return new TargetKey(entity.level().dimension(), entity.getUUID());
        }
    }

    private static final class TargetRecord {
        private float recordHealth;
        private final int expireTick;

        private TargetRecord(float recordHealth, int expireTick) {
            this.recordHealth = recordHealth;
            this.expireTick = expireTick;
        }
    }
}
