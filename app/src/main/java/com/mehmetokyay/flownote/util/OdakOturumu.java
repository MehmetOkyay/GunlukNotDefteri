package com.mehmetokyay.flownote.util;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

public final class OdakOturumu {

    public static final OdakOturumu PAYLASILAN = new OdakOturumu();

    public interface Dinleyici {
        void guncelle();
    }

    public int mod;
    public boolean kronometre;
    public boolean calisiyor;
    public boolean kuruldu;
    public long kalan;
    public long toplam;
    public long kronometreMs;

    private Dinleyici dinleyici;
    private Context uygulama;
    private CountDownTimer geriSayim;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable kronometreAdimi = new Runnable() {
        @Override
        public void run() {
            if (!calisiyor || !kronometre) return;
            kronometreMs += 1000;
            haberVer();
            handler.postDelayed(this, 1000);
        }
    };

    private OdakOturumu() {
    }

    public void dinleyiciAta(Dinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }

    public void modaGec(int mod, long sure) {
        duraklat();
        this.mod = mod;
        kronometre = false;
        toplam = sure;
        kalan = sure;
        kuruldu = true;
        haberVer();
    }

    public void ozel(long sure) {
        duraklat();
        mod = -1;
        kronometre = false;
        toplam = sure;
        kalan = sure;
        kuruldu = true;
        haberVer();
    }

    public void kronometreyeGec() {
        duraklat();
        mod = -2;
        kronometre = true;
        kronometreMs = 0;
        kuruldu = true;
        haberVer();
    }

    public void dakikaKaydir(int yon) {
        if (calisiyor || kronometre) return;
        if (yon < 0 && kalan <= 60_000L) return;
        toplam += yon * 60_000L;
        kalan += yon * 60_000L;
        if (toplam < 60_000L) toplam = 60_000L;
        kuruldu = true;
        haberVer();
    }

    public void baslat(Context context) {
        uygulama = context.getApplicationContext();
        if (kronometre) {
            handler.removeCallbacks(kronometreAdimi);
            calisiyor = true;
            handler.postDelayed(kronometreAdimi, 1000);
            haberVer();
            return;
        }
        if (kalan <= 0) kalan = toplam;
        if (geriSayim != null) geriSayim.cancel();
        final long baslangic = kalan;
        geriSayim = new CountDownTimer(baslangic, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                kalan = millisUntilFinished;
                haberVer();
            }

            @Override
            public void onFinish() {
                calisiyor = false;
                kalan = 0;
                haberVer();
                if (dinleyici == null && uygulama != null) {
                    BildirimMerkezi.odakBitti(uygulama);
                }
            }
        }.start();
        calisiyor = true;
        haberVer();
    }

    public void duraklat() {
        calisiyor = false;
        handler.removeCallbacks(kronometreAdimi);
        if (geriSayim != null) {
            geriSayim.cancel();
            geriSayim = null;
        }
        haberVer();
    }

    public void sifirla() {
        duraklat();
        if (kronometre) kronometreMs = 0;
        else kalan = toplam;
        haberVer();
    }

    private void haberVer() {
        if (dinleyici != null) dinleyici.guncelle();
    }
}
