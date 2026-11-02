package dev.welles.elytrabuff.mixin;

//? if >=1.21.2 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.welles.elytrabuff.WingAnimation;
import net.minecraft.world.entity.ElytraAnimationState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/^* Moves the wings for wing flaps and cold starts, see {@link WingAnimation}. *^/
@Mixin(ElytraAnimationState.class)
public abstract class ElytraAnimationStateMixin {
    @Shadow
    @Final
    private LivingEntity entity;

    @ModifyReturnValue(method = "getRotX", at = @At("RETURN"))
    private float elytrabuff$beatWingsX(float rotX, float partialTicks) {
        return WingAnimation.wingRotX(entity, partialTicks, rotX);
    }

    @ModifyReturnValue(method = "getRotZ", at = @At("RETURN"))
    private float elytrabuff$beatWingsZ(float rotZ, float partialTicks) {
        return WingAnimation.wingRotZ(entity, partialTicks, rotZ);
    }
}
*///?}
