package com.mehmetokyay.flownote.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.adapter.YorumAdapter;
import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.model.Yorum;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.BildirimMerkezi;
import com.mehmetokyay.flownote.util.IcerikDenetimi;
import com.mehmetokyay.flownote.util.KullaniciGorunum;
import com.mehmetokyay.flownote.util.YerelKullanici;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class YorumlarBottomSheet extends BottomSheetDialogFragment implements YorumAdapter.Dinleyici {

    public static final String ETIKET = "YorumlarBottomSheet";
    private static final String TAG = "YorumlarBottomSheet";
    private static final String ARG_POST_ID = "postId";
    private static final String ARG_AKTIF_ID = "aktifUid";

    public interface YorumSayisiDinleyici {
        void yorumSayisiDegisti(int sayi);
    }

    private String postId;
    private String aktifUid;
    private String gonderiSahibiUid = "";

    private RecyclerView recyclerYorumlar;
    private TextView textBos, textBaslik;
    private View progress, btnGonder;
    private EditText editYorum;
    private YorumAdapter adapter;
    private YorumSayisiDinleyici dinleyici;

    public static YorumlarBottomSheet yeni(String postId, String aktifUid) {
        YorumlarBottomSheet f = new YorumlarBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_POST_ID, postId);
        b.putString(ARG_AKTIF_ID, aktifUid);
        f.setArguments(b);
        return f;
    }

    public void setYorumSayisiDinleyici(YorumSayisiDinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_yorumlar, container, false);
        Bundle args = requireArguments();
        postId = args.getString(ARG_POST_ID);
        aktifUid = args.getString(ARG_AKTIF_ID);

        recyclerYorumlar = view.findViewById(R.id.recyclerYorumlar);
        textBos = view.findViewById(R.id.textYorumBos);
        textBaslik = view.findViewById(R.id.textYorumlarBaslik);
        progress = view.findViewById(R.id.progressYorumlar);
        editYorum = view.findViewById(R.id.editYorum);
        btnGonder = view.findViewById(R.id.btnYorumGonder);

        adapter = new YorumAdapter(this);
        recyclerYorumlar.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerYorumlar.setAdapter(adapter);

        btnGonder.setOnClickListener(v -> yorumGonder());
        yorumlariYukle(false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            FrameLayout sheet = ((BottomSheetDialog) getDialog()).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void yorumlariYukle(boolean sonaKaydir) {
        BulutSosyal.yorumlariGetir(postId, (yorumlar, hata) -> {
            if (!gorunumHazir()) return;
            progress.setVisibility(View.GONE);
            if (hata != null || yorumlar == null) {
                Toast.makeText(requireContext(), R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.listeyiDegistir(yorumlar, null);
            textBos.setVisibility(yorumlar.isEmpty() ? View.VISIBLE : View.GONE);
            textBaslik.setText(yorumlar.isEmpty() ? getString(R.string.yorumlar) : getString(R.string.yorumlar_sayili, yorumlar.size()));
            if (sonaKaydir && !yorumlar.isEmpty()) recyclerYorumlar.scrollToPosition(yorumlar.size() - 1);
        });
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("gonderiler").document(postId).get()
                .addOnSuccessListener(d -> {
                    if (d.exists()) gonderiSahibiUid = d.getString("uid") == null ? "" : d.getString("uid");
                });
    }

    private void yorumGonder() {
        String metin = editYorum.getText().toString().trim();
        if (metin.isEmpty()) return;
        IcerikDenetimi.Kategori yasak = IcerikDenetimi.metniDenetle(metin);
        if (yasak != null) {
            Toast.makeText(requireContext(), yasak.mesaj, Toast.LENGTH_LONG).show();
            return;
        }
        btnGonder.setEnabled(false);
        final Context appContext = requireContext().getApplicationContext();
        final com.google.firebase.auth.FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) {
            btnGonder.setEnabled(true);
            return;
        }
        TaskDatabase.ARKA_PLAN.execute(() -> {
            User ben;
            try {
                ben = YerelKullanici.garantiEt(appContext, fUser);
            } catch (Exception e) {
                ben = null;
            }
            if (ben == null) {
                View kok = getView();
                if (kok != null) kok.post(() -> {
                    if (!gorunumHazir()) return;
                    btnGonder.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                });
                return;
            }
            final User yazar = ben;
            BulutSosyal.yorumEkle(postId, aktifUid, metin, yazar, (sayi, hata) -> {
                if (!gorunumHazir()) return;
                btnGonder.setEnabled(true);
                if (hata != null || sayi == null) {
                    Toast.makeText(requireContext(), R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                    return;
                }
                editYorum.setText("");
                if (dinleyici != null) dinleyici.yorumSayisiDegisti(sayi);
                BulutSosyal.bildirimBirak(gonderiSahibiUid, BildirimMerkezi.TIP_YORUM, aktifUid,
                        KullaniciGorunum.gorunenAd(yazar), metin, postId);
                yorumlariYukle(true);
            });
        });
    }

    @Override
    public void yorumUzunBasildi(Yorum yorum) {
        boolean silebilir = aktifUid != null && (aktifUid.equals(yorum.getYazarUid()) || aktifUid.equals(gonderiSahibiUid));
        if (silebilir) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.yorumu_sil)
                    .setMessage(R.string.yorumu_sil_onay)
                    .setPositiveButton(R.string.task_delete, (d, w) -> yorumuSil(yorum.getBulutId()))
                    .setNegativeButton(R.string.iptal, null)
                    .show();
            return;
        }
        if (aktifUid == null || aktifUid.equals(yorum.getYazarUid())) return;
        final int[] secilen = {-1};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.yorumu_sikayet_et)
                .setSingleChoiceItems(R.array.sikayet_sebepleri, -1, (d, which) -> secilen[0] = which)
                .setPositiveButton(R.string.sikayet_et, (d, w) -> {
                    if (secilen[0] < 0) return;
                    com.google.firebase.auth.FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (fUser == null) return;
                    String ad = fUser.getDisplayName() == null ? "" : fUser.getDisplayName();
                    BulutSosyal.yorumSikayetEt(postId, yorum, fUser.getUid(), ad, secilen[0], (basarili, hata) -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                Boolean.TRUE.equals(basarili) ? R.string.sikayet_alindi : R.string.sikayet_gonderilemedi,
                                Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton(R.string.iptal, null)
                .show();
    }

    private void yorumuSil(String yorumId) {
        BulutSosyal.yorumSil(postId, yorumId, aktifUid, (sayi, hata) -> {
            if (!gorunumHazir()) return;
            if (hata != null || sayi == null || sayi < 0) {
                Toast.makeText(requireContext(), R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            if (dinleyici != null) dinleyici.yorumSayisiDegisti(sayi);
            yorumlariYukle(false);
        });
    }

    private boolean gorunumHazir() {
        return isAdded() && getView() != null && progress != null && textBos != null
                && textBaslik != null && recyclerYorumlar != null && editYorum != null && btnGonder != null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerYorumlar = null;
        textBos = textBaslik = null;
        progress = btnGonder = null;
        editYorum = null;
    }
}
