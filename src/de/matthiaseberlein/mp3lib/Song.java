package de.matthiaseberlein.mp3lib;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Informationen als Liste speichern und wenn etwas nicht in der Liste steht ne Exception werfen oder lieber alles untereinander festschreiben? Schnittstelle festlegen.
 * Idee: generiere einen Song aus den Tags und nur der Song selber weiß, wo die Infos herkommen und ändert die Tags gleich mit (Sprich ruft selber die änderungsmethoden auf).
 * Zusätzlich zu den Informationen wird der ID gespeichert um den richtigen Frame zu kennen der abgeändert wird.
 */
public class Song {
	/**
	 * metadata Contains all the Metadata of the Song in the following Hierarchy: The MetaID describes the Purpose and has one InformationHolder as Value,
	 * wrapping one or multiple InformationHolders identifying each Frame. For example: MetaID IMAGE will have an InformationHolder with 2 InformationHolders
	 * inside: the front cover and back cover.
	 * It contains all Information currently available. Duplicate Information Types like Images, that will be saved as separate Frames, will be saved under one Purpose with different Identifiers
	 */
	private HashMap<MetaID, InformationHolder> metadata;
	
	/**
	 * The frameorder displays what Order the Frames are currently in. It also shows what Information Types are currently saved in the Song. It updates automatically
	 * due to the CallByReference connection with metadata.
	 */
	private Set<MetaID> frameorder; //Frameorder always contains the current MetaIDs saved in metadata as Purpose Identifiers. This is because the Set is a callbyReference on the metadata.
	
	/**
	 * The mp3File is the class Managing the Read and write actions on the File itself as well as the creation of such if it does not exist yet.
	 */
	private MP3File mp3file;
	
	/**
	 * This Constructor is for duplicating Songs and saving them to a new Path.
	 * @param song
	 */
	public Song(String path, Song song) {
		metadata=new HashMap<>();
		mp3file=new MP3File(song);
		fillFromFile();
		this.frameorder=metadata.keySet();
	}
	
	/**
	 * This Constructor is used for reading a Song
	 * @param path
	 */
	public Song(String path) {
		metadata=new HashMap<>();
		mp3file=new MP3File(path);
		fillFromFile();
		frameorder=metadata.keySet();
	}
	
	/**
	 * ToDo: remove later since it does not serve any purpose
	 */
	private void fillFromFile() {
		mp3file.fillSong(this);
	}
	
	private void setDefaultFrameOrder() {
		ArrayList<MetaID> standardorder=new ArrayList<>();
		//ToDo: fill the standardorder and make the frameorder equally ordered
	}
	
	/**
	 * Returns the Title of this Song if it is known, otherwise null.
	 * @return A String containing the Title, or null if none is saved
	 */
	public String getTitle() {
		if(metadata.containsKey(MetaID.TITLE))
			return metadata.get(MetaID.TITLE).getHolder(MetaID.TITLE.toString()).getString(MetaID.TITLE.toString());
		return null;
	}
	
