package de.matthiaseberlein.mp3lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/*
    This Tag is for keeping the integrity of both id3v1 and id3v2 Tags and keeping all Information equivalent
*/
class ID3Tag {
	private ID3V1 id3V1=null;
	private ID3V2 id3V2=null;
	private boolean quickRead;
	
	ID3Tag(String path){
		try {
			id3V1 = new ID3V1(path);
			id3V2 = new ID3V2(path);
		} catch(IOException e){
			e.printStackTrace();
		}
		
	}
	ID3Tag(String path, boolean quickRead) {
		this.quickRead=quickRead;
		try {
			id3V1 = new ID3V1(path);
			if(!id3V1.isPresent() || id3V1.getTitle().equals(""))
			id3V2 = new ID3V2(path, quickRead);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	ID3Tag() {
		id3V1 = new ID3V1();
		id3V2 = new ID3V2();
	}
	ID3Tag(Song song){
		id3V1 = new ID3V1(song);
		id3V2 = new ID3V2(song);
	}
	
	/**
	 *
	 * @param value
	 * @return
	 */
	boolean changeValue(InformationHolder value){
		boolean ret=true;
		for(InformationHolder.Information info : value) {
			if(MetaID.ALBUM.toString().equals(info.getKey()) ||
					MetaID.ARTISTS.toString().equals(info.getKey()) ||
					MetaID.COMMENT.toString().equals(info.getKey()) ||
					MetaID.TITLE.toString().equals(info.getKey()) ||
					MetaID.GENRES.toString().equals(info.getKey()) ||
					MetaID.TRACK_NUMBER.toString().equals(info.getKey()) ||
					MetaID.YEAR.toString().equals(info.getKey())) {
				
				if(!id3V1.setData(value))
					ret=false;
			}
			
			if(!id3V2.setDataToAll(value))
				ret=false;
		}
		return ret;
	}
	
	boolean addValue(InformationHolder value){
		boolean ret=true;
		for(InformationHolder.Information info : value) {
			if(!id3V2.addData(MetaID.valueOf(info.getKey()), info.getHolder()))
				ret=false;
			if(ret && (info.getKey().equals(MetaID.GENRES.toString()) || info.getKey().equals(MetaID.ARTISTS.toString()))){
				id3V1.setData(getDataByPurpose(MetaID.valueOf(info.getKey())));
			}
		}
		return ret;
	}
	
	/**
	 * Removes a Value from both ID3 Tags with the identifier.
	 * @param holder
	 * @return
	 */
	boolean removeValue(InformationHolder holder){
		boolean ret=false;
		for(InformationHolder.Information purpose : holder) {
			if(InformationHolder.class.isAssignableFrom(purpose.getInnerClass())) {
				InformationHolder purposeholder =  purpose.getHolder();
				ret=id3V2.removeData(purposeholder);
				if(ret) {
					InformationHolder newData = id3V2.getDataByPurpose(MetaID.valueOf(purpose.getKey()));
					if(newData!=null)
						ret = id3V1.setData(newData);
					else {
						newData=new InformationHolder().with(purpose.getKey(), "");
						id3V1.setData(newData);
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * Shows the tag in a beautiful getup that clearly shows how the File looks.
	 */
	void showTagRepresentation(){
		//Log.v("Nachricht", "Frame overview:");
		System.out.println("Frame overview:");
		id3V2.showTagRepresentation();
		id3V1.showTagRepresentation();
	}
	
	/**
	 * Makes the Information in ID3V1 equal to ID3V2 Tag. If an Information is Missing in one of the two, it will be added.
	 * @param song
	 */
	void equalizeTagInformation(Song song){
		if(!song.getTitle().contains(id3V1.getTitle())) {
			id3V1.setTitle(song.getTitle());
		}
		if(!song.getArtists().containsAll(Arrays.asList(id3V1.getArtists().split(", ")))) {
			String s="";
			for(String li: song.getArtists())
				s+=li+", ";
			id3V1.setArtists(s);
		}
		if(!song.getAlbum().contains(id3V1.getAlbum())){
			id3V1.setAlbum(song.getAlbum());
		}
	}
	
	/**
	 * Fills up the Song with all important Data contained in the V2 and V1 Tag. If there are duplicate Information saved in the Tags and frames,
	 * only the first to be saved to the InformationHolder will be displayed. The save order is: id3v2, by frame order, then id3v1.
	 * @param s the song the Information is gonna be saved in.
	 */
	void fillSong(Song s) {
		s.setInformation(id3V2.getAllData().join(id3V1.getData()));
	}
	
	/**
	 * Returns the current Frame Order of ID3V2 in a List as an Array of Strings
	 * @return
	 */
	ArrayList<String> getCurrentFrameOrder() {
		return id3V2.getCurrentFrameOrder();
	}
	
	/**
	 * Returns the current Data contained in the Frame identified by the parameter.
	 * @param purpose The Identifier can be found in the InformationHolder originally put in the Song at creation time with the key IDENTIFIER from Song.MetaIds.
	 * @return true if the Frame was found and Data can be returned, false otherwise.
	 */
	InformationHolder getDataByPurpose(MetaID purpose) {
		return id3V2.getDataByPurpose(purpose);
	}
	
	/**
	 * Sets the Data contained in the InformationHolder for every Frame or Tag that uses it. Useful for setting the Encoding for every Text Frame or setting the language for the Comment- and Copyrightframe simultaniously
	 * @param holder the Data to be altered.
	 * @return false if an error occured while setting the data (wrong Format or Datatype or invalid value) and true otherwise.
	 */
	boolean setDataToAll(InformationHolder holder) {
		boolean ret=id3V2.setDataToAll(holder);
		if(ret)
			ret=id3V1.setData(holder);
		else
			id3V1.setData(holder);
		return ret;
	}
	
	/**
	 * Checks if the Identifier key exists in holder and if the key already exists in the frames of id3V2. If it does, the Data will get set through the setData method of the frame. If not, a Frame will be added.
	 * In id3V1, the Data will simply be set if the information exists there.
	 * @param holder cannot be null. Must contain one IDENTIFIER Key from Song.MetaID.
	 * @return true if a Frame with that ID was found and the Data could be set. False otherwise
	 */
	boolean setData(InformationHolder holder) {
		boolean ret;
		ret=id3V2.setData(holder);
		
		if(ret)
			ret=id3V1.setData(holder);
		return ret;
	}
}
