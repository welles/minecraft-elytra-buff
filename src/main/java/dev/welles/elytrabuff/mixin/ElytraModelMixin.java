package dev.welles.elytrabuff.mixin;

//? if <1.21.2 {
import dev.welles.elytrabuff.WingAnimation;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Moves the wings for wing flaps and cold starts, see {@link WingAnimation}. */
@Mixin(ElytraModel.class)
public abstract class ElytraModelMixin {
    @Shadow
    @Final
    private ModelPart leftWing;

    @Shadow
    @Final
    private ModelPart rightWing;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void elytrabuff$beatWings(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        // the age includes the partial tick
        float partialTick = ageInTicks - entity.tickCount;
        leftWing.xRot = WingAnimation.wingRotX(entity, partialTick, leftWing.xRot);
        leftWing.zRot = WingAnimation.wingRotZ(entity, partialTick, leftWing.zRot);
        rightWing.xRot = leftWing.xRot;
        rightWing.zRot = -leftWing.zRot;
    }
}
//?}
