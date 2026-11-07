package dev.nyx.utils.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.nyx.utils.features.modules.render.ModelChangerModule;
import dev.nyx.utils.features.modules.render.model.CustomModelRenderer;
import dev.nyx.utils.features.modules.render.model.GlbModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    // swaps the entity being drawn for a look-alike of another type (client side only)
    @ModifyVariable(method = "extractEntity", at = @At("HEAD"), argsOnly = true)
    private Entity nyx$swapModel(Entity entity) {
        return ModelChangerModule.substitute(entity);
    }

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void nyx$tagCustomModel(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        ModelChangerModule.tag(entity, cir.getReturnValue());
    }

    // draws a custom .glb model instead of the entity's own model; shadow, fire and name tag still render
    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"))
    private void nyx$customModel(EntityRenderer<?, ?> renderer, EntityRenderState state, PoseStack pose,
                                 SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
        GlbModel model = ModelChangerModule.customModel(state);
        if (model != null) {
            CustomModelRenderer.submit(model, state, pose, collector);
            // hide the original body but keep the name tag
            state.isInvisible = true;
            if (state instanceof LivingEntityRenderState living) living.isInvisibleToPlayer = true;
        }
        original.call(renderer, state, pose, collector, camera);
    }
}
