package jp.local.savingsguard;

import org.junit.Test;
import static org.junit.Assert.*;

public class GuardPolicyTest {
    @Test public void blocksExactlyTheThreeApps() {
        for (String pkg : GuardPolicy.PACKAGES) assertTrue(GuardPolicy.isTarget(pkg));
        assertFalse(GuardPolicy.isTarget("com.ubercab"));
        assertFalse(GuardPolicy.isTarget("com.android.chrome"));
        assertFalse(GuardPolicy.isTarget("com.cpone.customer.fake"));
        assertFalse(GuardPolicy.isTarget(null));
    }
    @Test public void initialSetupRemainsAccessible() {
        assertFalse(GuardPolicy.protectSettings("com.android.settings", "ToggleAccessibilityService",
                "貯金ガード ユーザー補助", true, false));
    }
    @Test public void protectsOwnServiceInJapaneseAndEnglish() {
        assertTrue(GuardPolicy.protectSettings("com.android.settings", "SubSettings",
                "貯金ガード ショートカット", true, true));
        assertTrue(GuardPolicy.protectSettings("com.android.settings", "SubSettings",
                "貯金ガード Accessibility shortcut", true, true));
        assertTrue(GuardPolicy.protectSettings("com.android.settings", "ToggleAccessibilityService",
                "貯金ガード", false, true));
    }
    @Test public void doesNotBlockOtherServicesOrSettingsList() {
        assertFalse(GuardPolicy.protectSettings("com.android.settings", "ToggleAccessibilityService",
                "TalkBack ユーザー補助", true, true));
        assertFalse(GuardPolicy.protectSettings("com.android.settings", "SubSettings",
                "インストール済みアプリ 貯金ガード ON", false, true));
        assertFalse(GuardPolicy.protectSettings("com.android.settings", "Settings",
                "Wi-Fi Bluetooth ユーザー補助", true, true));
    }
    @Test public void protectsOwnAppInfoButNotOtherApps() {
        assertTrue(GuardPolicy.protectSettings("com.android.settings", "SubSettings",
                "貯金ガード 強制停止 ストレージ", false, true));
        assertTrue(GuardPolicy.protectSettings("com.android.settings", "SubSettings",
                "貯金ガード Force stop Storage", false, true));
        assertFalse(GuardPolicy.protectSettings("com.android.settings", "SubSettings",
                "時計 強制停止", false, true));
    }
    @Test public void matchingWordsInBrowserOrOurAppDoNotBlock() {
        assertFalse(GuardPolicy.protectSettings("com.android.chrome", "ToggleAccessibilityService",
                "貯金ガード ユーザー補助 強制停止", true, true));
        assertFalse(GuardPolicy.protectSettings("jp.local.savingsguard", "MainActivity",
                "貯金ガード ユーザー補助 強制停止", true, true));
    }
}
