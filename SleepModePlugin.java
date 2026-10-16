package com.meusono.app;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Modo dormir nativo: baixa o brilho da janela, mantém a tela ligada
 * e ativa o Não Perturbe (somente alarmes), restaurando tudo ao acordar.
 */
@CapacitorPlugin(name = "SleepMode")
public class SleepModePlugin extends Plugin {

    private int previousFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
    private boolean saved = false;

    @PluginMethod
    public void enable(PluginCall call) {
        Float requested = call.getFloat("brightness", 0.05f);
        final float brightness = (requested == null) ? 0.05f : Math.max(0.01f, requested);
        boolean prompt = Boolean.TRUE.equals(call.getBoolean("prompt", false));
        boolean needsAccess = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                if (nm.isNotificationPolicyAccessGranted()) {
                    if (!saved) {
                        previousFilter = nm.getCurrentInterruptionFilter();
                        saved = true;
                    }
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS);
                } else {
                    needsAccess = true;
                    if (prompt) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getContext().startActivity(intent);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
                lp.screenBrightness = brightness;
                getActivity().getWindow().setAttributes(lp);
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        JSObject ret = new JSObject();
        ret.put("needsPolicyAccess", needsAccess);
        call.resolve(ret);
    }

    @PluginMethod
    public void disable(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && saved) {
            NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.isNotificationPolicyAccessGranted()) {
                nm.setInterruptionFilter(previousFilter);
            }
            saved = false;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                getActivity().getWindow().setAttributes(lp);
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        call.resolve();
    }
}
