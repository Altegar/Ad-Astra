package earth.terrarium.ad_astra.items.armour;

import earth.terrarium.ad_astra.AdAstra;
import earth.terrarium.ad_astra.client.screens.PlayerOverlayScreen;
import earth.terrarium.ad_astra.registry.ModItems;
import earth.terrarium.ad_astra.util.ModKeyBindings;
import earth.terrarium.ad_astra.util.ModResourceLocation;
import earth.terrarium.ad_astra.util.ModUtils;
import earth.terrarium.botarium.api.energy.EnergyHooks;
import earth.terrarium.botarium.api.energy.EnergyItem;
import earth.terrarium.botarium.api.energy.ItemEnergyContainer;
import earth.terrarium.botarium.api.energy.StatefulEnergyContainer;
import earth.terrarium.botarium.api.item.ItemStackHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JetSuit extends NetheriteSpaceSuit implements EnergyItem {

    public boolean isFallFlying;

    public JetSuit(ArmorMaterial material, EquipmentSlot slot, Properties settings) {
        super(material, slot, settings);
    }

    public static void spawnParticles(Level level, LivingEntity entity, HumanoidModel<LivingEntity> model) {
        if (!AdAstra.CONFIG.spaceSuit.spawnJetSuitParticles) {
            return;
        }

        if (entity instanceof Player player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            CompoundTag nbt = chest.getOrCreateTag();
            if (!nbt.getBoolean("SpawnParticles")) {
                return;
            }
        }
        spawnParticles(level, entity, model.rightArm.xRot + 0.05, entity.isFallFlying() ? 0.0 : 0.8, -0.45);
        spawnParticles(level, entity, model.leftArm.xRot + 0.05, entity.isFallFlying() ? 0.0 : 0.8, 0.45);

        spawnParticles(level, entity, model.rightLeg.xRot + 0.05, entity.isFallFlying() ? 0.1 : 0.0, -0.1);
        spawnParticles(level, entity, model.leftLeg.xRot + 0.05, entity.isFallFlying() ? 0.1 : 0.0, 0.1);
    }

    // Spawns particles at the limbs of the player
    private static void spawnParticles(Level level, LivingEntity entity, double pitch, double yOffset, double zOffset) {
        double yaw = entity.yBodyRot;
        double xRotator = Math.cos(yaw * Math.PI / 180.0) * zOffset;
        double zRotator = Math.sin(yaw * Math.PI / 180.0) * zOffset;
        double xRotator1 = Math.cos((yaw - 90) * Math.PI / 180.0) * pitch;
        double zRotator1 = Math.sin((yaw - 90) * Math.PI / 180.0) * pitch;

        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, true, entity.getX() + xRotator + xRotator1, entity.getY() + yOffset, entity.getZ() + zRotator1 + zRotator, 0.0, 0.0, 0.0);
    }

    public static boolean hasFullSet(LivingEntity entity) {
        for (ItemStack stack : entity.getArmorSlots()) {
            if (!(stack.getItem() instanceof JetSuit)) {
                return false;
            }
        }
        return true;
    }

    public static void updateBatteryOverlay(ItemStack suit) {
        var energy = EnergyHooks.getItemEnergyManager(suit);
        PlayerOverlayScreen.batteryRatio = energy.getStoredEnergy() / (double) energy.getCapacity();
    }

    @Override
    public long getTankSize() {
        return AdAstra.CONFIG.spaceSuit.jetSuitTankSize;
    }

    // Display energy
    @Override
    public void appendHoverText(ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag context) {
        super.appendHoverText(stack, level, tooltip, context);
        long energy = EnergyHooks.getItemEnergyManager(stack).getStoredEnergy();
        tooltip.add(Component.translatable("gauge_text.ad_astra.storage", energy, AdAstra.CONFIG.spaceSuit.jetSuitMaxEnergy).setStyle(Style.EMPTY.withColor(energy > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    public void fly(Player player, ItemStack stack) {
        // Don't fly the Jet Suit in creative
        ItemStackHolder stackHolder = new ItemStackHolder(stack);
        if (player.getAbilities().flying) {
            stack.getOrCreateTag().putBoolean("SpawnParticles", false);
            return;
        }

        // Don't fly if the Jet Suit has no energy
        if (EnergyHooks.getItemEnergyManager(stack).getStoredEnergy() <= 0) {
            stack.getOrCreateTag().putBoolean("SpawnParticles", false);
            return;
        }

        stack.getOrCreateTag().putBoolean("SpawnParticles", true);
        if (ModKeyBindings.sprintKeyDown(player)) {
            this.fallFly(player, stackHolder);
        } else {
            this.flyUpward(player, stackHolder);
        }

        if (!player.level.isClientSide) {
            if (isFallFlying) {
                if (!player.isFallFlying()) {
                    player.startFallFlying();
                }
            } else {
                if (player.isFallFlying()) {
                    player.stopFallFlying();
                }
            }
        }
        if (stackHolder.isDirty()) player.setItemSlot(EquipmentSlot.CHEST, stackHolder.getStack());
    }

    public void flyUpward(Player player, ItemStackHolder stack) {
        if (EnergyHooks.isEnergyItem(stack.getStack())) {
            player.fallDistance /= 2;

            var energy = EnergyHooks.getItemEnergyManager(stack.getStack());
            long tickEnergy = AdAstra.CONFIG.spaceSuit.jetSuitEnergyPerTick;
            if (!player.isCreative()) {
                energy.extract(stack, tickEnergy, false);
            }
            isFallFlying = false;

            double speed = AdAstra.CONFIG.spaceSuit.jetSuitUpwardsSpeed;
            player.setDeltaMovement(player.getDeltaMovement().add(0.0, speed, 0.0));
            if (player.getDeltaMovement().y() > speed) {
                player.setDeltaMovement(player.getDeltaMovement().x(), speed, player.getDeltaMovement().z());
            }
        }
    }

    public void propel(Player player, ItemStack stack, int rotation) {
        if (EnergyHooks.isEnergyItem(stack)) {
            if (EnergyHooks.getItemEnergyManager(stack).getStoredEnergy() <= 0) return;
            player.fallDistance /= 2;
            isFallFlying = false;
            double speed = AdAstra.CONFIG.spaceSuit.jetSuitPropelSpeed;
            var dir = calculateViewVector(0, player.getYHeadRot());
            dir = dir.yRot(rotation);
            dir = dir.normalize();
            player.push(dir.x * speed, 0, dir.z * speed);
        }
    }

    protected final Vec3 calculateViewVector(float xRot, float yRot) {
        float f = xRot * 0.017453292F;
        float g = -yRot * 0.017453292F;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3((double)(i * j), (double)(-k), (double)(h * j));
    }

    public void fallFly(Player player, ItemStackHolder stack) {
        if (player.isOnGround()) {
            player.fallDistance /= 2;
        }
        var energy = EnergyHooks.getItemEnergyManager(stack.getStack());
        long tickEnergy = AdAstra.CONFIG.spaceSuit.jetSuitEnergyPerTick;
        if (!player.isCreative()) {
            energy.extract(stack, tickEnergy, false);
        }
        isFallFlying = true;

        double speed = AdAstra.CONFIG.spaceSuit.jetSuitSpeed - (ModUtils.getPlanetGravity(player.level) * 0.25);
        Vec3 rotationVector = player.getLookAngle().scale(speed);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.add(rotationVector.x() * 0.1 + (rotationVector.x() * 1.5 - velocity.x()) * 0.5, rotationVector.y() * 0.1 + (rotationVector.y() * 1.5 - velocity.y()) * 0.5, rotationVector.z() * 0.1 + (rotationVector.z() * 1.5 - velocity.z()) * 0.5));
    }

    @Override
    public ItemEnergyContainer getEnergyStorage(ItemStack itemStack) {
        return new ItemEnergyContainer(itemStack, AdAstra.CONFIG.spaceSuit.jetSuitMaxEnergy) {
            @Override
            public long maxInsert() {
                return 512;
            }

            @Override
            public long maxExtract() {
                return 256;
            }
        };
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot.equals(EquipmentSlot.CHEST)) {
            if (stack.getItem() instanceof JetSuit) {
                var energy = EnergyHooks.getItemEnergyManager(stack);
                return new ModResourceLocation("textures/entity/armour/jet_suit/jet_suit_" + (energy.getStoredEnergy() == 0 ? 0 : ((int) Math.min((energy.getStoredEnergy() * 5 / energy.getCapacity()) + 1, 5))) + ".png").toString();
            }
        }
        return AdAstra.MOD_ID + ":textures/entity/armour/jet_suit/jet_suit_5.png";
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getEnergyStorage(stack).getStoredEnergy() > AdAstra.CONFIG.spaceSuit.jetSuitEnergyPerTick ? AdAstra.ETRIUM_COLOR : super.getBarColor(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        StatefulEnergyContainer<ItemStack> energyStorage = getEnergyStorage(stack);
        if(energyStorage.getStoredEnergy() > AdAstra.CONFIG.spaceSuit.jetSuitEnergyPerTick) return (int) (((double) energyStorage.getStoredEnergy() / energyStorage.getMaxCapacity()) * 13);
        return super.getBarWidth(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged() || getEnergyStorage(stack).getStoredEnergy() > AdAstra.CONFIG.spaceSuit.jetSuitEnergyPerTick;
    }
}