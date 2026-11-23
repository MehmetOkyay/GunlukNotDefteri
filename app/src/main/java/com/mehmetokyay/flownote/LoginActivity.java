package com.mehmetokyay.flownote;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.util.AuthHataYardimcisi;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.util.YerelKullanici;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmailLogin, editPasswordLogin;
    private Button btnGirisYap;
    private MaterialButton btnGoogleLogin;
    private TextView textSifremiUnuttum, textKayitOlYonlendir;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EkranYardimcisi.uygula(this, true, true);

        mAuth = FirebaseAuth.getInstance();

        editEmailLogin = findViewById(R.id.editEmailLogin);
        editPasswordLogin = findViewById(R.id.editPasswordLogin);
        btnGirisYap = findViewById(R.id.btnGirisYap);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        textSifremiUnuttum = findViewById(R.id.textSifremiUnuttum);
        textKayitOlYonlendir = findViewById(R.id.textKayitOlYonlendir);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGirisYap.setOnClickListener(v -> {
            String email = editEmailLogin.getText().toString().trim();
            String password = editPasswordLogin.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.login_bos_alan, Toast.LENGTH_SHORT).show();
                return;
            }

            btnGirisYap.setEnabled(false);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            yerelKullaniciyiHazirlaVeGec();
                        } else {
                            Toast.makeText(this, AuthHataYardimcisi.girisHatasi(task.getException()), Toast.LENGTH_LONG).show();
                            btnGirisYap.setEnabled(true);
                        }
                    });
        });

        btnGoogleLogin.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.google_yas_baslik)
                    .setMessage(R.string.google_yas_mesaj)
                    .setPositiveButton(R.string.google_yas_onay, (d, w) -> {
                        btnGoogleLogin.setEnabled(false);
                        mGoogleSignInClient.signOut().addOnCompleteListener(this, ignored ->
                                googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent()));
                    })
                    .setNegativeButton(R.string.iptal, null)
                    .show();
        });

        textSifremiUnuttum.setOnClickListener(v -> {
            String email = editEmailLogin.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, R.string.login_sifirla_eposta_gerekli, Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, R.string.sifre_mail_gonderildi, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, AuthHataYardimcisi.genelHata(task.getException()), Toast.LENGTH_LONG).show();
                }
            });
        });

        textKayitOlYonlendir.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                btnGoogleLogin.setEnabled(true);
                if (result.getData() == null) {
                    Toast.makeText(this, R.string.google_iptal, Toast.LENGTH_SHORT).show();
                    return;
                }
                com.google.android.gms.tasks.Task<GoogleSignInAccount> task =
                        GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    String idToken = account != null ? account.getIdToken() : null;
                    if (idToken == null || idToken.isEmpty()) {
                        Toast.makeText(this, R.string.google_kimlik_yok, Toast.LENGTH_LONG).show();
                        return;
                    }
                    firebaseAuthWithGoogle(idToken);
                } catch (ApiException e) {
                    int kod = e.getStatusCode();
                    Log.e("LoginActivity", "Google girişi hata kodu=" + kod, e);
                    if (kod == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                        Toast.makeText(this, R.string.google_iptal, Toast.LENGTH_SHORT).show();
                    } else if (kod == GoogleSignInStatusCodes.NETWORK_ERROR) {
                        Toast.makeText(this, R.string.auth_ag_hatasi, Toast.LENGTH_LONG).show();
                    } else if (kod == GoogleSignInStatusCodes.DEVELOPER_ERROR) {
                        Toast.makeText(this, R.string.google_yapilandirma_hatasi, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.google_firebase_hata, Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        btnGoogleLogin.setEnabled(false);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        yerelKullaniciyiHazirlaVeGec();
                    } else {
                        btnGoogleLogin.setEnabled(true);
                        Toast.makeText(this, R.string.google_firebase_hata, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void yerelKullaniciyiHazirlaVeGec() {
        final FirebaseUser user = mAuth.getCurrentUser();
        final android.content.Context appContext = getApplicationContext();

        TaskDatabase.ARKA_PLAN.execute(() -> {
            try {
                User yerelKullanici = YerelKullanici.garantiEt(appContext, user);

                if (yerelKullanici != null && user != null
                        && (yerelKullanici.getKullaniciAdi() == null || yerelKullanici.getKullaniciAdi().trim().isEmpty())) {
                    TaskDatabase db = TaskDatabase.getInstance(appContext);

                    String email = user.getEmail();
                    String tabanAd = "kullanici";

                    if (email != null && email.contains("@")) {
                        tabanAd = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase(Locale.ROOT);
                    }

                    String adayAd = "@" + (tabanAd.isEmpty() ? "kullanici" : tabanAd);
                    int sayac = 1;

                    while (sayac <= 40 && (db.userDao().kullaniciAdiSayisi(adayAd) > 0
                            || BulutSosyal.kullaniciAdiAlinmis(adayAd, user.getUid()))) {
                        adayAd = "@" + (tabanAd.isEmpty() ? "kullanici" : tabanAd) + sayac;
                        sayac++;
                    }

                    yerelKullanici.setKullaniciAdi(adayAd);
                    YerelKullanici.ekleVeyaGuncelle(appContext, yerelKullanici);
                }

                BulutSosyal.hesabiEsle(appContext, user, yerelKullanici);

            } catch (Exception e) {
                android.util.Log.e("LoginActivity", "Yerel kullanıcı oluşturulamadı", e);
            }

            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) anaEkranaGec();
            });
        });
    }

    private void anaEkranaGec() {
        String link = getIntent().getStringExtra(LinkActivity.EXTRA_PROFIL);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        if (link != null && !link.isEmpty()) intent.putExtra(LinkActivity.EXTRA_PROFIL, link);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            yerelKullaniciyiHazirlaVeGec();
        }
    }
}