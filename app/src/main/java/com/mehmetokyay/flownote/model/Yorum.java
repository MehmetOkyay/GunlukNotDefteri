package com.mehmetokyay.flownote.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "yorum_table")
public class Yorum {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int postId;
    private int userId;
    private String metin;
    private long timestamp;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMetin() { return metin; }
    public void setMetin(String metin) { this.metin = metin; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

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
