package io.github.zgxhzhr.playermaid.client.screen;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.client.gui.ITooltipButton;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.MaidSideTabButton;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.MaidTabButton;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.TaskButton;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.util.ParseI18n;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import io.github.zgxhzhr.playermaid.client.FoxMaidStateView;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.ScheduleMode;
import io.github.zgxhzhr.playermaid.menu.FoxMaidBaseMenu;
import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import io.github.zgxhzhr.playermaid.network.packet.UpdateFoxSchedulePacket;
import io.github.zgxhzhr.playermaid.network.packet.UpdateFoxTaskPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 人是狐界面屏幕基类：贴图、按钮、状态条坐标与车万女仆
 * {@code AbstractMaidContainerGui} 完全一致，保证打开后与原版女仆主界面观感相同。
 *
 * <p>本文件的任务列表侧栏、日程按钮时间轴提示等界面代码移植自
 * 车万女仆模组（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源），特此声明。</p>
 *
 * <p>日程按钮与工作模式（任务）切换会发包到服务端真实生效，任意访问者均可操作；
 * 顶部网格直接映射目标玩家的真实背包，底部访问者背包正常可操作。</p>
 */
public abstract class FoxMaidAbstractScreen<T extends FoxMaidBaseMenu> extends AbstractContainerScreen<T> {

