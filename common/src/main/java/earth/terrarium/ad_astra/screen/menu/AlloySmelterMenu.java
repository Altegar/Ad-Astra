package earth.terrarium.ad_astra.screen.menu;

import earth.terrarium.ad_astra.blocks.machines.entity.AlloySmelterBlockEntity;
import earth.terrarium.ad_astra.blocks.machines.entity.CoalGeneratorBlockEntity;
import earth.terrarium.ad_astra.networking.NetworkHandling;
import earth.terrarium.ad_astra.networking.packets.server.MachineInfoPacket;
import earth.terrarium.ad_astra.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class AlloySmelterMenu extends AbstractMachineMenu<AlloySmelterBlockEntity> {

    public AlloySmelterMenu(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, (AlloySmelterBlockEntity) inventory.player.level.getBlockEntity(buf.readBlockPos()));

    }

    public AlloySmelterMenu(int syncId, Inventory inventory, AlloySmelterBlockEntity entity) {
        super(ModMenuTypes.COAL_GENERATOR_MENU.get(), syncId, inventory, entity,
                new Slot[] { new Slot(entity, 0, 77, 71) });
    }

    @Override
    public int getPlayerInventoryOffset() {
        return 23;
    }

    @Override
    public void syncClientScreen() {
        NetworkHandling.CHANNEL.sendToPlayer(new MachineInfoPacket(machine.getEnergyStorage().getStoredEnergy(), List.of()), this.player);
    }
}