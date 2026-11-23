package com.mehmetokyay.flownote.util;

import com.mehmetokyay.flownote.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class GorevYardimcisi {

    public static final String TEKRAR_YOK = "none";
    public static final String TEKRAR_GUNLUK = "daily";
    public static final String TEKRAR_HAFTALIK = "weekly";
    public static final String TEKRAR_AYLIK = "monthly";
    public static final String TEKRAR_YILLIK = "yearly";

    private GorevYardimcisi() {
    }

    public static String tekrarTuru(String saklanan) {
        if (saklanan == null) return TEKRAR_YOK;
        String s = saklanan.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return TEKRAR_YOK;

        if (s.equals(TEKRAR_GUNLUK) || s.contains("günlük") || s.contains("gunluk")
                || s.contains("her gün") || s.contains("her gun")
                || s.contains("täglich") || s.contains("taglich") || s.contains("taeglich")) {
            return TEKRAR_GUNLUK;
        }
        if (s.equals(TEKRAR_HAFTALIK) || s.contains("haftalık") || s.contains("haftalik")
                || s.contains("her hafta") || s.contains("wöchentlich")
                || s.contains("wochentlich") || s.contains("woechentlich")) {
            return TEKRAR_HAFTALIK;
        }
        if (s.equals(TEKRAR_AYLIK) || s.contains("aylık") || s.contains("aylik")
                || s.contains("monatlich")) {
            return TEKRAR_AYLIK;
        }
        if (s.equals(TEKRAR_YILLIK) || s.contains("yıllık") || s.contains("yillik")
                || s.contains("jährlich") || s.contains("jahrlich") || s.contains("jaehrlich")) {
            return TEKRAR_YILLIK;
        }
        return TEKRAR_YOK;
    }

    public static boolean tekrarliMi(Task task) {
        return !TEKRAR_YOK.equals(tekrarTuru(task.getYinelemeDurumu()));
    }

    public static int[] tarihAyristir(String metin) {
        if (metin == null) return null;
        String[] p = metin.trim().split("\\.");
        if (p.length != 3) return null;
        try {
            int gun = Integer.parseInt(p[0].trim());
            int ay = Integer.parseInt(p[1].trim());
            int yil = Integer.parseInt(p[2].trim());
            if (yil < 1 || yil > 9999 || ay < 1 || ay > 12) return null;
            if (gun < 1 || gun > ayGunSayisi(yil, ay)) return null;
            return new int[]{gun, ay, yil};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String tarihMetni(int gun, int ay, int yil) {
        return gun + "." + ay + "." + yil;
    }

    public static String tarihMetni(Calendar takvim) {
        return tarihMetni(takvim.get(Calendar.DAY_OF_MONTH),
                takvim.get(Calendar.MONTH) + 1, takvim.get(Calendar.YEAR));
    }

    public static String tarihNormallestir(String metin) {
        int[] t = tarihAyristir(metin);
        if (t == null) return metin == null ? "" : metin.trim();
        return tarihMetni(t[0], t[1], t[2]);
    }

    public static boolean artikYil(int yil) {
        return (yil % 4 == 0 && yil % 100 != 0) || yil % 400 == 0;
    }

    public static int ayGunSayisi(int yil, int ay) {
        switch (ay) {
            case 2:
                return artikYil(yil) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    private static long epokGunu(int yil, int ay, int gun) {
        long y = yil - (ay <= 2 ? 1 : 0);
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153L * (ay + (ay > 2 ? -3 : 9)) + 2) / 5 + gun - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    public static boolean gorevGundeMi(Task task, int gun, int ay, int yil) {
        int[] b = tarihAyristir(task.getGorevTarihi());
        if (b == null) return false;

        long hedef = epokGunu(yil, ay, gun);
        long baslangic = epokGunu(b[2], b[1], b[0]);
        if (hedef < baslangic) return false;

        switch (tekrarTuru(task.getYinelemeDurumu())) {
            case TEKRAR_GUNLUK:
                return true;
            case TEKRAR_HAFTALIK:
                return (hedef - baslangic) % 7 == 0;
            case TEKRAR_AYLIK:
                return gun == Math.min(b[0], ayGunSayisi(yil, ay));
            case TEKRAR_YILLIK:
                return ay == b[1] && gun == Math.min(b[0], ayGunSayisi(yil, ay));
            default:
                return hedef == baslangic;
        }
    }

    public static boolean gorevGundeMi(Task task, Calendar gun) {
        return gorevGundeMi(task, gun.get(Calendar.DAY_OF_MONTH),
                gun.get(Calendar.MONTH) + 1, gun.get(Calendar.YEAR));
    }

    private static List<String> tamamlananlariOku(String ham) {
        List<String> liste = new ArrayList<>();
        if (ham == null || ham.trim().isEmpty()) return liste;
        for (String parca : ham.split(",")) {
            String t = tarihNormallestir(parca);
            if (!t.isEmpty() && !liste.contains(t)) liste.add(t);
        }
        return liste;
    }

    private static String tamamlananlariYaz(List<String> liste) {
        StringBuilder sb = new StringBuilder();
        for (String t : liste) sb.append(t).append(',');
        return sb.toString();
    }

    public static boolean tamamlandiMi(Task task, String tarihMetni) {
        List<String> liste = tamamlananlariOku(task.getTamamlananTarihler());
        if (tekrarliMi(task)) {
            return liste.contains(tarihNormallestir(tarihMetni));
        }
        return task.getGorevTamamlandiMi()
                || liste.contains(tarihNormallestir(task.getGorevTarihi()));
    }

    public static void tamamlanmayiDegistir(Task task, String tarihMetni) {
        boolean suAnTamam = tamamlandiMi(task, tarihMetni);
        List<String> liste = tamamlananlariOku(task.getTamamlananTarihler());

        if (tekrarliMi(task)) {
            String gun = tarihNormallestir(tarihMetni);
            if (suAnTamam) liste.remove(gun);
            else liste.add(gun);
        } else {
            task.setGorevTamamlandiMi(!suAnTamam);
            liste.remove(tarihNormallestir(task.getGorevTarihi()));
        }
        task.setTamamlananTarihler(tamamlananlariYaz(liste));
    }
}
