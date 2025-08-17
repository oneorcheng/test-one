package com.example.conveyorconnector;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class ConveyorConnectorItem extends BlockItem {
    
    public ConveyorConnectorItem(Block block) {
        super(block, new Item.Properties());
    }
    
    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        
        // 检查是否可以放置
        if (!canPlace(context)) {
            return false;
        }
        
        // 放置方块
        boolean success = level.setBlock(pos, state, 3);
        
        if (success) {
            // 更新连接状态
            updateConnections(level, pos, state);
        }
        
        return success;
    }
    
    /**
     * 检查是否可以放置传送带连接器
     */
    private boolean canPlace(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        
        // 检查基础放置条件
        if (!level.getBlockState(pos).canBeReplaced(context)) {
            return false;
        }
        
        // 检查是否有足够的空间
        if (!hasEnoughSpace(level, pos)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查是否有足够的空间放置传送带
     */
    private boolean hasEnoughSpace(Level level, BlockPos pos) {
        // 检查水平方向是否有20格空间
        for (int i = -10; i <= 10; i++) {
            BlockPos checkPos = pos.offset(i, 0, 0);
            if (!level.getBlockState(checkPos).isAir() && 
                !isConveyorBelt(level.getBlockState(checkPos))) {
                return false;
            }
        }
        
        // 检查斜45度方向是否有15格空间
        for (int i = -7; i <= 7; i++) {
            BlockPos checkPos = pos.offset(i, i, 0);
            if (!level.getBlockState(checkPos).isAir() && 
                !isConveyorBelt(level.getBlockState(checkPos))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 更新传送带连接状态
     */
    private void updateConnections(Level level, BlockPos pos, BlockState state) {
        // 更新周围传送带的连接状态
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                BlockState checkState = level.getBlockState(checkPos);
                
                if (isConveyorBelt(checkState)) {
                    // 通知传送带更新连接状态
                    level.updateNeighborsAt(checkPos, checkState.getBlock());
                }
            }
        }
    }
    
    /**
     * 检测方块是否为传送带
     */
    private boolean isConveyorBelt(BlockState state) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return blockId.contains("create") && blockId.contains("belt");
    }
}