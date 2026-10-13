package io.github.zgxhzhr.playermaid.client.screen;

import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.authlib.GameProfile;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.ScheduleMode;
import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidSettingsPacket;
import io.github.zgxhzhr.playermaid.network.packet.SubmitFoxMaidSettingsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 「人是狐设置」界面：由 {@code /playermaidgui [玩家]} 打开，不依赖调试器（superdbg）。
 *
 * <p>可配置项与调试器的「人是狐」页一致：开关、渲染名、主人、好感度、日程、无敌。
 * 全部修改在本地暂存，点「确认」时整份提交给服务端校验落库，点「取消」或按 ESC 不留改动。</p>
 *
 * <p>主人一栏点开后会展开在线玩家列表（含首行「清除主人」）：选定主人后，该玩家会成为
 * 被配置者的真实主人实体——此后只有主人能右键打开其女仆背包，主人的饰品联动也随之生效
 * （双心之链分担伤害、泣血之心为回血、发簪转移伤害、梦云水晶授予进度）。</p>
 */
public class FoxMaidSettingsScreen extends Screen {

    // ===== 布局常量 =====
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 250;
    private static final int ROW_HEIGHT = 24;
    private static final int FIRST_ROW_Y = 30;
    /** 控件列起始 x（相对面板左上角）。 */
    private static final int CONTROL_X = 130;
    private static final int CONTROL_WIDTH = 174;
    private static final int ROW_WIDGET_HEIGHT = 20;

    /** 主人列表折叠时的可见行数。 */
    private static final int OWNER_LIST_ROWS = 6;
    private static final int OWNER_ROW_HEIGHT = 18;

    private static final int COLOR_PANEL_BG = 0xF00E0E12;
    private static final int COLOR_PANEL_BORDER = 0xFF3A3A46;
    private static final int COLOR_LABEL = 0xFFE0E0E0;
    private static final int COLOR_HINT = 0xFF9A9AA8;
    private static final int COLOR_ROW_HOVER = 0x60FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x8040A0FF;

    // ===== 界面状态（本地暂存，确认时才提交） =====
    private final int entityId;
    private final String targetName;
    private boolean active;
    @Nullable
    private String ownerUuid;
    @Nullable
    private String ownerName;
    private ScheduleMode schedule;
    private boolean invulnerable;
    /** 渲染名暂存（窗口缩放重建控件时用它回填，避免输入内容丢失）。 */
    @Nullable
    private String pendingRenderName;
    /** 好感度暂存（同上）。 */
    private int pendingFavorability;

    // ===== 控件 =====
    private EditBox renderNameBox;
    private EditBox favorabilityBox;
    private Button activeButton;
    private Button ownerButton;
    private Button scheduleButton;
    private Button invulnerableButton;

    // ===== 主人列表 =====
    private final List<OwnerEntry> owners = new ArrayList<>();
    private boolean ownerListOpen;
    private int ownerScroll;

    private int leftPos;
    private int topPos;

    public FoxMaidSettingsScreen(OpenFoxMaidSettingsPacket packet) {
        super(Component.literal("人是狐设置"));
        this.entityId = packet.entityId();
        this.targetName = packet.targetName();
        this.active = packet.active();
        this.ownerUuid = packet.ownerUuid();
        this.ownerName = packet.ownerName();
        this.schedule = ScheduleMode.fromName(packet.schedule());
        this.invulnerable = packet.invulnerable();
        this.pendingRenderName = packet.renderName();
        this.pendingFavorability = packet.favorability();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

        // 人是狐开关
        activeButton = Button.builder(activeLabel(), b -> {
            active = !active;
            b.setMessage(activeLabel());
        }).bounds(rowX(CONTROL_X), rowY(0), CONTROL_WIDTH, ROW_WIDGET_HEIGHT).build();
        this.addRenderableWidget(activeButton);

        // 渲染名输入框
        renderNameBox = new EditBox(this.font, rowX(CONTROL_X), rowY(1) + 1, CONTROL_WIDTH, 18,
                Component.literal("渲染名"));
        renderNameBox.setMaxLength(32);
        renderNameBox.setValue(pendingRenderName == null ? "" : pendingRenderName);
        this.addRenderableWidget(renderNameBox);

        // 主人选择按钮（点开在线玩家列表）
        ownerButton = Button.builder(ownerLabel(), b -> {
            ownerListOpen = !ownerListOpen;
            ownerScroll = 0;
        }).bounds(rowX(CONTROL_X), rowY(2), CONTROL_WIDTH, ROW_WIDGET_HEIGHT).build();
        this.addRenderableWidget(ownerButton);

        // 好感度输入框（仅数字）
        favorabilityBox = new EditBox(this.font, rowX(CONTROL_X), rowY(3) + 1, 60, 18,
                Component.literal("好感度"));
        favorabilityBox.setMaxLength(3);
        favorabilityBox.setFilter(text -> text.isEmpty() || text.matches("\\d{1,3}"));
        favorabilityBox.setValue(String.valueOf(pendingFavorability));
        this.addRenderableWidget(favorabilityBox);

        // 日程循环按钮
        scheduleButton = Button.builder(scheduleLabel(), b -> {
            schedule = schedule.next();
            b.setMessage(scheduleLabel());
        }).bounds(rowX(CONTROL_X), rowY(4), CONTROL_WIDTH, ROW_WIDGET_HEIGHT).build();
        this.addRenderableWidget(scheduleButton);

        // 无敌开关
        invulnerableButton = Button.builder(invulnerableLabel(), b -> {
            invulnerable = !invulnerable;
            b.setMessage(invulnerableLabel());
        }).bounds(rowX(CONTROL_X), rowY(5), CONTROL_WIDTH, ROW_WIDGET_HEIGHT).build();
        this.addRenderableWidget(invulnerableButton);

        // 确认 / 取消
        int buttonY = topPos + PANEL_HEIGHT - 26;
        this.addRenderableWidget(Button.builder(Component.literal("确认"), b -> submit())
                .bounds(leftPos + 96, buttonY, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("取消"), b -> this.onClose())
                .bounds(leftPos + 164, buttonY, 60, 20).build());

        this.rebuildOwnerList();
    }

