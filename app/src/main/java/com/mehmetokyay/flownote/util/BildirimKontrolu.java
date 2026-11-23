package com.mehmetokyay.flownote.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BildirimKontrolu extends Worker {

    private static final String IS = "sosyal_bildirim_kontrolu";

    public BildirimKontrolu(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void planla(Context context) {
        Constraints ag = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest istek = new PeriodicWorkRequest.Builder(BildirimKontrolu.class, 15, TimeUnit.MINUTES)
                .setConstraints(ag)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(IS, ExistingPeriodicWorkPolicy.KEEP, istek);
    }

    public static void durdur(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(IS);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.success();
        CountDownLatch kapi = new CountDownLatch(1);
        BulutSosyal.bildirimleriGetir(user.getUid(), (sorgu, hata) -> {
            try {
                if (sorgu == null) return;
                for (DocumentSnapshot d : sorgu.getDocuments()) {
                    if (Boolean.TRUE.equals(d.getBoolean("okundu"))) continue;
                    if (!BildirimMerkezi.gosterilecekMi(getApplicationContext(), d.getId())) continue;
                    Object ad = d.get("gonderenAd");
                    Object metin = d.get("metin");
                    Object tip = d.get("tip");
                    Object gonderen = d.get("gonderenUid");
                    Object ilgili = d.get("ilgiliId");
                    BildirimMerkezi.uzakBildirimiGoster(
                            getApplicationContext(),
                            tip instanceof String ? (String) tip : "",
                            ad instanceof String ? (String) ad : "",
                            metin instanceof String ? (String) metin : "",
                            gonderen instanceof String ? (String) gonderen : "",
                            ilgili instanceof String ? (String) ilgili : "");
                }
            } finally {
                kapi.countDown();
            }
        });
        try {
            kapi.await(25, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }
        return Result.success();
    }
}
