package com.mehmetokyay.flownote.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mehmetokyay.flownote.AramaActivity;
import com.mehmetokyay.flownote.BildirimlerActivity;
import com.mehmetokyay.flownote.GonderiEkleActivity;
import com.mehmetokyay.flownote.MesajlarActivity;
import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.adapter.FeedAdapter;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AkisFragment extends Fragment {

    private RecyclerView recyclerFeed;
    private FeedAdapter feedAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View layoutBos;
    private TextView textBildirimRozet;

    public AkisFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_akis, container, false);

        ImageView iconGonderiEkle = view.findViewById(R.id.iconGonderiEkle);
        ImageView iconArama = view.findViewById(R.id.iconArama);
        ImageView iconMesajlar = view.findViewById(R.id.iconMesajlar);
        ImageView iconBildirimler = view.findViewById(R.id.iconBildirimler);
        textBildirimRozet = view.findViewById(R.id.textBildirimRozet);
        layoutBos = view.findViewById(R.id.layoutAkisBos);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::gonderileriGetir);

        recyclerFeed = view.findViewById(R.id.recyclerFeed);
        recyclerFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        feedAdapter = new FeedAdapter(requireActivity());
        feedAdapter.setDegisiklikDinleyici(this::bosDurumuGuncelle);
        recyclerFeed.setAdapter(feedAdapter);

        View.OnClickListener gonderiEkle = v -> startActivity(new Intent(getActivity(), GonderiEkleActivity.class));
        iconGonderiEkle.setOnClickListener(gonderiEkle);
        view.findViewById(R.id.btnAkisIlkGonderi).setOnClickListener(gonderiEkle);
        iconArama.setOnClickListener(v -> AramaActivity.baslat(requireContext(), false));
        iconMesajlar.setOnClickListener(v -> startActivity(new Intent(getActivity(), MesajlarActivity.class)));
        if (iconBildirimler != null) {
            iconBildirimler.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), BildirimlerActivity.class)));
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        gonderileriGetir();
        bildirimRozetiniGuncelle();
    }

    private void bildirimRozetiniGuncelle() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || textBildirimRozet == null) return;
        BulutSosyal.okunmamisSayisi(user.getUid(), (sayi, hata) -> {
            if (!isAdded() || textBildirimRozet == null || sayi == null) return;
            if (sayi <= 0) {
                textBildirimRozet.setVisibility(View.GONE);
            } else {
                textBildirimRozet.setVisibility(View.VISIBLE);
                textBildirimRozet.setText(sayi > 9 ? "9+" : String.valueOf(sayi));
            }
        });
    }

    private void gonderileriGetir() {
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null || swipeRefreshLayout == null) return;
        swipeRefreshLayout.setRefreshing(true);
        BulutSosyal.akisGetir(fUser.getUid(), (akis, hata) -> {
            if (!isAdded() || swipeRefreshLayout == null) return;
            swipeRefreshLayout.setRefreshing(false);
            if (hata != null) {
                Log.e("AkisFragment", "Gönderiler yüklenirken hata", hata);
                return;
            }
            feedAdapter.veriyiAyarla(fUser.getUid(), akis);
            bosDurumuGuncelle();
        });
    }

    private void bosDurumuGuncelle() {
        if (layoutBos == null) return;
        layoutBos.setVisibility(feedAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerFeed = null;
        swipeRefreshLayout = null;
        layoutBos = null;
        textBildirimRozet = null;
    }
}
