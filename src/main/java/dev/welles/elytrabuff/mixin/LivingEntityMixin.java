package dev.welles.elytrabuff.mixin;

import dev.welles.elytrabuff.ElytraBuff;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >=1.21.2 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Shadow;
*///?} else {
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.Slice;
//?}

/** Adds the energy bonus and wing flaps to the movement of every elytra flight tick, right after vanilla's drag. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    //? if >=1.21.2 {
    /*@Shadow
    protected abstract double getEffectiveGravity();

    @ModifyReturnValue(method = "updateFallFlyingMovement", at = @At("RETURN"))
    private Vec3 elytrabuff$flightBuffs(Vec3 movement) {
        return ElytraBuff.applyFlightBuffs((LivingEntity) (Object) this, movement, getEffectiveGravity());
    }
    *///?} else {
    /** The elytra branch of {@code travel} is the first one that checks {@code isFallFlying}, its drag is the first multiply after it. */
    @ModifyExpressionValue(
        method = "travel",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0)
    )
    private Vec3 elytrabuff$flightBuffs(Vec3 movement, @Local(ordinal = 0) double gravity) {
        return ElytraBuff.applyFlightBuffs((LivingEntity) (Object) this, movement, gravity);
    }
    //?}
}
