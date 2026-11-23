package com.mehmetokyay.flownote.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mesaj_table")
public class Mesaj {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int gonderenId;
    private int aliciId;
    private String metin;
    private long timestamp;
    private boolean okundu;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGonderenId() { return gonderenId; }
    public void setGonderenId(int gonderenId) { this.gonderenId = gonderenId; }

    public int getAliciId() { return aliciId; }
    public void setAliciId(int aliciId) { this.aliciId = aliciId; }

    public String getMetin() { return metin; }
    public void setMetin(String metin) { this.metin = metin; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isOkundu() { return okundu; }
    public void setOkundu(boolean okundu) { this.okundu = okundu; }
}
