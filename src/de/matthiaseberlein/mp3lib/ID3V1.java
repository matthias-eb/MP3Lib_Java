package de.matthiaseberlein.mp3lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is for Reading and changing values in the V1 Tag at the end of the .mp3 File. It can then write the complete Tag back to the File.
 * Reading is done with the standard Encoding iso8859-1.
 * This class also knows if the Information is written to the file or not.
 */
class ID3V1 {
	private long startbyte; //Startbyte: Erste Stelle an der das V1 Tag beginnt, MIT dem "TAG"
	private String title;
	private String artists;
	private String album;
	private String comment;
	private int year;
	private byte tracknmbr;
	private byte genre; //ToDo: make -1 a valid option for "no Genre"
	private boolean tag_written;
	private boolean valid;
	private boolean present;
	
	private static String genrelist[] = {
			"Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B",
			"Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop", "Vocal",
			"Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk",
			"Space", "Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnik", "Gothik", "Darkwave", "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance",
			"Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave", "Psychadelic",
			"Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk", "Folk-Rock",
			"National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival", "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock",
			"Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music", "Sonata", "Symphony",
			"Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rythmic Soul", "Freestyle", "Duet",
			"Punk Rock", "Drum Solo", "A capella", "Euro-House", "Dance Hall", "Goa", "Drum & Bass", "Club-House", "Hardcore Techno", "Terror", "Indie", "BritPop", "Negerpunk",
			"Polsk Punk", "Beat", "Christian Gangsta Rap", "Heavy Metal", "Black Metal", "Crossover", "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal",
			"Anime", "Jpop", "Synthpop", "Abstract", "Art Rock", "Baroque", "Bhangra", "Big beat", "Breakbeat", "Chillout", "Downtempo", "Dub", "EBM", "Eclectic", "Electro",
			"Electroclash", "Emo", "Experimental", "Garage", "Global", "IDM", "Illbient", "Industro-Goth", "Jam Band", "Krautrock", "Leftfield", "Lounge", "Math Rock", "New Romantic",
			"Nu-Breakz", "Post-Punk", "Post-Rock", "Psytrance", "Shoegaze", "Space Rock", "Trop Rock", "World Music", "Neoclassical", "Audiobook", "Audio theatre", "Neue Deutsche Welle",
			"Podcast", "Indie-Rock", "G-Funk", "Dubstep", "Garage Rock", "Psybient"};
	
	
	ID3V1() {
		present=false;
		tag_written=false;
		title="";
		year=0;
		tracknmbr=0;
		genre=-1;
		startbyte=-1;
		this.album="";
		this.comment="";
		valid=false;
	}
	
	/**
	 * Creates a V1 Tag for an existing Song and assigns its values. If the Song is null, this Tag will be marked as invalid.
	 * If a Songinformation is empty, the Bytes will be 0.
	 * If a Songinformation cannot be assigned due to it being too large, the Information is going to be saved until the point it runs out of space and a [...] is added instead.
	 * @param song the song with the information to be saved in the Tag.
	 */
	ID3V1(Song song) {
		
		if(song==null) {
			valid = false;
			present=false;
			tag_written=false;
			return;
		}
		this.title = song.getTitle();
		this.album = song.getAlbum();
		this.comment = song.getComment(0).getComment();
		this.artists = song.getArtists().toString();
		setTracknmbr(song.getTracknmbr());
		this.year = song.getYear();
		this.genre = -1;
		for(int i=0;i<genrelist.length;i++){
			for (String genr: song.getGenres()) {
				if (genrelist[i].equals(genr)) {
					this.genre = (byte) i;
					break;
				}
			}
			if(genre==i)
				break;
		}
		if(genre==-1){
			genre=12;
		}
		
		present=false;
		tag_written=false;
	}
	
