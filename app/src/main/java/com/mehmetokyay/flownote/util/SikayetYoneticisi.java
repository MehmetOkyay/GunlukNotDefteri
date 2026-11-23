package com.mehmetokyay.flownote.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SikayetYoneticisi {

    private static final String TAG = "SikayetYoneticisi";
    private static final String KOLEKSIYON = "sikayetler";
    private static final String PREF = "sikayet_tercihleri";
    private static final String PREF_CIHAZ = "cihaz_kimligi";

    public static final String DURUM_BEKLEMEDE = "beklemede";
    public static final String DURUM_KALDIRILDI = "kaldirildi";

    public static final String[] SEBEP_KODLARI = {
            "spam", "taciz", "nefret_soylemi", "siddet", "uygunsuz_icerik", "yanlis_bilgi", "diger"
    };

    public interface SonucDinleyici {
        void sonuc(boolean basarili);
    }

    private SikayetYoneticisi() {
    }

    public static synchronized String cihazKimligi(Context context) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String kimlik = sp.getString(PREF_CIHAZ, null);
        if (kimlik == null) {
            kimlik = UUID.randomUUID().toString();
            sp.edit().putString(PREF_CIHAZ, kimlik).apply();
        }
        return kimlik;
    }

    public static void sikayetEt(Context context, Post post, User yazar, User sikayetEden, String firebaseUid,
                                 int sebepIndeksi, SonucDinleyici dinleyici) {
        String cihaz = cihazKimligi(context);
        String belgeId = cihaz + "_" + post.getId() + "_" + sikayetEden.getUserId();

        Map<String, Object> veri = new HashMap<>();
        veri.put("durum", DURUM_BEKLEMEDE);
        veri.put("sebep", SEBEP_KODLARI[sebepIndeksi]);
        veri.put("gonderiMetni", post.getIcerik() != null ? post.getIcerik() : "");
        veri.put("gonderiResimVar", post.getResimUri() != null && !post.getResimUri().isEmpty());
        veri.put("gonderiId", post.getId());
        veri.put("gonderiZamani", post.getTimestamp());
        veri.put("yazarId", post.getUserId());
        veri.put("yazarAd", KullaniciGorunum.gorunenAd(yazar));
        veri.put("yazarKullaniciAdi", KullaniciGorunum.kullaniciAdi(yazar));
        veri.put("yazarEposta", yazar != null && yazar.getEmail() != null ? yazar.getEmail() : "");
        veri.put("sikayetEdenUid", firebaseUid);
        veri.put("sikayetEdenKullaniciAdi", KullaniciGorunum.kullaniciAdi(sikayetEden));
        veri.put("cihazId", cihaz);
        veri.put("olusturulma", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection(KOLEKSIYON).document(belgeId)
                .set(veri, SetOptions.merge())
                .addOnSuccessListener(v -> dinleyici.sonuc(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Şikayet gönderilemedi", e);
                    dinleyici.sonuc(false);
                });
    }

    public static void kaldirilanlariUygula(Context context, SonucDinleyici dinleyici) {
        final Context appContext = context.getApplicationContext();
        FirebaseFirestore.getInstance().collection(KOLEKSIYON)
                .whereEqualTo("cihazId", cihazKimligi(appContext))
                .whereEqualTo("durum", DURUM_KALDIRILDI)
                .get()
                .addOnSuccessListener(sorgu -> {
                    final List<DocumentSnapshot> belgeler = sorgu.getDocuments();
                    if (belgeler.isEmpty()) return;
                    TaskDatabase.ARKA_PLAN.execute(() -> {
                        boolean silindi = false;
                        for (DocumentSnapshot b : belgeler) {
                            Object hamId = b.get("gonderiId");
                            Object hamZaman = b.get("gonderiZamani");
                            if (!(hamId instanceof Number) || !(hamZaman instanceof Number)) continue;
                            int id = ((Number) hamId).intValue();
                            long zaman = ((Number) hamZaman).longValue();
                            try {
                                silindi |= GonderiIslemleri.moderasyonlaKaldir(appContext, id, zaman);
                            } catch (Exception e) {
                                Log.e(TAG, "Kaldırılan gönderi silinemedi: " + b.getId(), e);
                            }
                        }
                        if (silindi) {
                            new Handler(Looper.getMainLooper()).post(() -> dinleyici.sonuc(true));
                        }
                    });
                })
                .addOnFailureListener(e -> Log.w(TAG, "Moderasyon kararları alınamadı", e));
    }
}
