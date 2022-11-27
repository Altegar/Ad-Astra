package earth.terrarium.ad_astra.mixin;

import earth.terrarium.ad_astra.config.SpaceSuitConfig;
import earth.terrarium.ad_astra.items.armour.JetSuit;
import earth.terrarium.ad_astra.items.armour.NetheriteSpaceSuit;
import earth.terrarium.ad_astra.util.ModKeyBindings;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void adastra_damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (SpaceSuitConfig.netheriteSpaceSuitHasFireResistance) {
            Player player = ((Player) (Object) this);
            if (source.isFire() || source.equals(DamageSource.HOT_FLOOR)) {
                if (NetheriteSpaceSuit.hasFullSet(player)) {
                    player.setRemainingFireTicks(0);
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void adastra_tick(CallbackInfo ci) {
        if (SpaceSuitConfig.enableJetSuitFlight) {
            Player player = ((Player) (Object) this);
            if (!player.isPassenger()) {
                ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
                if (chest.getItem() instanceof JetSuit jetSuit) {
                    if (ModKeyBindings.jumpKeyDown(player)) {
                        if (JetSuit.hasFullSet(player)) {
                            if(ModKeyBindings.forwardKeyDown(player)) {
                                jetSuit.propel(player, chest, 0);
                            }
                            if(ModKeyBindings.leftKeyDown(player)) {
                                jetSuit.propel(player, chest, 90);
                            }
                            if(ModKeyBindings.rightKeyDown(player)) {
                                jetSuit.propel(player, chest, -90);
                            }
                            jetSuit.fly(player, chest);
                        }
                    } else {
                        jetSuit.isFallFlying = false;
                        if (!player.level.isClientSide) {
                            chest.getOrCreateTag().putBoolean("SpawnParticles", false);
                        }
                    }
                }
            }
        }
    }
}