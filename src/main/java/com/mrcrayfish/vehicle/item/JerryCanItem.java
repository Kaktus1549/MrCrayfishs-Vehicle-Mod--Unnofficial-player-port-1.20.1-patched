package com.mrcrayfish.vehicle.item;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.util.FluidUtils;
import com.mrcrayfish.vehicle.client.render.util.RenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class JerryCanItem extends Item
{
    private final DecimalFormat FUEL_FORMAT = new DecimalFormat("0.#%");

    private final Supplier<Integer> capacitySupplier;

    public JerryCanItem(Supplier<Integer> capacity, Item.Properties properties)
    {
        super(properties);
        this.capacitySupplier = capacity;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft)
    {
        CompoundTag tag = stack.getTag();
        if(tag != null)
        {
            tag.remove("UsingX");
            tag.remove("UsingY");
            tag.remove("UsingZ");
            tag.remove("UsingFace");
            tag.remove("FillFromBlock");
        }
        super.releaseUsing(stack, level, living, timeLeft);
    }


    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration)
    {
        if(level.isClientSide || !(living instanceof Player player))
            return;

        CompoundTag tag = stack.getTag();
        if(tag == null || !tag.contains("UsingX") || !tag.contains("UsingY") || !tag.contains("UsingZ") || !tag.contains("UsingFace"))
        {
            player.stopUsingItem();
            return;
        }

        BlockPos pos = new BlockPos(tag.getInt("UsingX"), tag.getInt("UsingY"), tag.getInt("UsingZ"));
        Direction face = Direction.values()[tag.getInt("UsingFace")];
        boolean fillFromBlock = tag.getBoolean("FillFromBlock");

        if(player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 25.0D)
        {
            player.stopUsingItem();
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if(blockEntity == null)
        {
            player.stopUsingItem();
            return;
        }

        LazyOptional<IFluidHandler> blockCap =
            blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, face);

        LazyOptional<IFluidHandlerItem> itemCap =
            stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);

        IFluidHandler blockHandler = blockCap.resolve().orElse(null);
        IFluidHandlerItem itemHandler = itemCap.resolve().orElse(null);

        if(blockHandler == null || itemHandler == null)
        {
            player.stopUsingItem();
            return;
        }

        int moved;
        if(fillFromBlock)
        {
            moved = FluidUtils.transferFluid(blockHandler, itemHandler, this.getFillRate());
        }
        else
        {
            moved = FluidUtils.transferFluid(itemHandler, blockHandler, this.getFillRate());
        }

        if(moved <= 0)
        {
            player.stopUsingItem();
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltips, @NotNull TooltipFlag flag)
    {
        if(Screen.hasShiftDown())
        {
            tooltips.addAll(RenderUtil.lines(Component.translatable(this.getDescriptionId() + ".info"), 150));
        }
        else if(level != null)
        {
            stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler ->
            {
                FluidStack fluidStack = handler.getFluidInTank(0);
                if(!fluidStack.isEmpty())
                {
                    tooltips.add(Component.translatable(fluidStack.getTranslationKey()).withStyle(ChatFormatting.BLUE));
                    tooltips.add(Component.literal(this.getCurrentFuel(stack) + " / " + this.capacitySupplier.get() + "mb").withStyle(ChatFormatting.GRAY));
                }
                else
                {
                    tooltips.add(Component.translatable("item.vehicle.jerry_can.empty").withStyle(ChatFormatting.RED));
                }
            });

            tooltips.add(Component.literal(ChatFormatting.YELLOW + I18n.get("vehicle.info_help")));
        }
    }

    // @Override
    // public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    // {
    //     // This is such ugly code
    //     // BlockEntity tileEntity = context.getLevel().getBlockEntity(context.getClickedPos());
    //     // if(tileEntity != null && context.getPlayer() != null)
    //     // {
    //     //     LazyOptional<IFluidHandler> lazyOptional = tileEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, context.getClickedFace());
    //     //     if(lazyOptional.isPresent())
    //     //     {
    //     //         Optional<IFluidHandler> optional = lazyOptional.resolve();
    //     //         if(optional.isPresent())
    //     //         {
    //     //             IFluidHandler source = optional.get();
    //     //             Optional<IFluidHandlerItem> itemOptional = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
    //     //             if(itemOptional.isPresent())
    //     //             {
    //     //                 if(context.getPlayer().isCrouching())
    //     //                 {
    //     //                     FluidUtils.transferFluid(source, itemOptional.get(), this.getFillRate());
    //     //                 }
    //     //                 else
    //     //                 {
    //     //                     FluidUtils.transferFluid(itemOptional.get(), source, this.getFillRate());
    //     //                 }
    //     //                 return InteractionResult.SUCCESS;
    //     //             }
    //     //         }
    //     //     }
    //     // }
    //     return super.onItemUseFirst(stack, context);
    // }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        Player player = context.getPlayer();
        if(player == null)
            return InteractionResult.PASS;

        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if(blockEntity == null)
            return InteractionResult.PASS;

        LazyOptional<IFluidHandler> blockCap =
            blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, context.getClickedFace());

        LazyOptional<IFluidHandlerItem> itemCap =
            stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);

        if(!blockCap.isPresent() || !itemCap.isPresent())
            return InteractionResult.PASS;

        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("UsingX", context.getClickedPos().getX());
        tag.putInt("UsingY", context.getClickedPos().getY());
        tag.putInt("UsingZ", context.getClickedPos().getZ());
        tag.putInt("UsingFace", context.getClickedFace().ordinal());

        // true = block -> can, false = can -> block
        tag.putBoolean("FillFromBlock", player.isCrouching());

        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    public int getCurrentFuel(ItemStack stack)
    {
        Optional<IFluidHandlerItem> optional = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        return optional.map(handler -> handler.getFluidInTank(0).getAmount()).orElse(0);
    }

    public int getCapacity()
    {
        return this.capacitySupplier.get();
    }

    public int getFillRate()
    {
        return Config.SERVER.jerryCanFillRate.get();
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack)
    {
        return this.getCurrentFuel(stack) > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack)
    {
        return (int) (1.0 - (this.getCurrentFuel(stack) / (double) this.capacitySupplier.get()));
    }

    @Override
    public int getBarColor(ItemStack stack)
    {
        Optional<IFluidHandlerItem> optional = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        return optional.map(handler -> {
            IClientFluidTypeExtensions.of(handler.getFluidInTank(0).getFluid()).getTintColor();
            int color = IClientFluidTypeExtensions.of(handler.getFluidInTank(0).getFluid()).getTintColor();
            if(color == 0xFFFFFFFF) color = FluidUtils.getAverageFluidColor(handler.getFluidInTank(0).getFluid());
            return color;
        }).orElse(0);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt)
    {
        return new FluidHandlerItemStack(stack, this.capacitySupplier.get());
    }
}
