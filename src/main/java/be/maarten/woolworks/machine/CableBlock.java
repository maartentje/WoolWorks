package be.maarten.woolworks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class CableBlock extends BaseEntityBlock {
    public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public CableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DOWN, false).setValue(UP, false).setValue(NORTH, false).setValue(SOUTH, false).setValue(WEST, false).setValue(EAST, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CableBlockEntity(pos, state); }
    private static BooleanProperty property(Direction direction) { return switch (direction) {
        case DOWN -> DOWN; case UP -> UP; case NORTH -> NORTH; case SOUTH -> SOUTH; case WEST -> WEST; case EAST -> EAST;
    }; }
    private static boolean connects(net.minecraft.world.level.LevelReader world, BlockPos pos, Direction direction) {
        BlockPos neighbor = pos.relative(direction);
        if (!world.hasChunkAt(neighbor)) return false;
        if (world.getBlockState(neighbor).is(ModMachines.CABLE.get())) return true;
        return world instanceof Level level && level.getCapability(Capabilities.Energy.BLOCK, neighbor, direction.getOpposite()) != null;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState result = defaultBlockState();
        for (Direction direction : Direction.values()) result = result.setValue(property(direction), connects(context.getLevel(), context.getClickedPos(), direction));
        return result;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) return;
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null && data.contains("ExtractionMask")) return;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor) || level.getBlockState(neighbor).is(ModMachines.CABLE.get())) continue;
            if (level.getBlockState(neighbor).is(ModMachines.GENERATOR.get())) cable.setExtracting(side, true);
        }
    }
    @Override protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos other, BlockState neighbor, net.minecraft.util.RandomSource random) {
        return state.setValue(property(direction), connects(level, pos, direction));
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPE;
        if (state.getValue(DOWN)) shape = Shapes.or(shape, Block.box(4, 0, 4, 12, 4, 12));
        if (state.getValue(UP)) shape = Shapes.or(shape, Block.box(4, 12, 4, 12, 16, 12));
        if (state.getValue(NORTH)) shape = Shapes.or(shape, Block.box(4, 4, 0, 12, 12, 4));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, Block.box(4, 4, 12, 12, 12, 16));
        if (state.getValue(WEST)) shape = Shapes.or(shape, Block.box(0, 4, 4, 4, 12, 12));
        if (state.getValue(EAST)) shape = Shapes.or(shape, Block.box(12, 4, 4, 16, 12, 12));
        return shape;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.SHEARS)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), stack)
                || serverPlayer.blockActionRestricted(level, pos, serverPlayer.gameMode.getGameModeForPlayer())) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            if (CommonHooks.fireBlockBreak(level, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer, pos, state).isCanceled()) return InteractionResult.FAIL;
            ItemStack cableItem = new ItemStack(ModMachines.CABLE_ITEM.get());
            if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) cable.preservePending(cableItem);
            if (!level.removeBlock(pos, false)) return InteractionResult.FAIL;
            if (!player.getAbilities().instabuild) ItemHandlerHelper.giveItemToPlayer(player, cableItem);
            return InteractionResult.CONSUME;
        }
        Direction side = hitSide(pos, hit);
        BlockPos neighbor = pos.relative(side);
        if (!level.hasChunkAt(neighbor) || level.getBlockState(neighbor).is(ModMachines.CABLE.get())) return InteractionResult.FAIL;
        var energy = level.getCapability(Capabilities.Energy.BLOCK, neighbor, side.getOpposite());
        // Existing worlds may contain cables placed before cables had block entities.
        var blockEntity = level.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
        if (energy == null || !(blockEntity instanceof CableBlockEntity cable)) return InteractionResult.FAIL;
        cable.setExtracting(side, !cable.isExtracting(side));
        serverPlayer.sendOverlayMessage(Component.translatable(cable.isExtracting(side) ? "message.woolworks.extraction_on" : "message.woolworks.extraction_off", side.getName()));
        return InteractionResult.CONSUME;
    }
    private static Direction hitSide(BlockPos pos, BlockHitResult hit) {
        var location = hit.getLocation();
        double x = location.x - pos.getX(), y = location.y - pos.getY(), z = location.z - pos.getZ();
        if (x < 0.25) return Direction.WEST;
        if (x > 0.75) return Direction.EAST;
        if (y < 0.25) return Direction.DOWN;
        if (y > 0.75) return Direction.UP;
        if (z < 0.25) return Direction.NORTH;
        if (z > 0.75) return Direction.SOUTH;
        return hit.getDirection();
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) CableNetworks.invalidate(level);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moving) {
        CableNetworks.invalidate(level);
        super.affectNeighborsAfterRemoval(state, level, pos, moving);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, net.minecraft.world.level.redstone.Orientation orientation, boolean moving) {
        super.neighborChanged(state, level, pos, block, orientation, moving);
        if (!level.isClientSide()) CableNetworks.invalidate(level);
    }
}
