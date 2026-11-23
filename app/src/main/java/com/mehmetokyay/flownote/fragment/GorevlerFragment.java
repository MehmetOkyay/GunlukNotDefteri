package com.mehmetokyay.flownote.fragment;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.adapter.TaskAdapter;
import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.Task;
import com.mehmetokyay.flownote.util.GorevDialogYardimcisi;
import com.mehmetokyay.flownote.util.GorevHatirlatma;
import com.mehmetokyay.flownote.util.GorevYardimcisi;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GorevlerFragment extends Fragment {
    private static final String TAG = "GorevlerFragment";

    private RecyclerView recyclerViewTask;
    private CalendarView calendarView;
    private TextView textViewGunBasligi;
    private TextView textViewEtkinlikSayisi;
    private TextView textViewTakvimAy;
    private String secilenTakvimTarihi;
    private Calendar seciliGunTakvimi;
    private Calendar gorunenAy;

    private List<Task> tumGorevlerCache = new ArrayList<>();

    private final Handler anaThread = new Handler(Looper.getMainLooper());
    private BottomSheetDialog aktifDialog;
    private TaskAdapter taskAdapter;
    private boolean tumGorevlerModu;
    private TextView textTumunuGorEtiket;
    private final ActivityResultLauncher<String> bildirimIzni = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> { });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gorevler, container, false);

        Calendar bugun = gununBaslangici(Calendar.getInstance());
        seciliGunTakvimi = (Calendar) bugun.clone();
        gorunenAy = (Calendar) bugun.clone();
        secilenTakvimTarihi = GorevYardimcisi.tarihMetni(bugun);

        recyclerViewTask = view.findViewById(R.id.recyclerViewTasks);
        calendarView = view.findViewById(R.id.calendarView);
        textViewGunBasligi = view.findViewById(R.id.textViewGunBasligi);
        textViewEtkinlikSayisi = view.findViewById(R.id.textViewEtkinlikSayisi);
        textViewTakvimAy = view.findViewById(R.id.textViewTakvimAy);

        if (recyclerViewTask != null) {
            recyclerViewTask.setLayoutManager(new LinearLayoutManager(requireContext()));
            taskAdapter = new TaskAdapter(new ArrayList<>(), secilenTakvimTarihi, this::verileriYenile);
            recyclerViewTask.setAdapter(taskAdapter);
        }

        if (calendarView != null) {
            takvimIcYuzeyleriniGorunmezYap();
            try {
                int headerId = getResources().getIdentifier("calendarHeader", "id", requireContext().getPackageName());
                if (headerId != 0) {
                    View applandeoHeader = calendarView.findViewById(headerId);
                    if (applandeoHeader != null) applandeoHeader.setVisibility(View.GONE);
                } else if (calendarView.getChildCount() > 0) {
                    calendarView.getChildAt(0).setVisibility(View.GONE);
                }
            } catch (Exception ignored) {}

            try { calendarView.setAbbreviationsBarVisibility(View.GONE); } catch (Exception ignored) {}
        }

        takvimAyBasliginiGuncelle();

        View textViewTumunuGor = view.findViewById(R.id.textViewTumunuGor);
        textTumunuGorEtiket = view.findViewById(R.id.textTumunuGorEtiket);
        if (textViewTumunuGor != null) {
            textViewTumunuGor.setOnClickListener(v -> {
                tumGorevlerModu = !tumGorevlerModu;
                if (textTumunuGorEtiket != null) {
                    textTumunuGorEtiket.setText(tumGorevlerModu ? R.string.gune_don : R.string.see_all);
                }
                arayuzuGuncelle();
            });
        }

        View btnOnceki = view.findViewById(R.id.buttonOncekiAy);
        if (btnOnceki != null) btnOnceki.setOnClickListener(v -> ayiDegistir(-1));

        View btnSonraki = view.findViewById(R.id.buttonSonrakiAy);
        if (btnSonraki != null) btnSonraki.setOnClickListener(v -> ayiDegistir(1));

        if (calendarView != null) {
            calendarView.setOnDayClickListener(new OnDayClickListener() {
                @Override
                public void onDayClick(EventDay eventDay) {
                    Calendar tiklananGun = gununBaslangici(eventDay.getCalendar());
                    seciliGunTakvimi = (Calendar) tiklananGun.clone();
                    gorunenAy = (Calendar) tiklananGun.clone();
                    secilenTakvimTarihi = GorevYardimcisi.tarihMetni(tiklananGun);
                    tumGorevlerModu = false;
                    if (textTumunuGorEtiket != null) textTumunuGorEtiket.setText(R.string.see_all);
                    calendarView.post(() -> {
                        if (!isAdded() || calendarView == null) return;
                        takvimAyBasliginiGuncelle();
                        arayuzuGuncelle();
                    });
                }
            });

            calendarView.setOnPreviousPageChangeListener(this::takvimSayfasiniSonraGuncelle);
            calendarView.setOnForwardPageChangeListener(this::takvimSayfasiniSonraGuncelle);
        }

        verileriYenile();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        anaThread.removeCallbacksAndMessages(null);
        if (aktifDialog != null) {
            aktifDialog.dismiss();
            aktifDialog = null;
        }
        if (recyclerViewTask != null) recyclerViewTask.setAdapter(null);
        recyclerViewTask = null;
        taskAdapter = null;
        calendarView = null;
        textViewGunBasligi = null;
        textViewEtkinlikSayisi = null;
        textViewTakvimAy = null;
    }

    private void verileriYenile() {
        Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        TaskDatabase.ARKA_PLAN.execute(() -> {
            try {
                final List<Task> liste = TaskDatabase.getInstance(appContext).taskDao().hepsiniListele();
                anaThread.post(() -> {
                    if (!isAdded() || recyclerViewTask == null) return;
                    tumGorevlerCache = liste != null ? liste : new ArrayList<>();
                    arayuzuGuncelle();
                });
            } catch (Exception e) {
                Log.e(TAG, "Görevler okunamadı", e);
            }
        });
    }

    private void arayuzuGuncelle() {
        if (!isAdded() || recyclerViewTask == null || taskAdapter == null) return;
        if (recyclerViewTask.isComputingLayout()) {
            recyclerViewTask.post(this::arayuzuGuncelle);
            return;
        }

        List<Task> seciliGunGorevleri = new ArrayList<>();
        if (tumGorevlerModu) {
            seciliGunGorevleri.addAll(tumGorevlerCache);
            Collections.sort(seciliGunGorevleri, (a, b) -> {
                int[] ta = GorevYardimcisi.tarihAyristir(a.getGorevTarihi());
                int[] tb = GorevYardimcisi.tarihAyristir(b.getGorevTarihi());
                int ka = ta == null ? 0 : ta[2] * 10000 + ta[1] * 100 + ta[0];
                int kb = tb == null ? 0 : tb[2] * 10000 + tb[1] * 100 + tb[0];
                if (ka != kb) return Integer.compare(ka, kb);
                String sa = a.getGorevSaati() == null ? "" : a.getGorevSaati();
                String sb = b.getGorevSaati() == null ? "" : b.getGorevSaati();
                return sa.compareTo(sb);
            });
        } else {
            for (Task task : tumGorevlerCache) {
                if (GorevYardimcisi.gorevGundeMi(task, seciliGunTakvimi)) {
                    seciliGunGorevleri.add(task);
                }
            }
        }

        String tarihBaglami = tumGorevlerModu
                ? GorevYardimcisi.tarihMetni(gununBaslangici(Calendar.getInstance()))
                : secilenTakvimTarihi;
        taskAdapter.guncelle(seciliGunGorevleri, tarihBaglami);
        if (tumGorevlerModu && textViewGunBasligi != null && textViewEtkinlikSayisi != null) {
            textViewGunBasligi.setText(getString(R.string.tum_gorevler));
            textViewEtkinlikSayisi.setText(seciliGunGorevleri.size() + " " + getString(R.string.aktiviteler));
        } else {
            gunOzetiniGuncelle(secilenTakvimTarihi, seciliGunGorevleri.size());
        }

        takvimRozetleriniCiz();
    }

    private void takvimSayfasiniSonraGuncelle() {
        if (calendarView == null) return;
        calendarView.post(() -> {
            if (!isAdded() || calendarView == null) return;
            takvimSayfasiniGuncelle(true);
        });
    }

    private void takvimRozetleriniCiz() {
        if (calendarView == null || !isAdded()) return;
        List<EventDay> events = new ArrayList<>();
        Map<Integer, Bitmap> rozetOnbellegi = new HashMap<>();

        Calendar taramaTakvimi = (Calendar) gorunenAy.clone();
        taramaTakvimi.set(Calendar.DAY_OF_MONTH, 1);
        taramaTakvimi.add(Calendar.DAY_OF_MONTH, -10);

        for (int i = 0; i < 50; i++) {
            int gunlukGorevSayisi = 0;
            int tamamlananGorevSayisi = 0;
            final int gun = taramaTakvimi.get(Calendar.DAY_OF_MONTH);
            final int ay = taramaTakvimi.get(Calendar.MONTH) + 1;
            final int yil = taramaTakvimi.get(Calendar.YEAR);
            final String taramaTarihiStr = GorevYardimcisi.tarihMetni(gun, ay, yil);

            for (Task task : tumGorevlerCache) {
                if (GorevYardimcisi.gorevGundeMi(task, gun, ay, yil)) {
                    gunlukGorevSayisi++;
                    if (GorevYardimcisi.tamamlandiMi(task, taramaTarihiStr)) {
                        tamamlananGorevSayisi++;
                    }
                }
            }

            if (gunlukGorevSayisi > 0) {
                boolean hepsiTamam = (gunlukGorevSayisi == tamamlananGorevSayisi);
                int anahtar = gunlukGorevSayisi * 2 + (hepsiTamam ? 1 : 0);
                Bitmap bitmap = rozetOnbellegi.get(anahtar);
                if (bitmap == null) {
                    bitmap = rozetBitmapOlustur(gunlukGorevSayisi, hepsiTamam);
                    rozetOnbellegi.put(anahtar, bitmap);
                }
                Drawable rozet = new BitmapDrawable(getResources(), bitmap);
                events.add(new EventDay((Calendar) taramaTakvimi.clone(), rozet));
            }
            taramaTakvimi.add(Calendar.DAY_OF_MONTH, 1);
        }
        try {
            calendarView.setEvents(events);
        } catch (RuntimeException e) {
            Log.e(TAG, "Takvim rozetleri çizilemedi", e);
        }
    }

    public void gorevEkleDialoguGoster() {
        final Context context = getContext();
        if (!isAdded() || context == null) return;
        final Context appContext = context.getApplicationContext();

        if (aktifDialog != null) aktifDialog.dismiss();
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        aktifDialog = bottomSheetDialog;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_gorev_ekle, null);
        bottomSheetDialog.setContentView(dialogView);

        EditText editTextBaslik = dialogView.findViewById(R.id.editTextBaslik);
        EditText editTextAciklama = dialogView.findViewById(R.id.editTextAciklama);
        TextView buttonTarihSec = dialogView.findViewById(R.id.buttonTarihSec);
        TextView buttonSaatSec = dialogView.findViewById(R.id.buttonSaatSec);
        Button buttonKaydet = dialogView.findViewById(R.id.buttonKaydet);
        ProgressBar progressBarKaydet = dialogView.findViewById(R.id.progressBarKaydet);
        RadioGroup radioGroupYineleme = dialogView.findViewById(R.id.radioGroupYineleme);
        CompoundButton switchHatirlat = dialogView.findViewById(R.id.switchHatirlat);
        if (switchHatirlat != null && switchHatirlat.isChecked()) hatirlatmaIzniIste();

        View btnKapat = dialogView.findViewById(R.id.buttonKapat);
        View btnVazgec = dialogView.findViewById(R.id.buttonVazgec);
        if (btnKapat != null) btnKapat.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnVazgec != null) btnVazgec.setOnClickListener(v -> bottomSheetDialog.dismiss());

        GorevDialogYardimcisi.gorunumuHazirla(dialogView);

        final String[] secilenTarih = {secilenTakvimTarihi};
        final String[] secilenSaat = {"12:00"};
        if (buttonTarihSec != null) buttonTarihSec.setText(secilenTarih[0]);
        if (buttonSaatSec != null) buttonSaatSec.setText(secilenSaat[0]);

        if (buttonTarihSec != null) {
            buttonTarihSec.setOnClickListener(v1 -> {
                Calendar takvim = (Calendar) seciliGunTakvimi.clone();
                DatePickerDialog datePicker = new DatePickerDialog(context,
                        (view1, secilenYil, secilenAy, secilenGun) -> {
                            secilenTarih[0] = GorevYardimcisi.tarihMetni(secilenGun, secilenAy + 1, secilenYil);
                            buttonTarihSec.setText(secilenTarih[0]);
                        }, takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            });
        }

        final boolean is24HourFormat = GorevDialogYardimcisi.saat24MuKullan(context);

        if (buttonSaatSec != null) {
            buttonSaatSec.setOnClickListener(v2 -> {
                Calendar saatZaman = Calendar.getInstance();
                TimePickerDialog timePicker = new TimePickerDialog(context,
                        (view1, s, d) -> {
                            secilenSaat[0] = String.format(Locale.getDefault(), "%02d:%02d", s, d);
                            buttonSaatSec.setText(secilenSaat[0]);
                        }, saatZaman.get(Calendar.HOUR_OF_DAY), saatZaman.get(Calendar.MINUTE), is24HourFormat);
                timePicker.show();
            });
        }

        if (buttonKaydet != null) {
            buttonKaydet.setOnClickListener(v3 -> {
                String baslik = editTextBaslik != null && editTextBaslik.getText() != null ? editTextBaslik.getText().toString().trim() : "";

                if (baslik.isEmpty()) {
                    if (editTextBaslik != null) editTextBaslik.setError(getString(R.string.task_title_required));
                    return;
                }
                if (baslik.length() > GorevDialogYardimcisi.BASLIK_MAX_UZUNLUK) {
                    if (editTextBaslik != null) editTextBaslik.setError(getString(R.string.task_title_too_long));
                    return;
                }

                Task yeniTask = new Task();
                yeniTask.setGorevBaslik(baslik);
                yeniTask.setGorevAciklama(editTextAciklama != null && editTextAciklama.getText() != null
                        ? editTextAciklama.getText().toString().trim() : "");
                yeniTask.setGorevSaati(secilenSaat[0]);
                yeniTask.setGorevTarihi(secilenTarih[0].isEmpty() ? secilenTakvimTarihi : secilenTarih[0]);
                yeniTask.setGorevTamamlandiMi(false);
                yeniTask.setTamamlananTarihler("");
                yeniTask.setYinelemeDurumu(GorevDialogYardimcisi.secilenTekrarKodu(radioGroupYineleme));
                yeniTask.setHatirlat(switchHatirlat == null || switchHatirlat.isChecked());
                final boolean hatirlatIstendi = yeniTask.isHatirlat();

                buttonKaydet.setEnabled(false);
                if (progressBarKaydet != null) progressBarKaydet.setVisibility(View.VISIBLE);

                TaskDatabase.ARKA_PLAN.execute(() -> {
                    boolean basarili = false;
                    boolean hatirlatmaGecmis = false;
                    try {
                        long id = TaskDatabase.getInstance(appContext).taskDao().gorevEkle(yeniTask);
                        yeniTask.setId(id);
                        GorevHatirlatma.planla(appContext, yeniTask);
                        hatirlatmaGecmis = hatirlatIstendi && GorevHatirlatma.sonrakiAn(yeniTask) < 0;
                        basarili = true;
                    } catch (Exception e) {
                        Log.e(TAG, "Görev kaydedilemedi", e);
                    }
                    final boolean sonuc = basarili;
                    final boolean gecmis = hatirlatmaGecmis;
                    anaThread.post(() -> {
                        if (sonuc) {
                            Toast.makeText(appContext, R.string.task_saved, Toast.LENGTH_SHORT).show();
                            if (gecmis) {
                                Toast.makeText(appContext, R.string.hatirlatma_gecmis, Toast.LENGTH_LONG).show();
                            }
                            bottomSheetDialog.dismiss();
                            verileriYenile();
                        } else {
                            buttonKaydet.setEnabled(true);
                            if (progressBarKaydet != null) progressBarKaydet.setVisibility(View.GONE);
                            Toast.makeText(appContext, R.string.task_save_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            });
        }

        bottomSheetDialog.setOnDismissListener(d -> {
            if (aktifDialog == bottomSheetDialog) aktifDialog = null;
        });
        bottomSheetDialog.show();
    }

    private void hatirlatmaIzniIste() {
        if (Build.VERSION.SDK_INT < 33 || getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            bildirimIzni.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void takvimIcYuzeyleriniGorunmezYap() {
        if (calendarView == null) return;
        calendarView.setBackgroundColor(Color.TRANSPARENT);
        for (int i = 0; i < calendarView.getChildCount(); i++) {
            View child = calendarView.getChildAt(i);
            if (child != null) child.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void ayiDegistir(int miktar) {
        if (seciliGunTakvimi == null) seciliGunTakvimi = Calendar.getInstance();
        if (gorunenAy == null) gorunenAy = (Calendar) seciliGunTakvimi.clone();

        Calendar hedefAy = (Calendar) gorunenAy.clone();
        hedefAy.set(Calendar.DAY_OF_MONTH, 1);
        hedefAy.add(Calendar.MONTH, miktar);

        Calendar hedefGun = (Calendar) seciliGunTakvimi.clone();
        hedefGun.set(Calendar.DAY_OF_MONTH, 1);
        hedefGun.set(Calendar.YEAR, hedefAy.get(Calendar.YEAR));
        hedefGun.set(Calendar.MONTH, hedefAy.get(Calendar.MONTH));
        int ayinSonGunu = hedefGun.getActualMaximum(Calendar.DAY_OF_MONTH);
        hedefGun.set(Calendar.DAY_OF_MONTH, Math.min(seciliGunTakvimi.get(Calendar.DAY_OF_MONTH), ayinSonGunu));
        hedefGun = gununBaslangici(hedefGun);

        seciliGunTakvimi = hedefGun;
        gorunenAy = hedefAy;
        secilenTakvimTarihi = GorevYardimcisi.tarihMetni(hedefGun);
        takvimAyBasliginiGuncelle();
        try { if (calendarView != null) calendarView.setDate(hedefGun); } catch (Exception ignored) {}
        arayuzuGuncelle();
    }

    private void takvimSayfasiniGuncelle(boolean secilenGunuYeniAyaTasi) {
        if (calendarView == null) return;
        Calendar sayfaAyi = calendarView.getCurrentPageDate();
        gorunenAy = gununBaslangici(sayfaAyi);

        if (secilenGunuYeniAyaTasi && seciliGunTakvimi != null) {
            Calendar hedefGun = (Calendar) gorunenAy.clone();
            int ayinSonGunu = hedefGun.getActualMaximum(Calendar.DAY_OF_MONTH);
            int gun = Math.min(seciliGunTakvimi.get(Calendar.DAY_OF_MONTH), ayinSonGunu);
            hedefGun.set(Calendar.DAY_OF_MONTH, gun);
            seciliGunTakvimi = gununBaslangici(hedefGun);
            secilenTakvimTarihi = GorevYardimcisi.tarihMetni(seciliGunTakvimi);
            arayuzuGuncelle();
        }
        takvimAyBasliginiGuncelle();
    }

    private void takvimAyBasliginiGuncelle() {
        if (textViewTakvimAy == null || gorunenAy == null) return;
        Locale locale = Locale.getDefault();
        String ay = gorunenAy.getDisplayName(Calendar.MONTH, Calendar.LONG, locale);
        String ayBuyuk = ay == null ? "" : ay.toUpperCase(locale);
        textViewTakvimAy.setText(String.format(locale, "%s %d", ayBuyuk, gorunenAy.get(Calendar.YEAR)));
    }

    private Calendar gununBaslangici(Calendar kaynak) {
        Calendar sonuc = (Calendar) kaynak.clone();
        sonuc.set(Calendar.HOUR_OF_DAY, 0);
        sonuc.set(Calendar.MINUTE, 0);
        sonuc.set(Calendar.SECOND, 0);
        sonuc.set(Calendar.MILLISECOND, 0);
        return sonuc;
    }

    private void gunOzetiniGuncelle(String tarih, int aktiviteSayisi) {
        if (!isAdded() || textViewGunBasligi == null || textViewEtkinlikSayisi == null) return;
        String bugununTarihi = GorevYardimcisi.tarihMetni(gununBaslangici(Calendar.getInstance()));
        textViewGunBasligi.setText(tarih.equals(bugununTarihi)
                ? getString(R.string.today).toUpperCase(Locale.getDefault()) : tarih);
        textViewEtkinlikSayisi.setText(aktiviteSayisi + " " + getString(R.string.aktiviteler));
    }

    private Bitmap rozetBitmapOlustur(int count, boolean hepsiTamam) {
        int size = 50;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.parseColor(hepsiTamam ? "#4CAF50" : "#E53935"));

        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(count > 9 ? 22f : 26f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        float textY = radius - ((paint.descent() + paint.ascent()) / 2);
        canvas.drawText(String.valueOf(count), radius, textY, paint);

        return bitmap;
    }
}
