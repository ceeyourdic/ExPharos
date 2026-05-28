package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.visual.AntiBlind;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Stream;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {

    @Redirect(method = "getFogModifier", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"))
    private static Stream<BackgroundRenderer.StatusEffectFogModifier> injectAntiBlind(List<BackgroundRenderer.StatusEffectFogModifier> list) {
        return list.stream().filter(modifier -> {
            final var effect = modifier.getStatusEffect();
            if (!Client.INSTANCE.getModuleManager().getModule(AntiBlind.class).isState()) {
                return true;
            }
            return !(MobEffects.BLINDNESS == effect && AntiBlind.blind.get()) || (MobEffects.DARKNESS == effect && AntiBlind.darkness.get());
        });
    }

}
