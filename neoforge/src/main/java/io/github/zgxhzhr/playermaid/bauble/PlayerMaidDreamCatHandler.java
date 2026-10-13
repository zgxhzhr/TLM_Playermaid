package io.github.zgxhzhr.playermaid.bauble;

import io.github.zgxhzhr.playermaid.data.FoxMaidDataComponents;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 梦云水晶「偷天换日」与人是狐玩家效果驱动。
 *
 * <p>三件事：</p>
 * <ol>
 *     <li><b>绑定覆盖</b>：服务端启动后，用反射把车万女仆 {@code BaubleManager} 的
 *     注册表内梦云水晶条目替换为 {@link PlayerMaidDreamCatBauble}（车万女仆初始化完成后
 *     其注册表被 {@code ImmutableMap.copyOf} 冻结，故需换回可变 Map 再写入）。
 *     此后女仆与人是狐玩家佩戴原版梦云水晶都走移植逻辑。</li>
 *     <li><b>玩家每 tick 驱动</b>：人是狐玩家饰品栏含梦云水晶时调用
 *     {@link PlayerMaidDreamCatBauble#tickEffects}。</li>
 *     <li><b>玩家侧事件管线</b>：受伤减免/免疫/复活/效果过滤/直接攻击真实伤害等，
 *     对应万法皆通原版为其内部管线注册的 Global 回调（其管线只处理女仆，
 *     人是狐玩家需要本事件等价物）。</li>
 * </ol>
 */
@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerMaidDreamCatHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private PlayerMaidDreamCatHandler() {
    }

    // ================================================================
    // 偷天换日：覆盖车万女仆 BaubleManager 的饰品绑定（统一走批量注册表）
    // ================================================================

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // 一次性覆盖本工程已移植的全部饰品（含梦云水晶，见 PlayerMaidBaubleRegistry）
        PlayerMaidBaubleRegistry.overrideBindings();
    }

    // ================================================================
    // 人是狐玩家每 tick 驱动
    // ================================================================

    /** 人是狐玩家饰品栏含梦云水晶时，按与原版一致的节奏驱动效果。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
        if (data == null || !data.isActive()) {
            return;
        }
        ItemStack crystal = PlayerMaidDreamCatBauble.findDreamCrystalStack(data.getBaubles());
        if (crystal.isEmpty()) {
            return;
        }
        PlayerMaidDreamCatBauble.tickEffects(player, crystal, player.tickCount);
    }

    // ================================================================
    // 玩家侧事件管线（对应万法皆通原版 Global 回调）
    // ================================================================

    /**
     * 受伤免疫：燃烧/溺水/爆炸/原版魔法/熔岩伤害，以及复活后的无敌窗口。
     * 与万法皆通 DreamCrystalMaidEvents#onMaidLivingAttack 一致，仅作用于人是狐玩家。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (!isFoxWithCrystal(victim)) {
            return;
        }
        DamageSource source = event.getSource();

        ItemStack crystal = PlayerMaidDreamCatBauble.findDreamCrystalStack(
                FoxMaidManager.getOrCreate((Player) victim).getBaubles());
        if (!crystal.isEmpty() && isInvulnerable(crystal)) {
            event.setCanceled(true);
            return;
        }
        if (isImmuneTo(source)) {
            event.setCanceled(true);
        }
    }

    /**
     * 受伤处理 + 直接攻击拦截：
     * <ul>
     *     <li>人是狐玩家受击：全伤害 ×0.7（30% 抗性）且单次伤害上限 40
     *     （对应万法皆通 Global.baubleSetHealthFinalHandlers）；</li>
     *     <li>人是狐玩家直接攻击：额外真实伤害 + 时停 1 秒 + 对目标周围 5 格内
     *     敌方（非玩家、非女仆）造成 10% 弹幕溅射（对应 Global 的 hurtHead 处理器；
     *     真实伤害为简化移植：直接削减生命，不经过其 EntityData/NBT 混合管线）。</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity victim = event.getEntity();

        // 直接攻击拦截：人是狐玩家作为伤害来源
        if (event.getSource().getEntity() instanceof Player attacker && isFoxWithCrystal(attacker)) {
            handlePlayerAttack(attacker, victim, event.getAmount());
        }

        // 受伤减免：人是狐玩家作为受害者
        if (isFoxWithCrystal(victim)) {
            float amount = event.getAmount() * 0.7f;
            event.setAmount(Math.min(amount, 40.0f));
        }
    }

    /** 概率复活：人是狐玩家佩戴梦云水晶死亡时触发（对应原版 onDeath）。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isFoxWithCrystal(player)) {
            return;
        }
        ItemStack crystal = PlayerMaidDreamCatBauble.findDreamCrystalStack(
                FoxMaidManager.getOrCreate(player).getBaubles());
        if (!crystal.isEmpty()
                && PlayerMaidDreamCatBauble.tryRevive(player, crystal, event.getSource())) {
            event.setCanceled(true);
        }
    }

    /**
     * 有害效果过滤：梦云水晶佩戴者免疫一切非正面效果
     * （对应万法皆通 Global.baubleEffectBlockFilters，其原实现通过 Mixin 拦截，
     * 此处用 Forge 的 MobEffectEvent.Applicable 达到同等效果）。
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!isFoxWithCrystal(event.getEntity())) {
            return;
        }
        if (event.getEffectInstance().getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // ================================================================
    // 时停/范围强化调度
    // ================================================================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (true) {
            PlayerMaidDreamCatBauble.processScheduledEffects(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        PlayerMaidDreamCatBauble.clearScheduledEffects();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PlayerMaidDreamCatBauble.clearScheduledEffects();
    }

    // ================================================================
    // 私有辅助
    // ================================================================

    /** 目标是否为佩戴梦云水晶的人是狐玩家（服务端）。 */
    private static boolean isFoxWithCrystal(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (player.level().isClientSide()) {
            return false;
        }
        FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
        if (data == null || !data.isActive()) {
            return false;
        }
        return PlayerMaidDreamCatBauble.containsDreamCrystal(data.getBaubles());
    }

    private static boolean isImmuneTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return source.is(DamageTypeTags.IS_FIRE)         // 燃烧
                || source.is(DamageTypeTags.IS_DROWNING)  // 溺水
                || source.is(DamageTypeTags.IS_EXPLOSION) // 爆炸
                || source.is(DamageTypes.MAGIC)           // 原版魔法伤害
                || source.is(DamageTypes.LAVA);           // 熔岩
    }

    private static boolean isInvulnerable(ItemStack baubleItem) {
        // 1.21 起 ItemStack 不再有 getTag()，无敌倒计时改读自定义数据组件（与 DreamCat 本体同一处读写）
        Integer ticks = baubleItem.get(FoxMaidDataComponents.DREAM_CRYSTAL_INVULNERABLE_TICKS);
        return ticks != null && ticks > 0;
    }

    private static void handlePlayerAttack(Player player, LivingEntity target, float damage) {
        if (target == null || !target.isAlive()) {
            return;
        }

        // 1. 真实伤害（额外等于攻击伤害；目标非玩家/女仆，与万法皆通一致）
        if (PlayerMaidDreamCatBauble.isTrueDamageEnabled()) {
            dealTrueDamage(target, damage, player);
        }

        // 2. 时停 1 秒（仅在服务端执行）
        if (target instanceof Mob mob) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                long unfreezeTime = saturatingAdd(globalGameTime(server), 20L);
                PlayerMaidDreamCatBauble.freezeTarget(mob, unfreezeTime);
            }
        }

        // 3. 弹幕溅射：对目标周围 5 格内的敌方实体造成 10% 伤害（排除玩家与女仆）
        player.level().getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(5.0),
                entity -> entity != player
                        && entity != target
                        && entity.isAlive()
                        && !(entity instanceof Player)
                        && !(entity instanceof EntityMaid)
        ).forEach(nearby -> dealTrueDamage(nearby, damage * 0.1f, player));
    }

    /**
     * 简化真实伤害：直接削减目标生命，不经过原版的 EntityData/NBT 混合写入管线
     * （该管线依赖万法皆通自身 Mixin，无法移植；对玩家/女仆目标不生效，与原版一致）。
     */
    private static void dealTrueDamage(LivingEntity target, float amount, LivingEntity attacker) {
        if (target == null || target instanceof Player || target instanceof EntityMaid) {
            return;
        }
        if (amount <= 0.0f || target.level().isClientSide() || !target.isAlive()) {
            return;
        }
        float newHealth = Math.max(0.0f, target.getHealth() - amount);
        target.setHealth(newHealth);
        if (newHealth <= 0.0f && target.isAlive()) {
            if (attacker instanceof Player player) {
                target.die(target.damageSources().playerAttack(player));
            } else if (attacker != null) {
                target.die(target.damageSources().mobAttack(attacker));
            } else {
                target.die(target.damageSources().generic());
            }
        }
    }

    private static long globalGameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0 && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }
}
