package cn.lazymoon.utils.math;

import cn.lazymoon.utils.entity.Rotation;
import net.minecraft.world.phys.Vec3;

public class VecRotation {
    public Vec3 vec;
    public Rotation rotation;

    public VecRotation(Vec3 vec, Rotation rotation) {
        this.vec = vec;
        this.rotation = rotation;
    }
}
