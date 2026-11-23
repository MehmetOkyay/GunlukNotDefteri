package com.mehmetokyay.flownote.util;

import android.content.Context;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.database.UserDao;
import com.mehmetokyay.flownote.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EngelYoneticisi {

    private static final Object KILIT = new Object();

    private EngelYoneticisi() {
    }

    public static List<Integer> listeyiCoz(String ham) {
        Set<Integer> idler = new LinkedHashSet<>();
        if (ham != null) {
            for (String parca : ham.split(",")) {
                try {
                    int id = Integer.parseInt(parca.trim());
                    if (id > 0) idler.add(id);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new ArrayList<>(idler);
    }

    public static String listeyiBirlestir(Collection<Integer> idler) {
        StringBuilder sb = new StringBuilder();
        for (Integer id : idler) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        return sb.toString();
    }

    public static List<User> engellenenleriGetir(Context context, int aktifKullaniciId) {
        synchronized (KILIT) {
            UserDao dao = TaskDatabase.getInstance(context).userDao();
            List<User> sonuc = new ArrayList<>();
            User aktif = dao.idIleKullaniciGetir(aktifKullaniciId);
            if (aktif == null) return sonuc;

            List<Integer> idler = listeyiCoz(aktif.getEngellenenKullanicilar());
            List<Integer> gecerli = new ArrayList<>();
            for (int id : idler) {
                User u = dao.idIleKullaniciGetir(id);
                if (u != null) {
                    sonuc.add(u);
                    gecerli.add(id);
                }
            }
            if (gecerli.size() != idler.size()) {
                dao.engellenenleriGuncelle(aktifKullaniciId, listeyiBirlestir(gecerli));
            }
            return sonuc;
        }
    }

    public static boolean engelle(Context context, int aktifKullaniciId, int hedefKullaniciId) {
        if (hedefKullaniciId <= 0 || hedefKullaniciId == aktifKullaniciId) return false;
        synchronized (KILIT) {
            UserDao dao = TaskDatabase.getInstance(context).userDao();
            User aktif = dao.idIleKullaniciGetir(aktifKullaniciId);
            if (aktif == null || dao.idIleKullaniciGetir(hedefKullaniciId) == null) return false;

            List<Integer> idler = listeyiCoz(aktif.getEngellenenKullanicilar());
            if (!idler.contains(hedefKullaniciId)) idler.add(hedefKullaniciId);
            dao.engellenenleriGuncelle(aktifKullaniciId, listeyiBirlestir(idler));
            return true;
        }
    }

    public static void engeliKaldir(Context context, int aktifKullaniciId, int hedefKullaniciId) {
        synchronized (KILIT) {
            UserDao dao = TaskDatabase.getInstance(context).userDao();
            User aktif = dao.idIleKullaniciGetir(aktifKullaniciId);
            if (aktif == null) return;

            List<Integer> idler = listeyiCoz(aktif.getEngellenenKullanicilar());
            if (idler.remove(Integer.valueOf(hedefKullaniciId))) {
                dao.engellenenleriGuncelle(aktifKullaniciId, listeyiBirlestir(idler));
            }
        }
    }

    public static boolean arasindaEngelVar(User a, User b) {
        if (a == null || b == null) return false;
        return listeyiCoz(a.getEngellenenKullanicilar()).contains(b.getUserId())
                || listeyiCoz(b.getEngellenenKullanicilar()).contains(a.getUserId());
    }

    public static boolean engellendiMi(Context context, int aktifKullaniciId, int hedefKullaniciId) {
        synchronized (KILIT) {
            User aktif = TaskDatabase.getInstance(context).userDao().idIleKullaniciGetir(aktifKullaniciId);
            return aktif != null && listeyiCoz(aktif.getEngellenenKullanicilar()).contains(hedefKullaniciId);
        }
    }
}
