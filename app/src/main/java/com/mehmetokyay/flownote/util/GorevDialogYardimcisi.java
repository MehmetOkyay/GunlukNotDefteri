package com.mehmetokyay.flownote.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mehmetokyay.flownote.R;

public final class GorevDialogYardimcisi {

    public static final int BASLIK_MAX_UZUNLUK = 80;

    private GorevDialogYardimcisi() {
    }

    public static void gorunumuHazirla(View dialogView) {
        TextView catIs = dialogView.findViewById(R.id.kategoriIs);
        TextView catKisisel = dialogView.findViewById(R.id.kategoriKisisel);
        TextView catSaglik = dialogView.findViewById(R.id.kategoriSaglik);
        final TextView[] kategoriler = {catIs, catKisisel, catSaglik};

        View.OnClickListener kategoriDinleyici = v -> {
            for (TextView k : kategoriler) {
                if (k != null) {
                    k.setBackgroundColor(Color.parseColor("#F4F6F9"));
                    k.setTextColor(Color.parseColor("#888888"));
                }
            }
            v.setBackgroundColor(Color.parseColor("#1A1756D3"));
            if (v instanceof TextView) ((TextView) v).setTextColor(Color.parseColor("#1756D3"));
        };
        for (TextView k : kategoriler) {
            if (k != null) k.setOnClickListener(kategoriDinleyici);
        }

        RadioGroup grup = dialogView.findViewById(R.id.radioGroupYineleme);
        if (grup != null) {
            grup.setOnCheckedChangeListener((g, checkedId) -> radyoStilleriniGuncelle(g));
            radyoStilleriniGuncelle(grup);
        }
    }

    private static void radyoStilleriniGuncelle(RadioGroup grup) {
        int seciliId = grup.getCheckedRadioButtonId();
        for (int i = 0; i < grup.getChildCount(); i++) {
            View cocuk = grup.getChildAt(i);
            if (!(cocuk instanceof RadioButton)) continue;
            RadioButton rb = (RadioButton) cocuk;
            if (rb.getId() == seciliId) {
                rb.setBackgroundResource(R.drawable.bg_calendar_gradient);
                rb.setTextColor(Color.WHITE);
            } else {
                rb.setBackground(null);
                rb.setTextColor(Color.parseColor("#333333"));
            }
        }
    }

    public static String secilenTekrarKodu(RadioGroup grup) {
        if (grup == null) return GorevYardimcisi.TEKRAR_YOK;
        int id = grup.getCheckedRadioButtonId();
        if (id == -1) return GorevYardimcisi.TEKRAR_YOK;
        View secili = grup.findViewById(id);
        if (secili != null && secili.getTag() != null) {
            return GorevYardimcisi.tekrarTuru(String.valueOf(secili.getTag()));
        }
        return GorevYardimcisi.TEKRAR_YOK;
    }

    public static void tekrarSec(RadioGroup grup, String saklananDeger) {
        if (grup == null) return;
        String hedef = GorevYardimcisi.tekrarTuru(saklananDeger);
        for (int i = 0; i < grup.getChildCount(); i++) {
            View cocuk = grup.getChildAt(i);
            if (cocuk instanceof RadioButton && cocuk.getTag() != null
                    && hedef.equals(String.valueOf(cocuk.getTag()))) {
                grup.check(cocuk.getId());
                break;
            }
        }
        radyoStilleriniGuncelle(grup);
    }

    public static boolean saat24MuKullan(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences("AppAyarlari", Context.MODE_PRIVATE);
        return prefs.getBoolean("saatFormati24", DateFormat.is24HourFormat(context));
    }
}
