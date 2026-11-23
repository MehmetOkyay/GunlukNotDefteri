package com.mehmetokyay.flownote.util;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class EkranYardimcisi {

    private EkranYardimcisi() {
    }

    public static void uygula(Activity activity, boolean altBoslukDahil, boolean klavyeDahil) {
        View kok = activity.findViewById(android.R.id.content);
        if (kok == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(kok, (v, insets) -> {
            Insets barlar = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int alt = altBoslukDahil ? barlar.bottom : 0;
            if (klavyeDahil) {
                alt = Math.max(alt, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            }
            v.setPadding(barlar.left, barlar.top, barlar.right, alt);
            return altBoslukDahil ? WindowInsetsCompat.CONSUMED : insets;
        });
        ViewCompat.requestApplyInsets(kok);
    }
}
