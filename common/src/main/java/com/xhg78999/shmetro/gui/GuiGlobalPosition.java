package com.xhg78999.shmetro.gui;

import com.xhg78999.shmetro.entity.BlockEntityGlobalPosition;
import com.xhg78999.shmetro.network.SHMPacketGDClient;
import com.xhg78999.shmetro.network.SHMPacketDefine;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientLevel;

public class GuiGlobalPosition
extends ScreenMapper
implements IGui {
    private final Button confirm;
    private final WidgetBetterTextField gdid;
    private final BlockPos pos;
    private String id;

    public GuiGlobalPosition(BlockPos pos) {
        super((Component)Text.literal((String)String.valueOf(Text.translatable((String)"gui.shmetro.global_position", (Object[])new Object[0]))));
        this.pos = pos;
        this.gdid = new WidgetBetterTextField("GD 000000");
        this.confirm = UtilitiesClient.newButton((Component)Text.translatable((String)"gui.yes", (Object[])new Object[0]), btn -> this.onClose());
    }

    /**
     * 把输入框内容截到允许的最大长度。
     *
     * <p>【1.1.53】原先这里调 {@code this.gdid.setMaxLength(MAX_GD_CODE_LENGTH)},
     * 但 MTR 的 {@code WidgetBetterTextField.setMaxLength(int)} 是个**空壳** ——
     * 字节码里只有 {@code super.setMaxLength(Integer.MAX_VALUE)}, 它既不保存参数,
     * 也不影响真正生效的 {@code private final int newMaxLength}(构造时固定为 128,
     * 由 {@code trySetLength} 使用)。</p>
     *
     * <p>后果: 输入框实际允许输入到 128 字符, 而服务端只接受
     * {@code MAX_GD_CODE_LENGTH}(16) 个字符 —— 用户可以毫无察觉地输入超长内容,
     * 发包时才被 {@code SHMPacketGDClient} 静默截断, 界面却毫无提示。</p>
     *
     * <p>这里改为在 responder 里显式截断并回写输入框, 让限制真正生效。
     * 回写会再次触发 responder, 但截断后的长度已等于上限, 不会递归。</p>
     */
    private void trimToMaxLength(String value) {
        int maxLength = SHMPacketDefine.MAX_GD_CODE_LENGTH;
        if (value != null && value.length() > maxLength) {
            String trimmed = value.substring(0, maxLength);
            this.gdid.setValue(trimmed);
            this.id = trimmed;
            return;
        }
        this.id = value;
    }

    @Override
    protected void init() {
        super.init();
        this.gdid.setResponder(id -> this.trimToMaxLength(id));
        IDrawing.setPositionAndWidth((AbstractWidget)this.gdid, (int)(this.width / 2 - (int)((double)this.width * 0.4) / 2), (int)(this.height / 2), (int)((int)((double)this.width * 0.4)));
        IDrawing.setPositionAndWidth((AbstractWidget)this.confirm, (int)(this.width / 2 - (int)((double)this.width * 0.2) / 2), (int)((int)((double)this.height * 0.6)), (int)((int)((double)this.width * 0.2)));
        this.addDrawableChild((AbstractWidget)this.gdid);
        this.addDrawableChild((AbstractWidget)this.confirm);
        try {
            BlockEntityGlobalPosition gd;
            ClientLevel world = Minecraft.getInstance().level;
            if (world != null && (gd = (BlockEntityGlobalPosition)world.getBlockEntity(this.pos)) != null) {
                gd.syncData();
                this.gdid.setValue(gd.code);
                this.tick();
            }
        }
        catch (Exception exception) {
            com.xhg78999.shmetro.Main.LOGGER.catching(exception);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.gdid.tick();
    }

    @Override
    public void onClose() {
        super.onClose();
        this.saveData();
    }

    protected void saveData() {
        if (this.id == null) {
            // 【1.1.60 修数据被意外清空】
            // 上游原实现是 `if (this.id == null) this.id = " ";` —— 也就是
            // 「玩家没碰过输入框就点『是』」时, 把方块上原有的编号覆盖成一个空格。
            //
            // 之所以会这样: this.id 只在输入框的 responder 回调里被赋值, 而
            // MC 1.20.1 的 EditBox.setValue(String) **不触发 responder**
            // (它只做 value = text + 移动光标), 所以 init() 里
            // `this.gdid.setValue(gd.code)` 回填原编号时, this.id 仍然是 null。
            // 于是「打开界面 -> 不动输入框 -> 点确定」这条完全正常的操作,
            // 会把玩家之前认真设好的编号清成一个空格。
            //
            // 改成: 没改过就不发包 —— 原值保持不变。
            // 正常改动路径完全不受影响(玩家动过输入框 => id != null => 照常发包)。
            //
            // 注: 这是上游同款缺陷, 但属于「数据被静默清空」这种用户可见的
            // 功能性错误, 且修法不改变任何其它行为, 因此予以修正。
            com.xhg78999.shmetro.Main.LOGGER.debug("SHM: GD 编号未改动, 不发送更新包, 保持原值");
            return;
        }
        SHMPacketGDClient.sendGDUpdateC2S(this.pos, this.id);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        try {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, delta);
            guiGraphics.drawCenteredString(this.font, Text.translatable("gui.shmetro.gd"), this.width / 2, (int)((double)this.height * 0.37), -1);
            drawLengthHint(guiGraphics, this.gdid.getValue(), this.width / 2, (int)((double)this.height * 0.5) + 12);
        }
        catch (Exception var6) {
            com.xhg78999.shmetro.Main.LOGGER.catching(var6);
        }
    }

    /**
     * 在输入框下方显示「已输入 N / 上限」。
     *
     * <p>GD 编号的长度上限是服务端协议的一部分({@code SHMPacketGDServer}
     * 会拒绝超长内容), 而 {@link #trimToMaxLength(String)} 是**静默**截断 ——
     * 用户输入到上限后继续打字, 那些字符会被直接丢掉, 不给任何提示。
     * 这里补一个实时计数, 让上限和已用额度可见。</p>
     */
    private void drawLengthHint(GuiGraphics guiGraphics, String value, int centerX, int y) {
        int length = value == null ? 0 : value.length();
        int maxLength = SHMPacketDefine.MAX_GD_CODE_LENGTH;
        int color = length >= maxLength ? 0xFFFF5555 : 0xFFAAAAAA;
        guiGraphics.drawCenteredString(this.font, Component.literal(length + " / " + maxLength), centerX, y, color);
    }
}
