package com.mehmetokyay.flownote.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.mehmetokyay.flownote.BildirimlerActivity;
import com.mehmetokyay.flownote.KullaniciProfilActivity;
import com.mehmetokyay.flownote.MainActivity;
import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.SohbetActivity;
import com.mehmetokyay.flownote.model.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;

public final class BildirimMerkezi {

    public static final String TIP_TAKIP = "takip";
    public static final String TIP_BEGENI = "begeni";
    public static final String TIP_YORUM = "yorum";
    public static final String TIP_MESAJ = "mesaj";

    private static final String KANAL_GOREV = "gorev_hatirlatma";
    private static final String KANAL_SOSYAL = "sosyal_bildirim";

    private static ListenerRegistration dinleyici;
    private static boolean ilkPaket = true;

    private BildirimMerkezi() {
    }

    public static void kanallariHazirla(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager yonetici = context.getSystemService(NotificationManager.class);
        if (yonetici == null) return;
        NotificationChannel gorev = new NotificationChannel(KANAL_GOREV,
                context.getString(R.string.bildirim_gorev_kanal), NotificationManager.IMPORTANCE_HIGH);
        gorev.setDescription(context.getString(R.string.bildirim_gorev_kanal_aciklama));
        NotificationChannel sosyal = new NotificationChannel(KANAL_SOSYAL,
                context.getString(R.string.bildirim_sosyal_kanal), NotificationManager.IMPORTANCE_HIGH);
        sosyal.setDescription(context.getString(R.string.bildirim_sosyal_kanal_aciklama));
        yonetici.createNotificationChannel(gorev);
        yonetici.createNotificationChannel(sosyal);
    }

    public static boolean izinVar(Context context) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void gorevBildirimiGoster(Context context, Task gorev) {
        if (!izinVar(context) || gorev == null) return;
        kanallariHazirla(context);
        Intent ac = new Intent(context, MainActivity.class);
        ac.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent bekleyen = PendingIntent.getActivity(context, (int) (gorev.getId() & 0x7fffffff), ac,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String metin = gorev.getGorevSaati() == null ? "" : gorev.getGorevSaati();
        if (gorev.getGorevAciklama() != null && !gorev.getGorevAciklama().trim().isEmpty()) {
            metin = metin.isEmpty() ? gorev.getGorevAciklama() : metin + " · " + gorev.getGorevAciklama();
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, KANAL_GOREV)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(gorev.getGorevBaslik() == null ? context.getString(R.string.gorev_zamani) : gorev.getGorevBaslik())
                .setContentText(metin.isEmpty() ? context.getString(R.string.gorev_zamani) : metin)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(metin))
                .setContentIntent(bekleyen)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(context).notify((int) (gorev.getId() & 0x7fffffff), builder.build());
    }

    public static void dinlemeyiBaslat(Context context, String uid) {
        dinlemeyiDurdur();
        if (uid == null || uid.isEmpty()) return;
        Context app = context.getApplicationContext();
        kanallariHazirla(app);
        ilkPaket = true;
        dinleyici = BulutSosyal.bildirimAkisi(uid, (anlik, hata) -> {
                    if (anlik == null || hata != null) return;
                    if (ilkPaket) {
                        ilkPaket = false;
                        return;
                    }
                    for (DocumentChange degisim : anlik.getDocumentChanges()) {
                        if (degisim.getType() != DocumentChange.Type.ADDED) continue;
                        if (Boolean.TRUE.equals(degisim.getDocument().getBoolean("okundu"))) continue;
                        String belgeId = degisim.getDocument().getId();
                        if (!gosterilecekMi(app, belgeId)) continue;
                        sosyalGoster(app, belgeId,
                                metin(degisim.getDocument().getString("tip")),
                                degisim.getDocument().getString("gonderenAd"),
                                degisim.getDocument().getString("metin"),
                                degisim.getDocument().getString("gonderenUid"),
                                degisim.getDocument().getString("ilgiliId"));
                    }
                });
    }

