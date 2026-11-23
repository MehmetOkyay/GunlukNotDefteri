package com.mehmetokyay.flownote.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class TemaYoneticisi {

    public static final int MOD_AYDINLIK = 0;
    public static final int MOD_KARANLIK = 1;
    public static final int MOD_SISTEM = 2;

    private static final int VARSAYILAN_MOD = MOD_AYDINLIK;

    private static final String TERCIHLER_ADI = "ayarlar_tercihleri";
    private static final String ANAHTAR_TEMA_MODU = "tema_modu";
    private static final String ANAHTAR_KARANLIK_TEMA = "karanlik_tema_aktif";

    private static final String ESKI_TERCIHLER_ADI = "AppAyarlari";
    private static final String ESKI_ANAHTAR_TEMA = "themeIndex";

    private static SharedPreferences tercihleriGetir(Context context) {
        return context.getApplicationContext().getSharedPreferences(TERCIHLER_ADI, Context.MODE_PRIVATE);
    }

    private static int gecerliMod(int mod) {
        return (mod == MOD_AYDINLIK || mod == MOD_KARANLIK || mod == MOD_SISTEM) ? mod : VARSAYILAN_MOD;
    }

    private static int nightModeKarsiligi(int mod) {
        switch (mod) {
            case MOD_KARANLIK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case MOD_SISTEM:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            default:
                return AppCompatDelegate.MODE_NIGHT_NO;
        }
    }

    public static int temaModuGetir(Context context) {
        SharedPreferences prefs = tercihleriGetir(context);
        if (prefs.contains(ANAHTAR_TEMA_MODU)) {
            return gecerliMod(prefs.getInt(ANAHTAR_TEMA_MODU, VARSAYILAN_MOD));
        }
        if (prefs.contains(ANAHTAR_KARANLIK_TEMA)) {
            return prefs.getBoolean(ANAHTAR_KARANLIK_TEMA, false) ? MOD_KARANLIK : MOD_AYDINLIK;
        }
        SharedPreferences eski = context.getApplicationContext()
                .getSharedPreferences(ESKI_TERCIHLER_ADI, Context.MODE_PRIVATE);
        if (eski.contains(ESKI_ANAHTAR_TEMA)) {
            return gecerliMod(eski.getInt(ESKI_ANAHTAR_TEMA, VARSAYILAN_MOD));
        }
        return VARSAYILAN_MOD;
    }

    public static void temaModunuKaydet(Context context, int mod) {
        tercihleriGetir(context).edit().putInt(ANAHTAR_TEMA_MODU, gecerliMod(mod)).apply();
    }

    public static void kayitliTemayiUygula(Context context) {
        AppCompatDelegate.setDefaultNightMode(nightModeKarsiligi(temaModuGetir(context)));
    }

    public static void temaModunuDegistir(Context context, int mod) {
        temaModunuKaydet(context, mod);
        AppCompatDelegate.setDefaultNightMode(nightModeKarsiligi(gecerliMod(mod)));
    }

    public static boolean karanlikTemaAktifMi(Context context) {
        return temaModuGetir(context) == MOD_KARANLIK;
    }

    public static void karanlikTemaTercihiKaydet(Context context, boolean aktif) {
        temaModunuKaydet(context, aktif ? MOD_KARANLIK : MOD_AYDINLIK);
    }

    public static void temayiDegistir(Context context, boolean karanlikAktif) {
        temaModunuDegistir(context, karanlikAktif ? MOD_KARANLIK : MOD_AYDINLIK);
    }
}
