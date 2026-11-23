package com.mehmetokyay.flownote.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class DilYoneticisi {

    private static final String TERCIHLER_ADI = "ayarlar_tercihleri";
    private static final String DIL_SECIMI = "uygulama_dili";

    public static final String TURKCE = "tr";
    public static final String ENGLISH = "en";
    public static final String DEUTSCH = "de";

    private DilYoneticisi() {
    }

    public static void kayitliDiliUygula(Context context) {
        android.content.SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(TERCIHLER_ADI, Context.MODE_PRIVATE);
        if (!prefs.contains(DIL_SECIMI)) {
            String ilkDil = eskiSecimiOku(context);
            prefs.edit().putString(DIL_SECIMI, ilkDil).apply();
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(ilkDil));
        } else {
            String dil = prefs.getString(DIL_SECIMI, TURKCE);
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(dil));
        }
    }

    private static String eskiSecimiOku(Context context) {
        String eski = context.getApplicationContext()
                .getSharedPreferences("AppAyarlari", Context.MODE_PRIVATE)
                .getString("language", null);
        if (eski == null) return TURKCE;
        if (eski.startsWith("English")) return ENGLISH;
        if (eski.startsWith("Deutsch")) return DEUTSCH;
        return TURKCE;
    }

    public static void diliDegistir(Context context, String dilKodu) {
        if (!TURKCE.equals(dilKodu) && !ENGLISH.equals(dilKodu) && !DEUTSCH.equals(dilKodu)) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(TERCIHLER_ADI, Context.MODE_PRIVATE)
                .edit().putString(DIL_SECIMI, dilKodu).apply();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(dilKodu));
    }

    public static String seciliDiliGetir(Context context) {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (!locales.isEmpty() && locales.get(0) != null) {
            String dil = locales.get(0).getLanguage();
            if (TURKCE.equals(dil) || ENGLISH.equals(dil) || DEUTSCH.equals(dil)) {
                return dil;
            }
        }
        return context.getApplicationContext()
                .getSharedPreferences(TERCIHLER_ADI, Context.MODE_PRIVATE)
                .getString(DIL_SECIMI, TURKCE);
    }
}
