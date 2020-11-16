package de.matthiaseberlein.mp3lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/*
    This class is for operations that affect the whole File itself, like creation of it or converting other File formats to mp3.
 */
class MP3File {
    private ID3Tag tag;
    private String filepath;
    private MP3Data data;
    private boolean present;

    MP3File(String filepath){
        this.filepath = filepath;
        File f=new File(filepath);
        System.out.println(f.getAbsolutePath());
        if(!f.isFile() || !f.exists() || !f.getName().endsWith(".mp3")) {
            present=false;
            String n = f.getName();
            boolean a = !f.isFile();
            boolean b = !f.exists();
            boolean c = !n.endsWith(".mp3");
            
            tag=new ID3Tag();
            return;
        }
        if(!f.exists()) {
            present=false;
            tag=new ID3Tag();
        }
        else
            tag=new ID3Tag(filepath);
    }

    MP3File(Song song) {
        tag=new ID3Tag(song);
        data=null;
        filepath=song.getPath();
    }
    void createIfNotExists(String path){
        this.filepath =path;
        if(tag==null)
            return;
        File f=new File(path);
        if(!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Date getLastModifiedDate(){
        File f=new File(filepath);
        return new Date(f.lastModified());
    }

    boolean changeValue(InformationHolder value){
        return tag.changeValue(value);
    }
    boolean addValue(InformationHolder value){
        return this.tag.addValue(value);
    }
    boolean removeValue(InformationHolder value) {
        return this.tag.removeValue(value);
    }
    void showTagRepresentation(){
        tag.showTagRepresentation();
    }

    void fillSong(Song s) {
        tag.fillSong(s);
    }

    MP3Data getMp3Data(){
        return data;
    }
    
    ArrayList<String> getCurrentFrameOrder() {
        return tag.getCurrentFrameOrder();
    }
    
    InformationHolder getDataByPurpose(MetaID purpose) {
        return tag.getDataByPurpose(purpose);
    }
    
    boolean setMetaDataForAll(InformationHolder holder) {
        return tag.setDataToAll(holder);
    }
    
    boolean setMetaData(InformationHolder holder) {
        return tag.setData(holder);
    }
    
    InformationHolder createMetaData(MetaID id, InformationHolder holder) {
        return null;//tag.addData(id, holder);
    }
    
    public String getPath() {
        return filepath;
    }
}
