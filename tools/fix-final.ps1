# 终版精修: Vineflower 错误类名归位 + 最后一批方法映射 (已验证)
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

$files = Get-ChildItem "$proj\work\decomp-fabric" -Recurse -Filter *.java | Where-Object { $_.FullName -notmatch 'architectury_inject|mappings' }

# Vineflower 编号假类名 -> 真类名
$classNameFix = @{
  'MapItemSavedData51' = 'BlockBehaviour'
  'StateSwitchingButton4' = 'Blocks'
  'AbstractContainerEventHandler0' = 'SoundType'
  'AmphibiousNodeEvaluator6' = 'PathNavigation'
  'CopyNameFunction1' = 'ArrayNode'
  'ObjectiveCriteria6' = 'ObjectiveCriteria'
  'ConditionUserBuilder1' = 'RenderType'
  'MapItemSavedData48' = 'MapItemSavedData'
  'MapBanner92' = 'MapItemSavedData'
}

# member 终表 (经 tiny+194 client.txt 逐个验证)
$memberFix = @{
  'method_9632' = 'destroyTime'        # BlockBehaviour$Properties.destroyTime(float)
  'method_11657' = 'setValue'           # StateHolder.setValue
  'method_26204' = 'getBlock'           # BlockBehaviour$BlockStateBase.getBlock()
  'method_9564' = 'defaultBlockState'   # BlockBehaviour.defaultBlockState()
  'method_9530' = 'getShape'            # dbp.getShape(BlockState,BlockGetter,BlockPos,CollisionContext)->VoxelShape
  'method_9571' = 'getOcclusionShape'   # dbp f (BlockState,BlockGetter,BlockPos)->VoxelShape
  'method_9605' = 'rotate'              # Block.rotate(BlockState,Rotation)
  'method_9534' = 'use'                 # dbp.use(BlockState,Level,BlockPos,Player,Hand,HitResult)->InteractionResult
  'method_8389' = 'asItem'             # dbp k ()->Item  (mtr 用途: asItem)
  'method_9631' = 'lightEmission'       # Properties.lightEmission(ToIntFunction)
  'method_10862' = 'withStyle'          # MutableComponent.withStyle(Style)
  'method_10123' = 'getCloneItemStack'  # EntityBlock.getCloneItemStack -> BlockEntity? cru a (gt,dbq)->cze: newBlockEntity
  'method_9549' = 'getCollisionShape'   # dbp c (BlockState,BlockGetter,BlockPos,CollisionContext)
  'method_9559' = 'updateShape'         # dbp a updateShape(BlockState,Direction,BlockState,LevelAccessor,BlockPos,BlockPos)
  'method_22488' = 'instab'             # Properties.instab()
  'method_15800' = 'isLiquid'           # Material.isLiquid()
  'method_7884' = 'use'                 # Item.use -> InteractionResultHolder? cfq a (cie=LBlockPlaceContext?)->bdy
  'method_26207' = 'getMaterial'        # BlockStateBase.getMaterial()
  'method_29292' = 'noLootTable'        # Properties.noLootTable()
  'method_15434' = 'getSerializedName'  # StringRepresentable.getSerializedName()
  'method_26245' = 'hasPostProcess'     # Properties.hasPostProcess(StatePredicate)
  'method_26243' = 'isViewBlocking'     # Properties.isViewBlocking(StatePredicate)
  'method_26235' = 'isValidSpawn'       # Properties.isValidSpawn(StateArgumentPredicate)
  'method_26236' = 'isRedstoneConductor' # Properties.isRedstoneConductor(StatePredicate)
  'method_25290' = 'drawCenteredString' # GuiComponent.drawCenteredString
  'method_14486' = 'getResource'        # ResourceProvider.getResource
  'method_14488' = 'listResources'      # ResourceManager.listResources
  'method_9574' = 'getCloneItemStack'   # Block.getCloneItemStack(BlockGetter,BlockPos,BlockState)->ItemStack
  'method_8316' = 'getFluidState'       # BlockGetter.getFluidState(BlockPos)
  'method_23761' = 'last'               # PoseStack$Pose.last()->Matrix4f
  'method_1478' = 'getResourceManager'   # Minecraft.getResourceManager()
  'method_27534' = 'blit'               # GuiComponent.blit(PoseStack,...)
  'method_9527' = 'getPushReaction'     # dbp d getPushReaction(BlockState)
  'method_9539' = 'getName'            # Block.getName()->String
  'method_10166' = 'getAxis'           # Direction.getAxis()
  'method_1531' = 'getTextureManager'  # Minecraft.getTextureManager()
  'method_26159' = 'getVisualShape'    # dbp b getVisualShape
  'method_30937' = 'getVisualOrder'    # Component.getVisualOrder()->FormattedCharSequence
  'method_10079' = 'relative'           # BlockPos.relative(Direction,int)
  'method_10160' = 'getOpposite'       # Direction.getOpposite
  'method_10170' = 'getClockWise'      # Direction.getClockWise
  'method_10093' = 'above'              # BlockPos.above()
  'method_10074' = 'below'              # BlockPos.below()
  'method_10086' = 'above'              # BlockPos.above(int)
  'method_10067' = 'south'             # BlockPos.south()
  'method_10072' = 'below'             # BlockPos.below(int)
  'method_10078' = 'east'              # BlockPos.east()
  'method_10084' = 'above'              # BlockPos.above()
  'method_10095' = 'north'             # BlockPos.north()
  'method_11016' = 'getBlockPos'        # BlockEntity.getBlockPos()
  'method_10997' = 'getLevel'           # BlockEntity.getLevel()
  'method_11654' = 'cycle'              # StateHolder.cycle(Property)
  'method_11667' = 'add'                # StateDefinition$Builder.add(Property[])
  'method_10558' = 'getLongArray'       # CompoundTag.getLongArray
  'method_10565' = 'getLongArray'       # 复数场景同一方法
}

$n = 0
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    $o = $c
    foreach ($e in $classNameFix.GetEnumerator()) {
        $c = [regex]::Replace($c, ('\b' + [regex]::Escape($e.Key) + '\b'), $e.Value)
    }
    foreach ($e in $memberFix.GetEnumerator()) {
        $c = [regex]::Replace($c, ('\b' + [regex]::Escape($e.Key) + '\b'), $e.Value)
    }
    if ($c -ne $o) { [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false))); $n++ }
}
Write-Host "修复 $n 文件"

# 残留统计
$pat = '(class_\d+|method_\d+|field_\d+)'
$counts = @{}
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    foreach ($m in [regex]::Matches($c, $pat)) { $k = $m.Value; if ($counts.ContainsKey($k)) { $counts[$k]++ } else { $counts[$k] = 1 } }
}
Write-Host ("残留: {0}" -f $counts.Count)
$counts.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object { "{0} x{1}" -f $_.Key, $_.Value }
