package com.mehmetokyay.flownote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mehmetokyay.flownote.util.BulutSosyal;
import com.google.firebase.auth.FirebaseAuth;

public class LinkActivity extends AppCompatActivity {

    public static final String EXTRA_PROFIL = "profil_link";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri == null && getIntent() != null && getIntent().getStringExtra(EXTRA_PROFIL) != null) {
            uri = Uri.parse(getIntent().getStringExtra(EXTRA_PROFIL));
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent giris = new Intent(this, LoginActivity.class);
            if (uri != null) giris.putExtra(EXTRA_PROFIL, uri.toString());
            giris.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(giris);
            finish();
            return;
        }
        if (uri == null || uri.getLastPathSegment() == null) {
            anaEkran();
            return;
        }
        final String ad = uri.getLastPathSegment();
        BulutSosyal.kullaniciAdiylaBul(ad, (profil, hata) -> {
            if (isFinishing() || isDestroyed()) return;
        boolean anaZatenVar = getIntent() != null && getIntent().getBooleanExtra("arkada_ana", false);
        if (profil == null || profil.getUid() == null || profil.getUid().isEmpty()) {
            Toast.makeText(this, R.string.kullanici_bulunamadi, Toast.LENGTH_SHORT).show();
            if (!anaZatenVar && isTaskRoot()) startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        if (!anaZatenVar && isTaskRoot()) {
            startActivity(new Intent(this, MainActivity.class));
        }
            KullaniciProfilActivity.baslat(this, profil.getUid());
            finish();
        });
    }

    private void anaEkran() {
        if (isTaskRoot()) startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