    public static boolean gosterilecekMi(Context context, String belgeId) {
        if (belgeId == null || belgeId.isEmpty()) return false;
        android.content.SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences("gosterilen_bildirimler", Context.MODE_PRIVATE);
        java.util.Set<String> eski = prefs.getStringSet("idler", java.util.Collections.emptySet());
        if (eski.contains(belgeId)) return false;
        java.util.Set<String> yeni = new java.util.HashSet<>(eski);
        if (yeni.size() > 200) yeni.clear();
        yeni.add(belgeId);
        prefs.edit().putStringSet("idler", yeni).apply();
        return true;
    }

    private static String metin(String tip) {
        return tip == null ? "" : tip;
    }

    public static void uzakBildirimiGoster(Context context, String tip, String ad, String govde,
                                           String gonderenUid, String ilgiliId) {
        String kod = (tip == null ? "" : tip) + "|" + (gonderenUid == null ? "" : gonderenUid) + "|" + System.currentTimeMillis();
        sosyalGoster(context, kod, tip, ad, govde, gonderenUid, ilgiliId);
    }

    private static void sosyalGoster(Context context, String belgeId, String tip, String ad, String govde,
                                    String gonderenUid, String ilgiliId) {
        if (!izinVar(context)) return;
        String isim = ad == null || ad.isEmpty() ? context.getString(R.string.bilinmeyen_kullanici) : ad;
        int baslikSablon;
        if (TIP_TAKIP.equals(tip)) baslikSablon = R.string.bildirim_takip;
        else if (TIP_BEGENI.equals(tip)) baslikSablon = R.string.bildirim_begeni;
        else if (TIP_YORUM.equals(tip)) baslikSablon = R.string.bildirim_yorum;
        else if (TIP_MESAJ.equals(tip)) baslikSablon = R.string.bildirim_mesaj;
        else return;
        String baslik = context.getString(baslikSablon, isim);
        String icerik = govde == null ? "" : govde.trim();
        Intent ac;
        if (TIP_MESAJ.equals(tip)) {
            String sohbetUid = ilgiliId != null && !ilgiliId.isEmpty() ? ilgiliId : gonderenUid;
            ac = new Intent(context, SohbetActivity.class);
            ac.putExtra("karsiKullaniciId", sohbetUid == null ? "" : sohbetUid);
        } else if (gonderenUid != null && !gonderenUid.isEmpty()) {
            ac = new Intent(context, KullaniciProfilActivity.class);
            ac.putExtra("kullaniciId", gonderenUid);
        } else {
            ac = new Intent(context, BildirimlerActivity.class);
        }
        ac.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int kod = belgeId == null ? 0 : belgeId.hashCode();
        PendingIntent bekleyen = PendingIntent.getActivity(context, kod, ac,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, KANAL_SOSYAL)
                .setSmallIcon(TIP_MESAJ.equals(tip)
                        ? android.R.drawable.ic_dialog_email
                        : android.R.drawable.ic_dialog_info)
                .setContentTitle(baslik)
                .setContentText(icerik.isEmpty() ? baslik : icerik)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(icerik.isEmpty() ? baslik : icerik))
                .setContentIntent(bekleyen)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(context).notify(kod, builder.build());
    }

    public static void odakBitti(Context context) {
        if (!izinVar(context)) return;
        kanallariHazirla(context);
        Intent ac = new Intent(context, MainActivity.class);
        ac.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent bekleyen = PendingIntent.getActivity(context, 42, ac,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, KANAL_SOSYAL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(context.getString(R.string.odak_bitti))
                .setContentText(context.getString(R.string.odak_bitti_metin))
                .setContentIntent(bekleyen)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(context).notify(42, builder.build());
    }

    public static void dinlemeyiDurdur() {
        if (dinleyici != null) {
            dinleyici.remove();
            dinleyici = null;
        }
        ilkPaket = true;
    }
}
