package com.mehmetokyay.flownote;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.mehmetokyay.flownote.fragment.AkisFragment;
import com.mehmetokyay.flownote.util.BildirimMerkezi;
import com.mehmetokyay.flownote.util.EkranYardimcisi;
import com.mehmetokyay.flownote.fragment.GorevlerFragment;
import com.mehmetokyay.flownote.fragment.OdakFragment;
import com.mehmetokyay.flownote.fragment.ProfilFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final ActivityResultLauncher<String> bildirimIzni = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        EkranYardimcisi.uygula(this, false, false);
        if (Build.VERSION.SDK_INT >= 33 && !BildirimMerkezi.izinVar(this)) {
            bildirimIzni.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        String profilLinki = getIntent().getStringExtra(LinkActivity.EXTRA_PROFIL);
        if (profilLinki != null && !profilLinki.isEmpty()) {
            getIntent().removeExtra(LinkActivity.EXTRA_PROFIL);
            Intent link = new Intent(this, LinkActivity.class);
            link.setData(android.net.Uri.parse(profilLinki));
            link.putExtra("arkada_ana", true);
            startActivity(link);
        }

        bottomNav = findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new GorevlerFragment())
                    .commit();
            bottomNav.setSelectedItemId(R.id.nav_gorevler);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.isStateSaved()) return false;

            Fragment secilenFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_akis) {
                secilenFragment = new AkisFragment();
            } else if (itemId == R.id.nav_gorevler) {
                secilenFragment = new GorevlerFragment();
            } else if (itemId == R.id.nav_odak) {
                secilenFragment = new OdakFragment();
            } else if (itemId == R.id.nav_profil) {
                secilenFragment = new ProfilFragment();
            }

            if (secilenFragment != null) {
                fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fm.beginTransaction()
                        .replace(R.id.fragment_container, secilenFragment)
                        .commit();
                return true;
            }
            return false;
        });

        FloatingActionButton fab = findViewById(R.id.fabEkle);
        if (fab != null) {
            fab.setOnClickListener(v -> gorevEkleDiyaloguAc());
        }
    }

    private void gorevEkleDiyaloguAc() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;

        Fragment mevcut = fm.findFragmentById(R.id.fragment_container);
        GorevlerFragment gorevler;
        if (mevcut instanceof GorevlerFragment && mevcut.isAdded()) {
            gorevler = (GorevlerFragment) mevcut;
        } else {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            gorevler = new GorevlerFragment();
            fm.beginTransaction()
                    .replace(R.id.fragment_container, gorevler)
                    .commitNow();
            bottomNav.getMenu().findItem(R.id.nav_gorevler).setChecked(true);
        }
        gorevler.gorevEkleDialoguGoster();
    }
}
