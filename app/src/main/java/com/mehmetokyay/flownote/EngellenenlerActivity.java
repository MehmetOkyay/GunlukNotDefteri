package com.mehmetokyay.flownote;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.adapter.EngellenenAdapter;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EngellenenlerActivity extends AppCompatActivity implements EngellenenAdapter.Dinleyici {

    private static final String TAG = "EngellenenlerActivity";

    private RecyclerView recyclerEngellenenler;
    private View layoutEngellenenBos, progressEngellenen;
    private TextView textEngellenenSayisi;
    private EngellenenAdapter adapter;

    private String aktifUid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engellenenler);
        EkranYardimcisi.uygula(this, true, false);

        recyclerEngellenenler = findViewById(R.id.recyclerEngellenenler);
        layoutEngellenenBos = findViewById(R.id.layoutEngellenenBos);
        progressEngellenen = findViewById(R.id.progressEngellenen);
        textEngellenenSayisi = findViewById(R.id.textEngellenenSayisi);

        adapter = new EngellenenAdapter(this);
        recyclerEngellenenler.setLayoutManager(new LinearLayoutManager(this));
        recyclerEngellenenler.setAdapter(adapter);

        findViewById(R.id.btnEngellenenGeri).setOnClickListener(v -> finish());
        findViewById(R.id.rowGizlilikPolitikasi).setOnClickListener(v -> gizlilikPolitikasiniAc());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        engellenenleriYukle();
    }

    private void gizlilikPolitikasiniAc() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.gizlilik_politikasi_url)));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.tarayici_bulunamadi, Toast.LENGTH_SHORT).show();
        }
    }

    private void engellenenleriYukle() {
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;
        final Context appContext = getApplicationContext();

        progressEngellenen.setVisibility(View.VISIBLE);
        layoutEngellenenBos.setVisibility(View.GONE);

        com.mehmetokyay.flownote.util.BulutSosyal.engellenenleriGetir(fUser.getUid(), (engellenenler, hata) -> {
            if (isFinishing() || isDestroyed()) return;
            progressEngellenen.setVisibility(View.GONE);
            if (hata != null || engellenenler == null) {
                Toast.makeText(this, R.string.engellenenler_yuklenemedi, Toast.LENGTH_SHORT).show();
                return;
            }
            aktifUid = fUser.getUid();
            adapter.listeyiDegistir(engellenenler);
            gorunumuGuncelle();
        });
    }

    private void gorunumuGuncelle() {
        int sayi = adapter.getItemCount();
        textEngellenenSayisi.setText(String.valueOf(sayi));
        layoutEngellenenBos.setVisibility(sayi == 0 ? View.VISIBLE : View.GONE);
        recyclerEngellenenler.setVisibility(sayi == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void engeliKaldirIstendi(final com.mehmetokyay.flownote.model.BulutProfil kullanici) {
        final String ad = EngellenenAdapter.gorunenAd(kullanici);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.engeli_kaldir_baslik)
                .setMessage(getString(R.string.engeli_kaldir_onay, ad))
                .setPositiveButton(R.string.engeli_kaldir, (dialog, which) -> engeliKaldir(kullanici, ad))
                .setNegativeButton(R.string.iptal, null)
                .show();
    }

    private void engeliKaldir(final com.mehmetokyay.flownote.model.BulutProfil kullanici, final String ad) {
        if (aktifUid.isEmpty()) return;
        com.mehmetokyay.flownote.util.BulutSosyal.engeliKaldir(aktifUid, kullanici.getUid(), (tamam, hata) -> {
            if (isFinishing() || isDestroyed()) return;
            if (Boolean.TRUE.equals(tamam)) {
                adapter.kaldir(kullanici.getUid());
                gorunumuGuncelle();
                Toast.makeText(this, getString(R.string.engel_kaldirildi, ad), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