    protected static final ResourceLocation BG =
            new ResourceLocation(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_main.png");
    protected static final ResourceLocation SIDE =
            new ResourceLocation(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_side.png");
    protected static final ResourceLocation BUTTON =
            new ResourceLocation(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_button.png");
    protected static final ResourceLocation TASK =
            new ResourceLocation(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_task.png");

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("00");

    // ===== 任务列表侧栏（静态以跨界面切换保持状态，与车万女仆一致） =====
    private static final int TASK_COUNT_PER_PAGE = 12;
    private static int taskPage = 0;
    private static boolean taskListOpen = false;

    /** 被查看的目标玩家。 */
    protected final Player target;

    /** 全部可切换的工作模式（玩家没有女仆实体，按全量任务展示）。 */
    private final List<IMaidTask> taskList;

    private StateSwitchingButton home;
    private StateSwitchingButton pick;
    private StateSwitchingButton ride;
    private ImageButton info;
    private ImageButton skin;
    private ImageButton sound;
    private ImageButton taskSwitch;
    private ImageButton modelDownload;
    private ImageButton pageDown;
    private ImageButton pageUp;
    private ImageButton pageClose;
    private FoxScheduleButton scheduleButton;
    private final List<MaidTabButton> tabs = Lists.newArrayList();

    protected FoxMaidAbstractScreen(T menu, net.minecraft.world.entity.player.Inventory inventory) {
        super(menu, inventory, menu.getTarget().getDisplayName());
        this.imageWidth = 256;
        this.imageHeight = 256;
        this.target = menu.getTarget();
        this.taskList = buildTaskList();
    }

    /**
     * 收集全部工作模式。个别任务在隐藏判定中可能假设存在女仆实体，
     * 人是狐玩家没有该实体，判定抛异常时按可见处理。
     */
    private static List<IMaidTask> buildTaskList() {
        List<IMaidTask> result = new ArrayList<>();
        for (IMaidTask task : TaskManager.getTaskIndex()) {
            boolean hidden;
            try {
                hidden = task.isHidden(null);
            } catch (Exception e) {
                hidden = false;
            }
            if (!hidden) {
                result.add(task);
            }
        }
        return result;
    }

    @Override
    protected void init() {
        // 1.20.1 只有 Screen#init(Minecraft,int,int) 会先清空控件，直接调无参 init()
        // 只是往旧列表里追加：展开/收起任务列表时必须先手动清理，否则旧任务按钮残留并吞掉点击
        this.clearWidgets();
        super.init();
        this.addBaseWidgets();
        this.initAdditionWidgets();
    }

    /** 子类添加的额外控件（主界面/饰品界面的切换按钮）。 */
    protected void initAdditionWidgets() {
    }

    private void addBaseWidgets() {
        // 信息、皮肤、音效小按钮（左侧顶部）
        skin = new ImageButton(leftPos + 62, topPos + 14, 9, 9, 72, 43, 10, BUTTON, b -> {
        });
        info = new ImageButton(leftPos + 8, topPos + 14, 9, 9, 72, 65, 10, BUTTON, b -> {
        });
        sound = new ImageButton(leftPos + 52, topPos + 14, 9, 9, 144, 43, 10, BUTTON, b -> {
        });
        this.addRenderableWidget(skin);
        this.addRenderableWidget(info);
        this.addRenderableWidget(sound);

        // 任务切换大按钮：展开/收起左侧任务列表（与车万女仆一致）
        taskSwitch = new ImageButton(leftPos + 4, topPos + 159, 71, 21, 0, 42, 22, BUTTON, b -> {
            taskListOpen = !taskListOpen;
            Minecraft.getInstance().execute(this::init);
        });
        this.addRenderableWidget(taskSwitch);

        // 模型下载按钮占位（保持外观一致，不打开任何界面）
        modelDownload = new ImageButton(leftPos + 20, topPos + 230, 41, 20, 0, 86, 20, BUTTON, b -> {
        });
        this.addRenderableWidget(modelDownload);

        // 三个状态切换按钮：仅本地视觉切换
        home = new StateSwitchingButton(leftPos + 9, topPos + 206, 20, 20, false) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                this.isStateTriggered = !this.isStateTriggered;
            }
        };
        home.initTextureValues(0, 0, 21, 21, BUTTON);
        this.addRenderableWidget(home);

        pick = new StateSwitchingButton(leftPos + 30, topPos + 206, 20, 20, false) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                this.isStateTriggered = !this.isStateTriggered;
            }
        };
        pick.initTextureValues(42, 0, 21, 21, BUTTON);
        this.addRenderableWidget(pick);

        ride = new StateSwitchingButton(leftPos + 51, topPos + 206, 20, 20, false) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                this.isStateTriggered = !this.isStateTriggered;
            }
        };
        ride.initTextureValues(84, 0, 21, 21, BUTTON);
        this.addRenderableWidget(ride);

        // 日程按钮：初始状态取服务端同步值
        ScheduleMode initial = ClientFoxState.get(target)
                .map(FoxMaidStateView::schedule).orElse(ScheduleMode.DAY);
        scheduleButton = new FoxScheduleButton(leftPos + 9, topPos + 187, initial);
        this.addRenderableWidget(scheduleButton);

        // 顶部三个页签：点击仅切换本地高亮
        tabs.clear();
        tabs.add(new MaidTabButton(leftPos + 94, topPos + 5, 107, "main", b -> selectTab(0)));
        tabs.add(new MaidTabButton(leftPos + 119, topPos + 5, 132, "task_config", b -> selectTab(1)));
        tabs.add(new MaidTabButton(leftPos + 144, topPos + 5, 157, "maid_config", b -> selectTab(2)));
        // 主界面页签默认处于“当前页”态（TLM 中当前页 active=false）
        selectTab(0);
        for (MaidTabButton tab : tabs) {
            this.addRenderableWidget(tab);
        }

        // 右侧栏两个页签按钮：任务书、全局配置。仅显示与提示，不打开实际界面。
        MaidSideTabButton taskBook = new MaidSideTabButton(leftPos + 251, topPos + 37, 0, b -> {
        }, List.of(
                Component.translatable("gui.touhou_little_maid.button.task_book"),
                Component.translatable("gui.touhou_little_maid.button.task_book.desc")));
        MaidSideTabButton globalConfig = new MaidSideTabButton(leftPos + 251, topPos + 62, 25, b -> {
        }, List.of(
                Component.translatable("gui.touhou_little_maid.button.global_config"),
                Component.translatable("gui.touhou_little_maid.button.global_config.desc")));
        this.addRenderableWidget(taskBook);
        this.addRenderableWidget(globalConfig);

        // 任务列表：翻页按钮 + 当前页任务按钮
        this.addTaskControlButtons();
        this.addTaskListButtons();
    }

    /** 翻页与关闭按钮（车万女仆坐标，列表收起时隐藏）。 */
    private void addTaskControlButtons() {
        pageDown = new ImageButton(leftPos - 72, topPos + 9, 16, 13, 93, 0, 14, TASK, b -> taskPageDown());
        pageUp = new ImageButton(leftPos - 89, topPos + 9, 16, 13, 110, 0, 14, TASK, b -> taskPageUp());
        pageClose = new ImageButton(leftPos - 19, topPos + 9, 13, 13, 127, 0, 14, TASK, b -> {
            taskListOpen = false;
            Minecraft.getInstance().execute(this::init);
        });
        this.addRenderableWidget(pageUp);
        this.addRenderableWidget(pageDown);
        this.addRenderableWidget(pageClose);
        pageUp.visible = taskListOpen;
        pageDown.visible = taskListOpen;
        pageClose.visible = taskListOpen;
    }

    private void taskPageUp() {
        if (taskPage > 0) {
            taskPage--;
            Minecraft.getInstance().execute(this::init);
        }
    }

    private void taskPageDown() {
        if (taskPage * TASK_COUNT_PER_PAGE + TASK_COUNT_PER_PAGE < taskList.size()) {
            taskPage++;
            Minecraft.getInstance().execute(this::init);
        }
    }

    /** 当前页的任务按钮（每页 12 个，坐标与车万女仆一致）。 */
    private void addTaskListButtons() {
        if (taskPage * TASK_COUNT_PER_PAGE >= taskList.size()) {
            taskPage = 0;
        }
        for (int count = 0; count < TASK_COUNT_PER_PAGE; count++) {
            int index = taskPage * TASK_COUNT_PER_PAGE + count;
            if (index >= taskList.size()) {
                break;
            }
            IMaidTask task = taskList.get(index);
            // 人是狐玩家没有女仆实体，任务一律视为可用，点击即发包切换
            TaskButton button = new TaskButton(task, true, leftPos - 89, topPos + 23 + 19 * count,
                    83, 19, 93, 28, 20, TASK, 256, 256,
                    b -> NetworkHandler.CHANNEL.sendToServer(
                            new UpdateFoxTaskPacket(target.getId(), task.getUid().toString())),
                    taskTooltips(task), Component.empty());
            this.addRenderableWidget(button);
            button.visible = taskListOpen;
        }
    }

    /** 任务按钮的悬浮描述（车万女仆格式：金色标题 + 灰色描述行）。 */
    private static List<Component> taskTooltips(IMaidTask task) {
        List<String> keys;
        try {
            keys = task.getDescription(null);
        } catch (Exception e) {
            // 个别任务的描述依赖女仆实体，人是狐玩家退化为默认描述键
            ResourceLocation uid = task.getUid();
            keys = List.of(String.format("task.%s.%s.desc", uid.getNamespace(), uid.getPath()));
        }
        List<Component> desc = ParseI18n.keysToTrans(keys, ChatFormatting.GRAY);
        if (!desc.isEmpty()) {
            desc.add(0, Component.translatable("task.touhou_little_maid.desc.title")
                    .withStyle(ChatFormatting.GOLD));
        }
        return desc;
    }

    /** 任务列表整体区域（滚轮翻页与背景贴图共用）。 */
    private Rect2i getTaskListArea() {
        return new Rect2i(leftPos - 93, topPos + 5, 92, 251);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (taskListOpen && getTaskListArea().contains((int) mouseX, (int) mouseY)) {
            if (scrollY > 0) {
                taskPageUp();
            } else {
                taskPageDown();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    private void selectTab(int index) {
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).active = (i != index);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.drawCurrentTaskText(graphics);
        this.renderFoxTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.pose().translate(0, 0, -100);
        this.renderBackground(graphics);
        graphics.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        this.drawFoxCharacter(graphics, mouseX, mouseY);
        this.drawBaseInfoGui(graphics);
        this.drawTaskListBg(graphics);
        // 子类随后叠加背包/饰品贴图
    }

    /** 任务列表展开时绘制左侧列表背景。 */
    private void drawTaskListBg(GuiGraphics graphics) {
        if (taskListOpen) {
            Rect2i area = getTaskListArea();
            graphics.blit(TASK, area.getX(), area.getY(), 0, 0, area.getWidth(), area.getHeight());
        }
    }

    /**
     * 左上角模型区渲染目标玩家实体。剪刀区域与车万女仆 drawMaidCharacter 完全一致；
     * 走原版实体渲染管线，安装 Yes Steve Model 时会自动显示 YSM 模型。
     */
    private void drawFoxCharacter(GuiGraphics graphics, int mouseX, int mouseY) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        RenderSystem.enableScissor((int) ((leftPos + 6) * scale), (int) ((topPos + 107 + 42) * scale),
                (int) (67 * scale), (int) (95 * scale));
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + 40, topPos + 100, 40,
                (leftPos + 40) - mouseX, (topPos + 70 - 20) - mouseY, target);
        RenderSystem.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染原版容器标题；任务列表展开时在左上角画页码（车万女仆坐标）
        if (taskListOpen) {
            String text = String.format("%d/%d", taskPage + 1,
                    (taskList.size() - 1) / TASK_COUNT_PER_PAGE + 1);
            graphics.drawString(font, text, -48, 12, 0x333333, false);
        }
    }

    /**
     * 生命/护甲/经验/好感度四条状态条，坐标与贴图完全照抄车万女仆。
     */
    private void drawBaseInfoGui(GuiGraphics graphics) {
        graphics.pose().translate(0, 0, 200);
        Optional<FoxMaidStateView> viewOpt = ClientFoxState.get(target);

        // 生命值
        graphics.blit(SIDE, leftPos + 53, topPos + 113, 0, 0, 9, 9);
        graphics.blit(SIDE, leftPos + 5, topPos + 113, 0, 9, 47, 9);
        double hp = target.getMaxHealth() <= 0 ? 0 : target.getHealth() / target.getMaxHealth();
        graphics.blit(SIDE, leftPos + 7, topPos + 115, 2, 18, (int) (43 * hp), 5);
        drawNumberScale(graphics, target.getHealth(), leftPos + 63, topPos + 114);

        // 护甲
        graphics.blit(SIDE, leftPos + 53, topPos + 124, 9, 0, 9, 9);
        graphics.blit(SIDE, leftPos + 5, topPos + 124, 0, 9, 47, 9);
        double armor = Math.min(target.getAttributeValue(Attributes.ARMOR) / 20.0, 1.0);
        graphics.blit(SIDE, leftPos + 7, topPos + 126, 2, 23, (int) (43 * armor), 5);
        drawNumberScale(graphics, target.getArmorValue(), leftPos + 63, topPos + 125);

        // 经验：直接取玩家经验等级与进度
        graphics.blit(SIDE, leftPos + 53, topPos + 135, 18, 0, 9, 9);
        graphics.blit(SIDE, leftPos + 5, topPos + 135, 0, 9, 47, 9);
        double expPercent = target.experienceProgress;
        graphics.blit(SIDE, leftPos + 7, topPos + 137, 2, 28, (int) (43 * expPercent), 5);
        drawNumberScale(graphics, target.experienceLevel, leftPos + 63, topPos + 136);

        // 好感度：等级内百分比
        graphics.blit(SIDE, leftPos + 53, topPos + 146, 27, 0, 9, 9);
        graphics.blit(SIDE, leftPos + 5, topPos + 146, 0, 9, 47, 9);
        int favorability = viewOpt.map(FoxMaidStateView::favorability).orElse(0);
        graphics.blit(SIDE, leftPos + 7, topPos + 148, 2, 33, (int) (43 * levelPercent(favorability)), 5);
        drawNumberScale(graphics, viewOpt.map(FoxMaidStateView::favorabilityLevel).orElse(0),
                leftPos + 63, topPos + 147);

        // 装饰条
        graphics.blit(SIDE, leftPos + 94, topPos + 7, 107, 0, 149, 21);
        graphics.blit(SIDE, leftPos + 6, topPos + 178, 0, 47, 67, 25);
        // 右侧栏底部装饰
        graphics.blit(SIDE, leftPos + 256, topPos + 37, 235, 107, 21, 50);
    }

    /**
     * 好感度在当前等级内的进度百分比（阈值 64/192/384，与车万女仆一致）。
     */
    private static double levelPercent(int favorability) {
        if (favorability < 64) {
            return favorability / 64.0;
        }
        if (favorability < 192) {
            return (favorability - 64) / 128.0;
        }
        if (favorability < 384) {
            return (favorability - 192) / 192.0;
        }
        return 1.0;
    }

    @SuppressWarnings("all")
    private void drawNumberScale(GuiGraphics graphics, double value, int posX, int posY) {
        String text = formatScale((long) value);
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1);
        graphics.drawString(font, text, posX * 2, posY * 2 + font.lineHeight / 2,
                ChatFormatting.DARK_GRAY.getColor(), false);
        graphics.pose().popPose();
    }

    /** 紧凑数字格式（与车万女仆相同规则）。 */
    private static String formatScale(long v) {
        if (v >= 1_000_000_000L) {
            long unit = v / 1_000_000_000L;
            return unit < 10 ? unit + "." + (v % 1_000_000_000L / 100_000_000L) + "G" : unit + "G";
        } else if (v >= 1_000_000L) {
            long unit = v / 1_000_000L;
            return unit < 10 ? unit + "." + (v % 1_000_000L / 100_000L) + "M" : unit + "M";
        } else if (v >= 1000L) {
            long unit = v / 1000L;
            return unit < 10 ? unit + "." + (v % 1000L / 100L) + "K" : unit + "K";
        } else {
            return DECIMAL_FORMAT.format(v);
        }
    }

    /** 当前工作模式的图标与名称（读服务端同步值，未同步时回退空闲）。 */
    private void drawCurrentTaskText(GuiGraphics graphics) {
        IMaidTask task = currentTask();
        graphics.renderItem(task.getIcon(), leftPos + 6, topPos + 161);
        List<FormattedCharSequence> splitTexts = font.split(task.getName(), 42);
        if (!splitTexts.isEmpty()) {
            graphics.drawString(font, splitTexts.get(0), leftPos + 28, topPos + 165, 0x333333, false);
        }
    }

    /** 按同步的任务 uid 解析任务实例，无效或未同步时回退到默认空闲任务。 */
    private IMaidTask currentTask() {
        String uid = ClientFoxState.get(target).map(FoxMaidStateView::taskUid)
                .orElse(FoxMaidData.DEFAULT_TASK_UID);
        ResourceLocation location = ResourceLocation.tryParse(uid);
        if (location != null) {
            Optional<IMaidTask> task = TaskManager.findTask(location);
            if (task.isPresent()) {
                return task.get();
            }
        }
        return TaskManager.getIdleTask();
    }

    /** 各类按钮的悬浮提示。 */
    private void renderFoxTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        renderStateTooltip(home, graphics, mouseX, mouseY, "gui.touhou_little_maid.button.home");
        renderStateTooltip(pick, graphics, mouseX, mouseY, "gui.touhou_little_maid.button.pickup");
        renderStateTooltip(ride, graphics, mouseX, mouseY, "gui.touhou_little_maid.button.maid_riding_set");
        renderImageTooltip(modelDownload, graphics, mouseX, mouseY, "gui.touhou_little_maid.button.model_download");
        renderImageTooltip(skin, graphics, mouseX, mouseY, "gui.touhou_little_maid.button.skin");
        renderImageTooltip(sound, graphics, mouseX, mouseY, "gui.touhou_little_maid.button.sound");
        renderImageTooltip(taskSwitch, graphics, mouseX, mouseY, "gui.touhou_little_maid.task.switch");
        renderImageTooltip(pageUp, graphics, mouseX, mouseY, "gui.touhou_little_maid.task.previous_page");
        renderImageTooltip(pageDown, graphics, mouseX, mouseY, "gui.touhou_little_maid.task.next_page");
        renderImageTooltip(pageClose, graphics, mouseX, mouseY, "gui.touhou_little_maid.task.close");
        renderInfoTooltip(graphics, mouseX, mouseY);
        renderScheduleTooltip(graphics, mouseX, mouseY);

        // TLM 自带提示接口的控件（页签、饰品按钮等）
        for (Object renderable : this.renderables) {
            if (renderable instanceof ITooltipButton tooltipButton && tooltipButton.isTooltipHovered()) {
                tooltipButton.renderTooltip(graphics, Minecraft.getInstance(), mouseX, mouseY);
            }
        }
    }

    private void renderStateTooltip(StateSwitchingButton button, GuiGraphics graphics, int x, int y, String key) {
        if (button.isHovered()) {
            graphics.renderComponentTooltip(font, Lists.newArrayList(
                    Component.translatable(key + "." + button.isStateTriggered()),
                    Component.translatable(key + ".desc")
            ), x, y);
        }
    }

    private void renderImageTooltip(ImageButton button, GuiGraphics graphics, int x, int y, String key) {
        if (button.isHovered()) {
            graphics.renderComponentTooltip(font,
                    Collections.singletonList(Component.translatable(key)), x, y);
        }
    }

    /** 信息按钮：主人/好感度/日程/无敌状态。 */
    private void renderInfoTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!info.isHovered()) {
            return;
        }
        Optional<FoxMaidStateView> viewOpt = ClientFoxState.get(target);
        List<Component> list = Lists.newArrayList();
        Component title = Component.translatable("tooltips.touhou_little_maid.info.title")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE);
        if (viewOpt.map(FoxMaidStateView::invulnerable).orElse(false)) {
            title = title.copy().append(Component.literal(" ✟").withStyle(ChatFormatting.BLUE));
        }
        list.add(title);
        String prefix = "§a█ ";
        viewOpt.ifPresent(view -> {
            if (view.ownerName() != null) {
                list.add(Component.literal(prefix).withStyle(ChatFormatting.WHITE)
                        .append(Component.translatable("tooltips.touhou_little_maid.info.owner")
                                .append(": ").withStyle(ChatFormatting.AQUA))
                        .append(view.ownerName()));
            }
            list.add(Component.literal(prefix).withStyle(ChatFormatting.WHITE)
                    .append(Component.translatable("tooltips.touhou_little_maid.info.favorability")
                            .append(": ").withStyle(ChatFormatting.AQUA))
                    .append(String.valueOf(view.favorability())));
            list.add(Component.literal(prefix).withStyle(ChatFormatting.WHITE)
                    .append(Component.translatable("gui.touhou_little_maid.schedule."
                            + view.schedule().activityKey()).copy().withStyle(ChatFormatting.AQUA)));
        });
        graphics.renderComponentTooltip(font, list, mouseX, mouseY);
    }

    /**
     * 日程按钮悬浮提示：完整移植车万女仆 {@code ScheduleButton.getTooltips} 的时间轴样式
     * （标题 + 当前游戏时间 + 各活动时间段，当前活动高亮绿色）。
     */
    private void renderScheduleTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!scheduleButton.isHovered()) {
            return;
        }
        ScheduleMode mode = scheduleButton.currentMode();
        int time = (int) (target.level().getDayTime() % 24000L);
        int hour = (time / 1000 + 6) % 24;
        int minute = (time % 1000) * 60 / 1000;
        String activity = currentActivityKey(mode, time);

        List<Component> out = Lists.newArrayList();
        out.add(Component.literal(String.format("§n%s§7 %s:%s",
                I18n.get("gui.touhou_little_maid.schedule." + mode.activityKey()),
                DECIMAL_FORMAT.format(hour), DECIMAL_FORMAT.format(minute))));
        // 这里刻意用 if/else 而非枚举 switch：枚举 switch 会让编译器生成一个惰性的
        // $SwitchMap 合成内部类，该类只在首次执行到此处时才加载，热替换 jar 时会因
        // 读不到 class 而抛 NoClassDefFoundError（线上就是这么崩的）
        if (mode == ScheduleMode.ALL) {
            out.add(timelineLine("work".equals(activity), "00:00-24:00", "work"));
        } else if (mode == ScheduleMode.NIGHT) {
            out.add(timelineLine("work".equals(activity), "18:00-06:00", "work"));
            out.add(timelineLine("rest".equals(activity), "06:00-14:00", "rest"));
            out.add(timelineLine("idle".equals(activity), "14:00-18:00", "idle"));
        } else {
            out.add(timelineLine("work".equals(activity), "06:00-18:00", "work"));
            out.add(timelineLine("idle".equals(activity), "18:00-22:00", "idle"));
            out.add(timelineLine("rest".equals(activity), "22:00-06:00", "rest"));
        }
        out.add(Component.translatable("tooltips.touhou_little_maid.schedule.desc"));
        graphics.renderComponentTooltip(font, out, mouseX, mouseY);
    }

    /** 单条时间段：当前活动绿色方块，其余灰色。 */
    private static Component timelineLine(boolean activeNow, String range, String activityKey) {
        return Component.literal(String.format("%s█ %s %s", activeNow ? "§a" : "§8", range,
                I18n.get("gui.touhou_little_maid.activity." + activityKey)));
    }

    /**
     * 由日程模式与当天时间推出当前活动，时间点与车万女仆的白班/夜班日程一致：
     * 白班 0-12000 工作、12000-16000 休息、16000-24000 睡觉；
     * 夜班 0-8000 睡觉、8000-12000 休息、12000-24000 工作；全天始终工作。
     */
    private static String currentActivityKey(ScheduleMode mode, int time) {
        // 同样避开枚举 switch，防止生成惰性合成类
        if (mode == ScheduleMode.ALL) {
            return "work";
        }
        if (mode == ScheduleMode.DAY) {
            return time < 12000 ? "work" : (time < 16000 ? "idle" : "rest");
        }
        return time < 8000 ? "rest" : (time < 12000 ? "idle" : "work");
    }

    /**
     * 日程循环按钮：贴图三态与车万女仆一致。任何访问者点击都会发包给服务端，
     * 服务端校验后修改目标玩家日程并广播；渲染时以同步状态为准（本地预测兜底）。
     */
    private final class FoxScheduleButton extends Button {
        /** 本地预测值：同步包未到达期间保证连续点击手感。 */
        private ScheduleMode mode;

        private FoxScheduleButton(int x, int y, ScheduleMode initial) {
            super(Button.builder(Component.empty(), b -> {
            }).pos(x, y).size(61, 13));
            this.mode = initial;
        }

        /** 当前模式：优先取服务端同步值。 */
        private ScheduleMode currentMode() {
            return ClientFoxState.get(target).map(FoxMaidStateView::schedule).orElse(mode);
        }

        @Override
        public void onPress() {
            this.mode = currentMode().next();
            NetworkHandler.CHANNEL.sendToServer(new UpdateFoxSchedulePacket(target.getId(), mode));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            ScheduleMode current = currentMode();
            graphics.blit(BUTTON, this.getX(), this.getY(),
                    82, 43 + 14 * current.ordinal(), this.getWidth(), this.getHeight(), 256, 256);
        }
    }
}
