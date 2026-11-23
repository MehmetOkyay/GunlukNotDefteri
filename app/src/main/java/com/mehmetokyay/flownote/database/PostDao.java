package com.mehmetokyay.flownote.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mehmetokyay.flownote.model.Post;

import java.util.List;

@Dao
public interface PostDao {

    @Query("SELECT * FROM post_table WHERE userId = :kullaniciId ORDER BY id DESC")
    List<Post> kullaniciGonderileriniGetir(int kullaniciId);

    @Query("SELECT * FROM post_table ORDER BY id DESC")
    List<Post> tumGonderileriGetir();

    @Insert
    void gonderiEkle(Post post);

    @Query("SELECT * FROM post_table WHERE id = :id LIMIT 1")
    Post idIleGetir(int id);

    @Query("DELETE FROM post_table WHERE id = :id")
    void gonderiSil(int id);

    @Query("SELECT p.* FROM post_table p INNER JOIN begeni_table b ON b.postId = p.id WHERE b.userId = :kullaniciId ORDER BY b.timestamp DESC")
    List<Post> begenilenGonderileriGetir(int kullaniciId);

    @Query("UPDATE post_table SET begeniSayisi = (SELECT COUNT(*) FROM begeni_table WHERE postId = :postId) WHERE id = :postId")
    void begeniSayisiniYenile(int postId);

    @Query("UPDATE post_table SET yorumSayisi = (SELECT COUNT(*) FROM yorum_table WHERE postId = :postId) WHERE id = :postId")
    void yorumSayisiniYenile(int postId);
}
