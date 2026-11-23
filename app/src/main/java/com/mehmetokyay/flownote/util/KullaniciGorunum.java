package com.mehmetokyay.flownote.util;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.mehmetokyay.flownote.model.User;

import java.util.Locale;

public final class KullaniciGorunum {

    private KullaniciGorunum() {
    }

    public static String gorunenAd(@Nullable User u) {
        if (u == null) return "";
        String isim = u.getUserIsim() != null ? u.getUserIsim().trim() : "";
        String soyisim = u.getUserSoyisim() != null ? u.getUserSoyisim().trim() : "";
        String tam = (isim + " " + soyisim).trim();
        return !tam.isEmpty() ? tam : kullaniciAdi(u);
    }

    public static String kullaniciAdi(@Nullable User u) {
        if (u == null || u.getKullaniciAdi() == null) return "";
        String ham = u.getKullaniciAdi().trim();
        while (ham.startsWith("@")) ham = ham.substring(1);
        return ham.isEmpty() ? "" : "@" + ham;
    }

    public static String ilkHarf(String metin) {
        if (metin == null) return "?";
        String temiz = metin.trim();
        while (temiz.startsWith("@")) temiz = temiz.substring(1);
        if (temiz.isEmpty()) return "?";
        int cp = temiz.codePointAt(0);
        return new String(Character.toChars(cp)).toUpperCase(Locale.getDefault());
    }

    public static void profilResmiYukle(ImageView iv, @Nullable User u) {
        String yol = u != null ? u.getProfilResmi() : null;
        if (yol != null && !yol.isEmpty()) {
            iv.setImageTintList(null);
            iv.setImageURI(Uri.parse(yol));
            if (iv.getDrawable() != null) return;
        }
        iv.setImageResource(android.R.drawable.ic_menu_camera);
        iv.setImageTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
    }

    public static void avatarBagla(ImageView resim, TextView basHarf, @Nullable User u) {
        String yol = u != null ? u.getProfilResmi() : null;
        if (yol != null && !yol.isEmpty()) {
            resim.setImageTintList(null);
            resim.setImageURI(Uri.parse(yol));
            if (resim.getDrawable() != null) {
                resim.setVisibility(View.VISIBLE);
                basHarf.setVisibility(View.GONE);
                return;
            }
        }
        resim.setImageDrawable(null);
        resim.setVisibility(View.GONE);
        basHarf.setVisibility(View.VISIBLE);
        basHarf.setText(ilkHarf(gorunenAd(u)));
    }

    public static String aramaIcinNormallestir(String metin) {
        if (metin == null) return "";
        return metin.trim().toLowerCase(new Locale("tr", "TR")).replace('ı', 'i');
    }
}
