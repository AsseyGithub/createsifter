package com.oierbravo.createsifter.content.contraptions.components.sifter;

import com.oierbravo.createsifter.content.contraptions.components.meshes.BaseMesh;
import com.oierbravo.createsifter.register.*;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.base.KineticBlock;
import com.simibubi.create.content.contraptions.relays.elementary.ICogWheel;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public class SifterBlock  extends KineticBlock implements ITE<SifterTileEntity>, ICogWheel {
    public SifterBlock(Properties properties) {
        super(properties);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return ModShapes.SIFTER;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        SifterTileEntity sifterTileEntity = (SifterTileEntity) worldIn.getBlockEntity(pos);
        ItemStack handInStack = player.getItemInHand(handIn);

        if (worldIn.isClientSide)
            return InteractionResult.SUCCESS;
        //if(handInStack.is())
        if(handInStack.getItem() instanceof BaseMesh){
        //if(handInStack.is(ModTags.ModItemTags.MESHES.tag)){
            sifterTileEntity.insertMesh(handInStack, player);
      //  }
      //  if(handInStack.sameItem(new ItemStack(ModItems.ANDESITE_MESH.get(),1))){
        //if(handInStack.sameItem(new ItemStack(ModItems.ANDESITE_MESH.get(),1))){

        }
        if(handInStack.isEmpty() && sifterTileEntity.hasMesh() && player.isShiftKeyDown()){
            sifterTileEntity.removeMesh(player);
        }
        if (!handInStack.isEmpty())
            return InteractionResult.PASS;
       //if(player.getItemInHand(handIn).isEmpty() &&){

        //}

        withTileEntityDo(worldIn, pos, sifter -> {
            boolean emptyOutput = true;
            IItemHandlerModifiable inv = sifter.outputInv;
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                ItemStack stackInSlot = inv.getStackInSlot(slot);
                if (!stackInSlot.isEmpty())
                    emptyOutput = false;
                player.getInventory()
                        .placeItemBackInInventory(stackInSlot);
                inv.setStackInSlot(slot, ItemStack.EMPTY);
            }

            if (emptyOutput) {
                inv = sifter.inputInv;
                for (int slot = 0; slot < inv.getSlots(); slot++) {
                    player.getInventory()
                            .placeItemBackInInventory(inv.getStackInSlot(slot));
                    inv.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }

            sifter.setChanged();
            sifter.sendData();
        });

        return InteractionResult.SUCCESS;
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
        super.updateEntityAfterFallOn(worldIn, entityIn);

        if (entityIn.level.isClientSide)
            return;
        if (!(entityIn instanceof ItemEntity))
            return;
        if (!entityIn.isAlive())
            return;

        SifterTileEntity sifter = null;
        for (BlockPos pos : Iterate.hereAndBelow(entityIn.blockPosition()))
            if (sifter == null)
                sifter = getTileEntity(worldIn, pos);

        if (sifter == null)
            return;

        ItemEntity itemEntity = (ItemEntity) entityIn;
        LazyOptional<IItemHandler> capability = sifter.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        if (!capability.isPresent())
            return;

        ItemStack remainder = capability.orElse(new ItemStackHandler())
                .insertItem(0, itemEntity.getItem(), false);
        if (remainder.isEmpty())
            itemEntity.discard();
        if (remainder.getCount() < itemEntity.getItem()
                .getCount())
            itemEntity.setItem(remainder);
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
            withTileEntityDo(worldIn, pos, te -> {
                ItemHelper.dropContents(worldIn, pos, te.inputInv);
                ItemHelper.dropContents(worldIn, pos, te.meshInv);
                ItemHelper.dropContents(worldIn, pos, te.outputInv);
            });

            worldIn.removeBlockEntity(pos);
        }
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public Class<SifterTileEntity> getTileEntityClass() {
        return SifterTileEntity.class;
    }

    @Override
    public BlockEntityType<? extends SifterTileEntity> getTileEntityType() {
        return ModTiles.SIFTER.get();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
        return false;
    }
}
