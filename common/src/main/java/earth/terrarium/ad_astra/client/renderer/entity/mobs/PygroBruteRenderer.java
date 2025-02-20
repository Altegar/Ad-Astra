package earth.terrarium.ad_astra.client.renderer.entity.mobs;

import earth.terrarium.ad_astra.client.renderer.entity.mobs.models.PygroBruteModel;
import earth.terrarium.ad_astra.entities.mobs.PygroBrute;
import earth.terrarium.ad_astra.util.ModResourceLocation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class PygroBruteRenderer extends MobRenderer<PygroBrute, PygroBruteModel> {
    public static final ResourceLocation TEXTURE = new ModResourceLocation("textures/entity/pygro_brute.png");

    public PygroBruteRenderer(EntityRendererProvider.Context context) {
        super(context, new PygroBruteModel(context.bakeLayer(PygroBruteModel.LAYER_LOCATION)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PIGLIN_INNER_ARMOR)), new HumanoidModel<>(context.bakeLayer(ModelLayers.PIGLIN_OUTER_ARMOR))));
    }

    @Override
    public ResourceLocation getTextureLocation(PygroBrute mobEntity) {
        return TEXTURE;
    }
}
