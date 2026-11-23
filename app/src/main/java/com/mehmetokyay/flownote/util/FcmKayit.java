package com.mehmetokyay.flownote.util;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public final class FcmKayit {

    private FcmKayit() {
    }

    public static void yenile(String uid) {
        if (uid == null || uid.isEmpty()) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> yaz(uid, token));
    }

    public static void yaz(String uid, String token) {
        if (uid == null || uid.isEmpty() || token == null || token.isEmpty()) return;
        Map<String, Object> veri = new HashMap<>();
        veri.put("token", token);
        veri.put("guncelleme", System.currentTimeMillis());
        FirebaseFirestore.getInstance()
                .collection("kullanicilar").document(uid)
                .collection("cihaz").document("ana")
                .set(veri, SetOptions.merge());
    }
}
