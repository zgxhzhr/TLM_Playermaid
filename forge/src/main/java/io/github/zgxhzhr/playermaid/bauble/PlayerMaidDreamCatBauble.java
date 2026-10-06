package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 梦云水晶（dream_cat_crystal）饰品逻辑——人是狐玩家版（「偷天换日」阶段 1 移植）。
 *
 * <p>本文件代码移植自第三方法术模组「万法皆通」（Touhou-Little-Maid-Spell，
 * 作者 yimeng261，MIT 协议开源）的 {@code DreamCatCrystalBauble}，保留版权声明要素，特此声明。
 * 移植时把作用对象由车万女仆女仆（EntityMaid）放宽为通用活体（LivingEntity）：</p>
 *
 * <ul>
 *     <li>作为车万女仆饰品绑定实现（见 {@link PlayerMaidDreamCatHandler#overrideDreamCrystalBinding()}）：
 *     女仆佩戴原版梦云水晶时由车万女仆按 {@link IMaidBauble} 接口调用本类，效果与原版一致；</li>
 *     <li>被人是狐玩家每 tick 驱动（见 {@link PlayerMaidDreamCatHandler#onPlayerTick}）：
 *     玩家把原版梦云水晶放入人是狐饰品栏后触发同样的效果。</li>
 * </ul>
 *
 * <p>主要效果（数值与节奏和万法皆通原版完全一致）：生命上限 +50%、攻击速度 ×2、
 * 铁魔法法强翻倍、维度特定增益（主世界/下界/末地）、20 格内邻近女仆攻击 +50%、
 * 背包每秒修复 1 点耐久、每 30 秒两种随机正面效果、概率复活（100% - N×10%）、
 * 复活后 15 秒无敌、被击 30% 全伤害抗性与单次伤害上限 40、
 * 免疫燃烧/溺水/爆炸/魔法/熔岩伤害、直接攻击附带真实伤害与时停 1 秒与弹幕溅射 10%。</p>
 *
 * <p>未移植项（依赖万法皆通内部管线或女仆专属上下文，此处注释说明）：</p>
 * <ul>
 *     <li>归隐之地（TheRetreatDimension）内的持续无敌：该维度为万法皆通私有维度，无法映射，降级跳过；</li>
 *     <li>万法皆通全局回调中的法术冷却取消与邻近女仆冷却缩短（Global.commonCoolDownCalc）：
 *     属其法术管线，女仆侧仍由万法皆通自身按原物品生效；</li>
 *     <li>与其他饰品的组合效果（双心之链不分担伤害、混沌之书真实伤害翻倍、紫荆银冠反伤等）；
 *     本饰品没有好感度相关效果，故无需映射到人是狐数据的好感度字段。</li>
 * </ul>
 */
public class PlayerMaidDreamCatBauble implements PlayerMaidBauble {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 梦云水晶物品 id（万法皆通注册） ==========
    public static final ResourceLocation DREAM_CRYSTAL_ID =
            new ResourceLocation("touhou_little_maid_spell", "dream_cat_crystal");

    // ========== 时停状态追踪（移植自 DreamCatCrystalBauble） ==========
    private static final Map<UUID, FrozenTargetState> FROZEN_TARGETS = new HashMap<>();
    private static final PriorityQueue<ScheduledExpiry> FROZEN_TARGET_EXPIRIES =
            new PriorityQueue<>(Comparator.comparingLong(ScheduledExpiry::expiry));

    // ========== 范围强化追踪（移植自 DreamCatCrystalBauble） ==========
    private static final Map<UUID, BoostedMaidState> BOOSTED_MAIDS = new HashMap<>();
    private static final PriorityQueue<ScheduledExpiry> BOOSTED_MAID_EXPIRIES =
            new PriorityQueue<>(Comparator.comparingLong(ScheduledExpiry::expiry));

    // ========== NBT 键名（与万法皆通原版一致，保证跨实现存档兼容） ==========
    private static final String NBT_REVIVE_CLOCK_VERSION = "dream_crystal_revive_clock_version";
    private static final String NBT_REVIVE_TIMESTAMPS = "dream_crystal_revive_timestamps";
    public static final String NBT_INVULNERABLE_TIME = "dream_crystal_invulnerable_time";
    private static final int REVIVE_CLOCK_VERSION = 1;
    private static final int MAX_REVIVE_TIMESTAMPS = 10;
    private static final long REVIVE_WINDOW_TICKS = 2400L;
    private static final long LEGACY_TIMESTAMP_GRACE_TICKS = 20L;

    // ========== 配置镜像（与万法皆通 Config 默认值一致；本移植不读取其配置文件） ==========
    private static final boolean USE_EFFECT_WHITELIST = true;
    private static final boolean EXTRA_TRUE_DAMAGE_ENABLED = true;
    private static final boolean SET_NO_AI_ENABLED = true;
    private static final List<String> EFFECT_WHITELIST = List.of(
            "regex:minecraft:.*",
            "regex:goety:.*",
            "regex:irons_spellbooks:.*",
            "regex:ars_nouveau:.*",
            "regex:youkaishomecoming:.*",
            "regex:farmersdelight:.*",
            "regex:kaleidoscope_cookery:.*",
            "regex:alexsmobs:.*",
            "regex:alexscaves:.*",
            "regex:cataclysm:.*"
    );
    private static final List<String> EFFECT_BLACKLIST = List.of(
            "irons_spellbooks:ascension",
            "irons_spellbooks:burning_dash",
            "irons_spellbooks:antigravity",
            "irons_spellbooks:volt_strike",
            "traveloptics:aqua_missiles_hover",
            "traveloptics:meteor_storm",
            "traveloptics:aerial_collapse",
            "traveloptics:aerial_collapse_helper",
            "soulsweapons:chungus_tonic_effect",
            "goety:fire_trail",
            "goety:charged",
            "goety:rampage",
            "goety:shadow_walk",
            "goety:fiery_aura",
            "goety:frosty_aura",
            "minecraft:invisibility",
            "irons_spellbooks:true_invisibility"
    );

    // ========== 正面效果缓存 ==========
    private static List<MobEffect> CACHED_BENEFICIAL_EFFECTS = null;

    // ========== 属性修饰符 UUID（与万法皆通原版一致） ==========
    private static final UUID DC_HP_UUID = UUID.fromString("dc000001-0000-0000-0000-000000000001");
    private static final UUID DC_ATTACK_SPEED_UUID = UUID.fromString("dc000001-0000-0000-0000-000000000002");
    private static final UUID DC_NEARBY_DAMAGE_UUID = UUID.fromString("dc000001-0000-0000-0000-000000000003");
    private static final UUID DC_CURIOS_SLOT_UUID = UUID.fromString("dc000001-0000-0000-0000-000000000004");

    // ========== 进度 ResourceLocation（万法皆通的"全法术精通"进度） ==========
    @SuppressWarnings("removal")
    private static final ResourceLocation ADVANCEMENT_ALL_SPELLS =
            new ResourceLocation("touhou_little_maid_spell:dream_crystal/all_spells_mastered");

    // ========== 铁魔法（Irons Spellbooks）属性列表 ==========
    private static final List<Attribute> ISS_ATTRIBUTES = new ArrayList<>();

    /** 梦云水晶物品（注册表查询，万法皆通未安装时为 null）。 */
    private static Item dreamCrystalItem;

    /** 直接攻击是否附带真实伤害（镜像万法皆通 Config.dreamCrystalExtraTrueDamageEnabled 默认值）。 */
    public static boolean isTrueDamageEnabled() {
        return EXTRA_TRUE_DAMAGE_ENABLED;
    }

    static {
        // 初始化铁魔法属性（若铁魔法加载）：与万法皆通原版一致，按描述 id 前缀筛选
        if (ModList.get().isLoaded("irons_spellbooks")) {
            ForgeRegistries.ATTRIBUTES.forEach(attr -> {
                if (attr.getDescriptionId().startsWith("attribute.irons_spellbooks.")) {
                    ISS_ATTRIBUTES.add(attr);
                }
            });
        }
    }

    // ================================================================
    // 车万女仆 IMaidBauble 接口实现（女仆侧由车万女仆调用）
    // ================================================================

    @Override
    public boolean syncClient(EntityMaid maid, ItemStack baubleItem) {
        return true;
    }

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        tickEffects(entity, baubleItem, tick);
    }

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        tickEffects(maid, baubleItem, maid.tickCount);
    }

    @Override
    public void onPutOn(EntityMaid maid, ItemStack baubleItem) {
        if (maid.level().isClientSide()) {
            return;
        }
        applyCuriosSlot(maid);
        grantAdvancement(maid);
    }

    @Override
    public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
        if (maid.level().isClientSide()) {
            return;
        }
        removeCuriosSlots(maid);
        removeAttributeModifier(maid, Attributes.MAX_HEALTH, DC_HP_UUID);
        removeAttributeModifier(maid, Attributes.ATTACK_SPEED, DC_ATTACK_SPEED_UUID);
        if (!ISS_ATTRIBUTES.isEmpty()) {
            for (Attribute attr : ISS_ATTRIBUTES) {
                removeAttributeModifier(maid, attr, new UUID("dream_crystal_iss".hashCode(),
                        attr.getDescriptionId().hashCode()));
            }
        }
    }

    @Override
    public boolean onDeath(EntityMaid maid, ItemStack baubleItem, DamageSource source) {
        return tryRevive(maid, baubleItem, source);
    }

    // ================================================================
    // 通用效果逻辑（作用对象为 LivingEntity：女仆与人是狐玩家共用）
    // ================================================================

    /**
     * 每 tick 驱动力：与万法皆通原版 {@code onTick} 的节奏一致
     * （每 tick 处理无敌倒计时；每 20 tick 处理属性/维度/范围强化/修复/随机效果）。
     */
    public static void tickEffects(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide()) {
            return;
        }

        // 每 tick：无敌倒计时
        handleInvulnerable(baubleItem);

        if (tick % 20 != 3) {
            return;
        }

        // ========== 每 20 tick（约 1 秒）的效果 ==========

        normalizeReviveHistory(entity, baubleItem);

        // 1. 血量上限 +50%
        applyAttributeModifier(entity, Attributes.MAX_HEALTH, DC_HP_UUID,
                "dream_crystal_hp", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // 2. 攻击速度 ×2
        applyAttributeModifier(entity, Attributes.ATTACK_SPEED, DC_ATTACK_SPEED_UUID,
                "dream_crystal_speed", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // 3. 铁魔法法强翻倍（如果铁魔法已加载）
        if (!ISS_ATTRIBUTES.isEmpty()) {
            for (Attribute attr : ISS_ATTRIBUTES) {
                UUID uuid = new UUID("dream_crystal_iss".hashCode(), attr.getDescriptionId().hashCode());
                applyAttributeModifier(entity, attr, uuid, "dream_crystal_iss", 1.0,
                        AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
        }

        // 4. 维度特定增益
        applyDimensionBuffs(entity);

        // 5. 扫描 20 格内邻近女仆并施加强化（作用对象放宽：女仆与人是狐玩家均作为强化源）
        applyRangeMaidBoost(entity);

        // 6. 修复整个背包物品耐久度（每 20 tick = 每秒 1 点）
        repairInventory(entity);

        // 7. 维持 Curios 额外槽位（transient 修饰符需要周期性刷新）
        applyCuriosSlot(entity);

        // ========== 每 600 tick（30 秒）随机正面效果 ==========
        if (tick % 600 == 3) {
            applyRandomBeneficialEffects(entity);
            // 佩戴后为（女仆的）主人/（人是狐的）玩家本人触发万法皆通"全法术精通"进度
            if (entity instanceof Player player) {
                grantAdvancement(player);
            }
        }
    }

    /** 无敌倒计时处理（NBT 存于饰品 ItemStack 上，与万法皆通原版同键）。 */
    private static void handleInvulnerable(ItemStack baubleItem) {
        CompoundTag tag = baubleItem.getTag();
        if (tag == null || !tag.contains(NBT_INVULNERABLE_TIME)) {
            return;
        }
        int invulTime = tag.getInt(NBT_INVULNERABLE_TIME);
        if (invulTime > 0) {
            tag.putInt(NBT_INVULNERABLE_TIME, invulTime - 1);
        } else {
            tag.remove(NBT_INVULNERABLE_TIME);
        }
    }

    // ========== 属性修饰符辅助方法 ==========

    private static void applyAttributeModifier(LivingEntity entity, Attribute attribute, UUID uuid,
                                               String name, double value,
                                               AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(uuid, name, value, operation);
        instance.removeModifier(uuid);
        instance.addTransientModifier(modifier);
    }

    private static void removeAttributeModifier(LivingEntity entity, Attribute attribute, UUID uuid) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(uuid);
    }

    // ========== 维度特定增益 ==========

    private static void applyDimensionBuffs(LivingEntity entity) {
        // 归隐之地：万法皆通私有维度，无法映射，降级跳过（原逻辑在 DreamCrystalMaidEvents 中维持无敌）
        Level level = entity.level();
        if (level.dimension() == Level.OVERWORLD) {
            // 主世界：饱和 II、抗性提升 II、生命恢复 II
            entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, 300, 1, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1, false, false));
        } else if (level.dimension() == Level.NETHER) {
            // 下界：力量 III、迅捷 III、急迫 III
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 2, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 2, false, false));
        } else if (level.dimension() == Level.END) {
            // 末地：夜视 IV、抗性提升 IV、伤害提升 IV
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1360, 3, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 3, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 3, false, false));
        }
    }

    // ========== 范围女仆强化 ==========

    private static void applyRangeMaidBoost(LivingEntity source) {
        MinecraftServer server = source.getServer();
        if (server == null) {
            return;
        }
        long expiry = saturatingAdd(globalGameTime(server), 40L);

        // 扫描 20 格内的女仆（排除自身）
        List<EntityMaid> nearbyMaids = source.level().getEntitiesOfClass(
                EntityMaid.class,
                source.getBoundingBox().inflate(20.0),
                m -> m != source && m.isAlive()
        );

        for (EntityMaid nearMaid : nearbyMaids) {
            applyOrRefreshMaidBoost(nearMaid, expiry);
        }
    }

    // ========== 修复背包耐久度 ==========

    private static void repairInventory(LivingEntity entity) {
        IItemHandler handler;
        if (entity instanceof EntityMaid maid) {
            handler = maid.getAvailableInv(false);
        } else if (entity instanceof Player player) {
            // 人是狐玩家：修复其真实背包（含快捷栏与副手）
            handler = new net.minecraftforge.items.wrapper.PlayerInvWrapper(player.getInventory());
        } else {
            return;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.isDamaged()) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }

    // ========== 随机正面效果 ==========

    /**
     * 获取缓存的正面效果列表（延迟初始化，白/黑名单镜像万法皆通 Config 默认值）。
     */
    private static List<MobEffect> getBeneficialEffects() {
        if (CACHED_BENEFICIAL_EFFECTS != null) {
            return CACHED_BENEFICIAL_EFFECTS;
        }

        List<MobEffect> candidates = new ArrayList<>();
        EffectMatcher whitelistMatcher = USE_EFFECT_WHITELIST
                ? EffectMatcher.from(EFFECT_WHITELIST)
                : EffectMatcher.empty();
        EffectMatcher blacklistMatcher = EffectMatcher.from(EFFECT_BLACKLIST);

        BuiltInRegistries.MOB_EFFECT.entrySet().forEach(entry -> {
            MobEffect effect = entry.getValue();
            if (effect.getCategory() == MobEffectCategory.BENEFICIAL) {
                ResourceLocation location = BuiltInRegistries.MOB_EFFECT.getKey(effect);
                if (location == null) {
                    return;
                }
                String effectId = location.toString();
                boolean allowedByWhitelist = !USE_EFFECT_WHITELIST || whitelistMatcher.matches(effectId);
                boolean blockedByBlacklist = blacklistMatcher.matches(effectId);
                if (allowedByWhitelist && !blockedByBlacklist) {
                    candidates.add(effect);
                }
            }
        });

        CACHED_BENEFICIAL_EFFECTS = candidates;
        return candidates;
    }

    private static void applyRandomBeneficialEffects(LivingEntity entity) {
        List<MobEffect> candidates = getBeneficialEffects();
        if (candidates.isEmpty()) {
            return;
        }

        RandomSource rng = entity.getRandom();
        // 随机选 2 个不重复的正面效果
        Set<Integer> chosen = new HashSet<>();
        int attempts = 0;
        while (chosen.size() < 2 && attempts < 50) {
            chosen.add(rng.nextInt(candidates.size()));
            attempts++;
        }

        for (int index : chosen) {
            MobEffect effect = candidates.get(index);
            int amplifier = 1 + rng.nextInt(9);       // 等级 2-10（amplifier 1-9）
            int duration = 600 + rng.nextInt(601);    // 30-60 秒（600-1200 tick）
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true));
        }
    }

    // ========== Curios 额外槽位 ==========

    /**
     * 为当前已有的每一种 Curios 槽位类型增加 1 格（transient，需要周期性刷新）。
     * Curios 未安装时跳过（与车万女仆的软依赖方式一致）。
     */
    private static void applyCuriosSlot(LivingEntity entity) {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        CuriosApi.getCuriosInventory(entity).ifPresent(PlayerMaidDreamCatBauble::applyCuriosSlots);
    }

    private static void applyCuriosSlots(ICuriosItemHandler handler) {
        for (String slotType : handler.getCurios().keySet()) {
            handler.removeSlotModifier(slotType, DC_CURIOS_SLOT_UUID);
            handler.addTransientSlotModifier(slotType, DC_CURIOS_SLOT_UUID,
                    "dream_crystal_slot", 1.0, AttributeModifier.Operation.ADDITION);
        }
    }

    private static void removeCuriosSlots(LivingEntity entity) {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            for (String slotType : handler.getCurios().keySet()) {
                handler.removeSlotModifier(slotType, DC_CURIOS_SLOT_UUID);
            }
        });
    }

    // ========== 概率复活 ==========

    /**
     * 概率复活（100% - N×10%，N 为 120 秒窗口内复活次数）。返回是否成功复活。
     * 女仆侧由 {@link #onDeath} 调用，人是狐玩家侧由 {@link PlayerMaidDreamCatHandler#onLivingDeath} 调用。
     */
    public static boolean tryRevive(LivingEntity entity, ItemStack baubleItem, DamageSource source) {
        MinecraftServer server = entity.getServer();
        if (server == null) {
            return false;
        }

        CompoundTag existingTag = baubleItem.getTag();
        int storedClockVersion = existingTag == null ? 0 : existingTag.getInt(NBT_REVIVE_CLOCK_VERSION);
        if (storedClockVersion > REVIVE_CLOCK_VERSION) {
            return false;
        }

        CompoundTag tag = baubleItem.getOrCreateTag();
        long currentTime = globalGameTime(server);
        long legacyTime = entity.level().getGameTime();
        ReviveHistory history = loadReviveHistory(tag, storedClockVersion, legacyTime, currentTime);
        List<Long> timestamps = history.timestamps();

        int reviveCount = timestamps.size();
        float reviveChance = Math.max(0.0f, 1.0f - reviveCount * 0.1f);
        boolean revived = reviveChance > 0.0f && entity.getRandom().nextFloat() < reviveChance;
        if (revived) {
            timestamps.add(currentTime);
        }
        if (history.changed() || revived) {
            saveReviveHistory(tag, timestamps);
        }
        if (!revived) {
            return false;
        }

        float healAmount = entity.getMaxHealth();
        entity.setHealth(healAmount);

        tag.putInt(NBT_INVULNERABLE_TIME, 300);

        entity.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.0f);

        LOGGER.info("[playermaid] 梦云水晶概率复活：实体={}，N={}，概率={}%，恢复至 {} 生命值",
                entity.getName().getString(), reviveCount, (int) (reviveChance * 100), healAmount);

        return true;
    }

    // ========== 进度触发 ==========

    /**
     * 给（女仆的）主人或（人是狐的）玩家本人授予万法皆通"全法术精通"进度。
     * 万法皆通未安装（进度不存在）或主人不在线时静默跳过。
     */
    private static void grantAdvancement(LivingEntity entity) {
        ServerPlayer target = null;
        if (entity instanceof EntityMaid maid) {
            if (!(maid.level() instanceof ServerLevel serverLevel) || maid.getOwnerUUID() == null) {
                return;
            }
            target = serverLevel.getServer().getPlayerList().getPlayer(maid.getOwnerUUID());
        } else if (entity instanceof ServerPlayer player) {
            target = player;
        }
        if (target == null) {
            return;
        }

        ServerAdvancementManager manager = target.server.getAdvancements();
        Advancement advancement = manager.getAdvancement(ADVANCEMENT_ALL_SPELLS);
        if (advancement == null) {
            return;
        }

        PlayerAdvancements playerAdvancements = target.getAdvancements();
        if (!playerAdvancements.getOrStartProgress(advancement).isDone()) {
            advancement.getCriteria().keySet().forEach(criterion ->
                    playerAdvancements.award(advancement, criterion));
        }
    }

    // ========== 调度器（时停/范围强化到期处理） ==========

    public static void processScheduledEffects(MinecraftServer server) {
        if (server == null) {
            return;
        }
        long currentTime = globalGameTime(server);
        processFrozenTargets(currentTime);
        processBoostedMaids(currentTime);
    }

    public static void clearScheduledEffects() {
        FROZEN_TARGETS.clear();
        FROZEN_TARGET_EXPIRIES.clear();
        BOOSTED_MAIDS.clear();
        BOOSTED_MAID_EXPIRIES.clear();
    }

    /**
     * 冻结目标（时停 1 秒）：设置 no-ai 并在到期后恢复。
     * 由 {@link PlayerMaidDreamCatHandler#onLivingHurt} 在人是狐玩家直接攻击时调用。
     */
    static void freezeTarget(Mob mob, long expiry) {
        UUID targetUUID = mob.getUUID();
        FrozenTargetState existing = FROZEN_TARGETS.get(targetUUID);
        if (existing != null && existing.expiry >= expiry) {
            existing.target = mob;
            if (SET_NO_AI_ENABLED) {
                mob.setNoAi(true);
            }
            return;
        }

        FrozenTargetState state = new FrozenTargetState(mob, expiry);
        FROZEN_TARGETS.put(targetUUID, state);
        FROZEN_TARGET_EXPIRIES.add(new ScheduledExpiry(targetUUID, expiry));
        if (SET_NO_AI_ENABLED) {
            mob.setNoAi(true);
        }
    }

    private static void processFrozenTargets(long currentTime) {
        while (!FROZEN_TARGET_EXPIRIES.isEmpty()) {
            ScheduledExpiry scheduled = FROZEN_TARGET_EXPIRIES.peek();
            FrozenTargetState state = FROZEN_TARGETS.get(scheduled.entityId());
            if (state == null || state.expiry != scheduled.expiry()) {
                FROZEN_TARGET_EXPIRIES.poll();
                continue;
            }

            Mob target = state.target;
            if (target == null || !target.isAlive()) {
                FROZEN_TARGETS.remove(scheduled.entityId());
                FROZEN_TARGET_EXPIRIES.poll();
                continue;
            }

            if (currentTime < scheduled.expiry()) {
                break;
            }

            FROZEN_TARGETS.remove(scheduled.entityId());
            FROZEN_TARGET_EXPIRIES.poll();
            if (SET_NO_AI_ENABLED) {
                target.setNoAi(false);
            }
        }
    }

    private static boolean isMaidBoosted(EntityMaid maid) {
        BoostedMaidState state = BOOSTED_MAIDS.get(maid.getUUID());
        if (state == null) {
            return false;
        }
        MinecraftServer server = maid.getServer();
        if (server == null) {
            return false;
        }
        state.maid = maid;
        return globalGameTime(server) < state.expiry;
    }

    private static void applyOrRefreshMaidBoost(EntityMaid maid, long expiry) {
        AttributeInstance attackDmg = maid.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDmg == null) {
            return;
        }

        if (attackDmg.getModifier(DC_NEARBY_DAMAGE_UUID) == null) {
            attackDmg.addTransientModifier(new AttributeModifier(
                    DC_NEARBY_DAMAGE_UUID,
                    "dream_crystal_nearby_boost",
                    0.5,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }

        UUID maidUUID = maid.getUUID();
        BoostedMaidState existing = BOOSTED_MAIDS.get(maidUUID);
        if (existing != null && existing.expiry >= expiry) {
            existing.maid = maid;
            return;
        }

        BOOSTED_MAIDS.put(maidUUID, new BoostedMaidState(maid, expiry));
        BOOSTED_MAID_EXPIRIES.add(new ScheduledExpiry(maidUUID, expiry));
    }

    private static void processBoostedMaids(long currentTime) {
        while (!BOOSTED_MAID_EXPIRIES.isEmpty()) {
            ScheduledExpiry scheduled = BOOSTED_MAID_EXPIRIES.peek();
            BoostedMaidState state = BOOSTED_MAIDS.get(scheduled.entityId());
            if (state == null || state.expiry != scheduled.expiry()) {
                BOOSTED_MAID_EXPIRIES.poll();
                continue;
            }

            EntityMaid maid = state.maid;
            if (maid == null || !maid.isAlive()) {
                BOOSTED_MAIDS.remove(scheduled.entityId());
                BOOSTED_MAID_EXPIRIES.poll();
                continue;
            }

            if (currentTime < scheduled.expiry()) {
                break;
            }

            removeAttributeModifier(maid, Attributes.ATTACK_DAMAGE, DC_NEARBY_DAMAGE_UUID);
            BOOSTED_MAIDS.remove(scheduled.entityId());
            BOOSTED_MAID_EXPIRIES.poll();
        }
    }

    // ========== 物品查找 ==========

    /**
     * 梦云水晶物品实例（注册表惰性查询；万法皆通未安装时返回 null，全链路自动失效）。
     */
    public static Item getDreamCrystalItem() {
        if (dreamCrystalItem == null) {
            dreamCrystalItem = ForgeRegistries.ITEMS.getValue(DREAM_CRYSTAL_ID);
        }
        return dreamCrystalItem;
    }

    /** 指定容器（饰品栏）内是否含有梦云水晶。 */
    public static boolean containsDreamCrystal(IItemHandler handler) {
        Item item = getDreamCrystalItem();
        if (item == null) {
            return false;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }

    /** 指定容器（饰品栏）内第一枚梦云水晶的 ItemStack，没有则返回空。 */
    public static ItemStack findDreamCrystalStack(IItemHandler handler) {
        Item item = getDreamCrystalItem();
        if (item == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() == item) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // ========== 复活历史 NBT 读写（移植自 DreamCatCrystalBauble） ==========

    private static ReviveHistory loadReviveHistory(CompoundTag tag, int storedClockVersion,
                                                   long legacyNow, long serverNow) {
        long[] storedTimestamps = tag.contains(NBT_REVIVE_TIMESTAMPS)
                ? tag.getLongArray(NBT_REVIVE_TIMESTAMPS)
                : new long[0];
        List<Long> normalized = new ArrayList<>(Math.min(storedTimestamps.length, MAX_REVIVE_TIMESTAMPS));
        long oldestAllowed = saturatingSubtract(serverNow, REVIVE_WINDOW_TICKS);

        for (long storedTimestamp : storedTimestamps) {
            long timestamp;
            if (storedClockVersion < REVIVE_CLOCK_VERSION) {
                timestamp = migrateTimestamp(
                        storedTimestamp, legacyNow, serverNow, REVIVE_WINDOW_TICKS, LEGACY_TIMESTAMP_GRACE_TICKS);
            } else {
                timestamp = storedTimestamp;
            }
            timestamp = Math.min(timestamp, serverNow);
            if (timestamp >= oldestAllowed) {
                normalized.add(timestamp);
            }
        }

        normalized.sort(Long::compareTo);
        if (normalized.size() > MAX_REVIVE_TIMESTAMPS) {
            normalized = new ArrayList<>(normalized.subList(
                    normalized.size() - MAX_REVIVE_TIMESTAMPS, normalized.size()));
        }

        long[] normalizedTimestamps = toLongArray(normalized);
        boolean changed = storedClockVersion != REVIVE_CLOCK_VERSION
                || !Arrays.equals(storedTimestamps, normalizedTimestamps)
                || (normalized.isEmpty() && tag.contains(NBT_REVIVE_TIMESTAMPS));
        return new ReviveHistory(normalized, changed);
    }

    private static void normalizeReviveHistory(LivingEntity entity, ItemStack baubleItem) {
        CompoundTag tag = baubleItem.getTag();
        if (tag == null
                || (!tag.contains(NBT_REVIVE_CLOCK_VERSION) && !tag.contains(NBT_REVIVE_TIMESTAMPS))) {
            return;
        }

        int storedClockVersion = tag.getInt(NBT_REVIVE_CLOCK_VERSION);
        if (storedClockVersion > REVIVE_CLOCK_VERSION) {
            return;
        }
        MinecraftServer server = entity.getServer();
        if (server == null) {
            return;
        }

        ReviveHistory history = loadReviveHistory(
                tag, storedClockVersion, entity.level().getGameTime(), globalGameTime(server));
        if (history.changed()) {
            saveReviveHistory(tag, history.timestamps());
        }
    }

    private static void saveReviveHistory(CompoundTag tag, List<Long> timestamps) {
        if (tag.getInt(NBT_REVIVE_CLOCK_VERSION) != REVIVE_CLOCK_VERSION) {
            tag.putInt(NBT_REVIVE_CLOCK_VERSION, REVIVE_CLOCK_VERSION);
        }
        if (timestamps.isEmpty()) {
            if (tag.contains(NBT_REVIVE_TIMESTAMPS)) {
                tag.remove(NBT_REVIVE_TIMESTAMPS);
            }
            return;
        }

        long[] values = toLongArray(timestamps);
        if (!Arrays.equals(tag.getLongArray(NBT_REVIVE_TIMESTAMPS), values)) {
            tag.putLongArray(NBT_REVIVE_TIMESTAMPS, values);
        }
    }

    private static long[] toLongArray(List<Long> timestamps) {
        long[] values = new long[timestamps.size()];
        for (int i = 0; i < timestamps.size(); i++) {
            values[i] = timestamps.get(i);
        }
        return values;
    }

    // ========== 时间工具（移植自万法皆通 PortableTimerMath） ==========

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

    private static long saturatingSubtract(long left, long right) {
        if (right > 0 && left < Long.MIN_VALUE + right) {
            return Long.MIN_VALUE;
        }
        if (right < 0 && left > Long.MAX_VALUE + right) {
            return Long.MAX_VALUE;
        }
        return left - right;
    }

    private static long clampRemaining(long remainingTicks, long maxRemainingTicks, long expiredGraceTicks) {
        long maximum = Math.max(0L, maxRemainingTicks);
        if (maximum == 0L) {
            return 0L;
        }
        if (remainingTicks <= 0L) {
            return Math.min(maximum, Math.max(0L, expiredGraceTicks));
        }
        return Math.min(remainingTicks, maximum);
    }

    private static long migrateDeadline(long oldDeadline, long oldNow, long newNow,
                                        long maxRemainingTicks, long expiredGraceTicks) {
        long remaining = saturatingSubtract(oldDeadline, oldNow);
        long boundedRemaining = clampRemaining(remaining, maxRemainingTicks, expiredGraceTicks);
        return saturatingAdd(newNow, boundedRemaining);
    }

    private static long migrateTimestamp(long legacyTimestamp, long legacyNow, long serverNow,
                                         long maxAge, long graceTicks) {
        long boundedMaxAge = Math.max(0L, maxAge);
        long legacyDeadline = saturatingAdd(legacyTimestamp, boundedMaxAge);
        long serverDeadline = migrateDeadline(
                legacyDeadline, legacyNow, serverNow, boundedMaxAge, graceTicks);
        return saturatingSubtract(serverDeadline, boundedMaxAge);
    }

    // ========== 内部记录 ==========

    private record ScheduledExpiry(UUID entityId, long expiry) {
    }

    private record ReviveHistory(List<Long> timestamps, boolean changed) {
    }

    private static final class FrozenTargetState {
        private Mob target;
        private final long expiry;

        private FrozenTargetState(Mob target, long expiry) {
            this.target = target;
            this.expiry = expiry;
        }
    }

    private static final class BoostedMaidState {
        private EntityMaid maid;
        private final long expiry;

        private BoostedMaidState(EntityMaid maid, long expiry) {
            this.maid = maid;
            this.expiry = expiry;
        }
    }

    /** 效果匹配器：精确 id 或正则（移植自 DreamCatCrystalBauble.EffectMatcher）。 */
    private record EffectMatcher(Set<String> exactMatches, List<Pattern> regexPatterns) {
        private static final String REGEX_PREFIX = "regex:";
        private static final EffectMatcher EMPTY =
                new EffectMatcher(Collections.emptySet(), Collections.emptyList());

        private static EffectMatcher empty() {
            return EMPTY;
        }

        private static EffectMatcher from(List<String> entries) {
            if (entries == null || entries.isEmpty()) {
                return empty();
            }

            Set<String> exactMatches = new HashSet<>();
            List<Pattern> regexPatterns = new ArrayList<>();

            for (String entry : entries) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                if (entry.startsWith(REGEX_PREFIX)) {
                    String regex = entry.substring(REGEX_PREFIX.length());
                    if (regex.isBlank()) {
                        continue;
                    }
                    try {
                        regexPatterns.add(Pattern.compile(regex));
                    } catch (PatternSyntaxException exception) {
                        LOGGER.warn("梦云水晶效果匹配正则无效：{}，已跳过", entry, exception);
                    }
                    continue;
                }
                exactMatches.add(entry);
            }

            if (exactMatches.isEmpty() && regexPatterns.isEmpty()) {
                return empty();
            }
            return new EffectMatcher(exactMatches, regexPatterns);
        }

        private boolean matches(String effectId) {
            if (exactMatches.contains(effectId)) {
                return true;
            }
            for (Pattern pattern : regexPatterns) {
                if (pattern.matcher(effectId).matches()) {
                    return true;
                }
            }
            return false;
        }
    }
}
