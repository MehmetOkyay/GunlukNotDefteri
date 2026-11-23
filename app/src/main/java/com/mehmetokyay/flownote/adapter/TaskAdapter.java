package com.mehmetokyay.flownote.adapter;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.Task;
import com.mehmetokyay.flownote.util.GorevDialogYardimcisi;
import com.mehmetokyay.flownote.util.GorevHatirlatma;
import com.mehmetokyay.flownote.util.GorevYardimcisi;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private static final String TAG = "TaskAdapter";

    private final List<Task> taskList = new ArrayList<>();
    private String secilenTarih;
    private final Runnable onDataChanged;
    private final Handler anaThread = new Handler(Looper.getMainLooper());

    public TaskAdapter(List<Task> taskList, String secilenTarih, Runnable onDataChanged) {
        this.secilenTarih = secilenTarih;
        this.onDataChanged = onDataChanged;
        if (taskList != null) this.taskList.addAll(taskList);
    }

    public void guncelle(List<Task> yeni, String tarih) {
        secilenTarih = tarih == null ? "" : tarih;
        taskList.clear();
        if (yeni != null) taskList.addAll(yeni);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        final Task task = taskList.get(position);
        final Context context = holder.itemView.getContext();

        holder.textViewBaslik.setText(task.getGorevBaslik());
        holder.textViewTarihSaat.setText(task.getGorevSaati());
        holder.textViewAciklama.setText(task.getGorevAciklama());

        boolean isCompleted = GorevYardimcisi.tamamlandiMi(task, secilenTarih);

        if (isCompleted) {
            holder.textViewBaslik.setPaintFlags(holder.textViewBaslik.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.textViewBaslik.setTextColor(ContextCompat.getColor(context, R.color.renk_metin_ipucu));
        } else {
            holder.textViewBaslik.setPaintFlags(holder.textViewBaslik.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.textViewBaslik.setTextColor(ContextCompat.getColor(context, R.color.renk_vurgu));
        }

        holder.iconMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), holder.iconMore);
            popup.inflate(R.menu.task_menu);

            if (GorevYardimcisi.tamamlandiMi(task, secilenTarih)) {
                popup.getMenu().findItem(R.id.action_tamamla).setTitle(R.string.task_undo_done);
            }

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_tamamla) {
                    GorevYardimcisi.tamamlanmayiDegistir(task, secilenTarih);
                    arkaPlandaGuncelle(context, task, null);
                    return true;

                } else if (id == R.id.action_duzenle) {
                    gorevDuzenleDialoguGoster(v.getContext(), task);
                    return true;

                } else if (id == R.id.action_sil) {
                    arkaPlandaSil(context, task);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    private void arkaPlandaGuncelle(Context context, Task task, Runnable basariSonrasi) {
        final Context appContext = context.getApplicationContext();
        TaskDatabase.ARKA_PLAN.execute(() -> {
            boolean basarili = false;
            try {
                TaskDatabase.getInstance(appContext).taskDao().gorevDuzenle(task);
                GorevHatirlatma.planla(appContext, task);
                basarili = true;
            } catch (Exception e) {
                Log.e(TAG, "Görev güncellenemedi", e);
            }
            final boolean sonuc = basarili;
            anaThread.post(() -> {
                if (!sonuc) Toast.makeText(appContext, R.string.task_save_failed, Toast.LENGTH_SHORT).show();
                if (sonuc && basariSonrasi != null) basariSonrasi.run();
                if (onDataChanged != null) onDataChanged.run();
            });
        });
    }

    private void arkaPlandaSil(Context context, Task task) {
        final Context appContext = context.getApplicationContext();
        TaskDatabase.ARKA_PLAN.execute(() -> {
            boolean basarili = false;
            try {
                TaskDatabase.getInstance(appContext).taskDao().gorevSil(task);
                GorevHatirlatma.iptal(appContext, task.getId());
                basarili = true;
            } catch (Exception e) {
                Log.e(TAG, "Görev silinemedi", e);
            }
            final boolean sonuc = basarili;
            anaThread.post(() -> {
                Toast.makeText(appContext, sonuc ? R.string.task_deleted : R.string.task_save_failed,
                        Toast.LENGTH_SHORT).show();
                if (onDataChanged != null) onDataChanged.run();
            });
        });
    }

    private void gorevDuzenleDialoguGoster(Context context, Task task) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_gorev_ekle, null);
        bottomSheetDialog.setContentView(dialogView);

        EditText editTextBaslik = dialogView.findViewById(R.id.editTextBaslik);
        EditText editTextAciklama = dialogView.findViewById(R.id.editTextAciklama);
        TextView buttonTarihSec = dialogView.findViewById(R.id.buttonTarihSec);
        TextView buttonSaatSec = dialogView.findViewById(R.id.buttonSaatSec);
        Button buttonKaydet = dialogView.findViewById(R.id.buttonKaydet);
        ProgressBar progressBarKaydet = dialogView.findViewById(R.id.progressBarKaydet);
        RadioGroup radioGroupYineleme = dialogView.findViewById(R.id.radioGroupYineleme);
        CompoundButton switchHatirlat = dialogView.findViewById(R.id.switchHatirlat);
        if (switchHatirlat != null) switchHatirlat.setChecked(task.isHatirlat());

        View btnKapat = dialogView.findViewById(R.id.buttonKapat);
        View btnVazgec = dialogView.findViewById(R.id.buttonVazgec);
        if (btnKapat != null) btnKapat.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnVazgec != null) btnVazgec.setOnClickListener(v -> bottomSheetDialog.dismiss());

        TextView textAnaBaslik = dialogView.findViewById(R.id.textAnaBaslik);
        if (textAnaBaslik != null) textAnaBaslik.setText(R.string.task_edit_heading);
        buttonKaydet.setText(R.string.task_update_button);

        editTextBaslik.setText(task.getGorevBaslik());
        editTextAciklama.setText(task.getGorevAciklama());
        buttonTarihSec.setText(task.getGorevTarihi());
        buttonSaatSec.setText(task.getGorevSaati());

        GorevDialogYardimcisi.gorunumuHazirla(dialogView);
        GorevDialogYardimcisi.tekrarSec(radioGroupYineleme, task.getYinelemeDurumu());

        final String[] secilenDialogTarih = {task.getGorevTarihi()};
        final String[] secilenDialogSaat = {task.getGorevSaati()};

        buttonTarihSec.setOnClickListener(v1 -> {
            Calendar takvim = Calendar.getInstance();
            int[] mevcut = GorevYardimcisi.tarihAyristir(secilenDialogTarih[0]);
            if (mevcut != null) {
                takvim.set(mevcut[2], mevcut[1] - 1, mevcut[0]);
            }
            DatePickerDialog datePicker = new DatePickerDialog(context,
                    (view1, secilenYil, secilenAy, secilenGun) -> {
                        secilenDialogTarih[0] = GorevYardimcisi.tarihMetni(secilenGun, secilenAy + 1, secilenYil);
                        buttonTarihSec.setText(secilenDialogTarih[0]);
                    }, takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        final boolean is24HourFormat = GorevDialogYardimcisi.saat24MuKullan(context);
        buttonSaatSec.setOnClickListener(v2 -> {
            Calendar saatZaman = Calendar.getInstance();
            TimePickerDialog timePicker = new TimePickerDialog(context,
                    (view1, s, d) -> {
                        secilenDialogSaat[0] = String.format(Locale.getDefault(), "%02d:%02d", s, d);
                        buttonSaatSec.setText(secilenDialogSaat[0]);
                    }, saatZaman.get(Calendar.HOUR_OF_DAY), saatZaman.get(Calendar.MINUTE), is24HourFormat);
            timePicker.show();
        });

        buttonKaydet.setOnClickListener(v3 -> {
            String baslik = editTextBaslik.getText() != null ? editTextBaslik.getText().toString().trim() : "";
            if (baslik.isEmpty()) {
                editTextBaslik.setError(context.getString(R.string.task_title_required));
                return;
            }
            if (baslik.length() > GorevDialogYardimcisi.BASLIK_MAX_UZUNLUK) {
                editTextBaslik.setError(context.getString(R.string.task_title_too_long));
                return;
            }

            task.setGorevBaslik(baslik);
            task.setGorevAciklama(editTextAciklama.getText() != null ? editTextAciklama.getText().toString().trim() : "");
            task.setGorevTarihi(secilenDialogTarih[0]);
            task.setGorevSaati(secilenDialogSaat[0]);
            task.setYinelemeDurumu(GorevDialogYardimcisi.secilenTekrarKodu(radioGroupYineleme));
            task.setHatirlat(switchHatirlat == null || switchHatirlat.isChecked());

            buttonKaydet.setEnabled(false);
            if (progressBarKaydet != null) progressBarKaydet.setVisibility(View.VISIBLE);

            final Context appContext = context.getApplicationContext();
            TaskDatabase.ARKA_PLAN.execute(() -> {
                boolean basarili = false;
                try {
                    TaskDatabase.getInstance(appContext).taskDao().gorevDuzenle(task);
                    GorevHatirlatma.planla(appContext, task);
                    basarili = true;
                } catch (Exception e) {
                    Log.e(TAG, "Görev güncellenemedi", e);
                }
                final boolean sonuc = basarili;
                anaThread.post(() -> {
                    if (sonuc) {
                        Toast.makeText(appContext, R.string.task_updated, Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.dismiss();
                    } else {
                        buttonKaydet.setEnabled(true);
                        if (progressBarKaydet != null) progressBarKaydet.setVisibility(View.GONE);
                        Toast.makeText(appContext, R.string.task_save_failed, Toast.LENGTH_SHORT).show();
                    }
                    if (onDataChanged != null) onDataChanged.run();
                });
            });
        });
        bottomSheetDialog.show();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView textViewBaslik, textViewTarihSaat, textViewAciklama;
        ImageView iconMore;
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewBaslik = itemView.findViewById(R.id.textViewBaslik);
            textViewTarihSaat = itemView.findViewById(R.id.textViewTarihSaat);
            textViewAciklama = itemView.findViewById(R.id.textViewAciklama);
            iconMore = itemView.findViewById(R.id.iconMore);
        }
    }
}
