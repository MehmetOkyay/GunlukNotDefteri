package com.mehmetokyay.flownote.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.mehmetokyay.flownote.HatirlatmaAlici;
import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.Task;

import java.util.Calendar;
import java.util.List;

public final class GorevHatirlatma {

    public static final String EKSTRA_GOREV_ID = "gorevId";

    private GorevHatirlatma() {
    }

    public static void tumunuPlanla(Context context) {
        Context app = context.getApplicationContext();
        TaskDatabase.ARKA_PLAN.execute(() -> {
            try {
                List<Task> gorevler = TaskDatabase.getInstance(app).taskDao().hepsiniListele();
                if (gorevler == null) return;
                for (Task gorev : gorevler) planla(app, gorev);
            } catch (Exception ignored) {
            }
        });
    }

    public static void planla(Context context, Task gorev) {
        if (gorev == null || gorev.getId() <= 0) return;
        iptal(context, gorev.getId());
        long an = sonrakiAn(gorev);
        if (an < 0) return;
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        PendingIntent bekleyen = bekleyen(context, gorev.getId());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, an, bekleyen);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, an, bekleyen);
            } else {
                alarm.setExact(AlarmManager.RTC_WAKEUP, an, bekleyen);
            }
        } catch (SecurityException e) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, an, bekleyen);
        }
    }

    public static void iptal(Context context, long gorevId) {
        if (gorevId <= 0) return;
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        alarm.cancel(bekleyen(context, gorevId));
    }

    public static long sonrakiAn(Task gorev) {
        if (gorev == null || !gorev.isHatirlat()) return -1;
        int[] baslangic = GorevYardimcisi.tarihAyristir(gorev.getGorevTarihi());
        if (baslangic == null) return -1;
        int[] saat = saatAyristir(gorev.getGorevSaati());

        Calendar gun = Calendar.getInstance();
        gun.set(Calendar.YEAR, baslangic[2]);
        gun.set(Calendar.MONTH, baslangic[1] - 1);
        gun.set(Calendar.DAY_OF_MONTH, baslangic[0]);
        gun.set(Calendar.HOUR_OF_DAY, 0);
        gun.set(Calendar.MINUTE, 0);
        gun.set(Calendar.SECOND, 0);
        gun.set(Calendar.MILLISECOND, 0);

        long simdi = System.currentTimeMillis();
        boolean tekrarli = GorevYardimcisi.tekrarliMi(gorev);
        for (int i = 0; i < 400; i++) {
            if (GorevYardimcisi.gorevGundeMi(gorev, gun)) {
                String tarih = GorevYardimcisi.tarihMetni(gun);
                if (!GorevYardimcisi.tamamlandiMi(gorev, tarih)) {
                    Calendar an = (Calendar) gun.clone();
                    an.set(Calendar.HOUR_OF_DAY, saat[0]);
                    an.set(Calendar.MINUTE, saat[1]);
                    if (an.getTimeInMillis() > simdi + 1500L) return an.getTimeInMillis();
                }
            }
            if (!tekrarli) break;
            gun.add(Calendar.DAY_OF_MONTH, 1);
        }
        return -1;
    }

    private static int[] saatAyristir(String metin) {
        int saat = 9;
        int dakika = 0;
        if (metin != null && metin.contains(":")) {
            String[] parca = metin.split(":");
            try {
                saat = Integer.parseInt(parca[0].trim());
                dakika = parca.length > 1 ? Integer.parseInt(parca[1].trim()) : 0;
            } catch (NumberFormatException ignored) {
            }
        }
        if (saat < 0 || saat > 23) saat = 9;
        if (dakika < 0 || dakika > 59) dakika = 0;
        return new int[]{saat, dakika};
    }

    private static PendingIntent bekleyen(Context context, long gorevId) {
        Intent intent = new Intent(context, HatirlatmaAlici.class);
        intent.putExtra(EKSTRA_GOREV_ID, gorevId);
        int kod = (int) (gorevId & 0x7fffffff);
        return PendingIntent.getBroadcast(context, kod, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
