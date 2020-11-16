package de.matthiaseberlein.mp3lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * FrameTCON is the Frame for the list of genres that the Song belongs to. It has an Encoding like every other Text Frame and cannot be used twice in the same Mp3 File.
 * What this class does:
 *  It reads the genres saved in the Frame and saves them as Strings so they are human readable. If there is a reference to a V1 genre, the genre is found out and saved. If there is
 *  a refinement for this genre, the refinement is saved. If th
 */
class FrameTCON extends Frame {
	private byte encoding;
	private ArrayList<String> genres;
	
	private String genrelist[] = {
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
	
	/**
	 * Reads the given File at the startbyte and reads until the framesize is reached. The Frame consists of an Encoding byte and A Numerical String containing one or mutliple genres.
	 * This Constructor reads the Genres and splits them up in individual Genres. It saves the Name of the most precise Genre, meaning if the House is dereferenced and the Refinement sais
	 * "Club House", the Refinement is saved since it contains the accurate Genre.
	 * @param framesize
	 * @param startbyte
	 * @param flags
	 * @param file
	 */
	FrameTCON(int framesize, long startbyte, byte[] flags, File file) {
		super("TCON", framesize, startbyte, flags, file);
		genres=new ArrayList<>();
		
		RandomAccessFile raf=null;
		BufferedReader br=null;
		try {
			
			raf=new RandomAccessFile(file,"r");
			raf.seek(startbyte);
			
			encoding=raf.readByte();
			if(encoding==1) //UTF-16LE funktioniert nicht als Ersatz für UCS-2, daher wird das ganze einfach von Hand gemacht indem UTF-8 gelesen wird und dann der Schrott raus geworfen wird.
				br=new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("ISO-8859-1")));
			else
				br=new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName(encodings[encoding])));
			
			br.skip(startbyte + 1);
			CharBuffer buffer=CharBuffer.allocate(framesize-1);
			int read_bytes = br.read(buffer);
			if(read_bytes == 0)
				System.err.println("No Bytes were read from the input! Unknown error");
			else if(read_bytes == -1)
				System.err.println("Error: End of File was reached while reading.");
			buffer.position(0);
			String s = String.valueOf(buffer);
			if(encoding==1){
				s=new String(cleanUCS_2(s.getBytes()));
			}
			String genre="";
			int genrenmbr;
			int ind_open=-1, ind_close=-1, ind_next=-1;
			do {
				ind_open=s.indexOf("(", ind_close+1);
				ind_close=s.indexOf(")", ind_open+1);
				if(ind_open==-1){   //If it is the first iteration or the loop and no opening Bracket was found
					genres.add(s);
				} else if(ind_next==-1 && ind_open!=0 && ind_close!=-1) {  //If it is the first iteration, a genre was written out and then a Number follows: xxxx(...
					genres.add(s.substring(0, ind_open));
				} else if(ind_next==-1 && ind_open!=0) {    //A genre was written out first and has an opening Bracket but no closing one. Should not happen but who knows...
					genres.add(s);
				} else {
					if(ind_close==-1) { //Weird case, should not happen: "yyyyyy(xxxx"
						genres.add(s.substring(ind_open));
						ind_next=-1;
					} else {    //A reference to a V1 genre or a RX/CR annotation was found.
						ind_next=s.indexOf("(", ind_close+1);
						if(ind_next==ind_close+1) { // There are two genres or a refinement with a bracket.
							genre=s.substring(ind_open+1, ind_close);
							try{
								genrenmbr=Integer.parseInt(genre);
								genre=genrelist[genrenmbr];
							} catch(NumberFormatException nfe) {
								if(genre.equals("RX"))
									genres.add("Remix");
								else if(genre.equals("CR"))
									genres.add("Cover");
							} catch(ArrayIndexOutOfBoundsException oob) {   //If the Number is not known, just use "Other" as genre
								genre=genrelist[12];
							}
							//If a Refinement is present, it is used as Genrename.
							if(s.charAt(ind_next+1)=='(') { //Refinement with Brackets
								ind_close=s.indexOf(")", ind_next+1);
								if(ind_close==-1) { //The Refinement does not have a closing bracket (Weird shit, I know)
									ind_close = s.indexOf(")", ind_open + 1);
									genre=s.substring(ind_next+1);
									ind_next=-1;
								} else
									genre=s.substring(ind_next+1, ind_close+1);
							}
						} else if(ind_next!=-1) {    //Refinement without a bracket
							genre=s.substring(ind_close+1, ind_next);
						}
						genres.add(genre);
					}
				}
			} while(ind_next!=-1);
			buffer.clear();
			if(encoding!=1) {
				for (int i = 0; i < genres.size(); i++) {
					if (genres.get(i).charAt(genres.get(i).length() - 1) == 0)
						genres.set(i, genres.get(i).substring(0, genres.get(i).length() - 1));
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				raf.close();
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	FrameTCON(long startbyte, InformationHolder data) {
		super("TCON", startbyte, data);
		this.genres=new ArrayList<>();
		this.encoding=0;
		setData(data);
		setValidity();
	}
	
	@Override
	public InformationHolder getData() {
		InformationHolder holder=new InformationHolder()
				                         .with(MetaID.GENRES.toString(), new InformationHolder()
						                                                         .with(getIdentifier(), new InformationHolder()
								                                                                                .with(MetaID.GENRES.toString(), new ArrayList<String>(genres))
								                                                                                .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
		return holder;
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	protected byte[] getBytes() {
		int cnt=1;
		String sw=createWriteableString();
		cnt+=sw.getBytes().length;
		byte ausg[] = new byte[cnt];
		ausg[0]=encoding;
		System.arraycopy(sw.getBytes(), 0, ausg, 1, sw.getBytes().length);
		return ausg;
	}
	
	private String createWriteableString() {
		StringBuilder ausg= new StringBuilder();
		for(String s: genres) {
			if(s.equals("Remix"))
				ausg.append("(").append("RX").append(")");
			else if(s.equals("Cover"))
				ausg.append("(").append("CR").append(")");
			else {
				String nmbr=getV1Genre(s);
				if(s.startsWith("("))
					ausg.append(nmbr).append("(").append(s);
				else
					ausg.append(nmbr).append(s);
			}
		}
		return ausg.toString();
	}
	
	private String getV1Genre(String genre){
		String v1nmbr=null;
		for(int i=0;i<genrelist.length;i++) {
			if(genrelist[i].toLowerCase().equals(genre.toLowerCase()))
				return "("+i+")";
			if(genre.toLowerCase().contains(genrelist[i].toLowerCase())) {
				if(v1nmbr==null) {
					v1nmbr="("+i+")";
				}
				else if(v1nmbr.length()<genrelist[i].length()) {
					v1nmbr="("+i+")";
				}
			}
		}
		if(v1nmbr==null)
			return "()";
		return v1nmbr;
	}
	
	@Override
	public boolean setData(InformationHolder data) {
		ArrayList<String> list=null;
		boolean ret=true;
		boolean sizeChanged=false;
		MetaID id;
		for(InformationHolder.Information info : data) {
			id= MetaID.valueOf(info.getKey());
			switch(id) {
				case GENRES:
					if(ArrayList.class.isAssignableFrom(info.getInnerClass())) {
						if(info.<String>getList().contains("")) {
							list = info.<String>getList();
							while (list.contains("")){
								list.remove("");
							}
							if(list.size()==0) {
								ret = false;
								break;
							}
						}
						genres.clear();
						for(String s: info.<String>getList()) {
							String s2 = "";
							if (s.contains("(")) {
								int i = -1, i2 = 0;
								while (i < s.length()) {
									i = s.indexOf("(", i + 1);
									if (i == -1)
										break;
									i2 = s.indexOf(")", i);
									if (i2 == -1)
										break;
									s2 = s.substring(i+1, i2);
									try {
										s2 = genrelist[Integer.parseInt(s2)];
									} catch (NumberFormatException | ArrayIndexOutOfBoundsException nfe) {
										System.out.println("Error: " + s2 + " was not a Number or an invalid one.");
										genres.add(s);
										continue;
									}
									genres.add(s2);
								}
							}
							else
								genres.add(s);
						}
						sizeChanged=true;
					}
					else if(String.class.isAssignableFrom(info.getInnerClass())) {
						if(info.getString().equals("")) {
							ret=false;
							break;
						}
						genres.clear();
						String s=info.getString();
						String s2="";
						if(s.contains("(")){
							int i=0, i2=0;
							while(i<s.length()){
								i=s.indexOf("(", i+1);
								if(i==-1)
									break;
								i2 = s.indexOf(")", i);
								if(i2==-1)
									break;
								s2=s.substring(i, i2-i);
								try{
									s2=genrelist[Integer.parseInt(s2)];
								} catch(NumberFormatException | ArrayIndexOutOfBoundsException nfe){
									System.out.println("Error: "+s2+" was not a Number or an invalid Number.");
									genres.add(s);
									continue;
								}
								genres.add(s2);
							}
						}
						else
							genres.add(info.getString());
						sizeChanged=true;
					}
					else ret=false;
					break;
				case SUB_ENCODING:
					if (Byte.class.isAssignableFrom(info.getInnerClass())) {
						if (info.getByte() < 0 || info.getByte() >= encodings.length)
							ret = false;
						else {
							encoding = info.getByte();
						}
					} else if(String.class.isAssignableFrom(info.getInnerClass())) {
						list = new ArrayList<>(Arrays.asList(encodings));
						if (list.indexOf(info.getString())==-1)
							ret=false;
						else {
							this.encoding = (byte) list.indexOf(info.getString());
						}
					}
					else
						ret=false;
					break;
			}
		}
		if(sizeChanged) {
			recalculateFrameSize();
			setValidity();
		}
		return ret;
	}
	
	@Override
	public boolean removeData(InformationHolder data) {
		boolean ret=true;
		boolean sizeChanged=false;
		MetaID id;
		for(InformationHolder.Information info : data) {
			id=MetaID.valueOf(info.getKey());
			if (id == MetaID.GENRES) {
				if (ArrayList.class.isAssignableFrom(info.getInnerClass())) {
					ArrayList<String> list=info.<String>getList();
					while(list.contains("")){
						list.remove("");
					}
					if(list.size()==0) {
						ret = false;
						continue;
					}
					ret=genres.removeAll(info.<String>getList());
					sizeChanged = true;
				} else if (Integer.class.isAssignableFrom(info.getInnerClass())) {
					int i=info.getInt();
					if(i<0 || i>=genres.size())
						ret=false;
					else {
						genres.remove(info.getInt());
						sizeChanged = true;
					}
				}
			}
		}
		if(sizeChanged) {
			recalculateFrameSize();
			setValidity();
		}
		return ret;
	}
	
	@Override
	public boolean addData(InformationHolder data) {
		boolean ret=true;
		boolean sizeChanged=false;
		MetaID id;
		
		for(InformationHolder.Information info : data) {
			id=MetaID.valueOf(info.getKey());
			if (id == MetaID.GENRES) {
				if (ArrayList.class.isAssignableFrom(info.getInnerClass())) {
					ArrayList<String> list=info.getList();
					while(list.contains("")) {
						list.remove("");
					}
					if(list.size()==0) {
						ret = false;
						continue;
					}
					for(String s: info.<String>getList()) {
						String s2 = "";
						if (s.contains("(")) {
							int i = -1, i2 = 0;
							while (i < s.length()) {
								i = s.indexOf("(", i + 1);
								if (i == -1)
									break;
								i2 = s.indexOf(")", i);
								if (i2 == -1)
									break;
								s2 = s.substring(i+1, i2);
								try {
									s2 = genrelist[Integer.parseInt(s2)];
								} catch (NumberFormatException | ArrayIndexOutOfBoundsException nfe) {
									System.out.println("Error: " + s2 + " was not a Number or an invalid one.");
									genres.add(s);
									continue;
								}
								genres.add(s2);
							}
						}
						else
							genres.add(s);
					}
					sizeChanged=true;
				}
			}
		}
		if(sizeChanged) {
			recalculateFrameSize();
			setValidity();
		}
		return ret;
	}
	
	@Override
	protected String[] getFrameString() {
		StringBuilder genrerepresentation= new StringBuilder();
		for(int i=0;i<genres.size();i++){
			genrerepresentation.append("\n\t").append(genres.get(i));
		}
		return new String[]{"FrameID: "+getId(), "Encoding: "+encodings[encoding], "Genres: "+genrerepresentation.toString()};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length) {
			invalidate();
			return;
		}
		if(genres==null) {
			invalidate();
			return;
		}
		if(genres.size()==0) {
			System.err.println("Genres were empty!");
			invalidate();
			return;
		}
		for(String gen: genres) {
			if(gen==null) {
				invalidate();
				System.err.println("Genre was null!");
			}
		}
	}
	
	@Override
	String getIdentifier() {
		return MetaID.GENRES.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.GENRES;
	}
}
