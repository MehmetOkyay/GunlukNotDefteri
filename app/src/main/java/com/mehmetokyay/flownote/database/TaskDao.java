package com.mehmetokyay.flownote.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.mehmetokyay.flownote.model.Task;
import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    long gorevEkle(Task yeniGorev);

    @Delete
    public void gorevSil(Task silinecekGorev);

    @Update
    public void gorevDuzenle(Task duzenlenecekGorev);

    @Query("SELECT * FROM tasks")
    public List<Task> hepsiniListele();

    @Query("SELECT * FROM tasks WHERE id = :secilenGorevId")
    public Task secileniGoster(long secilenGorevId);

    @Query("SELECT * FROM tasks WHERE gorevTarihi = :secilenGorevTarih")
    public List<Task> tariheGore(String secilenGorevTarih);
}
