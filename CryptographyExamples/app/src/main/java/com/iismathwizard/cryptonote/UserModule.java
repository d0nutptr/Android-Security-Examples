package com.iismathwizard.cryptonote;

import android.arch.persistence.room.Room;
import android.content.SharedPreferences;

import com.iismathwizard.cryptonote.Notes.NotesDao;
import com.iismathwizard.cryptonote.Notes.NotesDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class UserModule {
    private SharedPreferences preferences;
    private CryptoNoteApplication application;

    public UserModule(CryptoNoteApplication application, SharedPreferences preference) {
        this.application = application;
        this.preferences = preference;
    }

    @Provides
    @Singleton
    public UserSettings provideUserSettings() {
        return new UserSettings(preferences);
    }

    @Provides
    @Singleton
    public NotesDatabase provideDatabase(){
        return Room.databaseBuilder(application, NotesDatabase.class, "notes-database").allowMainThreadQueries().build();
    }

    @Provides
    @Singleton
    public NotesDao provideDao(NotesDatabase database){
        return database.notesDao();
    }
}