    // ===== 坐标辅助 =====
    private int rowX(int offset) {
        return leftPos + offset;
    }

    private int rowY(int index) {
        return topPos + FIRST_ROW_Y + index * ROW_HEIGHT;
    }

    private Component activeLabel() {
        return Component.literal("当前：" + (active ? "开" : "关"));
    }

    private Component invulnerableLabel() {
        return Component.literal("当前：" + (invulnerable ? "开" : "关"));
    }

    private Component scheduleLabel() {
        return Component.literal("当前：" + scheduleDisplayName(schedule));
    }

    private Component ownerLabel() {
        return Component.literal("主人：" + (ownerName == null ? "未认主" : ownerName) + " ▾");
    }

    private static String scheduleDisplayName(ScheduleMode mode) {
        return switch (mode) {
            case DAY -> "白天";
            case NIGHT -> "夜晚";
            case ALL -> "全天";
        };
    }

    // ===== 主人列表 =====

    /** 重建在线玩家列表：首行为「清除主人」，其余按名字排序，排除被配置者自己。 */
    private void rebuildOwnerList() {
        owners.clear();
        owners.add(new OwnerEntry(null, "（清除主人）"));
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.getOnlinePlayers().stream()
                    .map(PlayerInfo::getProfile)
                    .filter(profile -> !profile.getName().equals(targetName))
                    .sorted(Comparator.comparing(GameProfile::getName, String.CASE_INSENSITIVE_ORDER))
                    .forEach(profile -> owners.add(
                            new OwnerEntry(profile.getId().toString(), profile.getName())));
        }
    }

    /** 列表整体矩形（含边框）。 */
    private int ownerListTop() {
        return rowY(2) + ROW_WIDGET_HEIGHT + 2;
    }

    private int ownerListHeight() {
        return Math.min(OWNER_LIST_ROWS, Math.max(1, owners.size())) * OWNER_ROW_HEIGHT;
    }

    private boolean isOverOwnerList(double mouseX, double mouseY) {
        return ownerListOpen
                && mouseX >= rowX(CONTROL_X) && mouseX < rowX(CONTROL_X) + CONTROL_WIDTH
                && mouseY >= ownerListTop() && mouseY < ownerListTop() + ownerListHeight();
    }

    private int maxOwnerScroll() {
        return Math.max(0, owners.size() - OWNER_LIST_ROWS);
    }

    private void selectOwner(OwnerEntry entry) {
        ownerUuid = entry.uuid();
        ownerName = entry.uuid() == null ? null : entry.name();
        ownerListOpen = false;
        ownerButton.setMessage(ownerLabel());
    }

    // ===== 绘制 =====

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);

        // 面板底板 + 边框
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, COLOR_PANEL_BG);
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + 1, COLOR_PANEL_BORDER);
        graphics.fill(leftPos, topPos + PANEL_HEIGHT - 1, leftPos + PANEL_WIDTH,
                topPos + PANEL_HEIGHT, COLOR_PANEL_BORDER);
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + PANEL_HEIGHT, COLOR_PANEL_BORDER);
        graphics.fill(leftPos + PANEL_WIDTH - 1, topPos, leftPos + PANEL_WIDTH,
                topPos + PANEL_HEIGHT, COLOR_PANEL_BORDER);

        graphics.drawString(this.font, "人是狐设置 — " + targetName,
                leftPos + 12, topPos + 10, COLOR_LABEL, false);

        this.renderLabels(graphics);

        super.render(graphics, mouseX, mouseY, partialTicks);

        if (ownerListOpen) {
            this.renderOwnerList(graphics, mouseX, mouseY);
        }

        // 校验提示：好感度过高时以服务端钳制为准
        this.renderFavorabilityHint(graphics);
    }

    /** 左列标签。 */
    private void renderLabels(GuiGraphics graphics) {
        String[] labels = {"人是狐状态", "渲染名", "主人", "好感度", "日程", "无敌"};
        for (int i = 0; i < labels.length; i++) {
            graphics.drawString(this.font, labels[i], rowX(16), rowY(i) + 6, COLOR_LABEL, false);
        }
        // 认主规则说明
        graphics.drawString(this.font, "认主后：仅主人可右键打开其背包，饰品主人联动生效",
                leftPos + 16, topPos + PANEL_HEIGHT - 46, COLOR_HINT, false);
        graphics.drawString(this.font, "修改在点「确认」后由服务端校验保存",
                leftPos + 16, topPos + PANEL_HEIGHT - 36, COLOR_HINT, false);
    }

    /** 好感度等级提示（阈值与车万女仆一致：64/192/384）。 */
    private void renderFavorabilityHint(GuiGraphics graphics) {
        int value = currentFavorability();
        int level = value < 64 ? 0 : value < 192 ? 1 : value < 384 ? 2 : 3;
        int next = switch (level) {
            case 0 -> 64 - value;
            case 1 -> 192 - value;
            case 2 -> 384 - value;
            default -> 0;
        };
        String text = "等级 " + level + (level >= 3 ? "（已满）" : "，还需 " + next + " 点");
        graphics.drawString(this.font, text, rowX(CONTROL_X) + 68, rowY(3) + 6, COLOR_HINT, false);
    }

    /** 展开的在线玩家列表（覆盖下方各行，绘制在最上层）。 */
    private void renderOwnerList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = rowX(CONTROL_X);
        int y = ownerListTop();
        int height = ownerListHeight();
        graphics.fill(x - 1, y - 1, x + CONTROL_WIDTH + 1, y + height + 1, COLOR_PANEL_BORDER);
        graphics.fill(x, y, x + CONTROL_WIDTH, y + height, 0xFF18181E);

        int end = Math.min(owners.size(), ownerScroll + OWNER_LIST_ROWS);
        for (int i = ownerScroll; i < end; i++) {
            int rowTop = y + (i - ownerScroll) * OWNER_ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + CONTROL_WIDTH
                    && mouseY >= rowTop && mouseY < rowTop + OWNER_ROW_HEIGHT;
            OwnerEntry entry = owners.get(i);
            boolean selected = entry.uuid() == null ? ownerUuid == null : entry.uuid().equals(ownerUuid);
            if (selected) {
                graphics.fill(x, rowTop, x + CONTROL_WIDTH, rowTop + OWNER_ROW_HEIGHT, COLOR_ROW_SELECTED);
            } else if (hovered) {
                graphics.fill(x, rowTop, x + CONTROL_WIDTH, rowTop + OWNER_ROW_HEIGHT, COLOR_ROW_HOVER);
            }
            graphics.drawString(this.font, entry.name(), x + 6, rowTop + 5,
                    entry.uuid() == null ? COLOR_HINT : COLOR_LABEL, false);
        }

        // 滚动提示（列表超出一屏时）
        if (owners.size() > OWNER_LIST_ROWS) {
            String hint = "滚轮翻页 " + (ownerScroll + 1) + "-" + end + "/" + owners.size();
            graphics.drawString(this.font, hint, x + 6, y + height + 3, COLOR_HINT, false);
        }
    }

    private int currentFavorability() {
        if (favorabilityBox == null) {
            return pendingFavorability;
        }
        try {
            return Math.max(0, Math.min(Constants.MAX_FAVORABILITY,
                    Integer.parseInt(favorabilityBox.getValue().trim())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ===== 交互 =====

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ownerListOpen) {
            if (isOverOwnerList(mouseX, mouseY)) {
                int index = ownerScroll + (int) ((mouseY - ownerListTop()) / OWNER_ROW_HEIGHT);
                if (index >= 0 && index < owners.size()) {
                    selectOwner(owners.get(index));
                }
                return true;
            }
            // 点列表外任意处：收起列表并吞掉本次点击，避免误触到下层控件
            if (!isOverOwnerButton(mouseX, mouseY)) {
                ownerListOpen = false;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverOwnerButton(double mouseX, double mouseY) {
        return mouseX >= rowX(CONTROL_X) && mouseX < rowX(CONTROL_X) + CONTROL_WIDTH
                && mouseY >= rowY(2) && mouseY < rowY(2) + ROW_WIDGET_HEIGHT;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverOwnerList(mouseX, mouseY) && maxOwnerScroll() > 0) {
            ownerScroll = Math.max(0, Math.min(maxOwnerScroll(),
                    ownerScroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        // 与背包界面一致：不暂停世界，多人环境下行为统一
        return false;
    }

    // ===== 提交 =====

    /** 整份设置回传服务端（服务端会再做权限与取值校验）。 */
    private void submit() {
        String renderName = renderNameBox == null ? pendingRenderName : renderNameBox.getValue();
        int favorability = currentFavorability();
        PacketDistributor.sendToServer(new SubmitFoxMaidSettingsPacket(
                entityId, active, renderName, ownerUuid, favorability, schedule.name(), invulnerable));
        this.onClose();
    }

    /** 在线玩家列表条目；{@code uuid} 为 null 表示「清除主人」。 */
    private record OwnerEntry(@Nullable String uuid, String name) {
    }
}
