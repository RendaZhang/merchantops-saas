package com.renda.merchantops.api.context;

import com.renda.merchantops.api.security.CurrentUser;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER_HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) {
        CURRENT_USER_HOLDER.set(currentUser);
    }

    public static CurrentUser get() {
        return CURRENT_USER_HOLDER.get();
    }

    public static Long getUserId() {
        CurrentUser currentUser = CURRENT_USER_HOLDER.get();
        return currentUser != null ? currentUser.getUserId() : null;
    }

    public static String getUsername() {
        CurrentUser currentUser = CURRENT_USER_HOLDER.get();
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public static void clear() {
        CURRENT_USER_HOLDER.remove();
    }

}
