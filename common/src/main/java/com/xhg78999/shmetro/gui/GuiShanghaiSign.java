package com.xhg78999.shmetro.gui;

import net.minecraft.client.gui.GuiGraphics;
import com.xhg78999.shmetro.blocks.BlockShanghaiSign;
import com.xhg78999.shmetro.network.SHMPacketTrainDataGuiClient;
import com.xhg78999.shmetro.utils.SignCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mtr.block.BlockRailwaySign;
import mtr.block.BlockRouteSignBase;
import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.client.CustomResources;
import mtr.client.IDrawing;
import mtr.data.DataCache;
import mtr.data.DataConverter;
import mtr.data.IGui;
import mtr.data.NameColorDataBase;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderRailwaySign;
import mtr.screen.DashboardListSelectorScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientLevel;

public class GuiShanghaiSign
extends ScreenMapper
implements IGui {
    private int editingIndex = -1;
    private int page;
    private int totalPages;
    private int columns;
    private int rows;
    private final BlockPos signPos;
    private final boolean isRailwaySign;
    private final int length;
    private final String[] signIds;
    private final Set<Long> selectedIds;
    private final List<NameColorDataBase> exitsForList = new ArrayList<NameColorDataBase>();
    private final List<NameColorDataBase> platformsForList = new ArrayList<NameColorDataBase>();
    private final List<NameColorDataBase> routesForList = new ArrayList<NameColorDataBase>();
    private final List<NameColorDataBase> stationsForList = new ArrayList<NameColorDataBase>();
    private final List<String> allSignIds = new ArrayList<String>();
    private final Button[] buttonsEdit;
    private final Button[] buttonsSelection;
    private final Button buttonClear;
    private final ImageButton buttonPrevPage;
    private final ImageButton buttonNextPage;
    private static final int SIGN_SIZE = 32;
    private static final int SIGN_BUTTON_SIZE = 16;
    private static final int BUTTON_Y_START = 62;

    public GuiShanghaiSign(BlockPos signPos) {
        super((Component)Text.literal((String)""));
        this.signPos = signPos;
        // 确保资源重载回调已注册: 玩家换资源包后, 指示牌外观缓存要能失效。
        SignCache.ensureReloadListenerRegistered();
        ClientLevel world = Minecraft.getInstance().level;
        BlockRailwaySign.SignType[] var3 = BlockRailwaySign.SignType.values();
        int i = var3.length;
        for (int var5 = 0; var5 < i; ++var5) {
            BlockRailwaySign.SignType signType = var3[var5];
            this.allSignIds.add(signType.toString());
        }
        // 【1.1.65 补回泛型】上游是 List<String>, 移植时退化成了原始 ArrayList。
        // 原始类型下 Collections.sort 与 addAll 都失去编译期检查, 而且
        // CUSTOM_SIGNS 的 key 类型一旦变化, 这里就会在运行期抛
        // ClassCastException(而且是在渲染路径上, 很难定位)。
        ArrayList<String> sortedKeys = new ArrayList<>(CustomResources.CUSTOM_SIGNS.keySet());
        Collections.sort(sortedKeys);
        this.allSignIds.addAll(sortedKeys);
        try {
            Station station = RailwayData.getStation((Set)ClientData.STATIONS, (DataCache)ClientData.DATA_CACHE, (BlockPos)signPos);
            if (station != null) {
                Map<String, List<String>> exits = station.getGeneratedExits();
                ArrayList<String> exitParents = new ArrayList<>(exits.keySet());
                exitParents.sort(String::compareTo);
                exitParents.forEach(exitParent -> {
                    List<String> destinations = exits.get(exitParent);
                    if (destinations == null || destinations.isEmpty()) {
                        this.exitsForList.add((NameColorDataBase)new DataConverter(Station.serializeExit((String)exitParent), exitParent, 0));
                    } else {
                        this.exitsForList.add((NameColorDataBase)new DataConverter(Station.serializeExit((String)exitParent), exitParent + " " + (String)destinations.get(0), 0));
                    }
                });
                ArrayList<mtr.data.Platform> platforms = new ArrayList<>(ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id).values());
                Collections.sort(platforms);
                Stream<DataConverter> platformStream = platforms.stream().map(platform -> {
                    String platformName = platform.name;
                    // stationDetails 为空时原实现 get(size-1) = get(-1) 会抛
                    // IndexOutOfBoundsException。该异常被外层 catch 吃掉, 后果是
                    // 整个站点的出入口/站台/线路/换乘站列表全部丢失(不只是这一条线路),
                    // 玩家看到空白的牌子编辑界面。这里跳过空列表的线路。
                    return new DataConverter(platform.id, platformName + " " + IGui.mergeStations(ClientData.DATA_CACHE.requestPlatformIdToRoutes(platform.id).stream().filter(route -> !route.stationDetails.isEmpty()).map(route -> ((ClientCache.PlatformRouteDetails.StationDetails)route.stationDetails.get((int)(route.stationDetails.size() - 1))).stationName).collect(Collectors.toList())), 0);
                });
                List<NameColorDataBase> platformList = this.platformsForList;
                Objects.requireNonNull(platformList);
                platformStream.forEach(platformList::add);
                ClientData.DATA_CACHE.getAllRoutesIncludingConnectingStations(station).forEach((color, route) -> this.routesForList.add((NameColorDataBase)new DataConverter((long)route.color, route.name, route.color)));
                ClientData.DATA_CACHE.getConnectingStationsIncludingThisOne(station).forEach(connectingStation -> this.stationsForList.add((NameColorDataBase)new DataConverter(connectingStation.id, connectingStation.name, connectingStation.color)));
            }
        }
        catch (Exception var8) {
            com.xhg78999.shmetro.Main.LOGGER.catching(var8);
        }
        if (world != null) {
            BlockEntity entity = world.getBlockEntity(signPos);
            if (entity instanceof BlockShanghaiSign.TileEntityRailwaySign) {
                // 取副本而非 TE 内部引用: GUI 后续会直接改这两个容器
                // (edit() 改 signIds、选中项切换改 selectedIds)。
                // 若持有 TE 的引用, 客户端的编辑会直接写进客户端侧 TE:
                //  - 未点「保存」就关掉 GUI 时, 本地 TE 与服务端不一致
                //  - 单机档里客户端保存区块时, 会把未提交的编辑写进存档
                this.signIds = ((BlockShanghaiSign.TileEntityRailwaySign)entity).getSignIds().clone();
                this.selectedIds = new HashSet<Long>(((BlockShanghaiSign.TileEntityRailwaySign)entity).getSelectedIds());
                this.isRailwaySign = true;
            } else {
                this.signIds = new String[0];
                this.selectedIds = new HashSet<Long>();
                this.isRailwaySign = false;
                if (entity instanceof BlockRouteSignBase.TileEntityRouteSignBase) {
                    this.selectedIds.add(((BlockRouteSignBase.TileEntityRouteSignBase)entity).getPlatformId());
                }
            }
            this.length = world.getBlockState(signPos).getBlock() instanceof BlockShanghaiSign ? ((BlockShanghaiSign)world.getBlockState((BlockPos)signPos).getBlock()).length : 0;
        } else {
            this.length = 0;
            this.signIds = new String[0];
            this.selectedIds = new HashSet<Long>();
            this.isRailwaySign = false;
        }
        this.buttonsEdit = new Button[this.length];
        for (i = 0; i < this.buttonsEdit.length; ++i) {
            int finalI = i;
            this.buttonsEdit[i] = UtilitiesClient.newButton((Component)Text.translatable((String)"selectWorld.edit", (Object[])new Object[0]), button -> this.edit(finalI));
        }
        this.buttonsSelection = new Button[this.allSignIds.size()];
        for (i = 0; i < this.allSignIds.size(); ++i) {
            int finalI1 = i;
            this.buttonsSelection[i] = UtilitiesClient.newButton((int)16, (Component)Text.literal((String)""), button -> this.setNewSignId(this.allSignIds.get(finalI1)));
        }
        this.buttonClear = UtilitiesClient.newButton((Component)Text.translatable((String)"gui.mtr.reset_sign", (Object[])new Object[0]), button -> this.setNewSignId(null));
        this.buttonPrevPage = new ImageButton(0, 0, 0, 20, 0, 0, 20, new ResourceLocation("mtr:textures/gui/icon_left.png"), 20, 40, button -> this.setPage(this.page - 1));
        this.buttonNextPage = new ImageButton(0, 0, 0, 20, 0, 0, 20, new ResourceLocation("mtr:textures/gui/icon_right.png"), 20, 40, button -> this.setPage(this.page + 1));
    }

    protected void init() {
        int xOffsetSmall;
        super.init();
        for (xOffsetSmall = 0; xOffsetSmall < this.buttonsEdit.length; ++xOffsetSmall) {
            IDrawing.setPositionAndWidth((AbstractWidget)this.buttonsEdit[xOffsetSmall], (int)((this.width - 32 * this.length) / 2 + xOffsetSmall * 32), (int)32, (int)32);
            this.addDrawableChild((AbstractWidget)this.buttonsEdit[xOffsetSmall]);
        }
        this.columns = Math.max((this.width - 48) / 128 * 2, 1);
        this.rows = Math.max((this.height - 32 - 80) / 16, 1);
        xOffsetSmall = (this.width - 16 * (this.columns * 4 + 3)) / 2 + 16;
        int xOffsetBig = xOffsetSmall + 16 * (this.columns + 1);
        int finalXOffsetSmall = xOffsetSmall;
        this.totalPages = Math.max(this.loopSigns((index, x, y, isBig) -> {
            IDrawing.setPositionAndWidth((AbstractWidget)this.buttonsSelection[index], (int)((isBig ? xOffsetBig : finalXOffsetSmall) + x), (int)(62 + y), (int)(isBig ? 48 : 16));
            this.buttonsSelection[index].visible = false;
            this.addDrawableChild((AbstractWidget)this.buttonsSelection[index]);
        }, true), 1);
        int buttonClearX = (this.width - 144 - 80) / 2;
        int buttonY = this.height - 40;
        IDrawing.setPositionAndWidth((AbstractWidget)this.buttonClear, (int)buttonClearX, (int)buttonY, (int)144);
        this.buttonClear.visible = false;
        this.addDrawableChild((AbstractWidget)this.buttonClear);
        IDrawing.setPositionAndWidth((AbstractWidget)this.buttonPrevPage, (int)(buttonClearX + 144), (int)buttonY, (int)20);
        this.buttonPrevPage.visible = false;
        this.addDrawableChild((AbstractWidget)this.buttonPrevPage);
        IDrawing.setPositionAndWidth((AbstractWidget)this.buttonNextPage, (int)(buttonClearX + 144 + 60), (int)buttonY, (int)20);
        this.buttonNextPage.visible = false;
        this.addDrawableChild((AbstractWidget)this.buttonNextPage);
        if (!this.isRailwaySign && this.minecraft != null) {
            UtilitiesClient.setScreen((Minecraft)this.minecraft, (ScreenMapper)new DashboardListSelectorScreen(this::onClose, this.platformsForList, this.selectedIds, true, false));
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        try {
            int xOffsetSmall;
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, delta);
            if (this.minecraft == null) {
                return;
            }
            for (xOffsetSmall = 0; xOffsetSmall < this.signIds.length; ++xOffsetSmall) {
                if (this.signIds[xOffsetSmall] == null) continue;
                RenderRailwaySign.drawSign(guiGraphics.pose(), null, null, (Font)this.font, (BlockPos)this.signPos, (String)this.signIds[xOffsetSmall], (float)((float)(this.width - 32 * this.length) / 2.0f + (float)(xOffsetSmall * 32)), (float)0.0f, (float)32.0f, (float)RenderRailwaySign.getMaxWidth((String[])this.signIds, (int)xOffsetSmall, (boolean)false), (float)RenderRailwaySign.getMaxWidth((String[])this.signIds, (int)xOffsetSmall, (boolean)true), this.selectedIds, (Direction)Direction.UP, (int)0, (textureId, x, y, size, flipTexture) -> {
                    guiGraphics.blit((ResourceLocation)textureId, (int)x, (int)y, 0.0f, 0.0f, (int)size, (int)size, (int)(flipTexture ? -size : size), (int)size);
                });
            }
            if (this.editingIndex >= 0) {
                xOffsetSmall = (this.width - 16 * (this.columns * 4 + 3)) / 2 + 16;
                int xOffsetBig = xOffsetSmall + 16 * (this.columns + 1);
                int finalXOffsetSmall = xOffsetSmall;
                this.loopSigns((index, x, y, isBig) -> {
                    String signId = this.allSignIds.get(index);
                    CustomResources.CustomSign sign = SignCache.getSign(signId);
                    if (sign != null) {
                        boolean moveRight = sign.hasCustomText() && sign.flipCustomText;
                        RenderRailwaySign.drawSign(guiGraphics.pose(), null, null, this.font, this.signPos, signId, (isBig ? xOffsetBig : finalXOffsetSmall) + x + (moveRight ? 32 : 0), 62 + y, 16.0f, 2.0f, 2.0f, this.selectedIds, Direction.UP, 0, (textureId, x1, y1, size, flipTexture) -> guiGraphics.blit(textureId, (int)x1, (int)y1, 0.0f, 0.0f, (int)size, (int)size, (int)(flipTexture ? -size : size), (int)size));
                    }
                }, false);
                // 热路径: 每帧渲染, 直接拼接替代 String.format 的格式串解析
                guiGraphics.drawString(this.font, (this.page + 1) + "/" + this.totalPages, (this.width - 144 - 80) / 2 + 144 + 40, this.height - 40 + 6, -1);
            }
        }
        catch (Exception var7) {
            com.xhg78999.shmetro.Main.LOGGER.catching(var7);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.setPage(this.page + (int)Math.signum(-amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    public void onClose() {
        SHMPacketTrainDataGuiClient.sendSHMSignIdsC2S(this.signPos, this.selectedIds, this.signIds);
        super.onClose();
    }

    public boolean isPauseScreen() {
        return false;
    }

    public void resize(Minecraft client, int width, int height) {
        super.resize(client, width, height);
        for (Button button : this.buttonsEdit) {
            button.active = true;
        }
        for (Button button : this.buttonsSelection) {
            button.visible = false;
        }
        this.editingIndex = -1;
        // 重置到第一页: 窗口尺寸变化会让 init() 重新计算 rows/columns 与
        // totalPages, 新的每页容量通常更小 -> 旧的 this.page 可能超出新范围。
        // 而 init() 里的 loopSigns 是用旧 page 计算按钮可见性的, 超出范围时
        // 所有指示牌选项都会算成「不在当前页」-> 整个选项列表凭空消失。
        this.page = 0;
        this.setPage(0);
    }

    private int loopSigns(LoopSignsCallback loopSignsCallback, boolean ignorePage) {
        int pageCount = this.rows * this.columns;
        int indexSmall = 0;
        int indexBig = 0;
        int columnSmall = 0;
        int columnBig = 0;
        int rowSmall = 0;
        int rowBig = 0;
        int totalPagesSmallCount = 1;
        int totalPagesBigCount = 1;
        for (int i = 0; i < this.allSignIds.size(); ++i) {
            boolean onPage;
            CustomResources.CustomSign sign = SignCache.getSign((String)this.allSignIds.get(i));
            boolean isBig = sign != null && sign.hasCustomText();
            this.buttonsSelection[i].visible = onPage = (isBig ? indexBig : indexSmall) / pageCount == this.page;
            if (ignorePage || onPage) {
                loopSignsCallback.loopSignsCallback(i, (isBig ? columnBig * 3 : columnSmall) * 16, (isBig ? rowBig : rowSmall) * 16, isBig);
            }
            if (isBig) {
                ++columnBig;
                if (totalPagesBigCount < 0) {
                    totalPagesBigCount = -totalPagesBigCount + 1;
                }
                if (columnBig >= this.columns) {
                    columnBig = 0;
                    if (++rowBig >= this.rows) {
                        rowBig = 0;
                        totalPagesBigCount = -totalPagesBigCount;
                    }
                }
                ++indexBig;
                continue;
            }
            ++columnSmall;
            if (totalPagesSmallCount < 0) {
                totalPagesSmallCount = -totalPagesSmallCount + 1;
            }
            if (columnSmall >= this.columns) {
                columnSmall = 0;
                if (++rowSmall >= this.rows) {
                    rowSmall = 0;
                    totalPagesSmallCount = -totalPagesSmallCount;
                }
            }
            ++indexSmall;
        }
        return Math.max(Math.abs(totalPagesBigCount), Math.abs(totalPagesSmallCount));
    }

    private void edit(int editingIndex) {
        this.editingIndex = editingIndex;
        Button[] var2 = this.buttonsEdit;
        int var3 = var2.length;
        for (Button button : var2) {
            button.active = true;
        }
        this.buttonClear.visible = true;
        this.setPage(this.page);
        this.buttonsEdit[editingIndex].active = false;
    }

    private void setNewSignId(String newSignId) {
        if (this.editingIndex >= 0 && this.editingIndex < this.signIds.length) {
            boolean isStation;
            this.signIds[this.editingIndex] = newSignId;
            boolean isExitLetter = newSignId != null && (newSignId.equals(BlockRailwaySign.SignType.EXIT_LETTER.toString()) || newSignId.equals(BlockRailwaySign.SignType.EXIT_LETTER_FLIPPED.toString()));
            boolean isPlatform = newSignId != null && (newSignId.equals(BlockRailwaySign.SignType.PLATFORM.toString()) || newSignId.equals(BlockRailwaySign.SignType.PLATFORM_FLIPPED.toString()));
            boolean isLine = newSignId != null && (newSignId.equals(BlockRailwaySign.SignType.LINE.toString()) || newSignId.equals(BlockRailwaySign.SignType.LINE_FLIPPED.toString()));
            boolean bl = isStation = newSignId != null && (newSignId.equals(BlockRailwaySign.SignType.STATION.toString()) || newSignId.equals(BlockRailwaySign.SignType.STATION_FLIPPED.toString()));
            if ((isExitLetter || isPlatform || isLine || isStation) && this.minecraft != null) {
                UtilitiesClient.setScreen((Minecraft)this.minecraft, (ScreenMapper)new DashboardListSelectorScreen((ScreenMapper)this, isExitLetter ? this.exitsForList : (isPlatform ? this.platformsForList : (isLine ? this.routesForList : this.stationsForList)), this.selectedIds, false, false));
            }
        }
    }

    private void setPage(int newPage) {
        // 【1.1.87 保底 totalPages>=1】
        if (this.totalPages < 1) {
            this.totalPages = 1;
        }
        this.page = Mth.clamp((int)newPage, (int)0, (int)Math.max(this.totalPages - 1, 0));
        this.buttonPrevPage.visible = this.editingIndex >= 0 && this.page > 0;
        this.buttonNextPage.visible = this.editingIndex >= 0 && this.page < this.totalPages - 1;
    }

    @FunctionalInterface
    private static interface LoopSignsCallback {
        public void loopSignsCallback(int var1, int var2, int var3, boolean var4);
    }
}

