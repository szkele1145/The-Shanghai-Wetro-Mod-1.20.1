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
        // 限制输入长度: 服务端 SHMPacketGDServer 只接受 <= MAX_GD_CODE_LENGTH
        // 字符的编号, 这里在输入端就限住, 避免用户输入超长内容后发包被服务端拒绝。
        this.gdid.setMaxLength(SHMPacketDefine.MAX_GD_CODE_LENGTH);
        this.confirm = UtilitiesClient.newButton((Component)Text.translatable((String)"gui.yes", (Object[])new Object[0]), btn -> this.onClose());
    }

    @Override
    protected void init() {
        super.init();
        this.gdid.setResponder(id -> {
            this.id = id;
        });
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
            this.id = " ";
        }
        SHMPacketGDClient.sendGDUpdateC2S(this.pos, this.id);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        try {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, delta);
            guiGraphics.drawCenteredString(this.font, Text.translatable("gui.shmetro.gd"), this.width / 2, (int)((double)this.height * 0.37), -1);
        }
        catch (Exception var6) {
            com.xhg78999.shmetro.Main.LOGGER.catching(var6);
        }
    }
}
