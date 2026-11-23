package com.mehmetokyay.flownote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.adapter.FeedAdapter;
import com.mehmetokyay.flownote.model.BulutProfil;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.mehmetokyay.flownote.util.ResimYukleyici;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class KullaniciProfilActivity extends AppCompatActivity {

    private static final String TAG = "KullaniciProfil";
    private static final String EXTRA_KULLANICI_ID = "kullaniciId";

    private String hedefUid;
    private String aktifUid = "";
    private boolean benEngelledim;

    private View scroll, progress;
    private ImageView imageResim;
    private TextView textBaslik, textTamAd, textKullaniciAdi, textMeslek, textBiyografi, textKonum;
    private TextView textPaylasim, textBegeni, textBos;
    private MaterialButton btnMesaj, btnEngel, btnDuzenle, btnTakip;
    private TextView textTakipci;
    private boolean takipEdiyorum;
    private RecyclerView recyclerGonderiler;
    private FeedAdapter feedAdapter;

    public static void baslat(Context context, String kullaniciUid) {
        Intent intent = new Intent(context, KullaniciProfilActivity.class);
        intent.putExtra(EXTRA_KULLANICI_ID, kullaniciUid);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kullanici_profil);
        EkranYardimcisi.uygula(this, true, false);

        hedefUid = getIntent().getStringExtra(EXTRA_KULLANICI_ID);
        if (hedefUid == null || hedefUid.isEmpty() || FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        scroll = findViewById(R.id.scrollKp);
        progress = findViewById(R.id.progressKp);
        imageResim = findViewById(R.id.imageKpResim);
        textBaslik = findViewById(R.id.textKpBaslik);
        textTamAd = findViewById(R.id.textKpTamAd);
        textKullaniciAdi = findViewById(R.id.textKpKullaniciAdi);
        textMeslek = findViewById(R.id.textKpMeslek);
        textBiyografi = findViewById(R.id.textKpBiyografi);
        textKonum = findViewById(R.id.textKpKonum);
        textPaylasim = findViewById(R.id.textKpPaylasim);
        textBegeni = findViewById(R.id.textKpBegeni);
        textBos = findViewById(R.id.textKpBos);
        btnMesaj = findViewById(R.id.btnKpMesaj);
        btnEngel = findViewById(R.id.btnKpEngel);
        btnDuzenle = findViewById(R.id.btnKpDuzenle);
        btnTakip = findViewById(R.id.btnKpTakip);
        textTakipci = findViewById(R.id.textKpTakipci);
        recyclerGonderiler = findViewById(R.id.recyclerKpGonderiler);

        feedAdapter = new FeedAdapter(this);
        feedAdapter.setProfilAcilabilir(false);
        feedAdapter.setDegisiklikDinleyici(this::yukle);
        recyclerGonderiler.setLayoutManager(new LinearLayoutManager(this));
        recyclerGonderiler.setAdapter(feedAdapter);

        findViewById(R.id.btnKpGeri).setOnClickListener(v -> finish());
        btnMesaj.setOnClickListener(v -> SohbetActivity.baslat(this, hedefUid));
        btnDuzenle.setOnClickListener(v -> startActivity(new Intent(this, ProfiliDuzenleActivity.class)));
        btnEngel.setOnClickListener(v -> engelOnayi());
        if (btnTakip != null) btnTakip.setOnClickListener(v -> takipDegistir());
    }

    @Override
    protected void onResume() {
        super.onResume();
        yukle();
    }

    private void yukle() {
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;
        aktifUid = fUser.getUid();
        BulutSosyal.profilGetir(hedefUid, (hedef, hata) -> {
            if (isFinishing() || isDestroyed()) return;
            if (hata != null || hedef == null) {
                Toast.makeText(this, R.string.kullanici_bulunamadi, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            BulutSosyal.engelDurumu(aktifUid, hedefUid, (durum, engelHata) -> {
                if (isFinishing() || isDestroyed() || durum == null) return;
                benEngelledim = durum[0];
                boolean engellendim = durum[1];
                BulutSosyal.kullaniciGonderileri(hedefUid, aktifUid, (akis, gHata) -> {
                    if (isFinishing() || isDestroyed() || akis == null) return;
                    int begeni = 0;
                    for (Post p : akis.gonderiler) begeni += p.getBegeniSayisi();
                    ekranaYaz(hedef, akis.gonderiler.size(), begeni, engellendim);
                    feedAdapter.veriyiAyarla(aktifUid, akis);
                    bosDurum(benEngelledim, engellendim, akis.gonderiler.isEmpty());
                    takibiYukle(engellendim);
                });
            });
        });
    }

    private void ekranaYaz(BulutProfil hedef, int paylasim, int begeni, boolean engellendim) {
        progress.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);

        String kAdi = hedef.getKullaniciAdi() == null ? "" : hedef.getKullaniciAdi();
        textBaslik.setText(!kAdi.isEmpty() ? kAdi : getString(R.string.nav_profil));
        textTamAd.setText(BulutSosyal.gorunenAd(hedef));
        textKullaniciAdi.setText(kAdi);
        textKullaniciAdi.setVisibility(kAdi.isEmpty() ? View.GONE : View.VISIBLE);
        imageResim.setImageTintList(null);
        ResimYukleyici.goster(imageResim, hedef.getProfilResmiUrl());

        metinVeyaGizle(textMeslek, hedef.getMeslek());
        metinVeyaGizle(textBiyografi, hedef.getBiyografi());
        StringBuilder konum = new StringBuilder();
        if (hedef.getKonum() != null && !hedef.getKonum().trim().isEmpty()) konum.append("📍 ").append(hedef.getKonum().trim());
        if (hedef.getWebSitesi() != null && !hedef.getWebSitesi().trim().isEmpty()) {
            if (konum.length() > 0) konum.append("  •  ");
            konum.append("🔗 ").append(hedef.getWebSitesi().trim());
        }
        metinVeyaGizle(textKonum, konum.toString());

        textPaylasim.setText(String.valueOf(paylasim));
        textBegeni.setText(String.valueOf(begeni));

        boolean kendim = hedef.getUid().equals(aktifUid);
        btnDuzenle.setVisibility(kendim ? View.VISIBLE : View.GONE);
        btnMesaj.setVisibility(kendim ? View.GONE : View.VISIBLE);
        btnEngel.setVisibility(kendim ? View.GONE : View.VISIBLE);
        btnEngel.setText(benEngelledim ? R.string.engeli_kaldir : R.string.engelle);
        btnMesaj.setEnabled(!benEngelledim && !engellendim);
    }

    private void takibiYukle(boolean engellendim) {
        boolean kendim = hedefUid.equals(aktifUid);
        boolean gizle = kendim || benEngelledim || engellendim;
        if (btnTakip != null) btnTakip.setVisibility(gizle ? View.GONE : View.VISIBLE);
        if (kendim) return;
        BulutSosyal.takipSayilari(hedefUid, (sayi, hata) -> {
            if (isFinishing() || isDestroyed() || sayi == null || textTakipci == null) return;
            textTakipci.setText(String.valueOf(sayi[0]));
        });
        if (gizle) return;
        BulutSosyal.takipDurumu(aktifUid, hedefUid, (ediyor, hata) -> {
            if (isFinishing() || isDestroyed() || ediyor == null || btnTakip == null) return;
            takipEdiyorum = ediyor;
            btnTakip.setText(ediyor ? R.string.takiptesin : R.string.takip_et);
        });
    }

    private void takipDegistir() {
        BulutSosyal.Sonuc<Boolean> bitti = (tamam, hata) -> {
            if (isFinishing() || isDestroyed()) return;
            if (tamam == null || !tamam) {
                Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, takipEdiyorum ? R.string.takip_birakildi : R.string.takip_edildi, Toast.LENGTH_SHORT).show();
            yukle();
        };
        if (takipEdiyorum) BulutSosyal.takibiBirak(aktifUid, hedefUid, bitti);
        else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String ad = user != null && user.getDisplayName() != null ? user.getDisplayName() : "";
            BulutSosyal.takipEt(aktifUid, hedefUid, ad, bitti);
        }
    }

    private void bosDurum(boolean engelledim, boolean engellendim, boolean gonderiYok) {
        if (engelledim) {
            textBos.setText(R.string.profil_engelledin);
        } else if (engellendim) {
            textBos.setText(R.string.profil_goruntulenemiyor);
        } else {
            textBos.setText(R.string.henuz_gonderi_yok);
        }
        boolean bos = engelledim || engellendim || gonderiYok;
        textBos.setVisibility(bos ? View.VISIBLE : View.GONE);
        recyclerGonderiler.setVisibility(bos ? View.GONE : View.VISIBLE);
    }

    private void engelOnayi() {
        if (aktifUid.isEmpty()) return;
        String ad = textTamAd.getText().toString();
        if (benEngelledim) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.engeli_kaldir_baslik)
                    .setMessage(getString(R.string.engeli_kaldir_onay, ad))
                    .setPositiveButton(R.string.engeli_kaldir, (d, w) -> engelDegistir(false, ad))
                    .setNegativeButton(R.string.iptal, null)
                    .show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.kullaniciyi_engelle)
                    .setMessage(getString(R.string.engelle_onay, ad))
                    .setPositiveButton(R.string.engelle, (d, w) -> engelDegistir(true, ad))
                    .setNegativeButton(R.string.iptal, null)
                    .show();
        }
    }

    private void engelDegistir(boolean engelle, String ad) {
        BulutSosyal.Sonuc<Boolean> bitti = (tamam, hata) -> {
            if (isFinishing() || isDestroyed()) return;
            if (tamam == null || !tamam) {
                Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, getString(engelle ? R.string.kullanici_engellendi : R.string.engel_kaldirildi, ad),
                    Toast.LENGTH_SHORT).show();
            yukle();
        };
        if (engelle) BulutSosyal.engelle(aktifUid, hedefUid, bitti);
        else BulutSosyal.engeliKaldir(aktifUid, hedefUid, bitti);
    }

    private static void metinVeyaGizle(TextView tv, String metin) {
        String temiz = metin != null ? metin.trim() : "";
        tv.setText(temiz);
        tv.setVisibility(temiz.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
