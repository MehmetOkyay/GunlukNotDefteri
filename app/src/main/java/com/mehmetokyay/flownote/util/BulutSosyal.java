package com.mehmetokyay.flownote.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.BulutProfil;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.model.Yorum;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BulutSosyal {

    public static final String DURUM_YAYINDA = "yayinda";

    public interface Sonuc<T> {
        void bitti(@Nullable T veri, @Nullable Exception hata);
    }

    public static final class Akis {
        public final List<Post> gonderiler;
        public final Set<String> begenilenler;

        public Akis(List<Post> gonderiler, Set<String> begenilenler) {
            this.gonderiler = gonderiler;
            this.begenilenler = begenilenler;
        }
    }

    public static final class KonusmaOzet {
        public String karsiUid = "";
        public String ad = "";
        public String kullaniciAdi = "";
        public String resimUrl = "";
        public String sonMesaj = "";
        public long zaman;
        public int okunmamis;
        public boolean sonBenim;
    }

    public static final class SohbetMesaji {
        public String gonderenUid = "";
        public String metin = "";
        public long zaman;
        public boolean okundu;
    }

    private static boolean hazir;

    private BulutSosyal() {
    }

    public static void hazirla() {
        if (hazir) return;
        hazir = true;
        try {
            FirebaseFirestore.getInstance().setFirestoreSettings(
                    new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build());
        } catch (RuntimeException ignored) {
        }
    }

    private static FirebaseFirestore db() {
        hazirla();
        return FirebaseFirestore.getInstance();
    }

    public static String sohbetId(String a, String b) {
        return a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a;
    }

    public static void hesabiEsle(Context context, FirebaseUser firebase, User yerel) throws Exception {
        if (firebase == null || yerel == null) return;
        DocumentSnapshot snap = Tasks.await(db().collection("kullanicilar").document(firebase.getUid()).get());
        if (snap.exists()) {
            BulutProfil p = profilden(snap);
            yerel.setUserIsim(p.getIsim());
            yerel.setUserSoyisim(p.getSoyisim());
            yerel.setKullaniciAdi(p.getKullaniciAdi());
            yerel.setMeslek(p.getMeslek());
            yerel.setKonum(p.getKonum());
            yerel.setWebSitesi(p.getWebSitesi());
            yerel.setBiyografi(p.getBiyografi());
            if (p.getProfilResmiUrl() != null && !p.getProfilResmiUrl().isEmpty()) {
                yerel.setProfilResmi(p.getProfilResmiUrl());
            }
            TaskDatabase.getInstance(context).userDao().kullaniciGuncelle(yerel);
        } else {
            Tasks.await(profilYaz(firebase.getUid(), yerel, yerel.getProfilResmi()));
        }
    }

    public static void profiliYayinla(Context context, User yerel, String uid, @Nullable String resimYoluVeyaUrl, Sonuc<Boolean> sonuc) {
        if (resimYoluVeyaUrl != null && !resimYoluVeyaUrl.isEmpty()
                && !resimYoluVeyaUrl.startsWith("http")) {
            ResimYukleyici.yukle(context, Uri.fromFile(new java.io.File(resimYoluVeyaUrl)),
                    "profiller/" + uid + "/avatar.jpg", (url, hata) -> {
                        if (hata != null || url == null) {
                            sonuc.bitti(false, hata);
                            return;
                        }
                        yerel.setProfilResmi(url);
                        profilYaz(uid, yerel, url)
                                .addOnSuccessListener(v -> sonuc.bitti(true, null))
                                .addOnFailureListener(e -> sonuc.bitti(false, e));
                    });
            return;
        }
        profilYaz(uid, yerel, resimYoluVeyaUrl)
                .addOnSuccessListener(v -> sonuc.bitti(true, null))
                .addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    private static Task<Void> profilYaz(String uid, User yerel, @Nullable String resim) {
        Map<String, Object> veri = new HashMap<>();
        veri.put("uid", uid);
        veri.put("isim", bos(yerel.getUserIsim()));
        veri.put("soyisim", bos(yerel.getUserSoyisim()));
        veri.put("kullaniciAdi", bos(yerel.getKullaniciAdi()));
        veri.put("kullaniciAdiArama", KullaniciGorunum.aramaIcinNormallestir(yerel.getKullaniciAdi()));
        veri.put("isimArama", KullaniciGorunum.aramaIcinNormallestir(
                bos(yerel.getUserIsim()) + " " + bos(yerel.getUserSoyisim())));
        veri.put("meslek", bos(yerel.getMeslek()));
        veri.put("konum", bos(yerel.getKonum()));
        veri.put("webSitesi", bos(yerel.getWebSitesi()));
        veri.put("biyografi", bos(yerel.getBiyografi()));
        veri.put("profilResmiUrl", httpIse(resim));
        return db().collection("kullanicilar").document(uid).set(veri, SetOptions.merge());
    }

    public static boolean kullaniciAdiAlinmis(String kullaniciAdi, String benimUid) throws Exception {
        String aranan = KullaniciGorunum.aramaIcinNormallestir(kullaniciAdi);
        QuerySnapshot q = Tasks.await(db().collection("kullanicilar")
                .whereEqualTo("kullaniciAdiArama", aranan).limit(2).get());
        for (DocumentSnapshot d : q.getDocuments()) {
            if (!d.getId().equals(benimUid)) return true;
        }
        return false;
    }

    public static void profilGetir(String uid, Sonuc<BulutProfil> sonuc) {
        db().collection("kullanicilar").document(uid).get()
                .addOnSuccessListener(d -> sonuc.bitti(d.exists() ? profilden(d) : null, null))
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void kullanicilariGetir(String benimUid, Sonuc<List<BulutProfil>> sonuc) {
        Task<QuerySnapshot> kisiler = db().collection("kullanicilar").limit(200).get();
        Task<QuerySnapshot> engelledim = db().collection("engeller").whereEqualTo("eden", benimUid).get();
        Task<QuerySnapshot> engelledi = db().collection("engeller").whereEqualTo("hedef", benimUid).get();
        Tasks.whenAllSuccess(kisiler, engelledim, engelledi)
                .addOnSuccessListener(list -> {
                    Set<String> gizli = new HashSet<>();
                    for (DocumentSnapshot d : engelledim.getResult()) gizli.add(str(d, "hedef"));
                    for (DocumentSnapshot d : engelledi.getResult()) gizli.add(str(d, "eden"));
                    List<BulutProfil> liste = new ArrayList<>();
                    for (DocumentSnapshot d : kisiler.getResult()) {
                        if (gizli.contains(d.getId())) continue;
                        liste.add(profilden(d));
                    }
                    sonuc.bitti(liste, null);
                })
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void gonderiPaylas(Context context, String uid, String metin, @Nullable Uri resim,
                                     User yazar, Sonuc<Boolean> sonuc) {
        DocumentReference ref = db().collection("gonderiler").document();
        if (resim == null) {
            gonderiYaz(ref, uid, metin, "", yazar)
                    .addOnSuccessListener(v -> sonuc.bitti(true, null))
                    .addOnFailureListener(e -> sonuc.bitti(false, e));
            return;
        }
        ResimYukleyici.yukle(context, resim, "gonderiler/" + uid + "/" + ref.getId() + ".jpg", (url, hata) -> {
            if (hata != null || url == null) {
                sonuc.bitti(false, hata);
                return;
            }
            gonderiYaz(ref, uid, metin, url, yazar)
                    .addOnSuccessListener(v -> sonuc.bitti(true, null))
                    .addOnFailureListener(e -> sonuc.bitti(false, e));
        });
    }

    private static Task<Void> gonderiYaz(DocumentReference ref, String uid, String metin, String resimUrl, User yazar) {
        Map<String, Object> veri = new HashMap<>();
        veri.put("uid", uid);
        veri.put("icerik", metin);
        veri.put("resimUrl", resimUrl);
        veri.put("begeniSayisi", 0);
        veri.put("yorumSayisi", 0);
        veri.put("zaman", System.currentTimeMillis());
        veri.put("durum", DURUM_YAYINDA);
        veri.put("yazarAd", KullaniciGorunum.gorunenAd(yazar));
        veri.put("yazarKullaniciAdi", KullaniciGorunum.kullaniciAdi(yazar));
        veri.put("yazarResimUrl", httpIse(yazar.getProfilResmi()));
        return ref.set(veri);
    }

    public static void akisGetir(String uid, Sonuc<Akis> sonuc) {
        Task<QuerySnapshot> gonderiler = db().collection("gonderiler")
                .orderBy("zaman", Query.Direction.DESCENDING).limit(80).get();
        Task<QuerySnapshot> engelledim = db().collection("engeller").whereEqualTo("eden", uid).get();
        Task<QuerySnapshot> engelledi = db().collection("engeller").whereEqualTo("hedef", uid).get();
        Task<QuerySnapshot> kaldirilan = db().collection("sikayetler").whereEqualTo("durum", "kaldirildi").get();
        Task<QuerySnapshot> begeniler = db().collection("kullanicilar").document(uid)
                .collection("begenilenGonderiler").get();
        Tasks.whenAllSuccess(gonderiler, engelledim, engelledi, kaldirilan, begeniler)
                .addOnSuccessListener(list -> {
                    Set<String> gizliUid = new HashSet<>();
                    for (DocumentSnapshot d : engelledim.getResult()) gizliUid.add(str(d, "hedef"));
                    for (DocumentSnapshot d : engelledi.getResult()) gizliUid.add(str(d, "eden"));
                    Set<String> kaldirilanId = new HashSet<>();
                    for (DocumentSnapshot d : kaldirilan.getResult()) kaldirilanId.add(str(d, "gonderiId"));
                    Set<String> begeni = new HashSet<>();
                    for (DocumentSnapshot d : begeniler.getResult()) begeni.add(d.getId());

                    List<Post> gorunen = new ArrayList<>();
                    for (DocumentSnapshot d : gonderiler.getResult()) {
                        Post p;
                        try {
                            p = gonderiden(d);
                        } catch (RuntimeException ex) {
                            continue;
                        }
                        if (kaldirilanId.contains(p.getBulutId())) continue;
                        if (gizliUid.contains(p.getYazarUid())) continue;
                        String durum = str(d, "durum");
                        if (!DURUM_YAYINDA.equals(durum) && !durum.isEmpty()) continue;
                        gorunen.add(p);
                    }
                    sonuc.bitti(new Akis(gorunen, begeni), null);
                })
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void kullaniciGonderileri(String uid, String bakanUid, Sonuc<Akis> sonuc) {
        akisGetir(bakanUid, (akis, hata) -> {
            if (hata != null || akis == null) {
                sonuc.bitti(null, hata);
                return;
            }
            List<Post> benim = new ArrayList<>();
            for (Post p : akis.gonderiler) {
                if (uid.equals(p.getYazarUid())) benim.add(p);
            }
            sonuc.bitti(new Akis(benim, akis.begenilenler), null);
        });
    }

    public static void begenilenGonderiler(String uid, Sonuc<Akis> sonuc) {
        akisGetir(uid, (akis, hata) -> {
            if (hata != null || akis == null) {
                sonuc.bitti(null, hata);
                return;
            }
            List<Post> secilen = new ArrayList<>();
            for (Post p : akis.gonderiler) {
                if (akis.begenilenler.contains(p.getBulutId())) secilen.add(p);
            }
            sonuc.bitti(new Akis(secilen, akis.begenilenler), null);
        });
    }

    public static void begen(String gonderiId, String uid, boolean begen, Sonuc<Integer> sonuc) {
        DocumentReference gonderi = db().collection("gonderiler").document(gonderiId);
        DocumentReference like = gonderi.collection("begeniler").document(uid);
        DocumentReference benim = db().collection("kullanicilar").document(uid)
                .collection("begenilenGonderiler").document(gonderiId);
        db().runTransaction(t -> {
            DocumentSnapshot likeSnap = t.get(like);
            DocumentSnapshot postSnap = t.get(gonderi);
            long sayi = sayiUzun(postSnap, "begeniSayisi");
            if (begen && !likeSnap.exists()) {
                Map<String, Object> veri = new HashMap<>();
                veri.put("uid", uid);
                veri.put("zaman", System.currentTimeMillis());
                t.set(like, veri);
                t.set(benim, veri);
                sayi++;
                t.update(gonderi, "begeniSayisi", sayi);
            } else if (!begen && likeSnap.exists()) {
                t.delete(like);
                t.delete(benim);
                sayi = Math.max(0, sayi - 1);
                t.update(gonderi, "begeniSayisi", sayi);
            }
            return (int) sayi;
        }).addOnSuccessListener(sayi -> sonuc.bitti(sayi, null))
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void gonderiSil(String gonderiId, String uid, Sonuc<Boolean> sonuc) {
        DocumentReference ref = db().collection("gonderiler").document(gonderiId);
        ref.get().addOnSuccessListener(d -> {
            if (!d.exists() || !uid.equals(str(d, "uid"))) {
                sonuc.bitti(false, null);
                return;
            }
            String medyaId = ("gonderiler/" + uid + "/" + gonderiId + ".jpg").replace('/', '_');
            Tasks.whenAll(
                    ref.delete(),
                    db().collection("medya").document(medyaId).delete()
            ).addOnSuccessListener(v -> sonuc.bitti(true, null))
                    .addOnFailureListener(e -> sonuc.bitti(false, e));
        }).addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static void yorumlariGetir(String gonderiId, Sonuc<List<Yorum>> sonuc) {
        db().collection("gonderiler").document(gonderiId).collection("yorumlar")
                .orderBy("zaman", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(q -> {
                    List<Yorum> liste = new ArrayList<>();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        try {
                            liste.add(yorumdan(d));
                        } catch (RuntimeException ex) {
                        }
                    }
                    sonuc.bitti(liste, null);
                })
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void yorumEkle(String gonderiId, String uid, String metin, User yazar, Sonuc<Integer> sonuc) {
        DocumentReference gonderi = db().collection("gonderiler").document(gonderiId);
        DocumentReference yorum = gonderi.collection("yorumlar").document();
        Map<String, Object> veri = new HashMap<>();
        veri.put("uid", uid);
        veri.put("metin", metin);
        veri.put("zaman", System.currentTimeMillis());
        veri.put("yazarAd", KullaniciGorunum.gorunenAd(yazar));
        veri.put("yazarKullaniciAdi", KullaniciGorunum.kullaniciAdi(yazar));
        veri.put("yazarResimUrl", httpIse(yazar.getProfilResmi()));
        db().runTransaction(t -> {
            DocumentSnapshot post = t.get(gonderi);
            long sayi = sayiUzun(post, "yorumSayisi");
            t.set(yorum, veri);
            sayi++;
            t.update(gonderi, "yorumSayisi", sayi);
            return (int) sayi;
        }).addOnSuccessListener(sayi -> sonuc.bitti(sayi, null))
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void yorumSil(String gonderiId, String yorumId, String uid, Sonuc<Integer> sonuc) {
        DocumentReference gonderi = db().collection("gonderiler").document(gonderiId);
        DocumentReference yorum = gonderi.collection("yorumlar").document(yorumId);
        db().runTransaction(t -> {
            DocumentSnapshot y = t.get(yorum);
            DocumentSnapshot post = t.get(gonderi);
            if (!y.exists()) return -1;
            boolean yetkili = uid.equals(str(y, "uid")) || uid.equals(str(post, "uid"));
            if (!yetkili) return -1;
            long sayi = sayiUzun(post, "yorumSayisi");
            t.delete(yorum);
            sayi = Math.max(0, sayi - 1);
            t.update(gonderi, "yorumSayisi", sayi);
            return (int) sayi;
        }).addOnSuccessListener(sayi -> sonuc.bitti(sayi, null))
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void engelle(String benimUid, String hedefUid, Sonuc<Boolean> sonuc) {
        if (benimUid.equals(hedefUid)) {
            sonuc.bitti(false, null);
            return;
        }
        Map<String, Object> veri = new HashMap<>();
        veri.put("eden", benimUid);
        veri.put("hedef", hedefUid);
        veri.put("zaman", System.currentTimeMillis());
        db().collection("engeller").document(benimUid + "_" + hedefUid).set(veri)
                .addOnSuccessListener(v -> {
                    db().collection("takipler").document(benimUid + "_" + hedefUid).delete();
                    db().collection("takipler").document(hedefUid + "_" + benimUid).delete();
                    sonuc.bitti(true, null);
                })
                .addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static void engeliKaldir(String benimUid, String hedefUid, Sonuc<Boolean> sonuc) {
        db().collection("engeller").document(benimUid + "_" + hedefUid).delete()
                .addOnSuccessListener(v -> sonuc.bitti(true, null))
                .addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static void engelDurumu(String benimUid, String hedefUid, Sonuc<boolean[]> sonuc) {
        Task<DocumentSnapshot> ben = db().collection("engeller").document(benimUid + "_" + hedefUid).get();
        Task<DocumentSnapshot> o = db().collection("engeller").document(hedefUid + "_" + benimUid).get();
        Tasks.whenAllSuccess(ben, o).addOnSuccessListener(v -> {
            boolean[] dizi = new boolean[]{ben.getResult().exists(), o.getResult().exists()};
            sonuc.bitti(dizi, null);
        }).addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void engellenenleriGetir(String benimUid, Sonuc<List<BulutProfil>> sonuc) {
        db().collection("engeller").whereEqualTo("eden", benimUid).get()
                .addOnSuccessListener(q -> {
                    List<Task<DocumentSnapshot>> profiller = new ArrayList<>();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        String hedef = str(d, "hedef");
                        if (hedef.isEmpty()) continue;
                        profiller.add(db().collection("kullanicilar").document(hedef).get());
                    }
                    if (profiller.isEmpty()) {
                        sonuc.bitti(new ArrayList<>(), null);
                        return;
                    }
                    Tasks.whenAllSuccess(profiller).addOnSuccessListener(list -> {
                        List<BulutProfil> kisiler = new ArrayList<>();
                        for (Object o : list) {
                            DocumentSnapshot d = (DocumentSnapshot) o;
                            if (d.exists()) kisiler.add(profilden(d));
                        }
                        sonuc.bitti(kisiler, null);
                    }).addOnFailureListener(e -> sonuc.bitti(null, e));
                })
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void konusmalar(String benimUid, Sonuc<List<KonusmaOzet>> sonuc) {
        db().collection("sohbetler").whereArrayContains("uyeler", benimUid).get()
                .addOnSuccessListener(q -> {
                    List<KonusmaOzet> liste = new ArrayList<>();
                    List<Task<DocumentSnapshot>> profiller = new ArrayList<>();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        KonusmaOzet k = new KonusmaOzet();
                        Object hamUyeler = d.get("uyeler");
                        if (!(hamUyeler instanceof List)) continue;
                        for (Object uye : (List<?>) hamUyeler) {
                            if (uye instanceof String && !benimUid.equals(uye)) k.karsiUid = (String) uye;
                        }
                        if (k.karsiUid == null || k.karsiUid.isEmpty()) continue;
                        k.sonMesaj = str(d, "sonMesaj");
                        k.zaman = sayiUzun(d, "sonZaman");
                        k.sonBenim = benimUid.equals(str(d, "sonGonderenUid"));
                        Object harita = d.get("okunmamis");
                        if (harita instanceof Map) {
                            Object sayi = ((Map<?, ?>) harita).get(benimUid);
                            if (sayi instanceof Number) k.okunmamis = ((Number) sayi).intValue();
                        }
                        liste.add(k);
                        profiller.add(db().collection("kullanicilar").document(k.karsiUid).get());
                    }
                    Collections.sort(liste, (a, b) -> Long.compare(b.zaman, a.zaman));
                    if (profiller.isEmpty()) {
                        sonuc.bitti(liste, null);
                        return;
                    }
                    Tasks.whenAllSuccess(profiller).addOnSuccessListener(list -> {
                        Map<String, BulutProfil> harita = new HashMap<>();
                        for (Object o : list) {
                            DocumentSnapshot d = (DocumentSnapshot) o;
                            if (d.exists()) harita.put(d.getId(), profilden(d));
                        }
                        List<KonusmaOzet> gorunen = new ArrayList<>();
                        for (KonusmaOzet k : liste) {
                            BulutProfil p = harita.get(k.karsiUid);
                            if (p == null) continue;
                            k.ad = ((p.getIsim() + " " + p.getSoyisim()).trim());
                            if (k.ad.isEmpty()) k.ad = p.getKullaniciAdi();
                            k.kullaniciAdi = p.getKullaniciAdi();
                            k.resimUrl = p.getProfilResmiUrl();
                            gorunen.add(k);
                        }
                        sonuc.bitti(gorunen, null);
                    }).addOnFailureListener(e -> sonuc.bitti(null, e));
                })
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void sohbetiGetir(String benimUid, String karsiUid, Sonuc<List<SohbetMesaji>> sonuc) {
        String id = sohbetId(benimUid, karsiUid);
        DocumentReference sohbet = db().collection("sohbetler").document(id);
        sohbet.get().addOnCompleteListener(v -> {
            if (v.isSuccessful() && v.getResult() != null && v.getResult().exists()) {
                sohbet.update("okunmamis." + benimUid, 0);
            }
            sohbet.collection("mesajlar").orderBy("zaman", Query.Direction.ASCENDING).get()
                        .addOnSuccessListener(q -> {
                            List<SohbetMesaji> liste = new ArrayList<>();
                            for (DocumentSnapshot d : q.getDocuments()) {
                                SohbetMesaji m = new SohbetMesaji();
                                m.gonderenUid = str(d, "gonderenUid");
                                m.metin = str(d, "metin");
                                m.zaman = sayiUzun(d, "zaman");
                                m.okundu = Boolean.TRUE.equals(d.getBoolean("okundu")) || benimUid.equals(m.gonderenUid);
                                liste.add(m);
                                if (karsiUid.equals(m.gonderenUid) && !Boolean.TRUE.equals(d.getBoolean("okundu"))) {
                                    d.getReference().update("okundu", true);
                                }
                            }
                            sonuc.bitti(liste, null);
                        })
                        .addOnFailureListener(e -> sonuc.bitti(null, e));
        });
    }

    public static void mesajGonder(String benimUid, String karsiUid, String metin, Sonuc<Boolean> sonuc) {
        String id = sohbetId(benimUid, karsiUid);
        DocumentReference sohbet = db().collection("sohbetler").document(id);
        long simdi = System.currentTimeMillis();
        Map<String, Object> mesaj = new HashMap<>();
        mesaj.put("gonderenUid", benimUid);
        mesaj.put("metin", metin);
        mesaj.put("zaman", simdi);
        mesaj.put("okundu", false);
        sohbet.get().addOnSuccessListener(d -> {
            Task<Void> baslik;
            if (!d.exists()) {
                Map<String, Object> ilk = new HashMap<>();
                Map<String, Object> okunmamis = new HashMap<>();
                okunmamis.put(karsiUid, 1);
                ilk.put("uyeler", java.util.Arrays.asList(benimUid, karsiUid));
                ilk.put("sonMesaj", metin);
                ilk.put("sonZaman", simdi);
                ilk.put("sonGonderenUid", benimUid);
                ilk.put("okunmamis", okunmamis);
                baslik = sohbet.set(ilk);
            } else {
                baslik = sohbet.update(
                        "sonMesaj", metin,
                        "sonZaman", simdi,
                        "sonGonderenUid", benimUid,
                        "okunmamis." + karsiUid, FieldValue.increment(1));
            }
            baslik.continueWithTask(t -> {
                if (!t.isSuccessful()) throw t.getException();
                return sohbet.collection("mesajlar").add(mesaj);
            }).addOnSuccessListener(v -> {
                sonuc.bitti(true, null);
                bildirimBirak(karsiUid, BildirimMerkezi.TIP_MESAJ, benimUid, "", metin, benimUid);
            }).addOnFailureListener(e -> sonuc.bitti(false, e));
        }).addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static void sikayetEt(Post post, String sikayetEdenUid, String sikayetEdenAdi, int sebepIndeksi, Sonuc<Boolean> sonuc) {
        String belge = post.getBulutId() + "_" + sikayetEdenUid;
        Map<String, Object> veri = new HashMap<>();
        veri.put("durum", SikayetYoneticisi.DURUM_BEKLEMEDE);
        veri.put("sebep", SikayetYoneticisi.SEBEP_KODLARI[sebepIndeksi]);
        veri.put("gonderiId", post.getBulutId());
        veri.put("gonderiMetni", post.getIcerik() == null ? "" : post.getIcerik());
        veri.put("resimUrl", post.getResimUri() == null ? "" : post.getResimUri());
        veri.put("yazarUid", post.getYazarUid());
        veri.put("yazarAd", post.getYazarAd() == null ? "" : post.getYazarAd());
        veri.put("yazarKullaniciAdi", post.getYazarKullaniciAdi() == null ? "" : post.getYazarKullaniciAdi());
        veri.put("sikayetEdenUid", sikayetEdenUid);
        veri.put("sikayetEdenKullaniciAdi", sikayetEdenAdi);
        veri.put("olusturulma", FieldValue.serverTimestamp());
        db().collection("sikayetler").document(belge).set(veri, SetOptions.merge())
                .addOnSuccessListener(v -> sonuc.bitti(true, null))
                .addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static void yorumSikayetEt(String gonderiId, Yorum yorum, String sikayetEdenUid,
                                      String sikayetEdenAdi, int sebepIndeksi, Sonuc<Boolean> sonuc) {
        String belge = gonderiId + "_" + yorum.getBulutId() + "_" + sikayetEdenUid;
        Map<String, Object> veri = new HashMap<>();
        veri.put("durum", SikayetYoneticisi.DURUM_BEKLEMEDE);
        veri.put("sebep", SikayetYoneticisi.SEBEP_KODLARI[sebepIndeksi]);
        veri.put("gonderiId", gonderiId);
        veri.put("yorumId", yorum.getBulutId());
        veri.put("gonderiMetni", yorum.getMetin() == null ? "" : yorum.getMetin());
        veri.put("resimUrl", "");
        veri.put("yazarUid", yorum.getYazarUid());
        veri.put("yazarAd", yorum.getYazarAd() == null ? "" : yorum.getYazarAd());
        veri.put("yazarKullaniciAdi", yorum.getYazarKullaniciAdi() == null ? "" : yorum.getYazarKullaniciAdi());
        veri.put("sikayetEdenUid", sikayetEdenUid);
        veri.put("sikayetEdenKullaniciAdi", sikayetEdenAdi);
        veri.put("olusturulma", FieldValue.serverTimestamp());
        db().collection("sikayetler").document(belge).set(veri, SetOptions.merge())
                .addOnSuccessListener(v -> sonuc.bitti(true, null))
                .addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static void hesabiSil(String uid, Sonuc<Boolean> sonuc) {
        if (uid == null || uid.isEmpty()) {
            sonuc.bitti(false, null);
            return;
        }
        Task<QuerySnapshot> gonderiler = db().collection("gonderiler").whereEqualTo("uid", uid).get();
        Task<QuerySnapshot> engeller = db().collection("engeller").whereEqualTo("eden", uid).get();
        Task<QuerySnapshot> begeniler = db().collection("kullanicilar").document(uid)
                .collection("begenilenGonderiler").get();
        Task<QuerySnapshot> fotolar = db().collection("medya").whereEqualTo("uid", uid).get();
        Tasks.whenAllSuccess(gonderiler, engeller, begeniler, fotolar).addOnSuccessListener(v -> {
            List<Task<Void>> silmeler = new ArrayList<>();
            for (DocumentSnapshot d : gonderiler.getResult()) silmeler.add(d.getReference().delete());
            for (DocumentSnapshot d : engeller.getResult()) silmeler.add(d.getReference().delete());
            for (DocumentSnapshot d : begeniler.getResult()) silmeler.add(d.getReference().delete());
            for (DocumentSnapshot d : fotolar.getResult()) silmeler.add(d.getReference().delete());
            silmeler.add(db().collection("kullanicilar").document(uid).delete());
            Tasks.whenAll(silmeler)
                    .addOnSuccessListener(x -> sonuc.bitti(true, null))
                    .addOnFailureListener(e -> sonuc.bitti(false, e));
        }).addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static com.google.firebase.firestore.ListenerRegistration bildirimAkisi(
            String uid, com.google.firebase.firestore.EventListener<QuerySnapshot> dinleyici) {
        return db().collection("kullanicilar").document(uid).collection("bildirimler")
                .orderBy("zaman", Query.Direction.DESCENDING).limit(40)
                .addSnapshotListener(dinleyici);
    }

    public static void bildirimleriGetir(String uid, Sonuc<QuerySnapshot> sonuc) {
        db().collection("kullanicilar").document(uid).collection("bildirimler")
                .orderBy("zaman", Query.Direction.DESCENDING).limit(40).get()
                .addOnSuccessListener(q -> sonuc.bitti(q, null))
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void bildirimOkundu(String uid, String belgeId) {
        if (uid == null || belgeId == null || belgeId.isEmpty()) return;
        db().collection("kullanicilar").document(uid).collection("bildirimler")
                .document(belgeId).update("okundu", true);
    }

    public static void okunmamisSayisi(String uid, Sonuc<Integer> sonuc) {
        db().collection("kullanicilar").document(uid).collection("bildirimler")
                .whereEqualTo("okundu", false).get()
                .addOnSuccessListener(q -> sonuc.bitti(q.size(), null))
                .addOnFailureListener(e -> sonuc.bitti(0, e));
    }

    public static void bildirimBirak(String hedefUid, String tip, String gonderenUid,
                                     String gonderenAd, String metin, String ilgiliId) {
        if (hedefUid == null || gonderenUid == null || hedefUid.isEmpty() || gonderenUid.equals(hedefUid)) return;
        engelDurumu(gonderenUid, hedefUid, (durum, hata) -> {
            if (durum != null && (durum[0] || durum[1])) return;
            Runnable yaz = () -> {
                String ad = gonderenAd == null ? "" : gonderenAd;
                Map<String, Object> veri = new HashMap<>();
                veri.put("tip", tip == null ? "" : tip);
                veri.put("gonderenUid", gonderenUid);
                veri.put("gonderenAd", ad);
                String govde = metin == null ? "" : metin.trim();
                if (govde.length() > 140) govde = govde.substring(0, 140);
                veri.put("metin", govde);
                veri.put("ilgiliId", ilgiliId == null ? "" : ilgiliId);
                veri.put("zaman", System.currentTimeMillis());
                veri.put("okundu", false);
                db().collection("kullanicilar").document(hedefUid).collection("bildirimler").add(veri);
            };
            if (gonderenAd != null && !gonderenAd.trim().isEmpty()) {
                yaz.run();
                return;
            }
            profilGetir(gonderenUid, (profil, e) -> {
                Map<String, Object> veri = new HashMap<>();
                veri.put("tip", tip == null ? "" : tip);
                veri.put("gonderenUid", gonderenUid);
                veri.put("gonderenAd", profil == null ? "" : gorunenAd(profil));
                String govde = metin == null ? "" : metin.trim();
                if (govde.length() > 140) govde = govde.substring(0, 140);
                veri.put("metin", govde);
                veri.put("ilgiliId", ilgiliId == null ? "" : ilgiliId);
                veri.put("zaman", System.currentTimeMillis());
                veri.put("okundu", false);
                db().collection("kullanicilar").document(hedefUid).collection("bildirimler").add(veri);
            });
        });
    }

    public static void takipDurumu(String benimUid, String hedefUid, Sonuc<Boolean> sonuc) {
        db().collection("takipler").document(benimUid + "_" + hedefUid).get()
                .addOnSuccessListener(d -> sonuc.bitti(d.exists(), null))
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void takipEt(String benimUid, String hedefUid, String benimAd, Sonuc<Boolean> sonuc) {
        if (benimUid == null || hedefUid == null || benimUid.equals(hedefUid)) {
            sonuc.bitti(false, null);
            return;
        }
        Map<String, Object> veri = new HashMap<>();
        veri.put("takipEden", benimUid);
        veri.put("hedef", hedefUid);
        veri.put("zaman", System.currentTimeMillis());
        db().collection("takipler").document(benimUid + "_" + hedefUid).set(veri)
                .addOnSuccessListener(v -> {
                    bildirimBirak(hedefUid, BildirimMerkezi.TIP_TAKIP, benimUid, benimAd, "", benimUid);
                    sonuc.bitti(true, null);
                })
                .addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static void takibiBirak(String benimUid, String hedefUid, Sonuc<Boolean> sonuc) {
        db().collection("takipler").document(benimUid + "_" + hedefUid).delete()
                .addOnSuccessListener(v -> sonuc.bitti(true, null))
                .addOnFailureListener(e -> sonuc.bitti(false, e));
    }

    public static void takipSayilari(String uid, Sonuc<int[]> sonuc) {
        Task<QuerySnapshot> takipci = db().collection("takipler").whereEqualTo("hedef", uid).get();
        Task<QuerySnapshot> edilen = db().collection("takipler").whereEqualTo("takipEden", uid).get();
        Tasks.whenAllSuccess(takipci, edilen).addOnSuccessListener(v -> {
            int[] sayi = new int[]{takipci.getResult().size(), edilen.getResult().size()};
            sonuc.bitti(sayi, null);
        }).addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static void kullaniciAdiylaBul(String kullaniciAdi, Sonuc<BulutProfil> sonuc) {
        String govde = kullaniciAdi == null ? "" : kullaniciAdi.trim();
        while (govde.startsWith("@")) govde = govde.substring(1);
        if (govde.isEmpty()) {
            sonuc.bitti(null, null);
            return;
        }
        String aranan = KullaniciGorunum.aramaIcinNormallestir("@" + govde);
        db().collection("kullanicilar").whereEqualTo("kullaniciAdiArama", aranan).limit(1).get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) {
                        sonuc.bitti(null, null);
                        return;
                    }
                    sonuc.bitti(profilden(q.getDocuments().get(0)), null);
                })
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    public static BulutProfil profilden(DocumentSnapshot d) {
        BulutProfil p = new BulutProfil();
        p.setUid(d.getId());
        p.setIsim(str(d, "isim"));
        p.setSoyisim(str(d, "soyisim"));
        p.setKullaniciAdi(str(d, "kullaniciAdi"));
        p.setMeslek(str(d, "meslek"));
        p.setKonum(str(d, "konum"));
        p.setWebSitesi(str(d, "webSitesi"));
        p.setBiyografi(str(d, "biyografi"));
        p.setProfilResmiUrl(str(d, "profilResmiUrl"));
        return p;
    }

    public static String gorunenAd(BulutProfil p) {
        String tam = ((p.getIsim() == null ? "" : p.getIsim()) + " " + (p.getSoyisim() == null ? "" : p.getSoyisim())).trim();
        if (!tam.isEmpty()) return tam;
        return p.getKullaniciAdi() == null ? "" : p.getKullaniciAdi();
    }

    private static Post gonderiden(DocumentSnapshot d) {
        Post p = new Post();
        p.setBulutId(d.getId());
        p.setYazarUid(str(d, "uid"));
        p.setIcerik(str(d, "icerik"));
        p.setResimUri(str(d, "resimUrl"));
        p.setBegeniSayisi((int) sayiUzun(d, "begeniSayisi"));
        p.setYorumSayisi((int) sayiUzun(d, "yorumSayisi"));
        p.setTimestamp(sayiUzun(d, "zaman"));
        p.setYazarAd(str(d, "yazarAd"));
        p.setYazarKullaniciAdi(str(d, "yazarKullaniciAdi"));
        p.setYazarResimUrl(str(d, "yazarResimUrl"));
        return p;
    }

    private static Yorum yorumdan(DocumentSnapshot d) {
        Yorum y = new Yorum();
        y.setBulutId(d.getId());
        y.setYazarUid(str(d, "uid"));
        y.setMetin(str(d, "metin"));
        y.setTimestamp(sayiUzun(d, "zaman"));
        y.setYazarAd(str(d, "yazarAd"));
        y.setYazarKullaniciAdi(str(d, "yazarKullaniciAdi"));
        y.setYazarResimUrl(str(d, "yazarResimUrl"));
        return y;
    }

    private static String str(DocumentSnapshot d, String alan) {
        Object v = d.get(alan);
        return v instanceof String ? (String) v : "";
    }

    private static long sayiUzun(DocumentSnapshot d, String alan) {
        Object v = d.get(alan);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private static String bos(String s) {
        return s == null ? "" : s;
    }

    private static String httpIse(String s) {
        return s != null && (s.startsWith("http") || s.startsWith(ResimYukleyici.ONEK)) ? s : "";
    }

    public static String kullaniciAdiNorm(String ham) {
        if (ham == null) return "";
        String s = ham.trim().toLowerCase(Locale.ROOT);
        return s.startsWith("@") ? s : "@" + s;
    }
}
