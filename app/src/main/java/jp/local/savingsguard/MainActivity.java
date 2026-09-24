package jp.local.savingsguard;

import android.app.Activity;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status, detail;
    private Button enable, protect;
    private boolean lastEnabled, lastConnected, lastArmed;
    private final Runnable refresh = new Runnable() {
        @Override public void run() { updateStatus(); handler.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 242));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(28), dp(24), dp(28));
        scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
        addText(page, "SAVINGS GUARD", 12, Color.rgb(23, 107, 82), true);
        addText(page, "今日の衝動を、\n明日の貯金に。", 30, Color.rgb(24, 44, 37), true);
        addText(page, "3つのアプリを、開くたびにブロック。", 15, Color.DKGRAY, false);

        LinearLayout card = card(page);
        status = addText(card, "状態を確認しています", 21, Color.rgb(23, 107, 82), true);
        detail = addText(card, "", 14, Color.DKGRAY, false);
        enable = new Button(this);
        enable.setText("1. ユーザー補助を設定する");
        enable.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            try { startActivity(intent); }
            catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "設定アプリからユーザー補助を開いてください。", Toast.LENGTH_LONG).show();
            }
        });
        card.addView(enable);
        protect = new Button(this);
        protect.setText("2. 設定保護を開始する");
        protect.setOnClickListener(v -> {
            if (!enabled() || !GuardService.connected) return;
            if (!GuardState.arm(this)) {
                Toast.makeText(this, "保存できませんでした。もう一度お試しください。", Toast.LENGTH_LONG).show();
                return;
            }
            updateStatus();
        });
        card.addView(protect);

        addText(page, "ブロックするアプリ", 20, Color.rgb(24, 44, 37), true);
        for (int i = 0; i < GuardPolicy.PACKAGES.size(); i++) {
            String pkg = GuardPolicy.PACKAGES.get(i);
            LinearLayout app = card(page);
            addText(app, GuardPolicy.NAMES.get(i), 18, Color.rgb(24, 44, 37), true);
            boolean installed;
            try { getPackageManager().getApplicationInfo(pkg, 0); installed = true; }
            catch (PackageManager.NameNotFoundException e) { installed = false; }
            addText(app, installed ? "インストール済み · 保護対象" : "未インストール · インストール後も保護対象", 13, Color.DKGRAY, false);
        }
        addText(page, "はじめに", 20, Color.rgb(24, 44, 37), true);
        addText(page, "① ユーザー補助で「貯金ガード」をONにします。\n② 対象アプリを開いて、ホームに戻ることを確認します。\n③ この画面で設定保護を開始します。", 15, Color.DKGRAY, false);
        addText(page, "設定保護を開始すると、本アプリのユーザー補助の詳細やアプリ情報を開いた際にもホームへ戻します。アプリ内に解除ボタンはありません。端末によって検知できない設定画面があります。", 14, Color.DKGRAY, false);
        addText(page, "画面情報はブロック判定にだけ使い、保存・送信しません。DB・アカウント・通信は使いません。", 14, Color.DKGRAY, false);
        addText(page, "ユーザー補助がOFF・停止している間は保護できません。起動直後に対象アプリが一瞬見える場合があります。ブラウザー版は対象外です。", 14, Color.DKGRAY, false);
    }

    @Override protected void onResume() { super.onResume(); refresh.run(); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }

    private boolean enabled() {
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        ComponentName ours = new ComponentName(this, GuardService.class);
        for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (ours.equals(new ComponentName(info.getResolveInfo().serviceInfo.packageName,
                    info.getResolveInfo().serviceInfo.name))) return true;
        }
        return false;
    }

    private void updateStatus() {
        boolean active = enabled(), live = GuardService.connected, armed = GuardState.armed(this);
        if (status.getTag() != null && active == lastEnabled && live == lastConnected && armed == lastArmed) return;
        status.setTag(true);
        lastEnabled = active; lastConnected = live; lastArmed = armed;
        status.setText(!active ? "保護がOFFです" : !live ? "サービス接続待ち" : "3アプリを保護中");
        detail.setText(!active ? "ユーザー補助をONにするとブロックが始まります。"
                : !live ? "設定はONですが、まだ動作を確認できません。"
                : armed ? "設定保護も開始済みです。" : "ブロックを確認したら、設定保護を開始してください。");
        enable.setVisibility(active && live ? View.GONE : View.VISIBLE);
        protect.setVisibility(armed ? View.GONE : View.VISIBLE);
        protect.setEnabled(active && live);
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE); background.setCornerRadius(dp(18));
        card.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(12), 0, dp(8));
        parent.addView(card, params);
        return card;
    }

    private TextView addText(LinearLayout parent, String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(6), 0, dp(8));
        view.setLineSpacing(dp(3), 1);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        parent.addView(view);
        return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
