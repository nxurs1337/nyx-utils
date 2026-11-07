package dev.nyx.utils.features.modules.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.nyx.utils.features.modules.render.ModelChangerModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Draws a {@link GlbModel} where an entity's own model would be. The mesh is static, so walking
 * is faked with a little bob and sway driven by the entity's walk animation.
 */
public final class CustomModelRenderer {
    private CustomModelRenderer() {
    }

    /** {@code pose} is already translated to the entity's feet by the render dispatcher. */
    public static void submit(GlbModel model, EntityRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        ModelChangerModule module = ModelChangerModule.getInstance();
        float size = module != null ? module.customSize.getValue() : 1f;
        int yaw = module != null ? module.customYaw.getValue() : 0;
        float height = Math.max(0.2f, state.boundingBoxHeight) * size;

        pose.pushPose();
        if (state instanceof LivingEntityRenderState living) {
            // glTF models face +Z, which is where an entity with yaw 0 looks
            pose.mulPose(Axis.YP.rotationDegrees(-living.bodyRot + yaw));
            float speed = Math.min(1f, living.walkAnimationSpeed);
            float phase = living.walkAnimationPos * 0.6662f;
            pose.translate(0, Math.abs(Mth.sin(phase)) * 0.06f * speed * height, 0);
            pose.mulPose(Axis.ZP.rotation(Mth.sin(phase) * 0.12f * speed));
            if (living.isBaby) height *= 0.5f;
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
        }
        pose.scale(height, height, height);
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(model.texture()),
                (matrices, consumer) -> model.emit(matrices, consumer, light));
        pose.popPose();
    }
}
