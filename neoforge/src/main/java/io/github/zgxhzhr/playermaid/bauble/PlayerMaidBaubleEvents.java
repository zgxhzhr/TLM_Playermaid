package io.github.zgxhzhr.playermaid.bauble;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAfterEatEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 人是狐玩家侧饰品事件管线（批量移植的等价物）。
 *
 * <p>万法皆通各饰品的伤害/效果类逻辑原挂在其 Global 伤害管线（只处理女仆），
 * 本类为佩戴我们移植饰品的「人是狐」玩家提供等价事件，并为覆盖绑定集中的件
 * 补齐女仆侧事件等价物（如馥郁巧思进食好感、发簪效果过滤——原实现的
 * 实例订阅随绑定覆盖而失效，这里按物品佩戴判定恢复）。</p>
 *
 * <p><b>主人实体联动</b>：人是狐玩家认主后（{@code FoxMaidData.ownerUuid}），
 * 依赖「主人实体」的饰品效果在本类中按万法皆通原版规则完整生效——
 * 双心之链由主人分担一半伤害、泣血之心让主人一并回血、发簪把伤害转移给主人；
 * 未认主或主人不在场时退回各自的降级行为（详见各方法注释）。</p>
 */
@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerMaidBaubleEvents {

    private PlayerMaidBaubleEvents() {
    }

    /** 人是狐玩家饰品栏每 tick 驱动；关闭人是狐时回收已挂载效果。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
        if (data == null || !data.isActive()) {
            PlayerMaidBaubleRegistry.unequipAll(player);
            return;
        }
        PlayerMaidBaubleRegistry.tickPlayerBaubles(player, data);
    }

    /**
     * 受伤处理（与万法皆通 Global 伤害管线等价）：
     * <ul>
     *     <li>攻击方为人是狐玩家：泣血之心回血（本人与主人各回一次）、春之戒加成、
     *     混沌之书真实伤害、破愈咒锋记录目标生命；</li>
     *     <li>受害者为人是狐玩家：流心核心按好感度减免、发簪把伤害转移给主人、
     *     双心之链与主人各担一半（认主后）或 50% 减免（未认主）、
     *     魂之书伤害钳制+间隔免疫。</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamageModify(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        // 1.21 把旧的 LivingAttackEvent 与 LivingHurtEvent 合并成同一个事件：
        // 保护类饰品在 onIncomingDamageGuard（HIGHEST）里可能已经取消本次伤害，
        // 这里必须让位，避免在已取消的伤害上继续做加成与主人分担。
        if (event.isCanceled()) {
            return;
        }
        LivingEntity victim = event.getEntity();

        // ===== 攻击方：人是狐玩家（饰品栏佩戴判定在注册表内完成） =====
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (PlayerMaidBaubleRegistry.hasBauble(attacker, PlayerMaidBaubleRegistry.BLEEDING_HEART)) {
                float heal = event.getAmount() * PlayerMaidBleedingHeartBauble.HEAL_RATIO;
                // 原版为「女仆与其主人回血」：认主后主人一并回血，未认主时退回仅本人回血
                attacker.heal(heal);
                ServerPlayer owner = FoxMaidManager.resolveOwner(attacker);
                if (owner != null && owner != attacker && owner.isAlive()) {
                    owner.heal(heal);
                }
            }
            if (PlayerMaidBaubleRegistry.hasBauble(attacker, PlayerMaidBaubleRegistry.SPRING_RING)) {
                event.setAmount(event.getAmount() * PlayerMaidSpringRingBauble.damageMultiplier(attacker));
            }
            if (PlayerMaidBaubleRegistry.hasBauble(attacker, PlayerMaidBaubleRegistry.CHAOS_BOOK)) {
                boolean withDreamCrystal = PlayerMaidBaubleRegistry.hasBauble(
                        attacker, PlayerMaidDreamCatBauble.DREAM_CRYSTAL_ID);
                PlayerMaidChaosBookBauble.dealTrueDamage(victim,
                        PlayerMaidChaosBookBauble.trueDamageAmount(victim, withDreamCrystal), attacker);
            }
            if (PlayerMaidBaubleRegistry.hasBauble(attacker, PlayerMaidBaubleRegistry.WOUND_RIME_BLADE)) {
                PlayerMaidWoundRimeBladeBauble.recordHit(attacker, victim, attacker.tickCount);
            }
        }

        // ===== 受害者：人是狐玩家 =====
        if (PlayerMaidBaubleRegistry.hasBauble(victim, PlayerMaidBaubleRegistry.FLOW_CORE)) {
            int favorabilityLevel = PlayerMaidBaubleRegistry.favorabilityLevel(victim);
            event.setAmount(PlayerMaidFlowCoreBauble.reduceDamageByFavorability(favorabilityLevel, event.getAmount()));
        }
        if (PlayerMaidBaubleRegistry.hasBauble(victim, PlayerMaidBaubleRegistry.HAIRPIN)) {
            redirectHairpinDamage(victim, event);
        }
        if (PlayerMaidBaubleRegistry.hasBauble(victim, PlayerMaidBaubleRegistry.DOUBLE_HEART_CHAIN)) {
            applyDoubleHeartChain(victim, event);
        }
        if (PlayerMaidBaubleRegistry.hasBauble(victim, PlayerMaidBaubleRegistry.SOUL_BOOK)) {
            event.setAmount(PlayerMaidSoulBookBauble.handleHurt(victim, event.getAmount(), victim.tickCount));
        }
    }

    // ===== 双心之链：与主人分担伤害 =====

    /** 正在执行双心之链分担的佩戴者（按 UUID 记），防止主人受击回流触发无限递归。 */
    private static final ThreadLocal<Set<UUID>> SHARING_DAMAGE = ThreadLocal.withInitial(HashSet::new);

    /**
     * 双心之链结算（镜像万法皆通 {@code DoubleHeartChainBauble}）：
     * <ol>
     *     <li>组合梦云水晶：主人不承担，佩戴者只受 50%；</li>
     *     <li>已认主、主人在场且在 {@link PlayerMaidDoubleHeartChainBauble#MAX_SHARE_DISTANCE} 格内：
     *     佩戴者受 50%，另一半由主人实体承受；</li>
     *     <li>已认主但主人不在场、跨维度或超出距离：不分担，佩戴者承受全额（与原版一致）；</li>
     *     <li>未认主：保留本模组此前的降级行为（佩戴者直接减免 50%）。</li>
     * </ol>
     */
    private static void applyDoubleHeartChain(LivingEntity victim, LivingIncomingDamageEvent event) {
        if (!(victim instanceof Player wearer)) {
            return;
        }
        boolean withDreamCrystal = PlayerMaidBaubleRegistry.hasBauble(
                victim, PlayerMaidDreamCatBauble.DREAM_CRYSTAL_ID);
        if (withDreamCrystal) {
            event.setAmount(event.getAmount() * PlayerMaidDoubleHeartChainBauble.SELF_DAMAGE_RATIO);
            return;
        }
        ServerPlayer owner = FoxMaidManager.resolveOwner(wearer);
        if (owner == null || owner == wearer || !owner.isAlive()) {
            // 未认主 / 主人不在场：维持旧的 50% 减免，不让未认主玩家变脆
            event.setAmount(event.getAmount() * PlayerMaidDoubleHeartChainBauble.SELF_DAMAGE_RATIO);
            return;
        }
        double maxDistance = PlayerMaidDoubleHeartChainBauble.MAX_SHARE_DISTANCE;
        if (wearer.level() != owner.level() || wearer.distanceToSqr(owner) > maxDistance * maxDistance) {
            // 超出分担范围：不分担，佩戴者承受全额（与原版一致）
            return;
        }
        Set<UUID> sharing = SHARING_DAMAGE.get();
        if (!sharing.add(wearer.getUUID())) {
            // 回流（主人受击又触发到同一佩戴者）不再二次分担
            return;
        }
        try {
            float shared = event.getAmount() * PlayerMaidDoubleHeartChainBauble.SELF_DAMAGE_RATIO;
            event.setAmount(shared);
            owner.hurt(owner.damageSources().generic(), shared);
        } finally {
            sharing.remove(wearer.getUUID());
            if (sharing.isEmpty()) {
                SHARING_DAMAGE.remove();
            }
        }
    }

    // ===== 发簪：把受到的伤害转移给主人 =====

    /** 正在执行发簪转移的佩戴者（按 UUID 记），防止主人受击回流触发无限递归。 */
    private static final ThreadLocal<Set<UUID>> REDIRECTING_DAMAGE = ThreadLocal.withInitial(HashSet::new);

    /**
     * 发簪伤害转移（镜像万法皆通 {@code HairpinBauble} 的 hairpin_redirect 处理器）：
     * 把佩戴者这次要受的伤害施加给主人实体，再按主人**实际损失的生命值**回填佩戴者这一侧。
     *
     * <p>主人在本次伤害中实际未掉血（创造/旁观、或其他守卫拦下）时不清零，
     * 而是保留原伤害——原版此处会把佩戴者这一侧置 0，等于创造模式主人可让佩戴者免疫。</p>
     */
    private static void redirectHairpinDamage(LivingEntity victim, LivingIncomingDamageEvent event) {
        if (!(victim instanceof Player wearer)) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        ServerPlayer owner = FoxMaidManager.resolveOwner(wearer);
        if (owner == null || owner == wearer || !owner.isAlive()) {
            return;
        }
        double maxDistance = PlayerMaidHairpinBauble.MAX_REDIRECT_DISTANCE;
        if (wearer.level() != owner.level() || wearer.distanceToSqr(owner) > maxDistance * maxDistance) {
            return;
        }
        Set<UUID> redirecting = REDIRECTING_DAMAGE.get();
        if (!redirecting.add(wearer.getUUID())) {
            return;
        }
        try {
            // 原版同样会清无敌帧：主人若刚受过伤，hurt 会被原版直接忽略、取样失准
            boolean wasInvulnerable = owner.isInvulnerable();
            owner.setInvulnerable(false);
            owner.invulnerableTime = 0;
            float healthBefore = owner.getHealth();
            try {
                owner.hurt(owner.damageSources().generic(), amount);
            } finally {
                owner.setInvulnerable(wasInvulnerable);
            }
            float lost = Math.max(0.0F, healthBefore - owner.getHealth());
            if (lost > 0.0F) {
                event.setAmount(lost);
            }
        } finally {
            redirecting.remove(wearer.getUUID());
            if (redirecting.isEmpty()) {
                REDIRECTING_DAMAGE.remove();
            }
        }
    }

    /** 车万女仆保护类饰品 id 列表（溺水/爆炸/摔落/火焰/魔法/弹射物）。 */
    private static final ResourceLocation[] PROTECT_IDS = {
            PlayerMaidBaubleRegistry.DROWN_PROTECT,
            PlayerMaidBaubleRegistry.EXPLOSION_PROTECT,
            PlayerMaidBaubleRegistry.FALL_PROTECT,
            PlayerMaidBaubleRegistry.FIRE_PROTECT,
            PlayerMaidBaubleRegistry.MAGIC_PROTECT,
            PlayerMaidBaubleRegistry.PROJECTILE_PROTECT
    };

    /**
     * 攻击抵挡（对应车万女仆各保护类 {@code onInjured}）：
     * 迅捷布料（弹射物闪避+随机瞬移，优先于弹射物保护）与
     * 溺水/爆炸/摔落/火焰/魔法/弹射物保护（消耗 1 点耐久完全抵挡）。
     *
     * <p>1.21 起本事件与 {@link #onIncomingDamageModify} 是同一个事件，因此这里用
     * HIGHEST 优先级保证「先判定抵挡、再改伤害」，与 1.20.1 两个独立事件的先后一致。</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamageGuard(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)
                || !PlayerMaidBaubleRegistry.hasAnyBauble(player)) {
            return;
        }
        DamageSource source = event.getSource();

        // 迅捷布料：弹射物 → 消耗耐久抵挡并随机瞬移
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            ItemStack nimble = PlayerMaidBaubleRegistry.findStack(player, PlayerMaidBaubleRegistry.NIMBLE_FABRIC);
            if (!nimble.isEmpty()
                    && PlayerMaidBaubleRegistry.getBauble(nimble) instanceof PlayerMaidNimbleFabricBauble fabric) {
                fabric.tryDodge(player, nimble);
                event.setCanceled(true);
                return;
            }
        }

        // 保护类饰品：命中对应伤害类型则扣耐久并抵挡
        for (ResourceLocation id : PROTECT_IDS) {
            ItemStack stack = PlayerMaidBaubleRegistry.findStack(player, id);
            if (stack.isEmpty()) {
                continue;
            }
            if (PlayerMaidBaubleRegistry.getBauble(stack) instanceof PlayerMaidProtectionBauble protection
                    && protection.tryBlockPlayer(player, stack, source)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * 死亡保护（对应车万女仆 {@code ExtraLifeBauble} / {@code UndyingTotemBauble} 的 onDeath）：
     * 群青灵珠（可反复用，消耗耐久回满生命，先于一次性图腾判定）与
     * 不死图腾（一次性，消耗图腾并施加不死效果）。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)
                || !PlayerMaidBaubleRegistry.hasAnyBauble(player)) {
            return;
        }
        DamageSource source = event.getSource();

        ItemStack orb = PlayerMaidBaubleRegistry.findStack(player, PlayerMaidBaubleRegistry.ULTRAMARINE_ORB);
        if (!orb.isEmpty()
                && PlayerMaidBaubleRegistry.getBauble(orb) instanceof PlayerMaidExtraLifeBauble extraLife
                && extraLife.tryRevivePlayer(player, orb, source)) {
            event.setCanceled(true);
            return;
        }

        ItemStack totem = PlayerMaidBaubleRegistry.findStack(player, PlayerMaidBaubleRegistry.TOTEM_OF_UNDYING);
        if (!totem.isEmpty()
                && PlayerMaidBaubleRegistry.getBauble(totem) instanceof PlayerMaidUndyingTotemBauble undying
                && undying.tryTotemPlayer(player, totem, source)) {
            event.setCanceled(true);
        }
    }

    /**
     * 发簪效果过滤（对应万法皆通 HairpinBauble 的 Applicable 订阅）：
     * 好感度 ≥3 级免疫非正面效果；≥2 级延长正面效果持续时间。
     * 覆盖绑定后原实例订阅失效，这里同时按女仆/人是狐玩家佩戴判定恢复女仆侧效果。
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        boolean hasHairpin;
        if (entity instanceof Player) {
            hasHairpin = PlayerMaidBaubleRegistry.hasBauble(entity, PlayerMaidBaubleRegistry.HAIRPIN);
        } else if (entity instanceof EntityMaid maid) {
            // 女仆侧：原 HairpinBauble 实例订阅随绑定覆盖失效，此处按物品佩戴恢复
            hasHairpin = PlayerMaidBaubleRegistry.maidHasBaubleItem(maid, PlayerMaidBaubleRegistry.HAIRPIN);
        } else {
            return;
        }
        if (!hasHairpin) {
            return;
        }
        int favorabilityLevel = PlayerMaidBaubleRegistry.favorabilityLevel(entity);
        if (favorabilityLevel >= 3 && !event.getEffectInstance().getEffect().value().isBeneficial()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        } else if (favorabilityLevel >= 2 && event.getEffectInstance().getEffect().value().isBeneficial()) {
            PlayerMaidHairpinBauble.extendBeneficial(event.getEffectInstance(), favorabilityLevel);
        }
    }

    /**
     * 馥郁巧思：女仆进食后增加好感度（覆盖绑定后原实例订阅失效，按物品佩戴恢复原版效果）。
     */
    @SubscribeEvent
    public static void onMaidAfterEat(MaidAfterEatEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide()) {
            return;
        }
        if (PlayerMaidBaubleRegistry.maidHasBaubleItem(maid, PlayerMaidBaubleRegistry.FRAGRANT_INGENUITY)) {
            PlayerMaidFragrantIngenuityBauble.addMaidFavorability(maid);
        }
    }

    /**
     * 馥郁巧思：人是狐玩家进食完成后增加好感度（映射女仆进食效果到玩家自身）。
     */
    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!PlayerMaidBaubleRegistry.hasBauble(player, PlayerMaidBaubleRegistry.FRAGRANT_INGENUITY)) {
            return;
        }
        ItemStack used = event.getItem();
        if (used.isEmpty() || !used.has(net.minecraft.core.component.DataComponents.FOOD)) {
            return;
        }
        FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
        if (data == null || !data.isActive()) {
            return;
        }
        data.setFavorability(data.getFavorability() + PlayerMaidFragrantIngenuityBauble.FAVORABILITY_GAIN);
        FoxMaidManager.persistAndSync(player);
    }
}
