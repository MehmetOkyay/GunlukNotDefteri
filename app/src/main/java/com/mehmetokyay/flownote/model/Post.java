package com.mehmetokyay.flownote.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "post_table")
public class Post {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int userId;
    private String baslik;
    private String icerik;
    private int begeniSayisi;
    private int yorumSayisi;
    private String resimUri;
    private long timestamp;

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getResimUri() { return resimUri; }

    public void setResimUri(String resimUri) { this.resimUri = resimUri; }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBaslik() {
        return baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik;
    }

    public String getIcerik() {
        return icerik;
    }

    public void setIcerik(String icerik) {
        this.icerik = icerik;
    }

    public int getBegeniSayisi() {
        return begeniSayisi;
    }

    public void setBegeniSayisi(int begeniSayisi) {
        this.begeniSayisi = begeniSayisi;
    }

    public int getYorumSayisi() {
        return yorumSayisi;
    }

    public void setYorumSayisi(int yorumSayisi) {
        this.yorumSayisi = yorumSayisi;
    }

    @Ignore private String bulutId;
    @Ignore private String yazarUid;
    @Ignore private String yazarAd;
    @Ignore private String yazarKullaniciAdi;
    @Ignore private String yazarResimUrl;

    public String getBulutId() { return bulutId; }
    public void setBulutId(String bulutId) { this.bulutId = bulutId; }
    public String getYazarUid() { return yazarUid; }
    public void setYazarUid(String yazarUid) { this.yazarUid = yazarUid; }
    public String getYazarAd() { return yazarAd; }
    public void setYazarAd(String yazarAd) { this.yazarAd = yazarAd; }
    public String getYazarKullaniciAdi() { return yazarKullaniciAdi; }
    public void setYazarKullaniciAdi(String yazarKullaniciAdi) { this.yazarKullaniciAdi = yazarKullaniciAdi; }
    public String getYazarResimUrl() { return yazarResimUrl; }
    public void setYazarResimUrl(String yazarResimUrl) { this.yazarResimUrl = yazarResimUrl; }
}