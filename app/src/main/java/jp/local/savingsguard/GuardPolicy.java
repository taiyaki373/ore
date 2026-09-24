package jp.local.savingsguard;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/** Pure decisions, kept independent of device/UI APIs for regression tests. */
public final class GuardPolicy {
    public static final List<String> PACKAGES = Collections.unmodifiableList(Arrays.asList(
            "jp.co.mcdonalds.android", "com.cpone.customer", "com.ubercab.eats"));
    public static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList("マクドナルド", "ロケットナウ", "Uber Eats"));
    private GuardPolicy() {}

    public static boolean isTarget(String packageName) { return PACKAGES.contains(packageName); }

    public static boolean protectSettings(String packageName, String className,
                                          String visibleText, boolean hasSwitch, boolean armed) {
        if (!armed || !"com.android.settings".equals(packageName)) return false;
        String text = visibleText.toLowerCase(Locale.ROOT);
        boolean ownApp = text.contains("貯金ガード") || text.contains("jp.local.savingsguard");
        if (!ownApp) return false;
        String screen = className.toLowerCase(Locale.ROOT);
        boolean serviceDetail = screen.contains("accessibilityservicedetail")
                || screen.contains("toggleaccessibilityservice");
        boolean serviceControls = hasSwitch && (text.contains("ユーザー補助")
                || text.contains("accessibility") || text.contains("ショートカット")
                || text.contains("shortcut"));
        boolean appDetails = text.contains("強制停止") || text.contains("force stop");
        return serviceDetail || serviceControls || appDetails;
    }
}
