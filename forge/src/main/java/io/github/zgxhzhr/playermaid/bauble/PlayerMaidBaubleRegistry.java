package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 饰品偷天换日注册表（批量绑定覆盖 + 玩家驱动）。
 *
 * <p>登记全部已移植到 playermaid 的饰品：物品注册 id → {@link PlayerMaidBauble} 实现。
 * 分两集：</p>
 * <ul>
 *     <li><b>覆盖绑定集</b>（{@link #OVERRIDE_IDS}）：效果完整移植、与万法皆通原版一致的件，
 *     在服务端启动后反射覆写车万女仆 {@code BaubleManager.BAUBLES} 中对应条目
 *     （女仆也走我们逻辑，效果与玩家一致且与原版一致）。</li>
 *     <li><b>仅玩家集</b>：玩家侧提供降级/占位实现（由 {@link PlayerMaidBaubleEvents} 驱动），
 *     <b>不覆盖</b>车万女仆绑定——女仆继续用万法皆通原版逻辑，零损伤。</li>
 * </ul>
 */
public final class PlayerMaidBaubleRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 已移植饰品的注册 id → 实现（插入序即覆盖顺序）。 */
    private static final Map<ResourceLocation, PlayerMaidBauble> BAUBLES = new LinkedHashMap<>();

    /** 覆盖绑定集：效果完整移植、允许覆写车万女仆绑定的物品 id。 */
    private static final Set<ResourceLocation> OVERRIDE_IDS = new HashSet<>();

    /** 玩家 UUID → 当前已佩戴（在容器中且处于生效状态）的饰品 id 集合，用于卸下回调。 */
    private static final Map<UUID, Set<ResourceLocation>> EQUIPPED = new HashMap<>();

    // ========== 饰品 id ==========
    public static final ResourceLocation ROCK_CRYSTAL = new ResourceLocation("touhou_little_maid_spell", "rock_crystal");
    public static final ResourceLocation FLOW_CORE = new ResourceLocation("touhou_little_maid_spell", "flow_core");
    public static final ResourceLocation BLEEDING_HEART = new ResourceLocation("touhou_little_maid_spell", "bleeding_heart");
    public static final ResourceLocation SPRING_RING = new ResourceLocation("touhou_little_maid_spell", "spring_ring");
    public static final ResourceLocation HAIRPIN = new ResourceLocation("touhou_little_maid_spell", "hairpin");
    public static final ResourceLocation ARC_CROSS = new ResourceLocation("touhou_little_maid_spell", "arc_cross");
    public static final ResourceLocation SOUL_BOOK = new ResourceLocation("touhou_little_maid_spell", "soul_book");
    public static final ResourceLocation FRAGRANT_INGENUITY = new ResourceLocation("touhou_little_maid_spell", "fragrant_ingenuity");
    public static final ResourceLocation DOUBLE_HEART_CHAIN = new ResourceLocation("touhou_little_maid_spell", "double_heart_chain");
    public static final ResourceLocation CHAOS_BOOK = new ResourceLocation("touhou_little_maid_spell", "chaos_book");
    public static final ResourceLocation WOUND_RIME_BLADE = new ResourceLocation("touhou_little_maid_spell", "wound_rime_blade");
    public static final ResourceLocation FLOATING_FOX_LEAF = new ResourceLocation("touhou_little_maid_spell", "floating_fox_leaf");
    public static final ResourceLocation MOLTEN_FOX_LEAF = new ResourceLocation("touhou_little_maid_spell", "molten_fox_leaf");
    public static final ResourceLocation SPELL_ENHANCEMENT_CORE = new ResourceLocation("touhou_little_maid_spell", "spell_enhancement_core");
    public static final ResourceLocation QUICK_CHANT_RING = new ResourceLocation("touhou_little_maid_spell", "quick_chant_ring");
    public static final ResourceLocation SPELL_OVERLIMIT_CORE = new ResourceLocation("touhou_little_maid_spell", "spell_overlimit_core");
    public static final ResourceLocation BLUE_NOTE = new ResourceLocation("touhou_little_maid_spell", "blue_note");
    public static final ResourceLocation SPRING_BLOOM_RETURN = new ResourceLocation("touhou_little_maid_spell", "spring_bloom_return");

    // ========== 车万女仆本体饰品 id ==========
    public static final ResourceLocation DROWN_PROTECT = new ResourceLocation("touhou_little_maid", "drown_protect_bauble");
    public static final ResourceLocation EXPLOSION_PROTECT = new ResourceLocation("touhou_little_maid", "explosion_protect_bauble");
    public static final ResourceLocation ULTRAMARINE_ORB = new ResourceLocation("touhou_little_maid", "ultramarine_orb_elixir");
    public static final ResourceLocation FALL_PROTECT = new ResourceLocation("touhou_little_maid", "fall_protect_bauble");
    public static final ResourceLocation FIRE_PROTECT = new ResourceLocation("touhou_little_maid", "fire_protect_bauble");
    public static final ResourceLocation ITEM_MAGNET = new ResourceLocation("touhou_little_maid", "item_magnet_bauble");
    public static final ResourceLocation MAGIC_PROTECT = new ResourceLocation("touhou_little_maid", "magic_protect_bauble");
    public static final ResourceLocation NIMBLE_FABRIC = new ResourceLocation("touhou_little_maid", "nimble_fabric");
    public static final ResourceLocation PROJECTILE_PROTECT = new ResourceLocation("touhou_little_maid", "projectile_protect_bauble");
    public static final ResourceLocation MUTE = new ResourceLocation("touhou_little_maid", "mute_bauble");
    public static final ResourceLocation WIRELESS_IO = new ResourceLocation("touhou_little_maid", "wireless_io");
    /** 原版不死图腾：饰品栏佩戴时走本实现（主手/副手的原版图腾逻辑不受影响）。 */
    public static final ResourceLocation TOTEM_OF_UNDYING = new ResourceLocation("minecraft", "totem_of_undying");

    static {
        // ===== 覆盖绑定集（完整移植，女仆与玩家一致） =====
        register(ROCK_CRYSTAL, new PlayerMaidRockCrystalBauble(), true);
        register(FLOW_CORE, new PlayerMaidFlowCoreBauble(), true);
        register(BLEEDING_HEART, new PlayerMaidBleedingHeartBauble(), true);
        register(SPRING_RING, new PlayerMaidSpringRingBauble(), true);
        register(HAIRPIN, new PlayerMaidHairpinBauble(), true);
        register(ARC_CROSS, new PlayerMaidArcCrossBauble(), true);
        // 魂之书：完整移植（伤害钳制+间隔），原版伤害处理按其 Global 管线按物品生效，覆盖安全
        register(SOUL_BOOK, new PlayerMaidSoulBookBauble(), true);
        // 馥郁巧思：完整移植（进食好感），女仆侧经 MaidAfterEatEvent 事件等价物补齐
        register(FRAGRANT_INGENUITY, new PlayerMaidFragrantIngenuityBauble(), true);
        // 阶段 1 已移植的梦云水晶纳入同一覆盖表；其玩家侧效果由 PlayerMaidDreamCatHandler 单独驱动
        // （避免双驱动导致 NBT 无敌倒计时重复递减），故本驱动循环跳过它。
        register(PlayerMaidDreamCatBauble.DREAM_CRYSTAL_ID, new PlayerMaidDreamCatBauble(), true);

        // ===== 车万女仆本体饰品（完整移植，覆盖绑定，女仆与玩家一致） =====
        register(DROWN_PROTECT, new PlayerMaidDrownProtectBauble(), true);
        register(EXPLOSION_PROTECT, new PlayerMaidExplosionProtectBauble(), true);
        register(ULTRAMARINE_ORB, new PlayerMaidExtraLifeBauble(), true);
        register(FALL_PROTECT, new PlayerMaidFallProtectBauble(), true);
        register(FIRE_PROTECT, new PlayerMaidFireProtectBauble(), true);
        register(ITEM_MAGNET, new PlayerMaidItemMagnetBauble(), true);
        register(MAGIC_PROTECT, new PlayerMaidMagicProtectBauble(), true);
        register(NIMBLE_FABRIC, new PlayerMaidNimbleFabricBauble(), true);
        register(PROJECTILE_PROTECT, new PlayerMaidProjectileProtectBauble(), true);
        register(MUTE, new PlayerMaidMuteBauble(), true);
        register(WIRELESS_IO, new PlayerMaidWirelessIOBauble(), true);
        register(TOTEM_OF_UNDYING, new PlayerMaidUndyingTotemBauble(), true);

        // ===== 仅玩家集（降级/占位，不覆盖车万女仆绑定，女仆走万法皆通原版） =====
        // 双心之链：主人分担部分无人承担（主人仅为显示名），降级为 50% 伤害减免
        register(DOUBLE_HEART_CHAIN, new PlayerMaidDoubleHeartChainBauble(), false);
        // 混沌之书：伤害拆分管线无法复刻，降级为"保留原伤害 + 简化真实伤害"
        register(CHAOS_BOOK, new PlayerMaidChaosBookBauble(), false);
        // 破愈咒锋：无 setHealth 注入点，降级为每 tick 巡检回退治疗
        register(WOUND_RIME_BLADE, new PlayerMaidWoundRimeBladeBauble(), false);
        // 浮生狐叶/熔岩狐叶：行走物理移植，轨迹方块（万法皆通私有方块）降级跳过
        register(FLOATING_FOX_LEAF, new PlayerMaidFloatingFoxLeafBauble(), false);
        register(MOLTEN_FOX_LEAF, new PlayerMaidMoltenFoxLeafBauble(), false);
        // 法术强化核心：原版把主人铁魔法属性复制给女仆；人是狐玩家佩戴即自复制，无意义
        register(SPELL_ENHANCEMENT_CORE, PlayerMaidNoOpBauble.INSTANCE, false);
        // 迅咏之戒：原版缩短女仆法术冷却（万法皆通施法管线）；玩家无施法管线，无效果
        register(QUICK_CHANT_RING, PlayerMaidNoOpBauble.INSTANCE, false);
        // 法术超限核心：万法皆通占位实现（空），无效果
        register(SPELL_OVERLIMIT_CORE, PlayerMaidNoOpBauble.INSTANCE, false);
        // 蓝符（法术白名单）：原版为容器界面入口物品，饰品绑定本身为空；玩家侧无对应 GUI，无效果
        register(BLUE_NOTE, PlayerMaidNoOpBauble.INSTANCE, false);
        // 春华返还：施法叠层+重伤保护，依赖万法皆通施法管线（玩家无法叠层），无效果
        register(SPRING_BLOOM_RETURN, PlayerMaidNoOpBauble.INSTANCE, false);
    }

    private static void register(ResourceLocation id, PlayerMaidBauble bauble, boolean overrideBinding) {
        BAUBLES.put(id, bauble);
        if (overrideBinding) {
            OVERRIDE_IDS.add(id);
        }
    }

    private PlayerMaidBaubleRegistry() {
    }

    // ================================================================
    // 绑定覆盖（偷天换日核心）
    // ================================================================

    /**
     * 一次性覆盖「覆盖绑定集」内饰品的车万女仆绑定（完整移植件才覆盖，
     * 降级/仅玩家件不覆盖，女仆继续用万法皆通原版逻辑）。
     * 万法皆通未安装（物品未注册）时对应条目自动跳过，不影响其它条目。
     */
    public static void overrideBindings() {
        try {
            Class<?> clazz = Class.forName("com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager");
            Field field = clazz.getDeclaredField("BAUBLES");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> current = (Map<Object, Object>) field.get(null);
            HashMap<Object, Object> copy = new HashMap<>(current);

            int overridden = 0;
            int skipped = 0;
            for (ResourceLocation id : OVERRIDE_IDS) {
                PlayerMaidBauble bauble = BAUBLES.get(id);
                if (bauble == null || ForgeRegistries.ITEMS.getValue(id) == null) {
                    skipped++;
                    continue;
                }
                RegistryObject<Item> key = RegistryObject.create(id, ForgeRegistries.ITEMS);
                Object original = copy.put(key, bauble);
                if (original != null) {
                    overridden++;
                }
            }
            field.set(null, copy);
            LOGGER.info("[playermaid] 饰品绑定覆盖完成：覆盖绑定集 {} 件，实际覆盖 {} 件，跳过 {} 件（物品未注册）",
                    OVERRIDE_IDS.size(), overridden, skipped);
        } catch (Exception e) {
            LOGGER.warn("[playermaid] 饰品绑定覆盖失败（不影响其他功能）", e);
        }
    }

    // ================================================================
    // 查询
    // ================================================================

    /** 某物品栈是否命中本注册表（即是否有我们移植的实现）。 */
    public static PlayerMaidBauble getBauble(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return null;
        }
        return BAUBLES.get(id);
    }

    /** 目标是佩戴了指定饰品的人是狐玩家（服务端；未激活/未缓存时视为否）。 */
    public static boolean hasBauble(LivingEntity entity, ResourceLocation id) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return false;
        }
        FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
        if (data == null || !data.isActive()) {
            return false;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return false;
        }
        ItemStackHandler baubles = data.getBaubles();
        for (int i = 0; i < baubles.getSlots(); i++) {
            if (baubles.getStackInSlot(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }

    /** 目标是佩戴了任一已移植饰品的人是狐玩家（服务端）。 */
    public static boolean hasAnyBauble(LivingEntity entity) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return false;
        }
        FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
        if (data == null || !data.isActive()) {
            return false;
        }
        ItemStackHandler baubles = data.getBaubles();
        for (int i = 0; i < baubles.getSlots(); i++) {
            if (getBauble(baubles.getStackInSlot(i)) != null) {
                return true;
            }
        }
        return false;
    }

    /** 女仆饰品栏（TLM BaubleItemHandler）是否含有指定饰品物品。 */
    public static boolean maidHasBaubleItem(EntityMaid maid, ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return false;
        }
        com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler handler = maid.getMaidBauble();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }

    /** 好感度等级：女仆取车万女仆好感度等级，人是狐玩家取 FoxMaidData 等级。 */
    public static int favorabilityLevel(LivingEntity entity) {
        if (entity instanceof EntityMaid maid) {
            return maid.getFavorabilityManager().getLevel();
        }
        if (entity instanceof Player player) {
            FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
            return data == null ? 0 : data.getFavorabilityLevel();
        }
        return 0;
    }

    // ================================================================
    // 玩家每 tick 驱动
    // ================================================================

    /**
     * 人是狐玩家饰品栏驱动：对每格命中本注册表的物品调用对应实现，
     * 并维护佩戴/卸下差异（新出现→onEquip，消失→onUnequip）。
     * 梦云水晶由 {@link PlayerMaidDreamCatHandler#onPlayerTick} 单独驱动，此处跳过。
     */
    public static void tickPlayerBaubles(Player player, FoxMaidData data) {
        if (player.level().isClientSide()) {
            return;
        }
        ItemStackHandler baubles = data.getBaubles();
        Set<ResourceLocation> present = new HashSet<>();
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !BAUBLES.containsKey(id)
                    || id.equals(PlayerMaidDreamCatBauble.DREAM_CRYSTAL_ID)) {
                continue;
            }
            present.add(id);
            BAUBLES.get(id).tickLivingEntity(player, stack, player.tickCount);
        }

        Set<ResourceLocation> equipped = EQUIPPED.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        // 新佩戴：触发 onEquip（传入真实物品栈）
        for (ResourceLocation id : present) {
            if (equipped.add(id)) {
                BAUBLES.get(id).onEquipLivingEntity(player, findStack(baubles, id));
            }
        }
        // 卸下：触发 onUnequip（回收属性修饰符等挂载效果）
        equipped.removeIf(id -> {
            if (present.contains(id)) {
                return false;
            }
            PlayerMaidBauble bauble = BAUBLES.get(id);
            if (bauble != null) {
                bauble.onUnequipLivingEntity(player, ItemStack.EMPTY);
            }
            return true;
        });
    }

    /** 关闭人是狐状态（或玩家未激活）：回收该玩家所有已挂载的饰品效果。 */
    public static void unequipAll(Player player) {
        Set<ResourceLocation> equipped = EQUIPPED.remove(player.getUUID());
        if (equipped == null) {
            return;
        }
        for (ResourceLocation id : equipped) {
            PlayerMaidBauble bauble = BAUBLES.get(id);
            if (bauble != null) {
                bauble.onUnequipLivingEntity(player, ItemStack.EMPTY);
            }
        }
    }

    private static ItemStack findStack(ItemStackHandler baubles, ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack.getItem() == item) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 人是狐玩家饰品栏中含指定饰品的真实物品栈（用于扣耐久/消耗），未佩戴返回空。 */
    public static ItemStack findStack(Player player, ResourceLocation id) {
        if (player.level().isClientSide()) {
            return ItemStack.EMPTY;
        }
        FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
        if (data == null) {
            return ItemStack.EMPTY;
        }
        return findStack(data.getBaubles(), id);
    }
}
