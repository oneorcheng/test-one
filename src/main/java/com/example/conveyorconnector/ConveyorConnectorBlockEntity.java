package com.example.conveyorconnector;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.energy.EnergyStorage;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.BeltPart;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConveyorConnectorBlockEntity extends BlockEntity {
    
    // 物品处理器
    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    // 能量存储
    private final EnergyStorage energyStorage = new EnergyStorage(1000, 100, 100);
    
    // 连接信息
    private List<BlockPos> connectedBelts = new ArrayList<>();
    private boolean isHorizontalConnected = false;
    private boolean isDiagonalConnected = false;
    
    public ConveyorConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ConveyorConnectorMod.CONVEYOR_CONNECTOR_BLOCK_ENTITY.get(), pos, state);
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        updateConnections();
    }
    
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null && !level.isClientSide) {
            updateConnections();
        }
    }
    
    /**
     * 更新传送带连接信息
     */
    public void updateConnections() {
        if (level == null || level.isClientSide) return;
        
        connectedBelts.clear();
        isHorizontalConnected = false;
        isDiagonalConnected = false;
        
        BlockState state = getBlockState();
        Direction facing = state.getValue(ConveyorConnectorBlock.FACING);
        BeltSlope slope = state.getValue(ConveyorConnectorBlock.SLOPE);
        
        // 检测水平连接
        checkHorizontalConnections(facing);
        
        // 检测斜45度连接
        if (slope == BeltSlope.UPWARD) {
            checkDiagonalConnections(facing);
        }
    }
    
    /**
     * 检测水平传送带连接
     */
    private void checkHorizontalConnections(Direction facing) {
        for (int distance = 1; distance <= 20; distance++) {
            BlockPos checkPos = worldPosition.relative(facing, distance);
            BlockState checkState = level.getBlockState(checkPos);
            
            if (isConveyorBelt(checkState)) {
                connectedBelts.add(checkPos);
                isHorizontalConnected = true;
            } else if (!checkState.isAir()) {
                break; // 遇到障碍物，停止检测
            }
        }
        
        // 检测反方向
        for (int distance = 1; distance <= 20; distance++) {
            BlockPos checkPos = worldPosition.relative(facing.getOpposite(), distance);
            BlockState checkState = level.getBlockState(checkPos);
            
            if (isConveyorBelt(checkState)) {
                connectedBelts.add(checkPos);
                isHorizontalConnected = true;
            } else if (!checkState.isAir()) {
                break;
            }
        }
    }
    
    /**
     * 检测斜45度传送带连接
     */
    private void checkDiagonalConnections(Direction facing) {
        for (int distance = 1; distance <= 15; distance++) {
            BlockPos checkPos = worldPosition.relative(facing, distance).above(distance);
            BlockState checkState = level.getBlockState(checkPos);
            
            if (isConveyorBelt(checkState)) {
                connectedBelts.add(checkPos);
                isDiagonalConnected = true;
            } else if (!checkState.isAir()) {
                break;
            }
        }
        
        // 检测反方向
        for (int distance = 1; distance <= 15; distance++) {
            BlockPos checkPos = worldPosition.relative(facing.getOpposite(), distance).below(distance);
            BlockState checkState = level.getBlockState(checkPos);
            
            if (isConveyorBelt(checkState)) {
                connectedBelts.add(checkPos);
                isDiagonalConnected = true;
            } else if (!checkState.isAir()) {
                break;
            }
        }
    }
    
    /**
     * 传输物品到连接的传送带
     */
    public void transferItems() {
        if (level == null || level.isClientSide) return;
        
        for (BlockPos beltPos : connectedBelts) {
            BlockEntity beltEntity = level.getBlockEntity(beltPos);
            if (beltEntity instanceof BeltBlockEntity) {
                BeltBlockEntity belt = (BeltBlockEntity) beltEntity;
                
                // 尝试传输物品
                for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    ItemStack stack = itemHandler.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        // 这里需要调用机械动力的API来传输物品
                        // 由于API限制，这里只是示例代码
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * 传递动力到连接的传送带
     */
    public void transferEnergy() {
        if (level == null || level.isClientSide) return;
        
        for (BlockPos beltPos : connectedBelts) {
            BlockEntity beltEntity = level.getBlockEntity(beltPos);
            if (beltEntity instanceof BeltBlockEntity) {
                BeltBlockEntity belt = (BeltBlockEntity) beltEntity;
                
                // 这里需要调用机械动力的API来传递动力
                // 由于API限制，这里只是示例代码
            }
        }
    }
    
    /**
     * 检测方块是否为传送带
     */
    private boolean isConveyorBelt(BlockState state) {
        if (state.getBlock() instanceof BeltBlock) {
            return true;
        }
        
        String blockId = state.getBlock().getDescriptionId();
        return blockId.contains("belt") || blockId.contains("conveyor");
    }
    
    // Capability 支持
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.of(() -> itemHandler).cast();
        }
        
        if (cap == ForgeCapabilities.ENERGY) {
            return LazyOptional.of(() -> energyStorage).cast();
        }
        
        return super.getCapability(cap, side);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
    }
    
    // 获取连接状态
    public boolean isHorizontalConnected() {
        return isHorizontalConnected;
    }
    
    public boolean isDiagonalConnected() {
        return isDiagonalConnected;
    }
    
    public List<BlockPos> getConnectedBelts() {
        return connectedBelts;
    }
}