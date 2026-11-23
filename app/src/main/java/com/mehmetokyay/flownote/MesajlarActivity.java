package com.mehmetokyay.flownote;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.adapter.KonusmaAdapter;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MesajlarActivity extends AppCompatActivity implements KonusmaAdapter.Dinleyici {

    private static final String TAG = "MesajlarActivity";

    private RecyclerView recycler;
    private View layoutBos, progress;
    private KonusmaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesajlar);
        EkranYardimcisi.uygula(this, true, false);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        recycler = findViewById(R.id.recyclerKonusmalar);
        layoutBos = findViewById(R.id.layoutMesajlarBos);
        progress = findViewById(R.id.progressMesajlar);

        adapter = new KonusmaAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.btnMesajlarGeri).setOnClickListener(v -> finish());
        View.OnClickListener yeniMesaj = v -> AramaActivity.baslat(this, true);
        findViewById(R.id.btnYeniMesaj).setOnClickListener(yeniMesaj);
        findViewById(R.id.btnMesajlarBosYeni).setOnClickListener(yeniMesaj);
    }

    @Override
    protected void onResume() {
        super.onResume();
        konusmalariYukle();
    }

    private void konusmalariYukle() {
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;
        final Context appContext = getApplicationContext();

        BulutSosyal.konusmalar(fUser.getUid(), (konusmalar, hata) -> {
            if (isFinishing() || isDestroyed()) return;
            progress.setVisibility(View.GONE);
            if (hata != null || konusmalar == null) {
                Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.listeyiDegistir(konusmalar);
            layoutBos.setVisibility(konusmalar.isEmpty() ? View.VISIBLE : View.GONE);
            recycler.setVisibility(konusmalar.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void konusmaSecildi(BulutSosyal.KonusmaOzet konusma) {
        SohbetActivity.baslat(this, konusma.karsiUid);
    }
}
