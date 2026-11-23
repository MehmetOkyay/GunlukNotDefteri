package com.mehmetokyay.flownote.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "baslik")
    private String gorevBaslik;

    @ColumnInfo(name = "aciklama")
    private String gorevAciklama;

    @ColumnInfo(name = "gorevTarihi")
    private String gorevTarihi;

    @ColumnInfo(name = "gorevSaati")
    private String gorevSaati;

    @ColumnInfo(name = "gorevTamamlandiMi")
    private boolean gorevTamamlandiMi;

    @ColumnInfo(name = "yenilemeDurumu")
    private String yinelemeDurumu;

    @ColumnInfo(name = "tamamlananTarih")
    private String tamamlananTarihler = "";

    @ColumnInfo(name = "hatirlat", defaultValue = "1")
    private boolean hatirlat = true;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getGorevBaslik() {
        return gorevBaslik;
    }

    public void setGorevBaslik(String gorevBaslik) {
        this.gorevBaslik = gorevBaslik;
    }

    public String getGorevAciklama() {
        return gorevAciklama;
    }

    public void setGorevAciklama(String gorevAciklama) {
        this.gorevAciklama = gorevAciklama;
    }

    public String getGorevTarihi() {
        return gorevTarihi;
    }

    public void setGorevTarihi(String gorevTarihi) {
        this.gorevTarihi = gorevTarihi;
    }

    public String getGorevSaati()  { return gorevSaati; }

    public void setGorevSaati(String gorevSaati) { this.gorevSaati = gorevSaati; }

    public boolean getGorevTamamlandiMi() {
        return gorevTamamlandiMi;
    }

    public void setGorevTamamlandiMi(boolean gorevTamamlandiMi) { this.gorevTamamlandiMi = gorevTamamlandiMi; }

    public String getYinelemeDurumu() { return yinelemeDurumu; }

    public void setYinelemeDurumu(String yinelemeDurumu) { this.yinelemeDurumu = yinelemeDurumu; }

    public String getTamamlananTarihler() { return tamamlananTarihler; }

    public void setTamamlananTarihler(String tamamlananTarihler) { this.tamamlananTarihler = tamamlananTarihler; }

    public boolean isHatirlat() { return hatirlat; }

    public void setHatirlat(boolean hatirlat) { this.hatirlat = hatirlat; }
}