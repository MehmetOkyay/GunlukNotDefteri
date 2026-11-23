package com.mehmetokyay.flownote;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.util.AuthHataYardimcisi;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.util.YerelKullanici;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText editKullaniciAdi, editIsim, editSoyisim, editDogumTarihi, editEmail, editSifre;
    private Button btnKayitOl;
    private TextView textGeriDon;
    private CheckBox checkSozlesme;

    private FirebaseAuth mAuth;

    private int secilenYil = -1, secilenAy = -1, secilenGun = -1;
    private String formatliDogumTarihi = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        EkranYardimcisi.uygula(this, true, true);

        mAuth = FirebaseAuth.getInstance();

        editKullaniciAdi = findViewById(R.id.editKayitKullaniciAdi);
        editIsim = findViewById(R.id.editKayitIsim);
        editSoyisim = findViewById(R.id.editKayitSoyisim);
        editDogumTarihi = findViewById(R.id.editKayitDogumTarihi);
        editEmail = findViewById(R.id.editKayitEmail);
        editSifre = findViewById(R.id.editKayitSifre);
        btnKayitOl = findViewById(R.id.btnKayitOl);
        textGeriDon = findViewById(R.id.textGeriDon);
        checkSozlesme = findViewById(R.id.checkSozlesme);

        editDogumTarihi.setOnClickListener(v -> takvimGoster());

        if (btnKayitOl != null) btnKayitOl.setOnClickListener(v -> kullaniciyiKaydet());
        if (textGeriDon != null) textGeriDon.setOnClickListener(v -> finish());
    }

    private void takvimGoster() {
        Calendar bugun = Calendar.getInstance();
        int yil = bugun.get(Calendar.YEAR);
        int ay = bugun.get(Calendar.MONTH);
        int gun = bugun.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            secilenYil = year;
            secilenAy = month;
            secilenGun = dayOfMonth;

            formatliDogumTarihi = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            editDogumTarihi.setText(formatliDogumTarihi);
        }, yil, ay, gun);

        Calendar sinir = Calendar.getInstance();
        sinir.set(Calendar.HOUR_OF_DAY, 23);
        sinir.set(Calendar.MINUTE, 59);
        sinir.set(Calendar.SECOND, 59);
        sinir.set(Calendar.MILLISECOND, 999);
        try {
            dialog.getDatePicker().setMaxDate(sinir.getTimeInMillis());
        } catch (IllegalArgumentException e) {
            Log.e("RegisterActivity", "Doğum tarihi üst sınırı ayarlanamadı", e);
        }
        dialog.show();
    }

    private static boolean sifreGuclu(String sifre) {
        if (sifre.length() < 8) return false;
        boolean buyuk = false, kucuk = false, rakam = false, sembol = false;
        for (int i = 0; i < sifre.length(); i++) {
            char c = sifre.charAt(i);
            if (Character.isUpperCase(c)) buyuk = true;
            else if (Character.isLowerCase(c)) kucuk = true;
            else if (Character.isDigit(c)) rakam = true;
            else if (!Character.isWhitespace(c)) sembol = true;
        }
        return buyuk && kucuk && rakam && sembol;
    }

    private void kullaniciyiKaydet() {
        final String hamKullaniciAdi = editKullaniciAdi.getText().toString().trim();
        final String isim = editIsim.getText().toString().trim();
        final String soyisim = editSoyisim.getText().toString().trim();
        final String email = editEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String sifre = editSifre.getText().toString();

        if (hamKullaniciAdi.isEmpty() || isim.isEmpty() || soyisim.isEmpty() || formatliDogumTarihi.isEmpty() || email.isEmpty() || sifre.isEmpty()) {
            Toast.makeText(this, R.string.register_tum_alanlar, Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar bugun = Calendar.getInstance();
        Calendar dogumGunu = Calendar.getInstance();
        dogumGunu.set(secilenYil, secilenAy, secilenGun);

        int yas = bugun.get(Calendar.YEAR) - dogumGunu.get(Calendar.YEAR);
        if (bugun.get(Calendar.MONTH) < dogumGunu.get(Calendar.MONTH) ||
                (bugun.get(Calendar.MONTH) == dogumGunu.get(Calendar.MONTH) && bugun.get(Calendar.DAY_OF_MONTH) < dogumGunu.get(Calendar.DAY_OF_MONTH))) {
            yas--;
        }

        if (yas < 18) {
            Toast.makeText(this, R.string.register_yas_siniri, Toast.LENGTH_LONG).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.register_gecersiz_eposta, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!sifreGuclu(sifre)) {
            Toast.makeText(this, R.string.register_sifre_zayif, Toast.LENGTH_LONG).show();
            return;
        }

        if (checkSozlesme != null && !checkSozlesme.isChecked()) {
            Toast.makeText(this, R.string.register_sozlesme_zorunlu, Toast.LENGTH_SHORT).show();
            return;
        }

        final String kullaniciAdi = hamKullaniciAdi.startsWith("@") ?
                hamKullaniciAdi.toLowerCase(Locale.ROOT) :
                "@" + hamKullaniciAdi.toLowerCase(Locale.ROOT);

        if (btnKayitOl != null) btnKayitOl.setEnabled(false);
        Toast.makeText(this, R.string.register_kullanici_adi_kontrol, Toast.LENGTH_SHORT).show();

        final android.content.Context appContext = getApplicationContext();

        TaskDatabase.ARKA_PLAN.execute(() -> {
            int kayitliSayi = 0;
            try {
                TaskDatabase db = TaskDatabase.getInstance(appContext);
                kayitliSayi = db.userDao().kullaniciAdiSayisi(kullaniciAdi);
            } catch (Exception e) {
                Log.e("RegisterActivity", "Yerel kullanıcı adı kontrolü", e);
            }
            boolean buluttaAlinmis = false;
            try {
                buluttaAlinmis = BulutSosyal.kullaniciAdiAlinmis(kullaniciAdi, "");
            } catch (Exception e) {
                Log.e("RegisterActivity", "Kullanıcı adı kontrolü", e);
            }

            if (kayitliSayi > 0 || buluttaAlinmis) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (btnKayitOl != null) btnKayitOl.setEnabled(true);
                    editKullaniciAdi.setError(getString(R.string.kullanici_adi_alinmis));
                    editKullaniciAdi.requestFocus();
                });
            } else {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(RegisterActivity.this, R.string.register_kayit_yapiliyor, Toast.LENGTH_SHORT).show();

                    mAuth.createUserWithEmailAndPassword(email, sifre)
                            .addOnCompleteListener(RegisterActivity.this, task -> {
                                if (task.isSuccessful()) {
                                    TaskDatabase.ARKA_PLAN.execute(() -> {
                                        try {
                                            User yeniUser = new User();
                                            yeniUser.setUserIsim(isim);
                                            yeniUser.setUserSoyisim(soyisim);
                                            yeniUser.setDogumTarihi(formatliDogumTarihi);
                                            yeniUser.setEmail(email);
                                            yeniUser.setKullaniciAdi(kullaniciAdi);
                                            YerelKullanici.ekleVeyaGuncelle(appContext, yeniUser);
                                        } catch (Exception e) {
                                            Log.e("RegisterActivity", "Yerel kayıt yazılamadı", e);
                                        }
                                        runOnUiThread(() -> {
                                            if (isFinishing() || isDestroyed()) return;
                                            Toast.makeText(RegisterActivity.this, R.string.register_basarili, Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        });
                                    });
                                } else {
                                    if (btnKayitOl != null) btnKayitOl.setEnabled(true);
                                    Toast.makeText(RegisterActivity.this, AuthHataYardimcisi.kayitHatasi(task.getException()), Toast.LENGTH_LONG).show();
                                }
                            });
                });
            }
        });
    }
}