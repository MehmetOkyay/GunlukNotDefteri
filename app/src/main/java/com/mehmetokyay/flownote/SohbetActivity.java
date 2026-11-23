package com.mehmetokyay.flownote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.adapter.MesajAdapter;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.mehmetokyay.flownote.util.KullaniciGorunum;
import com.mehmetokyay.flownote.util.ResimYukleyici;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class SohbetActivity extends AppCompatActivity {

    private static final String TAG = "SohbetActivity";
    private static final String EXTRA_KARSI_ID = "karsiKullaniciId";

    private String karsiUid;
    private String aktifUid = "";

    private RecyclerView recycler;
    private TextView textIsim, textKullaniciAdi, textBos, textEngel, textBasHarf;
    private ImageView imageAvatar;
    private View layoutGiris, btnGonder;
    private EditText editMesaj;
    private MesajAdapter adapter;

    public static void baslat(Context context, String karsiKullaniciUid) {
        Intent intent = new Intent(context, SohbetActivity.class);
        intent.putExtra(EXTRA_KARSI_ID, karsiKullaniciUid);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sohbet);
        EkranYardimcisi.uygula(this, true, true);

        karsiUid = getIntent().getStringExtra(EXTRA_KARSI_ID);
        if (karsiUid == null || karsiUid.isEmpty() || FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        recycler = findViewById(R.id.recyclerMesajlar);
        textIsim = findViewById(R.id.textSohbetIsim);
        textKullaniciAdi = findViewById(R.id.textSohbetKullaniciAdi);
        textBos = findViewById(R.id.textSohbetBos);
        textEngel = findViewById(R.id.textSohbetEngel);
        textBasHarf = findViewById(R.id.textAvatarBasHarf);
        imageAvatar = findViewById(R.id.imageAvatar);
        layoutGiris = findViewById(R.id.layoutSohbetGiris);
        editMesaj = findViewById(R.id.editMesaj);
        btnGonder = findViewById(R.id.btnMesajGonder);

        adapter = new MesajAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);
        recycler.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b < ob && adapter.getItemCount() > 0) {
                recycler.post(() -> {
                    if (isFinishing() || isDestroyed() || adapter.getItemCount() == 0) return;
                    recycler.scrollToPosition(adapter.getItemCount() - 1);
                });
            }
        });

        findViewById(R.id.btnSohbetGeri).setOnClickListener(v -> finish());
        findViewById(R.id.layoutSohbetKisi).setOnClickListener(v -> KullaniciProfilActivity.baslat(this, karsiUid));
        btnGonder.setOnClickListener(v -> mesajGonder());
    }

    @Override
    protected void onResume() {
        super.onResume();
        sohbetiYukle();
    }

    private void sohbetiYukle() {
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;
        aktifUid = fUser.getUid();
        if (aktifUid.equals(karsiUid)) {
            finish();
            return;
        }
        BulutSosyal.profilGetir(karsiUid, (karsi, hata) -> {
            if (isFinishing() || isDestroyed()) return;
            if (hata != null || karsi == null) {
                Toast.makeText(this, R.string.kullanici_bulunamadi, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            textIsim.setText(BulutSosyal.gorunenAd(karsi));
            String kAdi = karsi.getKullaniciAdi() == null ? "" : karsi.getKullaniciAdi();
            textKullaniciAdi.setText(kAdi);
            textKullaniciAdi.setVisibility(kAdi.isEmpty() ? View.GONE : View.VISIBLE);
            if (karsi.getProfilResmiUrl() != null && !karsi.getProfilResmiUrl().isEmpty()) {
                imageAvatar.setVisibility(View.VISIBLE);
                textBasHarf.setVisibility(View.GONE);
                ResimYukleyici.goster(imageAvatar, karsi.getProfilResmiUrl());
            } else {
                imageAvatar.setVisibility(View.GONE);
                textBasHarf.setVisibility(View.VISIBLE);
                textBasHarf.setText(KullaniciGorunum.ilkHarf(BulutSosyal.gorunenAd(karsi)));
            }
            BulutSosyal.engelDurumu(aktifUid, karsiUid, (durum, e) -> {
                if (isFinishing() || isDestroyed() || durum == null) return;
                if (durum[0] || durum[1]) {
                    textEngel.setText(durum[0] ? R.string.sohbet_engelledin : R.string.sohbet_engellendin);
                    textEngel.setVisibility(View.VISIBLE);
                    layoutGiris.setVisibility(View.GONE);
                } else {
                    textEngel.setVisibility(View.GONE);
                    layoutGiris.setVisibility(View.VISIBLE);
                }
            });
            BulutSosyal.sohbetiGetir(aktifUid, karsiUid, (mesajlar, mesajHata) -> {
                if (isFinishing() || isDestroyed() || mesajlar == null) return;
                mesajlariGoster(mesajlar);
            });
        });
    }

    private void mesajlariGoster(List<BulutSosyal.SohbetMesaji> mesajlar) {
        adapter.listeyiDegistir(aktifUid, mesajlar);
        textBos.setVisibility(mesajlar.isEmpty() ? View.VISIBLE : View.GONE);
        if (!mesajlar.isEmpty()) recycler.scrollToPosition(mesajlar.size() - 1);
    }

    private void mesajGonder() {
        String metin = editMesaj.getText().toString().trim();
        if (metin.isEmpty() || aktifUid.isEmpty()) return;
        btnGonder.setEnabled(false);
        BulutSosyal.engelDurumu(aktifUid, karsiUid, (durum, hata) -> {
            if (durum != null && (durum[0] || durum[1])) {
                btnGonder.setEnabled(true);
                sohbetiYukle();
                return;
            }
            BulutSosyal.mesajGonder(aktifUid, karsiUid, metin, (tamam, gonderHata) -> {
                if (isFinishing() || isDestroyed()) return;
                btnGonder.setEnabled(true);
                if (tamam == null || !tamam) {
                    Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                    return;
                }
                editMesaj.setText("");
                sohbetiYukle();
            });
        });
    }
}
