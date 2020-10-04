package io.github.franiscoder.mca.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.franiscoder.mca.entity.MCAVillagerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.VillagerProfession;

import java.util.Set;
import java.util.stream.Collectors;

public class GatherItemsVillagerTask extends Task<MCAVillagerEntity> {
    private Set<Item> items = ImmutableSet.of();

    public GatherItemsVillagerTask() {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_PRESENT, MemoryModuleType.VISIBLE_MOBS, MemoryModuleState.VALUE_PRESENT));
    }

    private static Set<Item> getGatherableItems(MCAVillagerEntity villagerEntity, MCAVillagerEntity villagerEntity2) {
        ImmutableSet<Item> immutableSet = villagerEntity2.getVillagerData().getProfession().getGatherableItems();
        ImmutableSet<Item> immutableSet2 = villagerEntity.getVillagerData().getProfession().getGatherableItems();
        return immutableSet.stream().filter((item) -> !immutableSet2.contains(item)).collect(Collectors.toSet());
    }

    private static void giveHalfOfStack(MCAVillagerEntity villager, Set<Item> validItems, LivingEntity target) {
        SimpleInventory simpleInventory = villager.getInventory();
        ItemStack itemStack = ItemStack.EMPTY;
        int i = 0;
        int size = simpleInventory.size();
        while (i < size) {
            ItemStack itemStack2;
            Item item;
            int k;
            label28:
            {
                itemStack2 = simpleInventory.getStack(i);
                if (!itemStack2.isEmpty()) {
                    item = itemStack2.getItem();
                    if (validItems.contains(item)) {
                        if (itemStack2.getCount() > itemStack2.getMaxCount() / 2) {
                            k = itemStack2.getCount() / 2;
                            break label28;
                        }

                        if (itemStack2.getCount() > 24) {
                            k = itemStack2.getCount() - 24;
                            break label28;
                        }
                    }
                }

                ++i;
                continue;
            }

            itemStack2.decrement(k);
            itemStack = new ItemStack(item, k);
            break;
        }

        if (!itemStack.isEmpty()) {
            LookTargetUtil.give(villager, itemStack, target.getPos());
        }

    }

    protected boolean shouldRun(ServerWorld serverWorld, MCAVillagerEntity villagerEntity) {
        return LookTargetUtil.canSee(villagerEntity.getBrain(), MemoryModuleType.INTERACTION_TARGET, EntityType.VILLAGER);
    }

    protected boolean shouldKeepRunning(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, long l) {
        return this.shouldRun(serverWorld, villagerEntity);
    }

    protected void run(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, long l) {
        MCAVillagerEntity villagerEntity2 = (MCAVillagerEntity) villagerEntity.getBrain().getOptionalMemory(MemoryModuleType.INTERACTION_TARGET).get();
        LookTargetUtil.lookAtAndWalkTowardsEachOther(villagerEntity, villagerEntity2, 0.5F);
        this.items = getGatherableItems(villagerEntity, villagerEntity2);
    }

    protected void keepRunning(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, long l) {
        MCAVillagerEntity villagerEntity2 = (MCAVillagerEntity) villagerEntity.getBrain().getOptionalMemory(MemoryModuleType.INTERACTION_TARGET).get();
        if (villagerEntity.squaredDistanceTo(villagerEntity2) <= 5.0D) {
            LookTargetUtil.lookAtAndWalkTowardsEachOther(villagerEntity, villagerEntity2, 0.5F);
            villagerEntity.talkWithVillager(serverWorld, villagerEntity2, l);
            if (villagerEntity.wantsToStartBreeding() && (villagerEntity.getVillagerData().getProfession() == VillagerProfession.FARMER || villagerEntity2.canBreed())) {
                giveHalfOfStack(villagerEntity, VillagerEntity.ITEM_FOOD_VALUES.keySet(), villagerEntity2);
            }

            if (villagerEntity2.getVillagerData().getProfession() == VillagerProfession.FARMER && villagerEntity.getInventory().count(Items.WHEAT) > Items.WHEAT.getMaxCount() / 2) {
                giveHalfOfStack(villagerEntity, ImmutableSet.of(Items.WHEAT), villagerEntity2);
            }

            if (!this.items.isEmpty() && villagerEntity.getInventory().containsAny(this.items)) {
                giveHalfOfStack(villagerEntity, this.items, villagerEntity2);
            }

        }
    }

    protected void finishRunning(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, long l) {
        villagerEntity.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
    }
}