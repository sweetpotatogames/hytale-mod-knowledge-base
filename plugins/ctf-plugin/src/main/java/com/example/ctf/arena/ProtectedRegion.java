package com.example.ctf.arena;

import com.hypixel.hytale.codec.BuilderCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Validators;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

import javax.annotation.Nonnull;

/**
 * Represents an axis-aligned bounding box (AABB) region where building is restricted.
 * Used to protect flag rooms and other important areas from player modification.
 */
public class ProtectedRegion {

    public static final BuilderCodec<ProtectedRegion> CODEC = BuilderCodec.builder(ProtectedRegion.class, ProtectedRegion::new)
        .appendInherited(new KeyedCodec<>("Name", Codec.STRING), ProtectedRegion::getName, ProtectedRegion::setName)
        .addValidator(Validators.nonNull())
        .add()
        .appendInherited(new KeyedCodec<>("Min", Vector3d.CODEC), ProtectedRegion::getMin, ProtectedRegion::setMin)
        .addValidator(Validators.nonNull())
        .add()
        .appendInherited(new KeyedCodec<>("Max", Vector3d.CODEC), ProtectedRegion::getMax, ProtectedRegion::setMax)
        .addValidator(Validators.nonNull())
        .add()
        .build();

    private String name;
    private Vector3d min;
    private Vector3d max;

    public ProtectedRegion() {
        this.name = "unnamed";
        this.min = new Vector3d(0, 0, 0);
        this.max = new Vector3d(0, 0, 0);
    }

    public ProtectedRegion(@Nonnull String name, @Nonnull Vector3d min, @Nonnull Vector3d max) {
        this.name = name;
        // Normalize min/max to ensure min values are actually smaller
        this.min = new Vector3d(
            Math.min(min.x(), max.x()),
            Math.min(min.y(), max.y()),
            Math.min(min.z(), max.z())
        );
        this.max = new Vector3d(
            Math.max(min.x(), max.x()),
            Math.max(min.y(), max.y()),
            Math.max(min.z(), max.z())
        );
    }

    /**
     * Checks if a block position is within this protected region.
     *
     * @param blockPos The block position to check
     * @return true if the block is within the protected region
     */
    public boolean containsBlock(@Nonnull Vector3i blockPos) {
        return blockPos.x() >= min.x() && blockPos.x() <= max.x()
            && blockPos.y() >= min.y() && blockPos.y() <= max.y()
            && blockPos.z() >= min.z() && blockPos.z() <= max.z();
    }

    /**
     * Checks if a position is within this protected region.
     *
     * @param position The position to check
     * @return true if the position is within the protected region
     */
    public boolean contains(@Nonnull Vector3d position) {
        return position.x() >= min.x() && position.x() <= max.x()
            && position.y() >= min.y() && position.y() <= max.y()
            && position.z() >= min.z() && position.z() <= max.z();
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public Vector3d getMin() {
        return min;
    }

    public void setMin(@Nonnull Vector3d min) {
        this.min = min;
    }

    @Nonnull
    public Vector3d getMax() {
        return max;
    }

    public void setMax(@Nonnull Vector3d max) {
        this.max = max;
    }
}
