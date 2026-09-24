package jp.local.savingsguard;

import android.content.Context;

final class GuardState {
    private GuardState() {}
    // One local flag, no database, accounts, analytics or network access.
    static boolean armed(Context context) {
        return context.getSharedPreferences("guard", Context.MODE_PRIVATE).getBoolean("armed", false);
    }
    static boolean arm(Context context) {
        return context.getSharedPreferences("guard", Context.MODE_PRIVATE)
                .edit().putBoolean("armed", true).commit();
    }
}
