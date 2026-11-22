package chatwave.core;

import chatwave.core.Models.Account;

/** Trạng thái dùng chung: backend đang dùng và tài khoản đang đăng nhập. */
public final class Session {
    private Session() {}
    public static Backend backend;
    public static Account account;
    public static String me() { return account == null ? "" : account.username; }
}
