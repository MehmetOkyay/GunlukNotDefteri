package com.mehmetokyay.flownote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.adapter.FeedAdapter;
import com.mehmetokyay.flownote.adapter.KullaniciAdapter;
import com.mehmetokyay.flownote.model.BulutProfil;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.mehmetokyay.flownote.util.KullaniciGorunum;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class AramaActivity extends AppCompatActivity implements KullaniciAdapter.Dinleyici {

    private static final String TAG = "AramaActivity";
    private static final String EXTRA_SOHBET_MODU = "sohbetModu";
    private static final long GECIKME_MS = 300;

    private EditText editArama;
    private View btnTemizle, progress;
    private TextView textBos;
    private RecyclerView recycler;
    private MaterialButtonToggleGroup toggleTur;

    private KullaniciAdapter kullaniciAdapter;
    private FeedAdapter feedAdapter;
    private boolean sohbetModu;
    private boolean gonderiModu;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable aramaGorevi = this::aramaYap;
    private int aramaNo = 0;

    public static void baslat(Context context, boolean sohbetModu) {
        Intent intent = new Intent(context, AramaActivity.class);
        intent.putExtra(EXTRA_SOHBET_MODU, sohbetModu);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arama);
        EkranYardimcisi.uygula(this, true, true);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        sohbetModu = getIntent().getBooleanExtra(EXTRA_SOHBET_MODU, false);

        editArama = findViewById(R.id.editArama);
        btnTemizle = findViewById(R.id.btnAramaTemizle);
        progress = findViewById(R.id.progressArama);
        textBos = findViewById(R.id.textAramaBos);
        recycler = findViewById(R.id.recyclerArama);
        toggleTur = findViewById(R.id.toggleAramaTuru);

        kullaniciAdapter = new KullaniciAdapter(this);
        feedAdapter = new FeedAdapter(this);
        feedAdapter.setDegisiklikDinleyici(this::bosDurumuGuncelle);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(kullaniciAdapter);

        findViewById(R.id.btnAramaGeri).setOnClickListener(v -> finish());
        btnTemizle.setOnClickListener(v -> editArama.setText(""));

        if (sohbetModu) {
            toggleTur.setVisibility(View.GONE);
            editArama.setHint(R.string.mesaj_kisi_ara_ipucu);
        }
        toggleTur.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            gonderiModu = checkedId == R.id.btnAramaGonderiler;
            recycler.setAdapter(gonderiModu ? feedAdapter : kullaniciAdapter);
            aramaYap();
        });

        editArama.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                btnTemizle.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                handler.removeCallbacks(aramaGorevi);
                handler.postDelayed(aramaGorevi, GECIKME_MS);
            }
        });
        editArama.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            handler.removeCallbacks(aramaGorevi);
            aramaYap();
            klavyeyiKapat();
            return true;
        });

        editArama.requestFocus();
        aramaYap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gonderiModu || sohbetModu || editArama.length() > 0) aramaYap();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(aramaGorevi);
    }

    private void aramaYap() {
        final String sorgu = KullaniciGorunum.aramaIcinNormallestir(editArama.getText().toString());
        final boolean gonderiAramasi = gonderiModu && !sohbetModu;
        final int buArama = ++aramaNo;

        if (sorgu.isEmpty() && !sohbetModu) {
            kullaniciAdapter.listeyiDegistir(null);
            feedAdapter.temizle();
            progress.setVisibility(View.GONE);
            textBos.setText(R.string.arama_baslangic);
            textBos.setVisibility(View.VISIBLE);
            return;
        }

        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;
        final Context appContext = getApplicationContext();
        progress.setVisibility(View.VISIBLE);
        textBos.setVisibility(View.GONE);

        if (gonderiAramasi) {
            BulutSosyal.akisGetir(fUser.getUid(), (akis, hata) -> {
                if (buArama != aramaNo || isFinishing() || isDestroyed()) return;
                if (hata != null || akis == null) {
                    progress.setVisibility(View.GONE);
                    textBos.setText(R.string.hata_tekrar_deneyin);
                    textBos.setVisibility(View.VISIBLE);
                    return;
                }
                List<Post> eslesen = new ArrayList<>();
                for (Post p : akis.gonderiler) {
                    String icerik = KullaniciGorunum.aramaIcinNormallestir(p.getIcerik());
                    String yazar = KullaniciGorunum.aramaIcinNormallestir(p.getYazarAd() + " " + p.getYazarKullaniciAdi());
                    if (icerik.contains(sorgu) || yazar.contains(sorgu)) eslesen.add(p);
                }
                feedAdapter.veriyiAyarla(fUser.getUid(), new BulutSosyal.Akis(eslesen, akis.begenilenler));
                sonucGoster(eslesen.isEmpty());
            });
        } else {
            BulutSosyal.kullanicilariGetir(fUser.getUid(), (kisiler, hata) -> {
                if (buArama != aramaNo || isFinishing() || isDestroyed()) return;
                if (hata != null || kisiler == null) {
                    progress.setVisibility(View.GONE);
                    textBos.setText(R.string.hata_tekrar_deneyin);
                    textBos.setVisibility(View.VISIBLE);
                    return;
                }
                List<BulutProfil> eslesen = new ArrayList<>();
                for (BulutProfil u : kisiler) {
                    if (sohbetModu && fUser.getUid().equals(u.getUid())) continue;
                    String alanlar = KullaniciGorunum.aramaIcinNormallestir(
                            BulutSosyal.gorunenAd(u) + " " + u.getKullaniciAdi());
                    if (sorgu.isEmpty() || alanlar.contains(sorgu)) eslesen.add(u);
                }
                kullaniciAdapter.listeyiDegistir(eslesen);
                sonucGoster(eslesen.isEmpty());
            });
        }
    }

    private void sonucGoster(boolean bos) {
        progress.setVisibility(View.GONE);
        textBos.setText(R.string.arama_sonuc_yok);
        textBos.setVisibility(bos ? View.VISIBLE : View.GONE);
    }

    private void bosDurumuGuncelle() {
        sonucGoster(feedAdapter.getItemCount() == 0);
    }

    private void klavyeyiKapat() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(editArama.getWindowToken(), 0);
    }

    @Override
    public void kullaniciSecildi(BulutProfil kullanici) {
        klavyeyiKapat();
        if (sohbetModu) {
            SohbetActivity.baslat(this, kullanici.getUid());
            finish();
        } else {
            KullaniciProfilActivity.baslat(this, kullanici.getUid());
        }
    }
}
