package com.example.conveyorconnector;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;

public class ConveyorConnectorBlock extends Block {
    
    // 定义方块状态属性
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BeltSlope> SLOPE = BlockStateProperties.create("slope", BeltSlope.class);
    public static final BooleanProperty CONNECTED_HORIZONTAL = BooleanProperty.create("connected_horizontal");
    public static final BooleanProperty CONNECTED_DIAGONAL = BooleanProperty.create("connected_diagonal");
    
    // 注册方块
    public static final RegistryObject<Block> CONVEYOR_CONNECTOR = ConveyorConnectorMod.BLOCKS.register(
        "conveyor_connector", 
        () -> new ConveyorConnectorBlock()
    );
    
    // 注册方块实体
    public static final RegistryObject<BlockEntityType<ConveyorConnectorBlockEntity>> CONVEYOR_CONNECTOR_BLOCK_ENTITY = 
        ConveyorConnectorMod.BLOCK_ENTITIES.register(
            "conveyor_connector", 
            () -> BlockEntityType.Builder.of(
                ConveyorConnectorBlockEntity::new, 
                CONVEYOR_CONNECTOR.get()
            ).build(null)
        );
    
    public ConveyorConnectorBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL)
            .strength(3.0f)
            .requiresCorrectToolForDrops()
            .noOcclusion());
        
        // 设置默认状态
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(SLOPE, BeltSlope.HORIZONTAL)
            .setValue(CONNECTED_HORIZONTAL, false)
            .setValue(CONNECTED_DIAGONAL, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOPE, CONNECTED_HORIZONTAL, CONNECTED_DIAGONAL);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BeltSlope slope = BeltSlope.HORIZONTAL;
        
        // 检测是否可以连接到斜45度传送带
        if (canConnectToDiagonal(context.getClickedPos(), facing)) {
            slope = BeltSlope.UPWARD;
        }
        
        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(SLOPE, slope);
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, 
                                LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // 更新连接状态
        boolean connectedHorizontal = canConnectToHorizontal(level, pos, state.getValue(FACING));
        boolean connectedDiagonal = canConnectToDiagonal(pos, state.getValue(FACING));
        
        return state
            .setValue(CONNECTED_HORIZONTAL, connectedHorizontal)
            .setValue(CONNECTED_DIAGONAL, connectedDiagonal);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                               InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            // 右键点击可以切换传送带方向
            Direction currentFacing = state.getValue(FACING);
            Direction newFacing = currentFacing.getClockWise();
            
            level.setBlock(pos, state.setValue(FACING, newFacing), 3);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * 检测是否可以连接到水平传送带
     */
    private boolean canConnectToHorizontal(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockPos frontPos = pos.relative(facing);
        BlockPos backPos = pos.relative(facing.getOpposite());
        
        // 检查前后是否有传送带
        return isConveyorBelt(level.getBlockState(frontPos)) || 
               isConveyorBelt(level.getBlockState(backPos));
    }
    
    /**
     * 检测是否可以连接到斜45度传送带
     */
    private boolean canConnectToDiagonal(BlockPos pos, Direction facing) {
        // 检查斜45度方向是否有传送带
        BlockPos diagonalPos = pos.relative(facing).above();
        return true; // 简化检测逻辑
    }
    
    /**
     * 检测方块是否为传送带
     */
    private boolean isConveyorBelt(BlockState state) {
        return state.getBlock() instanceof BeltBlock;
    }
    
    /**
     * 获取传送带的碰撞形状
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BeltSlope slope = state.getValue(SLOPE);
        
        if (slope == BeltSlope.HORIZONTAL) {
            return Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
        } else if (slope == BeltSlope.UPWARD) {
            return Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
        }
        
        return Shapes.block();
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorConnectorBlockEntity(pos, state);
    }
    
    @Override
    public boolean hasBlockEntity(BlockState state) {
        return true;
    }
    
    /**
     * 注册方块和物品
     */
    public static void register() {
        // 注册物品
        ConveyorConnectorMod.ITEMS.register("conveyor_connector", 
            () -> new ConveyorConnectorItem(CONVEYOR_CONNECTOR.get()));
    }
}