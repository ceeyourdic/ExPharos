package net.minecraft.client.player;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.input.MoveInputEvent;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyboardInput extends ClientInput {
    private final Options options;

    public KeyboardInput(Options pOptions) {
        this.options = pOptions;
    }

    private static float calculateImpulse(boolean pInput, boolean pOtherInput) {
        if (pInput == pOtherInput) {
            return 0.0F;
        } else {
            return pInput ? 1.0F : -1.0F;
        }
    }

    @Override
    public void tick() {
        this.keyPresses = new Input(
            this.options.keyUp.isDown(),
            this.options.keyDown.isDown(),
            this.options.keyLeft.isDown(),
            this.options.keyRight.isDown(),
            this.options.keyJump.isDown(),
            this.options.keyShift.isDown(),
            this.options.keySprint.isDown()
        );
        this.forwardImpulse = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
        this.leftImpulse = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
        // Arcane mixin port: MoveInputEvent can rewrite or cancel keyboard movement after vanilla input is sampled.
        MoveInputEvent moveInputEvent = new MoveInputEvent(this.forwardImpulse, this.leftImpulse, this.keyPresses.jump(), this.keyPresses.shift());
        Client.INSTANCE.getEventManager().call(moveInputEvent);
        if (moveInputEvent.isCancelled()) {
            this.keyPresses = new Input(false, false, false, false, false, false, false);
            this.forwardImpulse = 0.0F;
            this.leftImpulse = 0.0F;
        } else {
            this.forwardImpulse = moveInputEvent.forward;
            this.leftImpulse = moveInputEvent.strafe;
            this.keyPresses = new Input(
                arcane$keyState(this.forwardImpulse),
                arcane$keyState(-this.forwardImpulse),
                arcane$keyState(this.leftImpulse),
                arcane$keyState(-this.leftImpulse),
                moveInputEvent.jump,
                moveInputEvent.sneak,
                this.keyPresses.sprint() && this.forwardImpulse > 0.0F
            );
        }
    }

    private static boolean arcane$keyState(float pValue) {
        return pValue > 0.0F;
    }
}