	/**
	 * Changes the title of the Song. If the data is not present yet, it will get added.
	 * @param title the new Title of the Song.
	 * @return true if the Title could be changed. False if not.
	 */
	public boolean setTitle(String title) {
		if(title==null)
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.TITLE.toString(), new InformationHolder()
						                                                        .with(MetaID.TITLE.toString(), new InformationHolder()
								                                                                                       .with(MetaID.TITLE.toString(), title)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.TITLE, mp3file.getDataByPurpose(MetaID.TITLE));
		}
		return ret;
	}
	
	/**
	 * Removes the Title Metadata completely from all Tags.
	 * @return true if removing the Title was possible, false otherwise.
	 */
	public boolean removeTitle() {
		if(!metadata.containsKey(MetaID.TITLE))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.TITLE.toString(), new InformationHolder()
						                                                          .with(MetaID.TITLE.toString(), MetaID.TITLE.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.TITLE);
			if(newInfo!=null)
				metadata.put(MetaID.TITLE, newInfo);
			else
				metadata.remove(MetaID.TITLE);
		}
		return ret;
	}
	
	/**
	 * Returns a List of Artists if the Information is present, otherwise null.
	 * @return A List of Artists or null
	 */
	public ArrayList<String> getArtists() {
		if(metadata.containsKey(MetaID.ARTISTS))
			return metadata.get(MetaID.ARTISTS).getHolder(MetaID.ARTISTS.toString()).getList(MetaID.ARTISTS.toString());
		return null;
	}
	
	/**
	 * Sets the given List of Artists as the new Artists of this Song. Will be updated in every Tag of the Song.
	 * @param artists A List of Strings containing artists
	 * @return true if the new artists could be saved and false if not.
	 */
	public boolean setArtists(ArrayList<String> artists) {
		if(artists==null)
			return false;
		if(artists.size()==0)
			return removeAllArtists();
		boolean ret;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.ARTISTS.toString(), new InformationHolder()
						                                                        .with(MetaID.ARTISTS.toString(), new InformationHolder()
								                                                                                       .with(MetaID.ARTISTS.toString(), artists)));
		ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.ARTISTS, mp3file.getDataByPurpose(MetaID.ARTISTS));
		}
		return ret;
	}
	
	/**
	 * adds all Artists to the currently existing ones. If there are no artists yet, this method calls setArtists().
	 * @param artists
	 * @return true if the artists could be added, false otherwise.
	 */
	public boolean addArtists(ArrayList<String> artists) {
		if(artists==null)
			return false;
		if(artists.size()==0)
			return false;
		if(!metadata.containsKey(MetaID.ARTISTS))
			return setArtists(artists);
		boolean ret;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.ARTISTS.toString(), new InformationHolder()
						                                                          .with(MetaID.ARTISTS.toString(), new InformationHolder()
								                                                                                           .with(MetaID.ARTISTS.toString(), artists)));
		ret=mp3file.addValue(holder);
		if(ret)
			metadata.put(MetaID.ARTISTS, mp3file.getDataByPurpose(MetaID.ARTISTS));
		return ret;
	}
	
	/**
	 * Removes all the artists contained in the list. If the artists are empty afterwards, the Frame will get destroyed and the Information will be deleted from the metadata list.
	 * @param artists the List of Artists that are to be deleted
	 * @return true if every artists could be deleted, false if artists was null or had no element or one or more artists could not be found.
	 */
	public boolean removeArtists(ArrayList<String> artists) {
		if(artists==null)
			return false;
		if(artists.size()==0)
			return false;
		boolean ret;
		if(!metadata.containsKey(MetaID.ARTISTS))
			ret=false;
		else {
			InformationHolder holder=new InformationHolder()
					                         .with(MetaID.ARTISTS.toString(), new InformationHolder()
							                                                          .with(MetaID.ARTISTS.toString(), new InformationHolder()
									                                                                                           .with(MetaID.ARTISTS.toString(), artists)));
			ret=mp3file.removeValue(holder);
			InformationHolder newData=mp3file.getDataByPurpose(MetaID.ARTISTS);
			if(newData == null)
				metadata.remove(MetaID.ARTISTS);
			else
				metadata.put(MetaID.ARTISTS, newData);
		}
		return ret;
	}
	
	/**
	 * Removes the Artists of this Song completely from all Tags.
	 * @return true if removing the Artists was possible or false if not.
	 */
	public boolean removeAllArtists(){
		if(!metadata.containsKey(MetaID.ARTISTS))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.ARTISTS.toString(), new InformationHolder()
						                                                        .with(MetaID.ARTISTS.toString(), MetaID.ARTISTS.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.ARTISTS);
			if(newInfo!=null)
				metadata.put(MetaID.ARTISTS, newInfo);
			else
				metadata.remove(MetaID.ARTISTS);
		}
		return ret;
	}
	
	/**
	 * Returns the album if there is one, otherwise null.
	 * @return the Album if the information exists, otherwise null.
	 */
	public String getAlbum() {
		if(metadata.containsKey(MetaID.ALBUM))
			return metadata.get(MetaID.ALBUM).getHolder(MetaID.ALBUM.toString()).getString(MetaID.ALBUM.toString());
		return null;
	}
	
	/**
	 * Sets a new Album that the Song belongs to for all Tags.
	 * @param album the new Album for the Song
	 * @return true if setting the Album was successful and false if not
	 */
	public boolean setAlbum(String album) {
		if(album==null) {
			return false;
		}
		if(album.equals(""))
			return removeAlbum();
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.ALBUM.toString(), new InformationHolder()
						                                                        .with(MetaID.ALBUM.toString(), new InformationHolder()
								                                                                                       .with(MetaID.ALBUM.toString(), album)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.ALBUM, mp3file.getDataByPurpose(MetaID.ALBUM));
		}
		return ret;
	}
	
	/**
	 * removes the Album from all Tags.
	 * @return true if removing it was possible and false otherwise
	 */
	public boolean removeAlbum() {
		if(!metadata.containsKey(MetaID.ALBUM))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.ALBUM.toString(), new InformationHolder()
						                                                        .with(MetaID.ALBUM.toString(), MetaID.ALBUM.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.ALBUM);
			if(newInfo!=null)
				metadata.put(MetaID.ALBUM, newInfo);
			else
				metadata.remove(MetaID.ALBUM);
		}
		return ret;
	}
	
	/**
	 * ToDo
	 * @return
	 */
	public ArrayList<String> getFeatures() {
		if(metadata.containsKey(MetaID.FEATURES))
			return metadata.get(MetaID.FEATURES).getHolder(MetaID.FEATURES.toString()).getList(MetaID.FEATURES.toString());
		return null;
	}
	
	/**
	 * ToDO
	 * @param features
	 * @return
	 */
	public boolean setFeatures(ArrayList<String> features) {
		if(features==null)
			return false;
		if(features.size()==0)
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.FEATURES.toString(), new InformationHolder()
						                                                        .with(MetaID.FEATURES.toString(), new InformationHolder()
								                                                                                       .with(MetaID.FEATURES.toString(), features)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.FEATURES, mp3file.getDataByPurpose(MetaID.FEATURES));
		}
		return ret;
	}
	
	/**
	 * ToDo
	 * @param features
	 * @return
	 */
	public boolean addFeatures(ArrayList<String> features) {
		if(features==null)
			return false;
		if(features.size()==0)
			return false;
		boolean ret;
		if(!metadata.containsKey(MetaID.FEATURES))
			ret=setFeatures(features);
		else {
			ret=mp3file.addValue(new InformationHolder()
					                     .with(MetaID.FEATURES.toString(), new InformationHolder()
							                                                       .with(MetaID.FEATURES.toString(), new InformationHolder()
									                                                                                         .with(MetaID.FEATURES.toString(), features))));
			if(ret)
				metadata.put(MetaID.FEATURES, mp3file.getDataByPurpose(MetaID.FEATURES));
		}
		return ret;
	}
	
	/**
	 * ToDo
	 * @param features
	 * @return
	 */
	public boolean removeFeatures(ArrayList<String> features) {
		if(features==null)
			return false;
		if(features.size()==0)
			return false;
		boolean ret;
		if(!metadata.containsKey(MetaID.FEATURES))
			ret=false;
		else {
			ret=mp3file.removeValue(new InformationHolder().with(MetaID.FEATURES.toString(), features).with(MetaID.IDENTIFIER.toString(), metadata.get(MetaID.FEATURES)));
			if(mp3file.getDataByPurpose(MetaID.FEATURES)==null)
				metadata.remove(MetaID.FEATURES);
			else
				metadata.put(MetaID.FEATURES, mp3file.getDataByPurpose(MetaID.FEATURES));
		}
		return ret;
	}
	
	/**
	 * ToDO
	 * @return
	 */
	public boolean removeFeatures() {
		if(!metadata.containsKey(MetaID.FEATURES))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.FEATURES.toString(), new InformationHolder()
						                                                        .with(MetaID.FEATURES.toString(), MetaID.FEATURES.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.FEATURES);
			if(newInfo!=null)
				metadata.put(MetaID.FEATURES, newInfo);
			else
				metadata.remove(MetaID.FEATURES);
		}
		return ret;
	}
	
	/**
	 * Returns a List of Genres that are currently saved for this Song.
	 * @return a List of Genres or null if there is none saved.
	 */
	public ArrayList<String> getGenres() {
		if(metadata.containsKey(MetaID.GENRES)) {
			return metadata.get(MetaID.GENRES).getHolder(MetaID.GENRES.toString()).getList(MetaID.GENRES.toString());
		}
		return null;
	}
	
	public boolean setGenres(ArrayList<String> genres) {
		if(genres==null)
			return false;
		if(genres.size()==0)
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.GENRES.toString(), new InformationHolder()
						                                                        .with(MetaID.GENRES.toString(), new InformationHolder()
								                                                                                       .with(MetaID.GENRES.toString(), genres)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.GENRES, mp3file.getDataByPurpose(MetaID.GENRES));
		}
		return ret;
	}
	
	public boolean addGenres(ArrayList<String> genres) {
		if(genres==null)
			return false;
		if(genres.size()==0)
			return false;
		boolean ret;
		if(!metadata.containsKey(MetaID.GENRES))
			ret=setGenres(genres);
		else {
			ret=mp3file.addValue(new InformationHolder()
					                     .with(MetaID.GENRES.toString(), new InformationHolder()
							                                                     .with(MetaID.GENRES.toString(), new InformationHolder()
									                                                                                     .with(MetaID.GENRES.toString(), genres))));
			metadata.put(MetaID.GENRES, mp3file.getDataByPurpose(MetaID.GENRES));
		}
		return ret;
	}
	
	public boolean removeGenres(ArrayList<String> genres) {
		if(genres==null)
			return false;
		if(genres.size()==0)
			return false;
		if(!metadata.containsKey(MetaID.GENRES))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.GENRES.toString(), new InformationHolder()
						                                                         .with(MetaID.GENRES.toString(), new InformationHolder()
								                                                                                         .with(MetaID.GENRES.toString(), genres)));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.GENRES);
			if(newInfo!=null)
				metadata.put(MetaID.GENRES, newInfo);
			else
				metadata.remove(MetaID.GENRES);
		}
		return ret;
	}
	
	public boolean removeGenres() {
		if(!metadata.containsKey(MetaID.GENRES))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.GENRES.toString(), new InformationHolder()
						                                                        .with(MetaID.GENRES.toString(), MetaID.GENRES.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.GENRES);
			if(newInfo!=null)
				metadata.put(MetaID.GENRES, newInfo);
			else
				metadata.remove(MetaID.GENRES);
		}
		return ret;
	}
	
	public ArrayList<Comment> getComments() {
		ArrayList<Comment> comments=new ArrayList<>();
		InformationHolder holder;
		if(metadata.containsKey(MetaID.COMMENT)) {
			holder = metadata.get(MetaID.COMMENT);
			if(holder==null)
				return null;
			for(InformationHolder.Information info : holder){
				InformationHolder comment=info.getHolder();
				comments.add((Comment) comment.getObject(MetaID.COMMENT.toString()));
			}
		}
		if(comments.size()==0)
			return null;
		return comments;
	}
	
	public Comment getComment(int index){
		int i=0;
		if(index<0)
			return null;
		InformationHolder holder=metadata.get(MetaID.COMMENT);
		for(InformationHolder.Information info : holder) {
			if(index==i){
				return (Comment) info.getHolder().getObject(MetaID.COMMENT.toString());
			}
			i++;
		}
		return null;
	}
	
	public boolean setComments(ArrayList<Comment> comments) {
		if(comments==null)
			return false;
		if(comments.size()==0)
			return false;
		removeAllComments();
		boolean ret=true;
		for(Comment comment : comments) {
			if(!addComment(comment))
				ret=false;
		}
		return ret;
	}
	
	public boolean addComment(Comment comment) {
		if(comment==null)
			return false;
		ArrayList<Comment> currentComments=null;
		if(metadata.containsKey(MetaID.COMMENT)) {
			currentComments = metadata.get(MetaID.COMMENT).getList(MetaID.COMMENT.toString());
			for(Comment c : currentComments) {
				if(c.equals(comment))
					return false;
			}
		}
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.COMMENT.toString(), new InformationHolder()
						                                                          .with(comment.getIdentifier(), new InformationHolder()
								                                                                                       .with(MetaID.SUB_ENCODING.toString(), Frame.encodings[0])
								                                                                                       .with(MetaID.COMMENT.toString(), comment)));
		boolean ret=mp3file.addValue(holder);
		if(ret) {
			metadata.put(MetaID.COMMENT, mp3file.getDataByPurpose(MetaID.COMMENT));
		}
		return ret;
	}
	
	public boolean addComments(ArrayList<Comment> comments) {
		return false;
	}
	
	public boolean removeComment(Comment comment) {
		return false;
	}
	
	public boolean removeComments(ArrayList<Comment> comments) {
		return false;
	}
	
	public boolean replaceComment(Comment oldComment, Comment newComment) {
		return replaceComment(oldComment.getIdentifier(), newComment);
	}
	
	/**
	 * This Method replaces a Comment identified by the old Identifier with a new Comment. If the Comment given is invalid, the Identifier or the comment is null,
	 * this Method returns false. If the
	 * @param oldIdentifier
	 * @param comment
	 * @return
	 */
	public boolean replaceComment(String oldIdentifier, Comment comment) {
		if(comment==null || oldIdentifier==null) {
			return false;
		}
		if(comment.getComment()==null || comment.getLanguage()==null)
			return false;
		InformationHolder holder;
		holder = new InformationHolder()
				         .with(MetaID.COMMENT.toString(), new InformationHolder()
						                                          .with(oldIdentifier, new InformationHolder()
								                                                               .with(MetaID.COMMENT.toString(), comment)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.COMMENT, mp3file.getDataByPurpose(MetaID.COMMENT));
		}
		return ret;
	}
	
	public boolean removeComment(String identifier) {
		if(!metadata.containsKey(MetaID.COMMENT))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.COMMENT.toString(), new InformationHolder()
						                                                          .with(identifier, identifier));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.COMMENT);
			if(newInfo!=null)
				metadata.put(MetaID.COMMENT, newInfo);
			else
				metadata.remove(MetaID.COMMENT);
		}
		return ret;
	}
	
	public boolean removeAllComments() {
		if(!metadata.containsKey(MetaID.COMMENT))
			return false;
		InformationHolder currentComments=metadata.get(MetaID.COMMENT);
		assert currentComments != null;
		InformationHolder commentIDs=new InformationHolder();
		for(InformationHolder.Information info: currentComments){
			commentIDs.with(info.getKey(), info.getKey());
		}
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.COMMENT.toString(), currentComments);
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.COMMENT);
			if(newInfo!=null)
				metadata.put(MetaID.COMMENT, newInfo);
			else
				metadata.remove(MetaID.COMMENT);
		}
		return ret;
	}
	
	public String getPath() {
		return mp3file.getPath();
	}
	
	public boolean setTracknmbr(int tracknmbr, int total_tracks){
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.TRACK_NUMBER.toString(), new InformationHolder()
						                                                         .with(MetaID.TRACK_NUMBER.toString(), new InformationHolder()
								                                                                                               .with(MetaID.TRACK_NUMBER.toString(), tracknmbr)
								                                                                                               .with(MetaID.SUB_TOTAL_TRACKS.toString(), total_tracks)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.TRACK_NUMBER, mp3file.getDataByPurpose(MetaID.TRACK_NUMBER));
		}
		return ret;
	}
	
	public int getTracknmbr(){
		if(metadata.containsKey(MetaID.TRACK_NUMBER))
			return metadata.get(MetaID.TRACK_NUMBER).getHolder(MetaID.TRACK_NUMBER.toString()).getInt(MetaID.TRACK_NUMBER.toString());
		return -1;
	}
	
	public int getTotalTracks(){
		if(metadata.containsKey(MetaID.TRACK_NUMBER)){
			return metadata.get(MetaID.TRACK_NUMBER).getHolder(MetaID.TRACK_NUMBER.toString()).getInt(MetaID.SUB_TOTAL_TRACKS.toString());
		}
		return -1;
	}
	
	public boolean removeTracknmbr(){
		if(!metadata.containsKey(MetaID.TRACK_NUMBER))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.TRACK_NUMBER.toString(), new InformationHolder()
						                                                               .with(MetaID.TRACK_NUMBER.toString(), MetaID.TRACK_NUMBER.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.TRACK_NUMBER);
			if(newInfo!=null)
				metadata.put(MetaID.TRACK_NUMBER, newInfo);
			else
				metadata.remove(MetaID.TRACK_NUMBER);
		}
		return ret;
	}
	
	public int getYear() {
		if(metadata.containsKey(MetaID.YEAR))
			try {
				return Integer.parseInt(metadata.get(MetaID.YEAR).getHolder(MetaID.YEAR.toString()).getString(MetaID.YEAR.toString()));
			} catch(NumberFormatException nfe) {
				return 0;
			}
		return -1;
	}
	
	public boolean setYear(int year) {
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.YEAR.toString(), new InformationHolder()
						                                                               .with(MetaID.YEAR.toString(), new InformationHolder()
								                                                                                                     .with(MetaID.YEAR.toString(), year)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.YEAR, mp3file.getDataByPurpose(MetaID.YEAR));
		}
		return ret;
	}
	
	public boolean removeYear() {
		if(!metadata.containsKey(MetaID.YEAR))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.YEAR.toString(), new InformationHolder()
						                                                        .with(MetaID.YEAR.toString(), MetaID.YEAR.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.YEAR);
			if(newInfo!=null)
				metadata.put(MetaID.YEAR, newInfo);
			else
				metadata.remove(MetaID.YEAR);
		}
		return ret;
	}
	
	public ArrayList<Image> getAllImages() {
		if(metadata.containsKey(MetaID.IMAGE)) {
			ArrayList<Image> images=new ArrayList<>();
			InformationHolder holder=metadata.get(MetaID.IMAGE);
			holder.setPosition(0);
			for(InformationHolder.Information info : holder) {
				InformationHolder imageholder=info.getHolder();
				if(imageholder!=null) {
					Image image= (Image) imageholder.getObject(MetaID.IMAGE.toString());
					if(image!=null)
						images.add(image);
				}
			}
			return images;
		}
		return null;
	}
	
	/**
	 * Returns the Image with this exact identifier or null if there is none with this id.
	 * @param identifier the identifier that can be obtained through the Image class.
	 * @return an Image if one with this ID can be found or null if there is none.
	 */
	public Image getImage(String identifier) {
		if(identifier==null)
			return null;
		if(metadata.containsKey(MetaID.IMAGE)) {
			InformationHolder holder=metadata.get(MetaID.IMAGE).getHolder(identifier);
			if(holder!=null)
				return (Image) holder.getObject(MetaID.IMAGE.toString());
		}
		return null;
	}
	
	public ArrayList<Image> getImages() {
		if(!metadata.containsKey(MetaID.IMAGE))
			return null;
		ArrayList<Image> images=new ArrayList<>();
		InformationHolder imageHolder=metadata.get(MetaID.IMAGE);
		assert imageHolder != null;
		for(InformationHolder.Information identifier: imageHolder){
			images.add((Image) identifier.getHolder().getObject(MetaID.IMAGE.toString()));
		}
		return images;
	}
	
	/**
	 *
	 * @param image
	 * @return
	 */
	public boolean addImage(Image image) {
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.IMAGE.toString(), new InformationHolder()
						                                                          .with(image.getIdentifier(), new InformationHolder()
								                                                                                       .with(MetaID.SUB_ENCODING.toString(), Frame.encodings[0])
								                                                                                       .with(MetaID.IMAGE.toString(), image)));
		boolean ret;
		
		ret=mp3file.setMetaData(new InformationHolder().with(MetaID.IMAGE.toString(), image));
		if(ret)
			metadata.put(MetaID.IMAGE, mp3file.getDataByPurpose(MetaID.IMAGE));
		return ret;
	}
	
	public boolean removeImage(String identifier) {
		if(!metadata.containsKey(MetaID.IMAGE))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.IMAGE.toString(), new InformationHolder()
						                                                        .with(identifier, identifier));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.IMAGE);
			if(newInfo!=null)
				metadata.put(MetaID.IMAGE, newInfo);
			else
				metadata.remove(MetaID.IMAGE);
		}
		return ret;
	}
	
	public boolean removeAllImages() {
		if(!metadata.containsKey(MetaID.IMAGE))
			return false;
		InformationHolder currentImages=metadata.get(MetaID.IMAGE);
		assert currentImages!=null;
		InformationHolder imageIDs=new InformationHolder();
		for(InformationHolder.Information info: currentImages){
			imageIDs.with(info.getKey(), info.getKey());
		}
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.IMAGE.toString(), imageIDs);
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.IMAGE);
			if(newInfo!=null)
				metadata.put(MetaID.IMAGE, newInfo);
			else
				metadata.remove(MetaID.IMAGE);
		}
		return ret;
	}
	
	public String getContentGroup() {
		if(metadata.containsKey(MetaID.CONTENT_GROUP))
			return metadata.get(MetaID.CONTENT_GROUP).getHolder(MetaID.CONTENT_GROUP.toString()).getString(MetaID.CONTENT_GROUP.toString());
		return null;
	}
	
	public boolean setContentGroup(String contentGroup) {
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.CONTENT_GROUP.toString(), new InformationHolder()
						                                                               .with(MetaID.CONTENT_GROUP.toString(), new InformationHolder()
								                                                                                                     .with(MetaID.CONTENT_GROUP.toString(), contentGroup)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.CONTENT_GROUP, mp3file.getDataByPurpose(MetaID.CONTENT_GROUP));
		}
		return ret;
	}
	
	public boolean removeContentGroup(){
		if(!metadata.containsKey(MetaID.CONTENT_GROUP))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.CONTENT_GROUP.toString(), new InformationHolder()
						                                                        .with(MetaID.CONTENT_GROUP.toString(), MetaID.CONTENT_GROUP.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.CONTENT_GROUP);
			if(newInfo!=null)
				metadata.put(MetaID.CONTENT_GROUP, newInfo);
			else
				metadata.remove(MetaID.CONTENT_GROUP);
		}
		return ret;
	}
	
	public String getSubtitle() {
		if(metadata.containsKey(MetaID.SUBTITLE))
			return metadata.get(MetaID.SUBTITLE).getHolder(MetaID.SUBTITLE.toString()).getString(MetaID.SUBTITLE.toString());
		return null;
	}
	
	public boolean setSubtitle(String subtitle) {
		if(subtitle==null) {
			return false;
		}
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.SUBTITLE.toString(), new InformationHolder()
						                                                               .with(MetaID.SUBTITLE.toString(), new InformationHolder()
								                                                                                                     .with(MetaID.SUBTITLE.toString(), subtitle)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.SUBTITLE, mp3file.getDataByPurpose(MetaID.SUBTITLE));
		}
		return ret;
	}
	
	public boolean removeSubtitle() {
		if(!metadata.containsKey(MetaID.SUBTITLE))
			return false;
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.SUBTITLE.toString(), new InformationHolder()
						                                                        .with(MetaID.SUBTITLE.toString(), MetaID.SUBTITLE.toString()));
		boolean ret=mp3file.removeValue(holder);
		if(ret){
			InformationHolder newInfo=mp3file.getDataByPurpose(MetaID.SUBTITLE);
			if(newInfo!=null)
				metadata.put(MetaID.SUBTITLE, newInfo);
			else
				metadata.remove(MetaID.SUBTITLE);
		}
		return ret;
	}
	
	public int getBPM() {
		if(metadata.containsKey(MetaID.BPM))
			return metadata.get(MetaID.BPM).getHolder(MetaID.BPM.toString()).getInt(MetaID.BPM.toString());
		return 0;
	}
	
	public boolean setBPM(int bpm) {
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.BPM.toString(), new InformationHolder()
						                                                               .with(MetaID.BPM.toString(), new InformationHolder()
								                                                                                                     .with(MetaID.BPM.toString(), bpm)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.BPM, mp3file.getDataByPurpose(MetaID.BPM));
		}
		return ret;
	}
	
	public boolean removeBPM() {
		if (!metadata.containsKey(MetaID.BPM))
			return false;
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.BPM.toString(), new InformationHolder()
						                                                          .with(MetaID.BPM.toString(), MetaID.BPM.toString()));
		boolean ret = mp3file.removeValue(holder);
		if (ret) {
			InformationHolder newInfo = mp3file.getDataByPurpose(MetaID.BPM);
			if (newInfo!=null)
				metadata.put(MetaID.BPM, newInfo);
			else
				metadata.remove(MetaID.BPM);
		}
		return ret;
	}
	
	public String getCopyrightMessage() {
		if(metadata.containsKey(MetaID.COPYRIGHT))
			return metadata.get(MetaID.COPYRIGHT).getHolder(MetaID.COPYRIGHT.toString()).getString(MetaID.COPYRIGHT.toString());
		return null;
	}
	
	public boolean setCopyrightMessage(String copyrightMessage) {
		if(copyrightMessage==null) {
			return false;
		}
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.COPYRIGHT.toString(), new InformationHolder()
						                                                      .with(MetaID.COPYRIGHT.toString(), new InformationHolder()
								                                                                                   .with(MetaID.COPYRIGHT.toString(), copyrightMessage)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.COPYRIGHT, mp3file.getDataByPurpose(MetaID.COPYRIGHT));
		}
		return ret;
	}
	
	public boolean removeCopyrightMessage() {
		if (!metadata.containsKey(MetaID.COPYRIGHT))
			return false;
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.COPYRIGHT.toString(), new InformationHolder()
						                                                        .with(MetaID.COPYRIGHT.toString(), MetaID.COPYRIGHT.toString()));
		boolean ret = mp3file.removeValue(holder);
		if (ret) {
			InformationHolder newInfo = mp3file.getDataByPurpose(MetaID.COPYRIGHT);
			if (newInfo!=null)
				metadata.put(MetaID.COPYRIGHT, newInfo);
			else
				metadata.remove(MetaID.COPYRIGHT);
		}
		return ret;
	}
	
	public String getPerformerInfo() {
		if(metadata.containsKey(MetaID.PERFORMER_INFO))
			return metadata.get(MetaID.PERFORMER_INFO).getHolder(MetaID.PERFORMER_INFO.toString()).getString(MetaID.PERFORMER_INFO.toString());
		return null;
	}
	
	public boolean setPerformerInfo(String performerInfo) {
		if(performerInfo==null) {
			return false;
		}
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.PERFORMER_INFO.toString(), new InformationHolder()
						                                                            .with(MetaID.PERFORMER_INFO.toString(), new InformationHolder()
								                                                                                               .with(MetaID.PERFORMER_INFO.toString(), performerInfo)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.PERFORMER_INFO, mp3file.getDataByPurpose(MetaID.PERFORMER_INFO));
		}
		return ret;
	}
	
	public boolean removePerformerInfo() {
		if (!metadata.containsKey(MetaID.PERFORMER_INFO))
			return false;
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.PERFORMER_INFO.toString(), new InformationHolder()
						                                                        .with(MetaID.PERFORMER_INFO.toString(), MetaID.PERFORMER_INFO.toString()));
		boolean ret = mp3file.removeValue(holder);
		if (ret) {
			InformationHolder newInfo = mp3file.getDataByPurpose(MetaID.PERFORMER_INFO);
			if (newInfo!=null)
				metadata.put(MetaID.PERFORMER_INFO, newInfo);
			else
				metadata.remove(MetaID.PERFORMER_INFO);
		}
		return ret;
	}
	
	public String getConductor() {
		if(metadata.containsKey(MetaID.CONDUCTOR))
			return metadata.get(MetaID.CONDUCTOR).getHolder(MetaID.CONDUCTOR.toString()).getString(MetaID.CONDUCTOR.toString());
		return null;
	}
	
	public boolean setConductor(String conductor) {
		if(conductor==null) {
			return false;
		}
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.CONDUCTOR.toString(), new InformationHolder()
						                                                            .with(MetaID.CONDUCTOR.toString(), new InformationHolder()
								                                                                                               .with(MetaID.CONDUCTOR.toString(), conductor)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.CONDUCTOR, mp3file.getDataByPurpose(MetaID.CONDUCTOR));
		}
		return ret;
	}
	
	public boolean removeConductor() {
		if (!metadata.containsKey(MetaID.CONDUCTOR))
			return false;
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.CONDUCTOR.toString(), new InformationHolder()
						                                                        .with(MetaID.CONDUCTOR.toString(), MetaID.CONDUCTOR.toString()));
		boolean ret = mp3file.removeValue(holder);
		if (ret) {
			InformationHolder newInfo = mp3file.getDataByPurpose(MetaID.CONDUCTOR);
			if (newInfo!=null)
				metadata.put(MetaID.CONDUCTOR, newInfo);
			else
				metadata.remove(MetaID.CONDUCTOR);
		}
		return ret;
	}
	
	public String getEncoder() {
		if(metadata.containsKey(MetaID.ENCODER))
			return metadata.get(MetaID.ENCODER).getHolder(MetaID.ENCODER.toString()).getString(MetaID.ENCODER.toString());
		return null;
	}
	
	public boolean setEncoder(String encoder) {
		if(encoder==null) {
			return false;
		}
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.ENCODER.toString(), new InformationHolder()
						                                                            .with(MetaID.ENCODER.toString(), new InformationHolder()
								                                                                                               .with(MetaID.ENCODER.toString(), encoder)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.ENCODER, mp3file.getDataByPurpose(MetaID.ENCODER));
		}
		return ret;
	}
	
	public boolean removeEncoder() {
		if (!metadata.containsKey(MetaID.ENCODER))
			return false;
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.ENCODER.toString(), new InformationHolder()
						                                                        .with(MetaID.ENCODER.toString(), MetaID.ENCODER.toString()));
		boolean ret = mp3file.removeValue(holder);
		if (ret) {
			InformationHolder newInfo = mp3file.getDataByPurpose(MetaID.ENCODER);
			if (newInfo!=null)
				metadata.put(MetaID.ENCODER, newInfo);
			else
				metadata.remove(MetaID.ENCODER);
		}
		return ret;
	}
	
	public int getPosition() {
		if(metadata.containsKey(MetaID.POSITION))
			return metadata.get(MetaID.POSITION).getHolder(MetaID.POSITION.toString()).getInt(MetaID.POSITION.toString());
		return 0;
	}
	
	public String getTimeStampFormat() {
		if(metadata.containsKey(MetaID.POSITION))
			return metadata.get(MetaID.POSITION).getHolder(MetaID.POSITION.toString()).getString(MetaID.SUB_TIMESTAMP_FORMAT.toString());
		return null;
	}
	
	public boolean setPosition(int position, String timestampformat) {
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.POSITION.toString(), new InformationHolder()
						                                                           .with(MetaID.POSITION.toString(), new InformationHolder()
								                                                                                             .with(MetaID.POSITION.toString(), position)
								                                                                                             .with(MetaID.SUB_TIMESTAMP_FORMAT.toString(), timestampformat)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.POSITION, mp3file.getDataByPurpose(MetaID.POSITION));
		}
		return ret;
	}
	
	public boolean removePosition() {
		if (!metadata.containsKey(MetaID.POSITION))
			return false;
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.POSITION.toString(), new InformationHolder()
						                                                             .with(MetaID.POSITION.toString(), MetaID.POSITION.toString()));
		boolean ret = mp3file.removeValue(holder);
		if (ret) {
			InformationHolder newInfo = mp3file.getDataByPurpose(MetaID.POSITION);
			if (newInfo!=null)
				metadata.put(MetaID.POSITION, newInfo);
			else
				metadata.remove(MetaID.POSITION);
		}
		return ret;
	}
	
	/**
	 * This Method returns a java.util.Date class containing the Date and Year of the creation of this Song. If the Year or Date is not known, this Method will return null.
	 * @return A Date containing the Day and Month as well as Year of this Song or null if one of those is not known.
	 */
	public Date getDate() {
		if(!metadata.containsKey(MetaID.DATE) || !metadata.containsKey(MetaID.YEAR))
			return null;
		InformationHolder yearholder=metadata.get(MetaID.YEAR);
		InformationHolder dateHolder=metadata.get(MetaID.DATE);
		return new Date(Integer.parseInt(yearholder.getString(MetaID.YEAR.toString())), dateHolder.getByte(MetaID.SUB_DAY.toString()), dateHolder.getByte(MetaID.SUB_MONTH.toString()));
	}
	
	public String getStringDate() {
		if(!metadata.containsKey(MetaID.DATE))
			return null;
		return metadata.get(MetaID.DATE).getString(MetaID.DATE.toString());
	}
	
	public boolean setDate(Date date) {
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.DATE.toString(), new InformationHolder()
						                                                       .with(MetaID.DATE.toString(), new InformationHolder()
								                                                                                     .with(MetaID.DATE.toString(), date)))
				                         .with(MetaID.YEAR.toString(), new InformationHolder()
						                                                       .with(MetaID.YEAR.toString(), new InformationHolder()
								                                                                                     .with(MetaID.YEAR.toString(), date.getYear())));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.DATE, mp3file.getDataByPurpose(MetaID.DATE));
			metadata.put(MetaID.YEAR, mp3file.getDataByPurpose(MetaID.YEAR));
		}
		return ret;
	}
	
	public boolean setDate(byte  day, byte month){
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.DATE.toString(), new InformationHolder()
						                                                       .with(MetaID.DATE.toString(), new InformationHolder()
								                                                                                     .with(MetaID.SUB_DAY.toString(), day)
								                                                                                     .with(MetaID.SUB_MONTH.toString(), month)));
		boolean ret=mp3file.setMetaData(holder);
		if(ret) {
			metadata.put(MetaID.DATE, mp3file.getDataByPurpose(MetaID.DATE));
		}
		return ret;
	}
	
	public boolean removeDate() {
		if (!metadata.containsKey(MetaID.DATE))
			return false;
		InformationHolder holder = new InformationHolder()
				                           .with(MetaID.DATE.toString(), new InformationHolder()
						                                                             .with(MetaID.DATE.toString(), MetaID.DATE.toString()));
		boolean ret = mp3file.removeValue(holder);
		if (ret) {
			InformationHolder newInfo = mp3file.getDataByPurpose(MetaID.DATE);
			if (newInfo!=null)
				metadata.put(MetaID.DATE, newInfo);
			else
				metadata.remove(MetaID.DATE);
		}
		return ret;
	}
	
	public void showTagRepresentation(){
		mp3file.showTagRepresentation();
	}
	
	void setInformation(InformationHolder holder) {
		for(InformationHolder.Information info: holder) {
			InformationHolder identifier=info.getHolder();
			metadata.put(MetaID.valueOf(info.getKey()), identifier);
		}
	}
	
	public ArrayList<String> getCurrentFrameOrder() {
		ArrayList<String> ausg=new ArrayList<>();
		for(MetaID id: frameorder)
			ausg.add(id.toString());
		return ausg;
	}
	
	public InformationHolder getData(MetaID identifier) {
		return mp3file.getDataByPurpose(identifier);
	}
	
	public boolean setDataToAll(InformationHolder holder) {
		return mp3file.setMetaDataForAll(holder);
	}
	
	public boolean setData(InformationHolder holder) {
		boolean ret= mp3file.setMetaData(holder);
		if(ret) {
			holder.setPosition(0);
			String key=holder.next().getKey();
			metadata.put(MetaID.valueOf(key), mp3file.getDataByPurpose(MetaID.valueOf(key)));
		}
		return ret;
	}
	
	/**
	 * Returns a string representation of the object. In general, the
	 * {@code toString} method returns a string that
	 * "textually represents" this object. The result should
	 * be a concise but informative representation that is easy for a
	 * person to read.
	 * It is recommended that all subclasses override this method.
	 * <p>
	 * The {@code toString} method for class {@code Object}
	 * returns a string consisting of the name of the class of which the
	 * object is an instance, the at-sign character `{@code @}', and
	 * the unsigned hexadecimal representation of the hash code of the
	 * object. In other words, this method returns a string equal to the
	 * value of:
	 * <blockquote>
	 * <pre>
	 * getClass().getName() + '@' + Integer.toHexString(hashCode())
	 * </pre></blockquote>
	 *
	 * @return a string representation of the object.
	 */
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append("||Song||\n");
		sb.append("Path: ").append(mp3file.getPath()).append("\n");
		sb.append("Title: ").append(getTitle()).append("\n");
		sb.append("Subtitle: ").append(getSubtitle()).append("\n");
		sb.append("Album: ").append(getAlbum()).append("\n");
		sb.append("Artists: \n");
		ArrayList<String> artists=getArtists();
		if(artists!=null) {
			for (String s : artists)
				sb.append("\t").append(s).append("\n");
			sb.append("Genres: \n");
		}
		ArrayList<String> genres=getGenres();
		if(genres!=null) {
			for (String s : genres)
				sb.append("\t").append(s).append("\n");
		}
		sb.append("Content Group: ").append(getContentGroup()).append("\n");
		sb.append("Features: \n");
		ArrayList<String> features=getFeatures();
		if(features!=null) {
			for (String s : features)
				sb.append("\t").append(s).append("\n");
		}
		sb.append("Tracknumber: ").append(getTracknmbr()).append("\n");
		sb.append("Total Tracks: ").append(getTotalTracks()).append("\n");
		sb.append("Year of creation: ").append(getYear()).append("\n");
		sb.append("Date of creation: ").append("").append("\n");
		sb.append("Comments: ").append(getComments()).append("\n");
		sb.append("Images: \n");
		sb.append(getNmbrOfImages()).append(" Images Present\n");
		ArrayList<Image> images=getAllImages();
		if(images!=null) {
			for (Image image : images) {
				sb.append("\t").append(image.getMime_type()).append("\n");
				sb.append("\t").append(image.getPicture_type()).append("\n");
				//sb.append("\t").append(image.).append("\n");
				sb.append("\t---------------\n");
			}
		}
		sb.append("Position in the MP3 File: ").append(getPosition()).append(" ").append(getTimeStampFormat()).append("\n");
		sb.append("Encoder: ").append(getEncoder()).append("\n");
		sb.append("Conductor: ").append(getConductor()).append("\n");
		sb.append("Beats per Minute: ").append(getBPM()).append("\n");
		sb.append("Copyright Message: ").append(getCopyrightMessage()).append("\n");
		sb.append("Performer Info: ").append(getPerformerInfo()).append("\n");
		return sb.toString();
	}
	
	public int getNmbrOfImages() {
		if(metadata.containsKey(MetaID.IMAGE)) {
			return metadata.get(MetaID.IMAGE).size();
		}
		return 0;
	}
	
	public InformationHolder getDataByID(String dataID) {
		return metadata.get(MetaID.valueOf(dataID));
	}
	
	/**
	 * Returns a List of every Data currently saved in this Song
	 * @return
	 */
	ArrayList<InformationHolder> getAllData() {
		ArrayList<InformationHolder> list=new ArrayList<>();
		for(MetaID id : MetaID.values()) {
			if(metadata.containsKey(id)) {
				InformationHolder holder=metadata.get(id);
				if(!holder.contains(MetaID.IDENTIFIER.toString()))
					holder.with(MetaID.IDENTIFIER.toString(), id.toString());
				list.add(holder);
			}
		}
		return list;
	}
	
	public InformationHolder get(String s) {
		return metadata.get(MetaID.valueOf(s));
	}
	
	boolean isFramesEqualToMetaData(){
		ArrayList<String> identifiers=new ArrayList<>();
		for(MetaID id:frameorder) {
			InformationHolder identifierHolder=metadata.get(id);
			for(InformationHolder.Information identifier : identifierHolder) {
				identifiers.add(identifier.getKey());
			}
		}
		ArrayList<String> framesList = mp3file.getCurrentFrameOrder();
		if(framesList.containsAll(identifiers))
			return identifiers.containsAll(framesList);
		return false;
	}
	
	/**
	 * Debugging Method:
	 */
	/*boolean containsFrame(String identifier) {
		return mp3file.containsFrame(identifier);
	}*/
}
