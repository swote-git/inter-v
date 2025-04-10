package dev.swote.interv.interceptor;

import dev.swote.interv.domain.user.entity.User;

public record CurrentUser(
        Integer id
) {
    public static CurrentUser from(User user) {
        return new CurrentUser(user.getId());
    }
}