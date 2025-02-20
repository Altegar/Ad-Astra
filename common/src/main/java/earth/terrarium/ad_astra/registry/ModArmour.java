package earth.terrarium.ad_astra.registry;

import earth.terrarium.ad_astra.items.armour.JetSuitMaterial;
import earth.terrarium.ad_astra.items.armour.NetheriteSpaceSuitMaterial;
import earth.terrarium.ad_astra.items.armour.SpaceSuitMaterial;
import net.minecraft.world.item.ArmorMaterial;

public class ModArmour {
    public static final ArmorMaterial SPACE_SUIT_ARMOUR_MATERIAL = new SpaceSuitMaterial();
    public static final ArmorMaterial NETHERITE_SPACE_SUIT_ARMOUR_MATERIAL = new NetheriteSpaceSuitMaterial();
    public static final ArmorMaterial JET_SUIT_ARMOUR_MATERIAL = new JetSuitMaterial();
}