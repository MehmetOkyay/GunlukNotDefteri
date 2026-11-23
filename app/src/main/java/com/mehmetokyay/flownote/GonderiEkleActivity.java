package com.mehmetokyay.flownote;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.FotoDenetimi;
import com.mehmetokyay.flownote.util.IcerikDenetimi;
import com.mehmetokyay.flownote.util.YerelKullanici;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GonderiEkleActivity extends AppCompatActivity {

    private ImageView btnIptal, btnGaleri, imageOnizleme;
    private MaterialButton btnPaylas;
    private EditText editGonderiMetni;
    private Uri secilenResimUri = null;

    private final ActivityResultLauncher<Intent> resimSecici = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri aday = result.getData().getData();
                    if (aday != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    aday, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {
                        }
                        fotoKontrolEt(aday);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gonderi_ekle);

        btnIptal = findViewById(R.id.btnIptal);
        btnPaylas = findViewById(R.id.btnPaylas);
        editGonderiMetni = findViewById(R.id.editGonderiMetni);
        btnGaleri = findViewById(R.id.btnGaleri);
        imageOnizleme = findViewById(R.id.imageOnizleme);

        btnIptal.setOnClickListener(v -> finish());

        btnGaleri.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            resimSecici.launch(intent);
        });

        btnPaylas.setOnClickListener(v -> paylas());
    }

    private void fotoKontrolEt(Uri aday) {
        paylasmayiKilitle(true, getString(R.string.foto_kontrol_ediliyor));
        final android.content.Context app = getApplicationContext();
        TaskDatabase.ARKA_PLAN.execute(() -> {
            FotoDenetimi.Sonuc sonuc = FotoDenetimi.denetle(app, aday);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                paylasmayiKilitle(false, getString(R.string.paylas));
                if (sonuc == FotoDenetimi.Sonuc.UYGUNSUZ) {
                    resmiTemizle();
                    Toast.makeText(this, R.string.icerik_red_foto, Toast.LENGTH_LONG).show();
                    return;
                }
                if (sonuc == FotoDenetimi.Sonuc.OKUNAMADI) {
                    resmiTemizle();
                    Toast.makeText(this, R.string.foto_kontrol_edilemedi, Toast.LENGTH_LONG).show();
                    return;
                }
                secilenResimUri = aday;
                imageOnizleme.setImageURI(aday);
                imageOnizleme.setVisibility(View.VISIBLE);
            });
        });
    }

    private void paylas() {
        String metin = editGonderiMetni.getText().toString().trim();

        if (metin.isEmpty() && secilenResimUri == null) {
            Toast.makeText(this, R.string.gonderi_bos, Toast.LENGTH_SHORT).show();
            return;
        }

        IcerikDenetimi.Kategori yasak = IcerikDenetimi.metniDenetle(metin);
        if (yasak != null) {
            Toast.makeText(this, yasak.mesaj, Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser firebaseKullanici = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseKullanici == null) {
            Toast.makeText(this, R.string.oturum_bulunamadi, Toast.LENGTH_SHORT).show();
            return;
        }

        paylasmayiKilitle(true, getString(R.string.foto_kontrol_ediliyor));
        final Uri resim = secilenResimUri;
        final android.content.Context app = getApplicationContext();

        TaskDatabase.ARKA_PLAN.execute(() -> {
            if (resim != null) {
                FotoDenetimi.Sonuc foto = FotoDenetimi.denetle(app, resim);
                if (foto != FotoDenetimi.Sonuc.UYGUN) {
                    runOnUiThread(() -> {
                        paylasmayiKilitle(false, getString(R.string.paylas));
                        if (foto == FotoDenetimi.Sonuc.UYGUNSUZ) {
                            resmiTemizle();
                            Toast.makeText(this, R.string.icerik_red_foto, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, R.string.foto_kontrol_edilemedi, Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
            }
            try {
                User aktif = YerelKullanici.garantiEt(app, firebaseKullanici);
                if (aktif == null) {
                    runOnUiThread(() -> {
                        paylasmayiKilitle(false, getString(R.string.paylas));
                        Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                BulutSosyal.gonderiPaylas(app, firebaseKullanici.getUid(), metin, resim, aktif, (tamam, hata) -> {
                    paylasmayiKilitle(false, getString(R.string.paylas));
                    if (Boolean.TRUE.equals(tamam)) {
                        Toast.makeText(this, R.string.gonderi_paylasildi, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    paylasmayiKilitle(false, getString(R.string.paylas));
                    Toast.makeText(this, R.string.hata_tekrar_deneyin, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void resmiTemizle() {
        secilenResimUri = null;
        imageOnizleme.setImageDrawable(null);
        imageOnizleme.setVisibility(View.GONE);
    }

    private void paylasmayiKilitle(boolean kilitli, String dugmeMetni) {
        btnPaylas.setEnabled(!kilitli);
        btnGaleri.setEnabled(!kilitli);
        btnPaylas.setText(dugmeMetni);
    }
}
