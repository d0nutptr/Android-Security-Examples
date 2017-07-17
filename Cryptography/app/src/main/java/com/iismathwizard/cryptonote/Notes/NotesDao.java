package com.iismathwizard.cryptonote.Notes;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface NotesDao {
    @Insert(onConflict = REPLACE)
    void saveNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("SELECT * FROM note WHERE id = :id")
    LiveData<Note> loadNote(int id);

    @Query("SELECT * FROM note")
    LiveData<List<Note>> loadAllNotes();
}
