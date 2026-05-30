package com.skillup.common;

import java.util.UUID;

/**
 * Holds the current branch ID for the duration of a request.
 *
 * Uses InheritableThreadLocal so the branch ID propagates to child threads
 * (e.g., @Async tasks spawned from a request thread).
 *
 * Usage:
 *   - Set by BranchResolutionFilter at the start of every request
 *   - Read by any service that needs branch-scoped data
 *   - Always cleared in a finally block to prevent thread-pool leakage
 */
public final class BranchContext {

    private static final InheritableThreadLocal<UUID> CURRENT_BRANCH =
            new InheritableThreadLocal<>();

    private BranchContext() {}

    public static void set(UUID branchId) {
        CURRENT_BRANCH.set(branchId);
    }

    public static UUID get() {
        return CURRENT_BRANCH.get();
    }

    public static boolean isSet() {
        return CURRENT_BRANCH.get() != null;
    }

    public static void clear() {
        CURRENT_BRANCH.remove();
    }
}
