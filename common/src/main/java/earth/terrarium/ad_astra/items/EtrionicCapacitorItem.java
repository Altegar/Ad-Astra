package earth.terrarium.ad_astra.items;

import earth.terrarium.ad_astra.AdAstra;
import earth.terrarium.botarium.api.energy.*;
import earth.terrarium.botarium.api.item.ItemStackHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class EtrionicCapacitorItem extends Item implements EnergyItem {
    public static final String TOGGLE_KEY = "ToggledOn";

    public EtrionicCapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        long energy = getEnergyStorage(stack).getStoredEnergy();
        tooltip.add(Component.translatable("gauge_text.ad_astra.storage", energy, AdAstra.CONFIG.capacitorConfig.maxEnergy).setStyle(Style.EMPTY.withColor(energy > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(usedHand == InteractionHand.MAIN_HAND) {
            ItemStack stack = player.getItemInHand(usedHand);
            if(player.isShiftKeyDown()) {
                DistributionMode mode = DistributionMode.switchMode(stack);
                player.displayClientMessage(Component.translatable("item.ad_astra.etrionic_capacitor.mode_change", mode.getComponent()), true);
                return InteractionResultHolder.success(stack);
            } else {
                boolean toggled = toggle(stack);
                player.displayClientMessage(Component.translatable("item.ad_astra.etrionic_capacitor." + (toggled ? "on" : "off")), true);
            }
        }
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
    }

    @Override
    public StatefulEnergyContainer<ItemStack> getEnergyStorage(ItemStack object) {
        return new ItemEnergyContainer(object, AdAstra.CONFIG.capacitorConfig.maxEnergy);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        long transferRate = AdAstra.CONFIG.capacitorConfig.transferRate;
        ItemStackHolder from = new ItemStackHolder(stack);
        if(getEnergyStorage(stack).getStoredEnergy() > transferRate && isToggled(stack) && entity instanceof Player player) {
            DistributionMode mode = DistributionMode.getMode(stack);
            if(mode == DistributionMode.ROUND_ROBIN) {
                Map<SlottedItem, Long> containers = new HashMap<>();
                AtomicLong total = new AtomicLong();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    int finalI = i;
                    EnergyHooks.safeGetItemEnergyManager(item).ifPresent((ignored) -> {
                        long movable = ignored.insert(new ItemStackHolder(item.copy()), transferRate, true);
                        if(movable > 0) {
                            containers.put(new SlottedItem(item, finalI), movable);
                            total.addAndGet(movable);
                        }
                    });
                }
                for (var container : containers.entrySet()) {
                    ItemStackHolder to = new ItemStackHolder(container.getKey().stack());
                    if(total.get() <= transferRate) {
                        EnergyHooks.safeMoveItemToItemEnergy(from, to, transferRate);
                    } else {
                        EnergyHooks.safeMoveItemToItemEnergy(from, to, transferRate * (container.getValue() / total.get()));
                    }
                    if(to.isDirty()) player.getInventory().setItem(container.getKey().slot(), to.getStack());
                    if(from.isDirty()) player.getInventory().setItem(slotId, from.getStack());
                }
            } else if(mode == DistributionMode.SEQUENTIAL) {
                long transfered = 0;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    ItemStackHolder to = new ItemStackHolder(item);
                    transfered += EnergyHooks.safeMoveItemToItemEnergy(from, to, transferRate - transfered);
                    if(transfered == transferRate) break;
                    if(to.isDirty()) player.getInventory().setItem(i, to.getStack());
                    if(from.isDirty()) player.getInventory().setItem(slotId, from.getStack());
                }
            }
        }
    }

    public static boolean toggle(ItemStack stack) {
        boolean mode = !isToggled(stack);
        stack.getOrCreateTag().putBoolean(TOGGLE_KEY, mode);
        return mode;
    }

    public static boolean isToggled(ItemStack stack) {
        if (stack.getOrCreateTag().contains(TOGGLE_KEY)) {
            return stack.getOrCreateTag().getBoolean(TOGGLE_KEY);
        } else {
            stack.getOrCreateTag().putBoolean(TOGGLE_KEY, true);
            return true;
        }
    }

    public static float itemProperty(ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int i) {
        return isToggled(itemStack) ? 0 : 1;
    }

    public enum DistributionMode {
        ROUND_ROBIN,
        SEQUENTIAL;

        public static final String KEY = "DistributionMode";
        public static final DistributionMode[] VALUES = values();

        public static DistributionMode getMode(ItemStack stack) {
            if (stack.getOrCreateTag().contains(KEY)) {
                try {
                    return DistributionMode.valueOf(stack.getOrCreateTag().getString(KEY));
                } catch (Error ignored) {}
            }
            stack.getOrCreateTag().putString(KEY, DistributionMode.SEQUENTIAL.toString());
            return DistributionMode.SEQUENTIAL;
        }

        public static DistributionMode switchMode(ItemStack stack) {
            DistributionMode mode = getMode(stack);
            DistributionMode newMode = VALUES[((mode.ordinal() + 1) % VALUES.length)];
            stack.getOrCreateTag().putString(KEY, newMode.name());
            return newMode;
        }

        public Component getComponent() {
            return Component.translatable("misc.ad_astra.distribution_mode." + toString().toLowerCase(Locale.ROOT));
        }
    }

    public record SlottedItem(ItemStack stack, int slot) {}
}
