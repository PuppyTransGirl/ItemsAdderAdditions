package toutouchien.itemsadderadditions.nms;

import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.*;
import toutouchien.itemsadderadditions.nms.api.component.INmsItemComponentHandler;

@NullMarked
public final class NmsHandler_v26_3 implements INmsHandler {
    private final NmsBedHandler_v26_3 bed = new NmsBedHandler_v26_3();
    private final NmsBiomeHandler_v26_3 biome = new NmsBiomeHandler_v26_3();
    private final NmsCampfireRecipeHandler_v26_3 campfireRecipes = new NmsCampfireRecipeHandler_v26_3();
    private final NmsCraftingRecipeHandler_v26_3 craftingRecipes = new NmsCraftingRecipeHandler_v26_3();
    private final NmsCreativeMenuHandler_v26_3 creativeMenu = new NmsCreativeMenuHandler_v26_3();
    private final NmsPaintingHandler_v26_3 paintings = new NmsPaintingHandler_v26_3();
    private final NmsStonecutterRecipeHandler_v26_3 stonecutterRecipes = new NmsStonecutterRecipeHandler_v26_3();
    private final NmsToastHandler_v26_3 toasts = new NmsToastHandler_v26_3();
    private final NmsTextDisplayHandler_v26_3 textDisplays = new NmsTextDisplayHandler_v26_3();
    private final NmsAdvancementHandler_v26_3 advancements = new NmsAdvancementHandler_v26_3();
    private final NmsItemComponentHandler_v26_3 itemComponents = new NmsItemComponentHandler_v26_3();

    @Override
    public INmsBedHandler bed() {
        return bed;
    }

    @Override
    public INmsBiomeHandler biome() {
        return biome;
    }

    @Override
    public INmsCampfireRecipeHandler campfireRecipes() {
        return campfireRecipes;
    }

    @Override
    public INmsCraftingRecipeHandler craftingRecipes() {
        return craftingRecipes;
    }

    @Override
    public INmsCreativeMenuHandler creativeMenu() {
        return creativeMenu;
    }


    @Override
    public INmsPaintingHandler paintings() {
        return paintings;
    }

    @Override
    public INmsStonecutterRecipeHandler stonecutterRecipes() {
        return stonecutterRecipes;
    }

    @Override
    public INmsToastHandler toasts() {
        return toasts;
    }


    @Override
    public INmsTextDisplayHandler textDisplays() {
        return textDisplays;
    }

    @Override
    public INmsAdvancementHandler advancements() {
        return advancements;
    }

    @Override
    public INmsItemComponentHandler itemComponents() {
        return itemComponents;
    }

    @Override
    public void finalizeRecipes() {
        MinecraftServer.getServer().getRecipeManager().finalizeRecipeLoading();
    }

    @Override
    public boolean matchesEntityNbt(Entity entity, String snbt) {
        try {
            CompoundTag expected = TagParser.parseCompoundFully(snbt);
            net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
            TagValueOutput output = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING,
                    handle.level().registryAccess()
            );
            handle.saveWithoutId(output);
            return containsCompound(output.buildResult(), expected);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean containsCompound(CompoundTag actual, CompoundTag expected) {
        for (String key : expected.keySet()) {
            net.minecraft.nbt.Tag expectedValue = expected.get(key);
            net.minecraft.nbt.Tag actualValue = actual.get(key);
            if (expectedValue instanceof CompoundTag expectedCompound) {
                if (!(actualValue instanceof CompoundTag actualCompound)
                        || !containsCompound(actualCompound, expectedCompound)) return false;
            } else if (expectedValue instanceof net.minecraft.nbt.ListTag expectedList) {
                if (!(actualValue instanceof net.minecraft.nbt.ListTag actualList)
                        || !containsList(actualList, expectedList)) return false;
            } else if (!expectedValue.equals(actualValue)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsList(net.minecraft.nbt.ListTag actual, net.minecraft.nbt.ListTag expected) {
        if (expected.size() > actual.size()) return false;
        for (int index = 0; index < expected.size(); index++) {
            net.minecraft.nbt.Tag expectedItem = expected.get(index);
            net.minecraft.nbt.Tag actualItem = actual.get(index);
            if (expectedItem instanceof CompoundTag expectedCompound) {
                if (!(actualItem instanceof CompoundTag actualCompound)
                        || !containsCompound(actualCompound, expectedCompound)) return false;
            } else if (!expectedItem.equals(actualItem)) {
                return false;
            }
        }
        return true;
    }
}
