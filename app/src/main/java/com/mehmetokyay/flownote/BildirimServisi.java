package com.mehmetokyay.flownote;

import androidx.annotation.NonNull;

import com.mehmetokyay.flownote.util.BildirimMerkezi;
import com.mehmetokyay.flownote.util.FcmKayit;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BildirimServisi extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        FcmKayit.yaz(FirebaseAuth.getInstance().getCurrentUser().getUid(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        if (message.getData().isEmpty()) return;
        BildirimMerkezi.uzakBildirimiGoster(
                this,
                deger(message, "tip"),
                deger(message, "gonderenAd"),
                deger(message, "metin"),
                deger(message, "gonderenUid"),
                deger(message, "ilgiliId"));
    }

    private static String deger(RemoteMessage message, String anahtar) {
        String deger = message.getData().get(anahtar);
        return deger == null ? "" : deger;
    }
}
