package com.mehmetokyay.flownote;

import android.app.Application;

import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.BildirimKontrolu;
import com.mehmetokyay.flownote.util.BildirimMerkezi;
import com.mehmetokyay.flownote.util.DilYoneticisi;
import com.mehmetokyay.flownote.util.GorevHatirlatma;
import com.mehmetokyay.flownote.util.TemaYoneticisi;

public class GunlukNotDefteriApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BulutSosyal.hazirla();
        DilYoneticisi.kayitliDiliUygula(this);
        TemaYoneticisi.kayitliTemayiUygula(this);
        BildirimMerkezi.kanallariHazirla(this);
        GorevHatirlatma.tumunuPlanla(this);
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                BildirimMerkezi.dinlemeyiDurdur();
                BildirimKontrolu.durdur(this);
            } else {
                BildirimMerkezi.dinlemeyiBaslat(this, user.getUid());
                BildirimKontrolu.planla(this);
            }
        });
    }
}
