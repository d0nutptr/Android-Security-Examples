package com.iismathwizard.cryptonote.Notes;

import android.arch.lifecycle.LiveData;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class NotesRepository {
    private NotesDao notesDao;

    @Inject
    public NotesRepository(NotesDao notesDao){
        this.notesDao = notesDao;
    }

    public LiveData<List<Note>> getNotes(){
        return notesDao.loadAllNotes();
    }

    public LiveData<Note> getNote(int id){
        return notesDao.loadNote(id);
    }

    public void deleteNote(Note note){
        notesDao.deleteNote(note);
    }

    public void saveNote(Note note) {
        notesDao.saveNote(note);
    }

    public void createNote(){
        Note newNote = new Note();

        //handle encryption later
        newNote.setTitle("New Note");
        newNote.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        newNote.setContents("");

        saveNote(newNote);
    }
}
