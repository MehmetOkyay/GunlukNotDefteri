package com.mehmetokyay.flownote.util;

import android.content.Context;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.Begeni;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.model.Yorum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GonderiIslemleri {

    private GonderiIslemleri() {
    }

    public static final class AkisVerisi {
        public final int aktifKullaniciId;
        public final List<Post> gonderiler;
        public final Map<Integer, User> kullanicilar;
        public final Set<Integer> begenilenler;

        AkisVerisi(int aktifKullaniciId, List<Post> gonderiler, Map<Integer, User> kullanicilar, Set<Integer> begenilenler) {
            this.aktifKullaniciId = aktifKullaniciId;
            this.gonderiler = gonderiler;
            this.kullanicilar = kullanicilar;
            this.begenilenler = begenilenler;
        }
    }

    public static AkisVerisi akisVerisiHazirla(Context context, User aktif, List<Post> hamGonderiler) {
        TaskDatabase db = TaskDatabase.getInstance(context);

        Set<Integer> yazarIdleri = new HashSet<>();
        if (hamGonderiler != null) {
            for (Post p : hamGonderiler) yazarIdleri.add(p.getUserId());
        }
        Map<Integer, User> kullanicilar = new HashMap<>();
        if (!yazarIdleri.isEmpty()) {
            for (User u : db.userDao().idlerIleGetir(new ArrayList<>(yazarIdleri))) {
                kullanicilar.put(u.getUserId(), u);
            }
        }

        List<Post> gorunur = new ArrayList<>();
        if (hamGonderiler != null) {
            for (Post p : hamGonderiler) {
                User yazar = kullanicilar.get(p.getUserId());
                if (!EngelYoneticisi.arasindaEngelVar(aktif, yazar)) gorunur.add(p);
            }
        }

        Set<Integer> begenilenler = new HashSet<>(db.begeniDao().begenilenPostIdleri(aktif.getUserId()));
        return new AkisVerisi(aktif.getUserId(), gorunur, kullanicilar, begenilenler);
    }

    public static int begeniAyarla(Context context, int postId, int kullaniciId, boolean begen) {
        TaskDatabase db = TaskDatabase.getInstance(context);
        db.runInTransaction(() -> {
            if (begen) {
                Begeni b = new Begeni();
                b.setPostId(postId);
                b.setUserId(kullaniciId);
                b.setTimestamp(System.currentTimeMillis());
                db.begeniDao().begeniEkle(b);
            } else {
                db.begeniDao().begeniSil(postId, kullaniciId);
            }
            db.postDao().begeniSayisiniYenile(postId);
        });
        Post guncel = db.postDao().idIleGetir(postId);
        return guncel != null ? guncel.getBegeniSayisi() : 0;
    }

    public static int yorumEkle(Context context, int postId, int kullaniciId, String metin) {
        TaskDatabase db = TaskDatabase.getInstance(context);
        db.runInTransaction(() -> {
            Yorum y = new Yorum();
            y.setPostId(postId);
            y.setUserId(kullaniciId);
            y.setMetin(metin);
            y.setTimestamp(System.currentTimeMillis());
            db.yorumDao().yorumEkle(y);
            db.postDao().yorumSayisiniYenile(postId);
        });
        Post guncel = db.postDao().idIleGetir(postId);
        return guncel != null ? guncel.getYorumSayisi() : 0;
    }

    public static int yorumSil(Context context, int yorumId, int kullaniciId) {
        TaskDatabase db = TaskDatabase.getInstance(context);
        Yorum y = db.yorumDao().idIleGetir(yorumId);
        if (y == null) return -1;
        Post p = db.postDao().idIleGetir(y.getPostId());
        boolean yetkili = y.getUserId() == kullaniciId || (p != null && p.getUserId() == kullaniciId);
        if (!yetkili) return -1;

        db.runInTransaction(() -> {
            db.yorumDao().yorumSil(yorumId);
            db.postDao().yorumSayisiniYenile(y.getPostId());
        });
        Post guncel = db.postDao().idIleGetir(y.getPostId());
        return guncel != null ? guncel.getYorumSayisi() : 0;
    }

    public static boolean gonderiSil(Context context, int postId, int kullaniciId) {
        TaskDatabase db = TaskDatabase.getInstance(context);
        Post p = db.postDao().idIleGetir(postId);
        if (p == null || p.getUserId() != kullaniciId) return false;
        tamamenSil(db, postId);
        return true;
    }

    public static boolean moderasyonlaKaldir(Context context, int postId, long gonderiZamani) {
        TaskDatabase db = TaskDatabase.getInstance(context);
        Post p = db.postDao().idIleGetir(postId);
        if (p == null || p.getTimestamp() != gonderiZamani) return false;
        tamamenSil(db, postId);
        return true;
    }

    private static void tamamenSil(TaskDatabase db, int postId) {
        db.runInTransaction(() -> {
            db.begeniDao().gonderiBegenileriniSil(postId);
            db.yorumDao().gonderiYorumlariniSil(postId);
            db.postDao().gonderiSil(postId);
        });
    }
}
