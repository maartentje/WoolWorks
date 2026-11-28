package be.maarten.woolworks.machine;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

public final class CableNetworkGameTests {
    private CableNetworkGameTests() {}

    public static void invalidationDuringRouteDiscovery(GameTestHelper helper) {
        var state = new CableNetworks.NetworkState();
        var root = BlockPos.ZERO;
        var stale = List.of(new CableNetworks.Route(root, List.of(root)));
        // A different cached route makes clear() structurally modify the map, as in the crash.
        state.routes.put(BlockPos.ZERO.above(), stale);
        var result = CableNetworks.cachedRoutes(state, root, () -> {
            state.invalidateRoutes();
            return stale;
        });
        if (result != null || !state.routes.isEmpty()) {
            helper.fail("Invalidated topology was used or cached"); return;
        }
        var fresh = List.of(new CableNetworks.Route(root.east(), List.of(root, root.east())));
        if (CableNetworks.cachedRoutes(state, root, () -> fresh) != fresh
                || CableNetworks.cachedRoutes(state, root, () -> { throw new AssertionError("Cache miss"); }) != fresh) {
            helper.fail("Fresh topology was not cached"); return;
        }
        state.invalidateRoutes();
        if (!state.routes.isEmpty()) { helper.fail("Cached topology survived invalidation"); return; }
        helper.succeed();
    }
}