	/**
	 * Reads the Tag at the end of the given File. If the file is nonexistend or null, the valid bit will be false. This also applies if the path points to a directory or link or if the File does not end with .mp3.
	 * @param path  The path to be read from. Must contain a .mp3 File.
	 * @throws IOException  Gets thrown if The file is not present or readable. Should not happen.
	 */
	ID3V1(String path) throws IOException {
		File f=new File(path);
		RandomAccessFile raf=null;
		try {
			raf=new RandomAccessFile(f, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		byte b[]=new byte[3];
		String s="";
		//ID3V1 Tag auslesen und füllen
		try {
			startbyte=raf.length()-128;
			raf.seek(startbyte);
			raf.read(b, 0, 3);
			s=new String(b);
			if(s.equals("TAG")) {
				//Log.v("Nachricht", "ID3 V1 Tag gefunden");
				System.out.println("ID3V1 Tag gefunden");
				tag_written=true;
				valid=true;
			}
			else{
				//Log.v("Nachricht", "ID3 V1 Tag fehlt."yte);
				System.out.println("ID3V1 Tag fehlt.");
				startbyte=raf.length();
				tag_written=false;
				present=false;
				valid=false;
				genre=12;
				title="";
				artists ="";
				album="";
				comment="";
				
				return;
			}
			b=new byte[30];
			raf.read(b, 0, 30);
			title=new String(purgeZeros(b));
			raf.read(b, 0, 30);
			artists =new String(purgeZeros(b));
			raf.read(b, 0, 30);
			album=new String(purgeZeros(b));
			b=new byte[4];
			raf.read(b, 0, 4);
			s=new String(purgeZeros(b));
			if(s.length()==0)
				year=0;
			else {
				try {
					year = Integer.parseInt(s);
				} catch(NumberFormatException nfe){
					System.err.println("Year could not be parsed: \""+s+"\". It will be replaced by 0.");
					year=0;
				}
			}
			b=new byte[30];
			raf.read(b, 0, 30);
			if(b[28]==0) {
				tracknmbr = b[29];
				comment=new String(purgeZeros(Arrays.copyOf(b, 29)));
			}
			else {
				tracknmbr=0;
				comment=new String(b);
			}
			b=new byte[1];
			raf.read(b, 0, 1);
			genre=b[0];
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		finally {
			raf.close();
		}
		System.out.println("ID3V1 tag gelesen\n");
	}
	
	/**
	 * Method for developing purposes. Used for an accurate representation of a few Bytes in Bitform.
	 * @param barray    The bytes to be shown.
	 * @return a String containing the Bit values as ascii 0 or 1
	 */
	private String byteRepresentation(byte[] barray) {
		StringBuilder s= new StringBuilder();
		for (byte aBarray : barray) {
			for (int j = 0; j < 8; j++) {
				s.append(aBarray >> j & 1);
			}
			s.append(" ");
		}
		return s.toString();
	}
	
	/**
	 * Sets the startbyte of this Tag. Should only be used if the Mp3 Data is changed or the Startbyte is read from a File.
	 * @param startbyte must a positive Number >= 50
	 */
	void setStartbyte(long startbyte) {
		if(startbyte<50)
			return;
		tag_written=false;
		this.startbyte = startbyte;
	}
	
	String getTitle() {
		if(title.endsWith("[...]"))
			return title.substring(0, title.length()-5);
		return title;
	}
	
	void setTitle(String title) {
		if(title.length()>30)
			this.title=title.substring(0, 25)+"[...]";
		else
			this.title = title;
		tag_written=false;
	}
	
	/**
	 *
	 * @param purpose the data to be set. Cannot be null.
	 */
	boolean setData(InformationHolder purpose) {
		if(purpose == null)
			return false;
		boolean ret=true;
		String shortening="";
		ArrayList<String> list=null;
		MetaID id;
		for(InformationHolder.Information identifiers : purpose) {
			if(InformationHolder.class.isAssignableFrom(identifiers.getInnerClass())) {
				InformationHolder identifierHolder=identifiers.getHolder();
				for(InformationHolder.Information datatypes : identifierHolder) {
					if(InformationHolder.class.isAssignableFrom(datatypes.getInnerClass())) {
						InformationHolder dataHolder = datatypes.getHolder();
						for (InformationHolder.Information value : dataHolder) {
							id = MetaID.valueOf(value.getKey());
							switch (id) {
								case TITLE:
									shortening = value.getString();
									if (shortening.length() > 30)
										this.title = shortening.substring(0, 25) + "[...]";
									else
										this.title = shortening;
									tag_written = false;
									break;
								case ARTISTS:
									list = value.<String>getList();
									for (String s : list) {
										shortening += s + ", ";
									}
									shortening = shortening.substring(0, shortening.length() - 2);
									if (shortening.length() > 30)
										this.artists = shortening.substring(0, 25) + "[...]";
									else
										this.artists = shortening;
									tag_written = false;
									break;
								case TRACK_NUMBER:
									System.out.println(value.toString());
									if (Byte.class.isAssignableFrom(value.getInnerClass())) {
										this.tracknmbr = value.getByte();
										tag_written = false;
									} else if (Integer.class.isAssignableFrom(value.getInnerClass())) {
										this.tracknmbr = (byte) value.getInt();
										tag_written = false;
									}
									
									break;
								case COMMENT:
									Comment comm = (Comment) value.getObject();
									shortening = comm.getComment();
									if (shortening.length() > 30)
										this.comment = shortening.substring(0, 25) + "[...]";
									else
										this.comment = shortening;
									tag_written = false;
									break;
								case GENRES:
									if (List.class.isAssignableFrom(value.getInnerClass())) {
										list = value.<String>getList();
										ret = setGenre(list.get(0));
									}
									break;
								case YEAR:
									if (Integer.class.isAssignableFrom(value.getInnerClass())) {
										year = value.getInt();
										tag_written = false;
									} else if (String.class.isAssignableFrom(value.getInnerClass())) {
										try {
											year = Integer.parseInt(value.getString());
											tag_written = false;
										} catch (NumberFormatException nfe) {
											nfe.printStackTrace();
										}
									} else if (Byte.class.isAssignableFrom(value.getInnerClass())) {
										year = value.getByte();
										tag_written = false;
									}
								case ALBUM:
									if (String.class.isAssignableFrom(value.getInnerClass())) {
										shortening = value.getString();
										if (shortening.length() > 30)
											this.album = shortening.substring(0, 25) + "[...]";
										else
											this.album = shortening;
										tag_written = false;
									}
									break;
								default:
								
							}
						}
					}
				}
			}
		}
		
		return ret;
	}
	
	String getArtists() {
		if(artists.endsWith("[...]"))
			return artists.substring(0, artists.length()-5);
		return artists;
	}
	
	/**
	 * Sets a new Interpret for this Track. If the String is too long, it gets shortened down to 25 bytes and a [...] is added.
	 * @param artists The new Interpret. Must not be null.
	 */
	void setArtists(String artists) {
		if(artists ==null)
			return;
		if(artists.length()>30)
			this.artists = artists.substring(0, 25)+"[...]";
		else
			this.artists = artists;
		tag_written=false;
	}
	
	String getAlbum() {
		if(album.endsWith("[...]"))
			return album.substring(0, album.length()-5);
		return album;
	}
	
	/**
	 * Same as above. Album will get shortened to 25 bytes plus a [...] if longer than 30. Won't be changed if parameter is null.
	 * @param album Non-Null album with <=30 Letters/Bytes
	 */
	void setAlbum(String album) {
		if(album==null)
			return;
		if(album.length()>30)
			this.album=album.substring(0, 25)+"[...]";
		else
			this.album = album;
		tag_written=false;
	}
	
	String getComment() {
		if(comment.endsWith("[...]"))
			return comment.substring(0, comment.length()-5);
		return comment;
	}
	
	/**
	 * The same procedure as every year, james. Comment has a maximum of 30 bytes, everything longer will get shortened down.
	 * @param comment Non-Null, Please in Human readable language and better not too long or it will get cut up in pieces and implanted with new parts.
	 */
	void setComment(String comment) {
		if(comment==null)
			return;
		if(comment.length()>30)
			this.comment=comment.substring(0, 25)+"[...]";
		else
			this.comment = comment;
		tag_written=false;
	}
	
	int getYear() {
		return year;
	}
	
	void setYear(int year) {
		tag_written=false;
		this.year = year;
	}
	
	int getTracknmbr() {
		return tracknmbr;
	}
	
	/**
	 * Sets a new Tracknumber for this Song. The Number has to be between 0 and 255.
	 * @param tracknmbr a Number between 0 and 255
	 */
	void setTracknmbr(int tracknmbr) {
		tag_written=false;
		if(tracknmbr<0 || tracknmbr>255)
			return;
		this.tracknmbr = (byte) tracknmbr;
	}
	
	String getGenre() {
		return genrelist[genre & 0xFF];
	}
	
	/**
	 * Sets a new Genre. If the genre was not recognized, the Genre will be "Other". Not case-sensitive.
	 * @param genre The Name of the genre. The genrelist can be looked at above.
	 * @return false if and only if the genre contained a '(' and inside was not a number.
	 */
	boolean setGenre(String genre) {
		System.out.println("Genre: "+genre);
		if(genre.contains("(") && genre.contains(")")){
			try{
				//Get the Number from in between the first Brackets of the String and set that genre
				this.genre=Byte.parseByte(genre.substring(genre.indexOf("(")+1, genre.indexOf(")", genre.indexOf("("))));
				tag_written=false;
				return true;
			} catch(NumberFormatException nfe){
				nfe.printStackTrace();
				return false;
			}
		}
		for(int i=0;i<genrelist.length;i++){
			if(genre.toLowerCase().equals(genrelist[i].toLowerCase())) {
				this.genre = (byte)i;
				tag_written=false;
				return true;
			}
		}
		for(int i=0;i<genrelist.length;i++){
			if(genre.toLowerCase().contains(genrelist[i].toLowerCase())){
				this.genre = (byte) i;
				tag_written=false;
				return true;
			}
		}
		this.genre=12;  //Set genre to "Other" and be done with it
		tag_written=false;
		return true;
	}
	
	/**
	 * Writes the changes made back to the File. If there are no changes, nothing happens. If the Tag is marked as invalid or the File is not a .mp3 File, nothing will be written as well.
	 * This method checks for an existing V1 Tag and will overwrite it if has found one, in the case that no Tag could be found at that position, the startbyte will get adjusted to try to find it that way.
	 * If the method still does not find a V1 Tag, a new Tag is written at the end of the file.
	 * @param path The path that the V1 Tag should be written to. Cannot be null and has to be pointing to an existing file.
	 * @return false if there was an error, true if not
	 * @throws IOException should not be thrown unless some kind of writing right problem occured or something similar to that happened.
	 */
	boolean write(String path) throws IOException {
		if(path==null) {
			System.err.println("The path that should have been written to was null!");
			return false;
		}
		File f=new File(path);
		if(!f.exists() || !path.endsWith(".mp3")) {
			System.err.println("The path does not point to a .mp3 File or the File does not exist! \nPath: "+path);
			return false;
		}
		if(!valid) {
			System.err.println("This Tag was marked as invalid! Nothing will be written.");
			return false;
		}
		RandomAccessFile raf=null;
		try {
			raf=new RandomAccessFile(f, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		raf.seek(startbyte);
		byte b[]=new byte[3];
		raf.read(b, 0, 3);
		if(!new String(b).equals("TAG")) {
			System.out.println("The V1 Tag could not be found at the old position. Trying to find the new startbyte...");
			startbyte = raf.length() - 128;
			raf.seek(startbyte);
			raf.read(b, 0, 3);
			if(!new String(b).equals("TAG")){
				startbyte=raf.length();
				raf.seek(startbyte);
				tag_written=false;
			}
		}
		if(tag_written) {
			System.out.println("The Tag information does not seem to have changed. Aborting..");
			return false;
		}
		
		//Write tag here
		
		
		tag_written=true;
		present=true;
		return tag_written;
	}
	
	/**
	 * Development helper method used for making the current ID3v1 Tag visible.
	 */
	void showTagRepresentation() {
		if(!valid)
			return;
		String wall="#";
		int distanceToWall=1;
		int maxwidth=45; //30 Bytes + 2 Bytes wall + 2 Bytes spacing + Max. 11 Bytes Titleing = 45 Bytes/Letters
		String message="";
		for(int i=0;i<maxwidth;i++){
			message+="#";
		}
		message+="\n";
		message+=addWall("Title: "+title, maxwidth, distanceToWall, wall)+"\n";
		message+=addWall("Interpret: "+ artists, maxwidth, distanceToWall, wall)+"\n";
		message+=addWall("Album: "+album, maxwidth, distanceToWall, wall)+"\n";
		message+=addWall("Comment: "+comment, maxwidth, distanceToWall, wall)+"\n";
		message+=addWall("Genre: "+genrelist[genre], maxwidth, distanceToWall, wall)+"\n";
		message+=addWall("Year: "+year, maxwidth, distanceToWall, wall)+"\n";
		message+=addWall("Tracknumber: "+tracknmbr, maxwidth, distanceToWall, wall)+"\n";
		message+=addWall("", maxwidth, distanceToWall, wall)+"\n";
		for(int i=0;i<maxwidth;i++){
			message+="#";
		}
		message+="\n";
		System.out.println(message);
		//Log.v("Nachricht", message);
	}
	
	/**
	 * Development helper method used for adding a Wall on the sides of a Text.
	 * @param s The String to be put inside the Walls
	 * @param maxWidth The width that the box should have. If s is bigger, then it will get shortened.
	 * @param distanceToWall The Spacing between the Wall and the String. Gets added on both sides.
	 * @param wall he String that should be used as Wall.
	 * @return The Walled in String. (aka Mexiko)
	 */
	private String addWall(String s, int maxWidth, int distanceToWall, String wall){    //Trump would be happy to have this method
		String ausg="";
		if(s.length()>(maxWidth-(distanceToWall*2)-(wall.length()*2))){
			if(s.lastIndexOf(" ")==-1){
				s=s.substring(0, maxWidth-(distanceToWall*2)-(wall.length()*2)-5)+"[...]";
			}
		}
		ausg=wall;
		for(int i=0;i<distanceToWall;i++){
			ausg+=" ";
		}
		ausg+=s;
		//Fill up the space until the maxwidth-wall-spacing is reached.
		int filling=maxWidth-ausg.length()-distanceToWall-wall.length();
		for(int i=0;i<filling;i++){
			ausg+=" ";
		}
		for(int i=0;i<distanceToWall;i++){
			ausg+=" ";
		}
		ausg+=wall;
		
		return ausg;
	}
	
	boolean isValid() {
		return valid;
	}
	
	boolean isPresent(){return present;}
	
	/**
	 * Used for removing all Zero bytes in a byte array. the resulting array is then returned in a new array.
	 * @param bytes the bytes with 0 values
	 * @return A byte array without zeros
	 */
	private byte[] purgeZeros(byte[] bytes){
		byte[] ausg;
		int cnt=0;
		for (byte aByte : bytes) {
			if (aByte != 0) {
				cnt++;
			}
		}
		ausg=new byte[cnt];
		cnt=0;
		for (byte aByte : bytes) {
			if (aByte != 0) {
				ausg[cnt] = aByte;
				cnt++;
			}
		}
		return ausg;
	}
	
	public InformationHolder getData() {
		if(!present)
			return new InformationHolder();
		return new InformationHolder()
				       .with(MetaID.YEAR.toString(), new InformationHolder()
						                                     .with(MetaID.YEAR.toString(), new InformationHolder()
								                                                                   .with(MetaID.YEAR.toString(), year)))
				       .with(MetaID.TRACK_NUMBER.toString(), new InformationHolder()
						                                             .with(MetaID.TRACK_NUMBER.toString(), new InformationHolder()
								                                                                                   .with(MetaID.TRACK_NUMBER.toString(), tracknmbr)))
				       .with(MetaID.TITLE.toString(), new InformationHolder()
						                                      .with(MetaID.TITLE.toString(), new InformationHolder()
								                                                                     .with(MetaID.TITLE.toString(), title)))
				       .with(MetaID.ARTISTS.toString(), new InformationHolder()
						                                        .with(MetaID.ARTISTS.toString(), new InformationHolder()
								                                                                         .with(MetaID.ARTISTS.toString(), artists)))
				       .with(MetaID.GENRES.toString(), new InformationHolder()
						                                       .with(MetaID.GENRES.toString(), new InformationHolder()
								                                                                       .with(MetaID.GENRES.toString(), genre)))
				       .with(MetaID.ALBUM.toString(), new InformationHolder()
						                                      .with(MetaID.ALBUM.toString(), new InformationHolder()
								                                                                     .with(MetaID.ALBUM.toString(), album)));
	}
	
	/**
	 * Uses the purpose - identifier - data Structure to determine what kind of Data should be removed. The identifier and data part are not used since
	 * there are no duplicates among the ID3V1 Datakinds.
	 * @param holder an InformationHolder ordered with the "root -> purposes -> Identifiers -> Data" Hierarchy. Only the Purpose is used so the inner Holders can be empty.
	 */
	public void removeData(InformationHolder holder) {
		for(InformationHolder.Information purpose : holder) {
			MetaID purpID=MetaID.valueOf(purpose.getKey());
			switch(purpID){
				case YEAR:
					year=0;
					break;
				case TITLE:
					title="";
					break;
				case TRACK_NUMBER:
					tracknmbr=0;
					break;
				case GENRES:
					genre=-1;
					break;
				case ARTISTS:
					artists="";
					break;
				case ALBUM:
					album="";
					break;
			}
		}
	}
	
	/**
	 * Removes the Data identified by its Purpose if the id3V1 Tag uses it.
	 * @param purpose
	 */
	void removeDataByPurpose(String purpose) {
		MetaID purpID=MetaID.valueOf(purpose);
		switch(purpID){
			case YEAR:
				year=0;
				break;
			case TITLE:
				title="";
				break;
			case TRACK_NUMBER:
				tracknmbr=0;
				break;
			case GENRES:
				genre=-1;
				break;
			case ARTISTS:
				artists="";
				break;
			case ALBUM:
				album="";
				break;
		}
	}
}
