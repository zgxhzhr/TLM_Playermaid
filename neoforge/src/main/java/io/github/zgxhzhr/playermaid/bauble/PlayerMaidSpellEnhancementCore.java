package io.github.zgxhzhr.playermaid.bauble;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 法术强化核心（spell_enhancement_core）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code SpellEnhancementBauble}：按注册表情形遍历全部「铁魔法」属性
 * （{@code attribute.irons_spellbooks.*}），把主人高于该属性默认值的部分，
 * 以 ADDITION 临时修饰符叠加到佩戴者身上；主人属性下降或无加成时移除修饰符。</p>
 *
 * <p><b>本次补齐</b>：此前的注释为「人是狐玩家佩戴即自复制，无意义」而登记为无效果。
 * 在玩家有了真实主人实体（{@link io.github.zgxhzhr.playermaid.data.FoxMaidData#getOwnerUuid()}）之后，
 * 该映射不再是无意义的自复制——主人即另一名真实玩家，可如实复制其铁魔法属性。
 * 未认主或主人不在线时不做任何加成，并清掉可能残留的旧主人加成。</p>
 *
 * <p>与万法皆通原版一致：仅服务端、每 40 tick（2 秒）刷新一次；只读取属性值，
 * 不引用铁魔法模组的任何类（注册表驱动，未安装铁魔法时属性列表为空即无副作用）。</p>
 */
public class PlayerMaidSpellEnhancementCore implements PlayerMaidBauble {

    /** 铁魔法属性的本地化键前缀。 */
    private static final String IRONS_ATTRIBUTE_PREFIX = "attribute.irons_spellbooks.";

    /** 属性刷新周期（tick），与原版一致。 */
    private static final int REFRESH_INTERVAL = 40;

    private static volatile List<Config> configs;
    private static volatile boolean initialized;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (tick % REFRESH_INTERVAL != 0) {
            return;
        }
        ensureInitialized();
        ServerPlayer owner = FoxMaidManager.resolveOwner(player);
        if (owner == null || owner == player) {
            // 无主人在场：清掉加成，避免残留上一位主人的属性
            removeAll(player);
            return;
        }
        updateFromOwner(player, owner);
    }

    @Override
    public void onUnequipLivingEntity(LivingEntity entity, ItemStack baubleItem) {
        if (entity instanceof ServerPlayer player && !player.level().isClientSide) {
            ensureInitialized();
            removeAll(player);
        }
    }

    /** 首次使用时按注册表构建铁魔法属性清单（未安装铁魔法即为空表）。 */
    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (PlayerMaidSpellEnhancementCore.class) {
            if (initialized) {
                return;
            }
            List<Config> list = new ArrayList<>();
            // 1.21：注册表属性以 Holder<Attribute> 传递（getAttribute/getAttributeValue 也只接受 Holder）
            BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
                Attribute attribute = holder.value();
                if (attribute.getDescriptionId().startsWith(IRONS_ATTRIBUTE_PREFIX)) {
                    String shortName = attribute.getDescriptionId().substring(IRONS_ATTRIBUTE_PREFIX.length());
                    list.add(new Config(holder, modifierId(shortName)));
                }
            });
            configs = List.copyOf(list);
            initialized = true;
        }
    }

    /**
     * 铁魔法属性的修饰符标识。
     *
     * <p>1.21 适配：AttributeModifier 的标识由 UUID 改为 ResourceLocation，名称参数已取消，
     * 故把原先的修饰符名 {@code playermaid_<短名>} 收进 id 的 path（命名空间 playermaid）。</p>
     */
    private static ResourceLocation modifierId(String shortName) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spell_enhancement_core_" + shortName);
    }

    /** 把主人高于默认值的属性差额叠加到佩戴者身上。 */
    private static void updateFromOwner(ServerPlayer player, ServerPlayer owner) {
        for (Config config : configs) {
            AttributeInstance instance = player.getAttribute(config.attribute());
            if (instance == null) {
                continue;
            }
            double bonus = Math.max(0.0D,
                    owner.getAttributeValue(config.attribute()) - config.attribute().value().getDefaultValue());
            AttributeModifier current = instance.getModifier(config.modifierId());
            if (bonus <= 0.0D) {
                if (current != null) {
                    instance.removeModifier(config.modifierId());
                }
                continue;
            }
            if (current != null && Double.compare(current.amount(), bonus) == 0) {
                continue;
            }
            if (current != null) {
                instance.removeModifier(config.modifierId());
            }
            // 1.21 的 AttributeModifier 是 record：只有 (id, amount, operation)
            instance.addTransientModifier(new AttributeModifier(config.modifierId(),
                    bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** 移除本饰品挂载的全部属性修饰符。 */
    private static void removeAll(ServerPlayer player) {
        for (Config config : configs) {
            AttributeInstance instance = player.getAttribute(config.attribute());
            if (instance != null && instance.getModifier(config.modifierId()) != null) {
                instance.removeModifier(config.modifierId());
            }
        }
    }

    /** 单条铁魔法属性配置（1.21：属性为 Holder，修饰符标识为 ResourceLocation）。 */
    private record Config(Holder<Attribute> attribute, ResourceLocation modifierId) {
    }
}
