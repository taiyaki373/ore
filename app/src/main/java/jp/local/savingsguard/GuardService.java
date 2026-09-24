package jp.local.savingsguard;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayDeque;

public final class GuardService extends AccessibilityService {
    static volatile boolean connected;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout cover;
    private long lastBlock;
    private String settingsClass = "";
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            inspectActiveWindow();
            handler.postDelayed(this, 600);
        }
    };
    private final Runnable clearCover = this::removeCover;

    @Override protected void onServiceConnected() {
        connected = true;
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        String pkg = String.valueOf(event.getPackageName());
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            settingsClass = "com.android.settings".equals(pkg) ? String.valueOf(event.getClassName()) : "";
        }
        // Inspect the current window, not stale background content-change events.
        inspectActiveWindow();
    }

    private void inspectActiveWindow() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            String pkg = String.valueOf(root.getPackageName());
            if (GuardPolicy.isTarget(pkg)) { block(false); return; }
            if (!"com.android.settings".equals(pkg) || !GuardState.armed(this)) return;
            Screen screen = readSettings(root);
            if (GuardPolicy.protectSettings(pkg, settingsClass, screen.text, screen.hasSwitch, true)) block(true);
        } finally { root.recycle(); }
    }

    private static final class Screen {
        String text;
        boolean hasSwitch;
    }

    /** Only Settings text is inspected; bounded traversal, never logged or persisted. */
    private Screen readSettings(AccessibilityNodeInfo root) {
        Screen result = new Screen();
        StringBuilder text = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(AccessibilityNodeInfo.obtain(root));
        int visited = 0;
        while (!queue.isEmpty() && visited++ < 250) {
            AccessibilityNodeInfo node = queue.removeFirst();
            try {
                if (!node.isVisibleToUser()) continue;
                if (node.getText() != null) text.append(node.getText()).append('\n');
                if (node.getContentDescription() != null) text.append(node.getContentDescription()).append('\n');
                String type = String.valueOf(node.getClassName());
                if (node.isCheckable() || type.contains("Switch")) result.hasSwitch = true;
                for (int i = 0; i < node.getChildCount() && queue.size() < 250; i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) queue.addLast(child);
                }
            } finally { node.recycle(); }
        }
        while (!queue.isEmpty()) queue.removeFirst().recycle();
        result.text = text.toString();
        return result;
    }

    private void block(boolean settings) {
        long now = SystemClock.elapsedRealtime();
        if (lastBlock != 0 && now - lastBlock < 500) return;
        lastBlock = now;
        showCover(settings);
        if (!performGlobalAction(GLOBAL_ACTION_HOME)) performGlobalAction(GLOBAL_ACTION_BACK);
        handler.removeCallbacks(clearCover);
        handler.postDelayed(clearCover, 900);
    }

    private void showCover(boolean settings) {
        if (cover != null) return;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 64, 48, 64);
        layout.setBackgroundColor(Color.rgb(23, 70, 54));
        TextView message = new TextView(this);
        message.setText(settings ? "設定保護中\nホームに戻ります" : "今日の貯金を守りました\nホームに戻ります");
        message.setTextSize(24); message.setTextColor(Color.WHITE); message.setGravity(Gravity.CENTER);
        layout.addView(message);
        Button home = new Button(this); home.setText("ホームへ戻る");
        home.setOnClickListener(v -> { performGlobalAction(GLOBAL_ACTION_HOME); removeCover(); });
        layout.addView(home);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE);
        try { getSystemService(WindowManager.class).addView(layout, params); cover = layout; }
        catch (WindowManager.BadTokenException | IllegalStateException e) {
            // HOME still works when an overlay cannot be attached during lifecycle changes.
        }
    }

    private void removeCover() {
        if (cover == null) return;
        try { getSystemService(WindowManager.class).removeView(cover); }
        catch (IllegalArgumentException ignored) { /* Already detached by the system. */ }
        cover = null;
    }
    @Override public void onInterrupt() { removeCover(); }
    @Override public boolean onUnbind(Intent intent) { stop(); return super.onUnbind(intent); }
    @Override public void onDestroy() { stop(); super.onDestroy(); }
    private void stop() { connected = false; handler.removeCallbacksAndMessages(null); removeCover(); }
}
