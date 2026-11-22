package dev.axiomutilities;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import net.minecraft.world.level.Level;

/**
 * Accepts blocks without collision (air, grass, flowers...) that lie within {@code thickness}
 * blocks of a block with collision, and that also pass the user's destination mask.
 * With thickness 1 this is exactly the one-block "coat" that touches the surface by a face.
 */
public final class AirLayerMask implements MaskElement {
    private final MaskElement userMask;
    private final int[] offsets; // packed x,y,z triples, sorted by distance (closest first)
    private final MaskContext probe;

    public AirLayerMask(MaskElement userMask, int thickness, Level level) {
        this.userMask = userMask;
        this.probe = new MaskContext(level);

        int r = Math.max(1, thickness);
        int rSq = r * r;
        java.util.List<int[]> list = new java.util.ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    int d = x * x + y * y + z * z;
                    if (d > 0 && d <= rSq) {
                        list.add(new int[]{x, y, z, d});
                    }
                }
            }
        }
        list.sort(java.util.Comparator.comparingInt(a -> a[3]));
        this.offsets = new int[list.size() * 3];
        for (int i = 0; i < list.size(); i++) {
            int[] o = list.get(i);
            this.offsets[i * 3] = o[0];
            this.offsets[i * 3 + 1] = o[1];
            this.offsets[i * 3 + 2] = o[2];
        }
    }

    @Override
    public boolean test(MaskContext context) {
        if (context.hasCollision()) {
            return false;
        }
        if (this.userMask != null && !this.userMask.test(context)) {
            return false;
        }
        int x = context.getX();
        int y = context.getY();
        int z = context.getZ();
        for (int i = 0; i < this.offsets.length; i += 3) {
            this.probe.setPosition(x + this.offsets[i], y + this.offsets[i + 1], z + this.offsets[i + 2]);
            if (this.probe.hasCollision()) {
                return true;
            }
        }
        return false;
    }
}
