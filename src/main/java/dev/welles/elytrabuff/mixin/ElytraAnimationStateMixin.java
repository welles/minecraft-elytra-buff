package dev.welles.elytrabuff.mixin;

//? if >=1.21.2 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.welles.elytrabuff.WingFlaps;
import net.minecraft.world.entity.ElytraAnimationState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/^* Beats the wings of flapping players, on top of the angles vanilla gives them for the flight. *^/
@Mixin(ElytraAnimationState.class)
public abstract class ElytraAnimationStateMixin {
    @Shadow
    @Final
    private LivingEntity entity;

    @ModifyReturnValue(method = "getRotX", at = @At("RETURN"))
    private float elytrabuff$beatWingsX(float rotX, float partialTicks) {
        return WingFlaps.wingRotX(entity, partialTicks, rotX);
    }

    @ModifyReturnValue(method = "getRotZ", at = @At("RETURN"))
    private float elytrabuff$beatWingsZ(float rotZ, float partialTicks) {
        return WingFlaps.wingRotZ(entity, partialTicks, rotZ);
    }
}
*///?}
