package com.mehmetokyay.flownote.util;

import android.content.Context;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.database.UserDao;
import com.mehmetokyay.flownote.model.User;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public final class YerelKullanici {

    private static final Object KILIT = new Object();

    private YerelKullanici() {
    }

    public static String epostaNormallestir(String eposta) {
        return eposta == null ? null : eposta.trim().toLowerCase(Locale.ROOT);
    }

    public static User garantiEt(Context context, FirebaseUser firebaseUser) {
        if (firebaseUser == null) return null;
        String eposta = epostaNormallestir(firebaseUser.getEmail());
        if (eposta == null || eposta.isEmpty()) return null;

        synchronized (KILIT) {
            UserDao dao = TaskDatabase.getInstance(context).userDao();
            User mevcut = dao.emailIleKullaniciGetir(eposta);
            if (mevcut != null) return mevcut;

            User yeni = new User();
            yeni.setEmail(eposta);

            String tamAd = firebaseUser.getDisplayName();
            String yerelKisim = eposta.contains("@") ? eposta.substring(0, eposta.indexOf('@')) : eposta;
            if (tamAd != null && tamAd.trim().contains(" ")) {
                tamAd = tamAd.trim();
                yeni.setUserIsim(tamAd.substring(0, tamAd.lastIndexOf(' ')));
                yeni.setUserSoyisim(tamAd.substring(tamAd.lastIndexOf(' ') + 1));
            } else {
                yeni.setUserIsim(tamAd != null && !tamAd.trim().isEmpty() ? tamAd.trim() : yerelKisim);
                yeni.setUserSoyisim("");
            }
            yeni.setKullaniciAdi("@" + yerelKisim);
            dao.kisiEkle(yeni);
            return dao.emailIleKullaniciGetir(eposta);
        }
    }

    public static void ekleVeyaGuncelle(Context context, User yeni) {
        synchronized (KILIT) {
            UserDao dao = TaskDatabase.getInstance(context).userDao();
            User mevcut = dao.emailIleKullaniciGetir(yeni.getEmail());
            if (mevcut != null) {
                yeni.setUserId(mevcut.getUserId());
                yeni.setTakipciSayisi(mevcut.getTakipciSayisi());
                yeni.setTakipEdilenSayisi(mevcut.getTakipEdilenSayisi());
                yeni.setEngellenenKullanicilar(mevcut.getEngellenenKullanicilar());
                dao.kisiGuncelle(yeni);
            } else {
                dao.kisiEkle(yeni);
            }
        }
    }
}
