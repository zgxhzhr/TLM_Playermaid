package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAfterEatEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 人是狐玩家侧饰品事件管线（批量移植的等价物）。
 *
 * <p>万法皆通各饰品的伤害/效果类逻辑原挂在其 Global 伤害管线（只处理女仆），
 * 本类为佩戴我们移植饰品的「人是狐」玩家提供等价事件，并为覆盖绑定集中的件
 * 补齐女仆侧事件等价物（如馥郁巧思进食好感、发簪效果过滤——原实现的
 * 实例订阅随绑定覆盖而失效，这里按物品佩戴判定恢复）。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerMaidBaubleEvents {

    private PlayerMaidBaubleEvents() {
    }

    /** 人是狐玩家饰品栏每 tick 驱动；关闭人是狐时回收已挂载效果。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
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
     *     <li>攻击方为人是狐玩家：泣血之心回血、春之戒加成、混沌之书真实伤害、
     *     破愈咒锋记录目标生命；</li>
     *     <li>受害者为人是狐玩家：流心核心按好感度减免、双心之链 50% 减免
     *     （降级：被分担部分无人承担）、魂之书伤害钳制+间隔免疫。</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity victim = event.getEntity();

        // ===== 攻击方：人是狐玩家（饰品栏佩戴判定在注册表内完成） =====
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (PlayerMaidBaubleRegistry.hasBauble(attacker, PlayerMaidBaubleRegistry.BLEEDING_HEART)) {
                attacker.heal(event.getAmount() * PlayerMaidBleedingHeartBauble.HEAL_RATIO);
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
        if (PlayerMaidBaubleRegistry.hasBauble(victim, PlayerMaidBaubleRegistry.DOUBLE_HEART_CHAIN)) {
            event.setAmount(event.getAmount() * PlayerMaidDoubleHeartChainBauble.SELF_DAMAGE_RATIO);
        }
        if (PlayerMaidBaubleRegistry.hasBauble(victim, PlayerMaidBaubleRegistry.SOUL_BOOK)) {
            event.setAmount(PlayerMaidSoulBookBauble.handleHurt(victim, event.getAmount(), victim.tickCount));
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
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
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
        if (favorabilityLevel >= 3 && !event.getEffectInstance().getEffect().isBeneficial()) {
            event.setResult(Event.Result.DENY);
        } else if (favorabilityLevel >= 2 && event.getEffectInstance().getEffect().isBeneficial()) {
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
        if (used.isEmpty() || !used.getItem().isEdible()) {
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
