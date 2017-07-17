package com.iismathwizard.cryptonote.Notes;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.iismathwizard.cryptonote.Crypto;

@Entity
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String date;
    private String contents;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return Crypto.decrypt(title);
    }

    public void setTitle(String title) {
        this.title = Crypto.encrypt(title);
    }

    public String getDate() {
        return Crypto.decrypt(date);
    }

    public void setDate(String date) {
        this.date = Crypto.encrypt(date);
    }

    public String getContents() {
        return Crypto.decrypt(contents);
    }

    public void setContents(String contents) {
        this.contents = Crypto.encrypt(contents);
    }
}
