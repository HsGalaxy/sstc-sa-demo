package com.sstc.platform.common.security;

/**
 * Request scoped holder of the authenticated {@link CurrentUser}. Kept separate from the
 * servlet request so services and the audit components can read the actor without
 * depending on the web layer.
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<CurrentUser>();

    private CurrentUserHolder() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long id() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getId();
    }

    public static String name() {
        CurrentUser user = HOLDER.get();
        return user == null ? "system" : user.getDisplayName();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
