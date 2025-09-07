package dev.doctor4t.trainmurdermystery.item;

import dev.doctor4t.trainmurdermystery.block.SmallDoorBlock;
import dev.doctor4t.trainmurdermystery.block_entity.SmallDoorBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RoomKeyItem extends Item {
    public RoomKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof SmallDoorBlock) {
            BlockPos lowerPos = state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
            if (world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity entity) {
                // Sneaking creative player with key sets the door to require a key with the same name
                ItemStack mainHandStack = player.getMainHandStack();
                if (player.isCreative() && player.isSneaking()) {
                    System.out.println(mainHandStack.getName().getString());
                    entity.setKeyName(mainHandStack.getName().getString());
                    return ActionResult.SUCCESS;
                } else {
                    if (mainHandStack.getName().getString().equals(entity.getKeyName())) {
                        SmallDoorBlock.toggleDoor(state, world, entity, lowerPos);
                        return ActionResult.SUCCESS;
                    } else if (world.isClient) {
                        player.sendMessage(Text.translatable("tip.door.requires_different_key"), true);
                        return ActionResult.FAIL;
                    }
                }
            }

            return ActionResult.PASS;
        }

        return super.useOnBlock(context);
    }


}
