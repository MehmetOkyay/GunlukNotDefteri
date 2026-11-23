package com.mehmetokyay.flownote;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.adapter.BildirimAdapter;
import com.mehmetokyay.flownote.util.BildirimMerkezi;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BildirimlerActivity extends AppCompatActivity implements BildirimAdapter.Dinleyici {

    private BildirimAdapter adapter;
    private View bos, progress;
    private RecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bildirimler);
        EkranYardimcisi.uygula(this, true, false);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        bos = findViewById(R.id.layoutBildirimBos);
        progress = findViewById(R.id.progressBildirim);
        recycler = findViewById(R.id.recyclerBildirimler);
        adapter = new BildirimAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.btnBildirimGeri).setOnClickListener(v -> finish());
        yukle();
    }

    private void yukle() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        progress.setVisibility(View.VISIBLE);
        BulutSosyal.bildirimleriGetir(user.getUid(), (sorgu, hata) -> {
            if (isFinishing() || isDestroyed()) return;
            progress.setVisibility(View.GONE);
            if (hata != null || sorgu == null) {
                Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            List<BildirimAdapter.Oge> liste = new ArrayList<>();
            for (DocumentSnapshot d : sorgu.getDocuments()) {
                BildirimAdapter.Oge oge = new BildirimAdapter.Oge();
                oge.id = d.getId();
                oge.tip = metin(d, "tip");
                oge.gonderenUid = metin(d, "gonderenUid");
                oge.gonderenAd = metin(d, "gonderenAd");
                oge.metin = metin(d, "metin");
                oge.ilgiliId = metin(d, "ilgiliId");
                Object zaman = d.get("zaman");
                oge.zaman = zaman instanceof Number ? ((Number) zaman).longValue() : 0;
                oge.okundu = Boolean.TRUE.equals(d.getBoolean("okundu"));
                liste.add(oge);
                if (!oge.okundu) BulutSosyal.bildirimOkundu(user.getUid(), oge.id);
            }
            adapter.listeyiDegistir(liste);
            boolean yok = liste.isEmpty();
            bos.setVisibility(yok ? View.VISIBLE : View.GONE);
            recycler.setVisibility(yok ? View.GONE : View.VISIBLE);
        });
    }

    private static String metin(DocumentSnapshot d, String alan) {
        Object v = d.get(alan);
        return v instanceof String ? (String) v : "";
    }

    @Override
    public void bildirimTiklandi(BildirimAdapter.Oge oge) {
        if (oge == null) return;
        if (BildirimMerkezi.TIP_MESAJ.equals(oge.tip)) {
            String uid = !oge.ilgiliId.isEmpty() ? oge.ilgiliId : oge.gonderenUid;
            if (!uid.isEmpty()) SohbetActivity.baslat(this, uid);
            return;
        }
        if (!oge.gonderenUid.isEmpty()) KullaniciProfilActivity.baslat(this, oge.gonderenUid);
    }
}
