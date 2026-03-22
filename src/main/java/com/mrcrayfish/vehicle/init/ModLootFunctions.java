package com.mrcrayfish.vehicle.init;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.world.storage.loot.functions.CopyFluidTanks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Author: MrCrayfish
 */
public class ModLootFunctions
{
    public static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTION_TYPES =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Reference.MOD_ID);

    public static final RegistryObject<LootItemFunctionType> COPY_FLUID_TANKS =
            LOOT_FUNCTION_TYPES.register("copy_fluid_tanks",
                    () -> new LootItemFunctionType(new CopyFluidTanks.Serializer()));

    public static void register(IEventBus eventBus)
    {
        LOOT_FUNCTION_TYPES.register(eventBus);
    }

    // Keep this so existing code calling ModLootFunctions.init() still compiles
    public static void init()
    {
    }
}