package com.mehmetokyay.flownote;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.util.ResimYukleyici;
import com.mehmetokyay.flownote.util.YerelKullanici;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class ProfiliDuzenleActivity extends AppCompatActivity {

    private ImageView imageProfilFoto;
    private EditText editIsim, editSoyisim, editKullaniciAdi, editMeslek, editKonum, editWeb, editBiyografi;
    private TextView textProfilKaydet, textProfilIptal;

    private User mevcutKullanici;
    private String orjinalKullaniciAdi = "";
    private String secilenResimYolu = "";

    private final ActivityResultLauncher<String> galeriLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String dahiliYol = resmiDahiliHafizayaKaydet(uri);
                    if (!dahiliYol.isEmpty()) {
                        secilenResimYolu = dahiliYol;
                        Bitmap bitmap = guvenliBitmapYukle(dahiliYol, 300, 300);
                        if (bitmap != null) {
                            imageProfilFoto.setImageTintList(null);
                            imageProfilFoto.clearColorFilter();
                            imageProfilFoto.setImageBitmap(bitmap);
                            Toast.makeText(this, getString(R.string.foto_secildi), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, getString(R.string.foto_islenemedi), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.foto_kopyalanamadi), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profili_duzenle);
        EkranYardimcisi.uygula(this, true, true);

        imageProfilFoto = findViewById(R.id.imageProfilFoto);
        editIsim = findViewById(R.id.editDuzenleIsim);
        editSoyisim = findViewById(R.id.editDuzenleSoyisim);
        editKullaniciAdi = findViewById(R.id.editDuzenleKullaniciAdi);
        editMeslek = findViewById(R.id.editDuzenleMeslek);
        editKonum = findViewById(R.id.editDuzenleKonum);
        editWeb = findViewById(R.id.editDuzenleWeb);
        editBiyografi = findViewById(R.id.editDuzenleBiyografi);

        textProfilKaydet = findViewById(R.id.textProfilKaydet);
        textProfilIptal = findViewById(R.id.textProfilIptal);

        textProfilIptal.setOnClickListener(v -> finish());
        textProfilKaydet.setOnClickListener(v -> bilgileriKaydet());

        imageProfilFoto.setOnClickListener(v -> {
            CharSequence[] secenekler = {getString(R.string.galeriden_sec), getString(R.string.fotografi_kaldir)};
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfiliDuzenleActivity.this);
            builder.setTitle(getString(R.string.profil_fotografi_baslik));
            builder.setItems(secenekler, (dialog, secim) -> {
                if (secim == 0) {
                    galeriLauncher.launch("image/*");
                } else if (secim == 1) {
                    secilenResimYolu = "";
                    imageProfilFoto.setImageResource(android.R.drawable.ic_menu_camera);
                    imageProfilFoto.setColorFilter(Color.parseColor("#888888"));
                    Toast.makeText(ProfiliDuzenleActivity.this, getString(R.string.foto_kaldirildi), Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        });

        mevcutBilgileriYukle();
    }

    private String resmiDahiliHafizayaKaydet(Uri uri) {
        File dosya = new File(getFilesDir(), "profil_" + System.currentTimeMillis() + ".jpg");
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return "";
            try (FileOutputStream fos = new FileOutputStream(dosya)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
            }
            return dosya.getAbsolutePath();
        } catch (Exception e) {
            Log.e("ProfiliDuzenle", "Fotoğraf kopyalanamadı", e);
            dosya.delete();
            return "";
        }
    }

    private Bitmap guvenliBitmapYukle(String dosyaYolu, int hedefGenislik, int hedefYukseklik) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(dosyaYolu, options);

            options.inSampleSize = hesaplaInSampleSize(options, hedefGenislik, hedefYukseklik);
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFile(dosyaYolu, options);
        } catch (Exception e) {
            Log.e("ProfiliDuzenle", "Bitmap yüklenemedi", e);
            return null;
        }
    }

    private int hesaplaInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void mevcutBilgileriYukle() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        TaskDatabase.ARKA_PLAN.execute(() -> {
            try {
                mevcutKullanici = YerelKullanici.garantiEt(getApplicationContext(), fUser);

                if (mevcutKullanici != null) {
                    orjinalKullaniciAdi = mevcutKullanici.getKullaniciAdi() != null ? mevcutKullanici.getKullaniciAdi() : "";

                    String dbResim = mevcutKullanici.getProfilResmi() != null ? mevcutKullanici.getProfilResmi() : "";
                    if (secilenResimYolu.isEmpty()) {
                        secilenResimYolu = dbResim;
                    }

                    final String yuklenecekYol = secilenResimYolu;

                    runOnUiThread(() -> {
                        if (mevcutKullanici.getUserIsim() != null) editIsim.setText(mevcutKullanici.getUserIsim());
                        if (mevcutKullanici.getUserSoyisim() != null) editSoyisim.setText(mevcutKullanici.getUserSoyisim());
                        if (mevcutKullanici.getKullaniciAdi() != null) editKullaniciAdi.setText(mevcutKullanici.getKullaniciAdi());
                        if (mevcutKullanici.getMeslek() != null) editMeslek.setText(mevcutKullanici.getMeslek());
                        if (mevcutKullanici.getKonum() != null) editKonum.setText(mevcutKullanici.getKonum());
                        if (mevcutKullanici.getWebSitesi() != null) editWeb.setText(mevcutKullanici.getWebSitesi());
                        if (mevcutKullanici.getBiyografi() != null) editBiyografi.setText(mevcutKullanici.getBiyografi());

                        if (yuklenecekYol.startsWith("http")) {
                            imageProfilFoto.setImageTintList(null);
                            imageProfilFoto.clearColorFilter();
                            ResimYukleyici.goster(imageProfilFoto, yuklenecekYol);
                        } else if (!yuklenecekYol.isEmpty()) {
                            File imgFile = new File(yuklenecekYol);
                            if (imgFile.exists()) {
                                Bitmap bitmap = guvenliBitmapYukle(imgFile.getAbsolutePath(), 300, 300);
                                if (bitmap != null) {
                                    imageProfilFoto.setImageTintList(null);
                                    imageProfilFoto.clearColorFilter();
                                    imageProfilFoto.setImageBitmap(bitmap);
                                }
                            }
                        } else {
                            imageProfilFoto.setImageResource(android.R.drawable.ic_menu_camera);
                            imageProfilFoto.setColorFilter(Color.parseColor("#888888"));
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("ProfiliDuzenle", "Yükleme hatası", e);
            }
        });
    }

    private void bilgileriKaydet() {
        String isim = editIsim.getText().toString().trim();
        String soyisim = editSoyisim.getText().toString().trim();
        String hamKullaniciAdi = editKullaniciAdi.getText().toString().trim();

        String meslek = editMeslek.getText().toString().trim();
        String konum = editKonum.getText().toString().trim();
        String web = editWeb.getText().toString().trim();
        String bio = editBiyografi.getText().toString().trim();

        if (isim.isEmpty() || hamKullaniciAdi.isEmpty()) {
            Toast.makeText(this, getString(R.string.ad_kullanici_zorunlu), Toast.LENGTH_LONG).show();
            return;
        }

        final String yeniKullaniciAdi = hamKullaniciAdi.startsWith("@") ?
                hamKullaniciAdi.toLowerCase(Locale.ROOT) :
                "@" + hamKullaniciAdi.toLowerCase(Locale.ROOT);

        textProfilKaydet.setEnabled(false);
        Toast.makeText(this, getString(R.string.kaydediliyor), Toast.LENGTH_SHORT).show();

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) {
            Toast.makeText(this, getString(R.string.oturum_bulunamadi), Toast.LENGTH_SHORT).show();
            textProfilKaydet.setEnabled(true);
            return;
        }

        TaskDatabase.ARKA_PLAN.execute(() -> {
            try {
                TaskDatabase db = TaskDatabase.getInstance(getApplicationContext());

                if (mevcutKullanici == null) {
                    mevcutKullanici = YerelKullanici.garantiEt(getApplicationContext(), fUser);
                }

                if (mevcutKullanici == null) {
                    runOnUiThread(() -> {
                        textProfilKaydet.setEnabled(true);
                        Toast.makeText(ProfiliDuzenleActivity.this, getString(R.string.kullanici_verisi_hata), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                if (!yeniKullaniciAdi.equals(orjinalKullaniciAdi)) {
                    int kayitliSayi = db.userDao().kullaniciAdiSayisi(yeniKullaniciAdi);
                    boolean buluttaAlinmis = BulutSosyal.kullaniciAdiAlinmis(yeniKullaniciAdi, fUser.getUid());
                    if (kayitliSayi > 0 || buluttaAlinmis) {
                        runOnUiThread(() -> {
                            textProfilKaydet.setEnabled(true);
                            editKullaniciAdi.setError(getString(R.string.kullanici_adi_alinmis));
                            editKullaniciAdi.requestFocus();
                        });
                        return;
                    }
                }

                mevcutKullanici.setUserIsim(isim);
                mevcutKullanici.setUserSoyisim(soyisim);
                mevcutKullanici.setKullaniciAdi(yeniKullaniciAdi);
                mevcutKullanici.setMeslek(meslek);
                mevcutKullanici.setKonum(konum);
                mevcutKullanici.setWebSitesi(web);
                mevcutKullanici.setBiyografi(bio);
                mevcutKullanici.setProfilResmi(secilenResimYolu);

                db.userDao().kullaniciGuncelle(mevcutKullanici);

                BulutSosyal.profiliYayinla(getApplicationContext(), mevcutKullanici, fUser.getUid(), secilenResimYolu,
                        (tamam, hata) -> {
                            if (Boolean.TRUE.equals(tamam)) {
                                TaskDatabase.ARKA_PLAN.execute(() -> {
                                    try {
                                        db.userDao().kullaniciGuncelle(mevcutKullanici);
                                    } catch (Exception ignored) {
                                    }
                                });
                                Toast.makeText(ProfiliDuzenleActivity.this, getString(R.string.profil_guncellendi), Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                textProfilKaydet.setEnabled(true);
                                Toast.makeText(ProfiliDuzenleActivity.this, getString(R.string.hata_tekrar_deneyin), Toast.LENGTH_LONG).show();
                            }
                        });

            } catch (Exception e) {
                Log.e("ProfiliDuzenle", "Kayıt hatası: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    textProfilKaydet.setEnabled(true);
                    Toast.makeText(ProfiliDuzenleActivity.this, getString(R.string.hata_tekrar_deneyin), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}