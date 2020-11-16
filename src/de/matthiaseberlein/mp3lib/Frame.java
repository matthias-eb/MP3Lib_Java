package de.matthiaseberlein.mp3lib;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

//ToDo: implement Comparator or Comparable
abstract class Frame implements Comparable<Frame>{
	private final String id;
	private int framesize;
	private long startbyte;
	private boolean flag_tag_alter_preservation;
	private boolean flag_file_alter_preservation;
	private boolean flag_read_only;
	private boolean flag_compression;
	private boolean flag_grouping_identity;
	private boolean flag_encryption;
	private boolean valid;
	static HashMap<String, String> frametitles=new HashMap<>(); //ToDo: either convert to array or make non-static
	static String[] encodings={"ISO-8859-1", "UCS-2", "UTF-16BE", "UTF-8"};
	
	Frame(String id, int framesize, long startbyte, byte[] flags, File file) {
		setFlags(flags);
		this.id=id;
		this.framesize=framesize;
		this.startbyte=startbyte;
		
		
	}
	
	Frame(String id, long startbyte, InformationHolder data){
		this.startbyte=startbyte;
		this.id=id;
		flag_tag_alter_preservation=false;
		flag_file_alter_preservation=false;
		flag_read_only=false;
		flag_compression=false;
		flag_grouping_identity=false;
		flag_encryption=false;
	}
	
	static void init() {
		frametitles.put("AENC", "Audio Encryption");
		frametitles.put("APIC", "Attached Picture");
		frametitles.put("COMM", "Comments");
		frametitles.put("COMR", "Commercial Frame");
		frametitles.put("ENCR", "Encryption Method Registration");
		frametitles.put("EQUA", "Equalization");
		frametitles.put("ETCO", "Event Timing Codes");
		frametitles.put("GEOB", "General Encapsulated Object");
		frametitles.put("GRID", "Group identification registration");
		frametitles.put("IPLS", "Involved people list");
		frametitles.put("LINK", "Linked information");
		frametitles.put("MCDI", "Music CD identifier");
		frametitles.put("MLLT", "MPEG location lookup table");
		frametitles.put("OWNE", "Ownership frame");
		frametitles.put("PRIV", "Private frame");
		frametitles.put("PCNT", "Play counter");
		frametitles.put("POPM", "Popularimeter");
		frametitles.put("POSS", "Position synchronisation frame");
		frametitles.put("RBUF", "Recommended buffer size");
		frametitles.put("RVAD", "Relative volume adjustment");
		frametitles.put("RVRB", "Reverb");
		frametitles.put("SYLT", "Synchronized lyric/text");
		frametitles.put("SYTC", "Synchronized tempo codes");
		frametitles.put("TALB", "Album/Movie/Show title");
		frametitles.put("TBPM", "BPM (beats per minute)");
		frametitles.put("TCOM", "Composer");
		frametitles.put("TCON", "Content type");
		frametitles.put("TCOP", "Copyright message");
		frametitles.put("TDAT", "Date");
		frametitles.put("TDLY", "Playlist delay");
		frametitles.put("TENC", "Encoded by");
		frametitles.put("TEXT", "Lyricist/Text writer");
		frametitles.put("TFLT", "File type");
		frametitles.put("TIME", "Time");
		frametitles.put("TIT1", "Content group description");
		frametitles.put("TIT2", "Title/songname/content description");
		frametitles.put("TIT3", "Subtitle/Description refinement");
		frametitles.put("TKEY", "Initial key");
		frametitles.put("TLAN", "Language(s)");
		frametitles.put("TLEN", "Length");
		frametitles.put("TMED", "Media type");
		frametitles.put("TOAL", "Original album/movie/show title");
		frametitles.put("TOFN", "Original filename");
		frametitles.put("TOLY", "Original lyricist(s)/text writer(s)");
		frametitles.put("TOPE", "Original artist(s)/performer(s)");
		frametitles.put("TORY", "Original release year");
		frametitles.put("TOWN", "File owner/licensee");
		frametitles.put("TPE1", "Lead performer(s)/Soloist(s)");
		frametitles.put("TPE2", "Band/orchestra/accompaniment");
		frametitles.put("TPE3", "Conductor/performer refinement");
		frametitles.put("TPE4", "Interpreted, remixed, or otherwise modified by");
		frametitles.put("TPOS", "Part of a set");
		frametitles.put("TPUB", "Publisher");
		frametitles.put("TRCK", "Track number/Position in set");
		frametitles.put("TRDA", "Recording dates");
		frametitles.put("TRSN", "Internet radio station name");
		frametitles.put("TRSO", "Internet radio station owner");
		frametitles.put("TSIZ", "Size");
		frametitles.put("TSRC", "ISRC (international standard recording code)");
		frametitles.put("TSSE", "Software/Hardware and settings used for encoding");
		frametitles.put("TYER", "Year");
		frametitles.put("TXXX", "User defined text information frame");
		frametitles.put("UFID", "Unique file identifier");
		frametitles.put("USER", "Terms of use");
		frametitles.put("USLT", "Unsychronized lyric/text transcription");
		frametitles.put("WCOM", "Commercial information");
		frametitles.put("WCOP", "Copyright/Legal information");
		frametitles.put("WOAF", "Official audio file webpage");
		frametitles.put("WOAR", "Official artist/performer webpage");
		frametitles.put("WOAS", "Official audio source webpage");
		frametitles.put("WORS", "Official internet radio station homepage");
		frametitles.put("WPAY", "Payment");
		frametitles.put("WPUB", "Publishers official webpage");
		frametitles.put("WXXX", "User defined URL link frame");
		frametitles.put("TCMP", "ITunes compilation Flag");
		
	}
	
	void setFlags(byte[] flags){
		flag_tag_alter_preservation=(((flags[0] >> 7) & 1) == 1);
		flag_file_alter_preservation=(((flags[0] >> 6) & 1) == 1);
		flag_read_only=(((flags[0] >> 5) & 1) == 1);
		flag_compression=(((flags[1] >> 7) & 1) == 1);
		flag_encryption = (((flags[1] >> 6) & 1) == 1);
		flag_grouping_identity = (((flags[1] >> 5) & 1) == 1);
	}
	
	/**
	 * This method should only be called after the Frame and the whole Frame List got checked for Validity. This method writes the Frame to the File if the valid Flag is true.
	 * @param raf The Writer used for writing the Information to the file. Cannot be null.
	 * @return true if the writing was successful and false if the Frame could or should not be written.
	 * @throws IOException
	 */
	boolean writeFrame(RandomAccessFile raf) throws IOException{
		
		//check for Validity
		if(!valid)
			return false;
		
		//write Header
		raf.seek(startbyte-10);
		raf.writeBytes(id);
		raf.writeInt(framesize);
		byte b=0;
		if(flag_tag_alter_preservation){
			b= (byte) (b | (1<<7));
		}
		if(flag_file_alter_preservation)
			b=(byte) (b | (1<<6));
		if(flag_read_only)
			b=(byte) (b | (1<<5));
		raf.writeByte(b);
		
		if(flag_compression){
			b= (byte) (b | (1<<7));
		}
		if(flag_encryption)
			b=(byte) (b | (1<<6));
		if(flag_grouping_identity)
			b=(byte) (b | (1<<5));
		raf.writeByte(b);
		
		//write The actual Frame and delegate it to the lower levels
		//Can't be done by this Method since the content isn't saved here and the Encoding is not clear.
		return write(raf);
	}
	
	/**
	 * calculate the new Frame length by adding all the Datalengths together in their written form.
	 */
	void recalculateFrameSize(){
		framesize= getBytes().length;
	}
	
	abstract InformationHolder getData();
	
	abstract boolean write(RandomAccessFile raf) throws IOException;
	
	abstract byte[] getBytes();
	
	static boolean isValidFrameId(String frameId){
		if(frametitles.size()==0)
			init();
		return frametitles.containsKey(frameId);
	}
	String getFrameTitle(String frameId){
		return frametitles.get(frameId);
	}
	
	abstract boolean setData(InformationHolder data);
	
	abstract boolean removeData(InformationHolder data);
	
	abstract boolean addData(InformationHolder data);
	
	abstract String[] getFrameString();
	
	String getFrameView(){
		String ausg="++  Startbyte: "+startbyte+"  ++";
		String linesplits[]=getFrameString();
		int maxwidth=ausg.length();
		ausg+="\n+";
		int wallspacing=2;
		String wall="|";
		ArrayList<String> linesplitsfinal = new ArrayList<>();
		for(String s:linesplits){
			String s2[]=s.split("\n");
			for(String ss:s2){
				if(ss.length()>maxwidth)
					maxwidth=ss.length();
				linesplitsfinal.add(ss);
			}
		}
		maxwidth+=(wallspacing*2)+(wall.length()*2);
		for(int i=0;i<maxwidth-(2*wall.length());i++){
			ausg+="-";
		}
		ausg+="+\n";
		for(String s: linesplitsfinal){
			ausg+=addWall(s, maxwidth, wallspacing, wall)+"\n";
		}
		ausg+="+";
		for(int i=0;i<maxwidth-(2*wall.length());i++){
			ausg+="-";
		}
		ausg+="+\n";
		ausg+="++  Endbyte: "+(startbyte+framesize)+"  ++\n\n";
		return ausg;
	}
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
	byte[] cleanUCS_2(byte[] bytes){
		byte[] ausg;
		int cnt=0;
		for(int i=4;i<bytes.length;i++) {
			if(bytes[i]!=0 && bytes[i] <= 0x7f){
				cnt++;
			}
		}
		ausg=new byte[cnt];
		cnt=0;
		for(int i=4;i<bytes.length;i++){
			if(bytes[i]!=0) {
				ausg[cnt] = bytes[i];
				cnt++;
			}
		}
		return ausg;
	}
	void showHexDump(byte[] bytes){
		int columns=8;
		char[] hexVal={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		int spacing=columns*3, current_spacing=0;
		System.out.println("Hex dump: ");
		byte cnt=0, b;
		for(int i=0;i<bytes.length;i+=columns) {
			for(int j=0;j<columns;j++) {
				if(bytes.length>i+j){
					b=bytes[i+j];
					System.out.print("" + hexVal[(b >> 4) & 0x0f] + hexVal[b & 0x0f] + " ");
					current_spacing+=3;
				}
				else {
					for(int x=current_spacing;x<spacing;x++){
						System.out.print(" ");
					}
					break;
				}
			}
			System.out.print("| ");
			for(int j=0;j<columns;j++){
				if(bytes.length> i+j) {
					b=bytes[i+j];
					System.out.print(new String(new byte[]{b})+" ");
				}
			}
			System.out.println();
			current_spacing=0;
		}
		System.out.println();
	}
	
	boolean isValid() {
		return valid;
	}
	
	/**
	 * Checks the attributes for validity and sets the valid flag depending on the result. Should always be called with super().
	 */
	void setValidity() {
		valid=true;
		if(id==null)
			valid=false;
		if(id.length()!=4)
			valid=false;
		if(framesize<=0)
			valid=false;
		if(startbyte<20)
			valid=false;
	}
	
	void invalidate(){
		this.valid=false;
	}
	
	/**
	 * This Method has two main tasks: Make sure that every Frame is accessible individually as well as making sure there can't be any duplicate Frame where there shouldn't be one:
	 * For example the Image class will either take the added content Hash Values as Content Descriptor or just "Picture Type 1" in Hash Values if that Picture Type is set.
	 * That way an additional class for controlling the Duplication of Frames is unnecessary.
	 * @return An Identifying String that depends on its content for Multi frames and will be the Purpose as String for Single Frames.
	 */
	abstract String getIdentifier();
	
	abstract MetaID getPurpose();
	
	int getFramesize() {
		return framesize;
	}
	
	long getStartbyte() {
		return startbyte;
	}
	
	void setStartbyte(long startbyte) {
		if(startbyte>20)
			this.startbyte = startbyte;
	}
	
	String getId() {
		return id;
	}
	
	/**
	 * Returns a positive Result if the current Frame should be placed before the Frame given by the parameter. The order is specified by frameorder.
	 * @param frame A Frame or an extension of it. Should not be null
	 * @return 0 if the Frame is null, positive if the Frame should be placed before the given one and negative otherwise.
	 */
	@Override
	public int compareTo(Frame frame) {
		if(frame == null)
			return 0;
		//int nmbr=frameorder.indexOf(id);
		//int nmbr_frgn=frameorder.indexOf(frame.id);
		//return nmbr-nmbr_frgn;
		return 0;   //ToDo: Irgendwie an frameorder wieder dran kommen?
	}
	
	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <p>
	 * The {@code equals} method implements an equivalence relation
	 * on non-null object references:
	 * <ul>
	 * <li>It is <i>reflexive</i>: for any non-null reference value
	 * {@code x}, {@code x.equals(x)} should return
	 * {@code true}.
	 * <li>It is <i>symmetric</i>: for any non-null reference values
	 * {@code x} and {@code y}, {@code x.equals(y)}
	 * should return {@code true} if and only if
	 * {@code y.equals(x)} returns {@code true}.
	 * <li>It is <i>transitive</i>: for any non-null reference values
	 * {@code x}, {@code y}, and {@code z}, if
	 * {@code x.equals(y)} returns {@code true} and
	 * {@code y.equals(z)} returns {@code true}, then
	 * {@code x.equals(z)} should return {@code true}.
	 * <li>It is <i>consistent</i>: for any non-null reference values
	 * {@code x} and {@code y}, multiple invocations of
	 * {@code x.equals(y)} consistently return {@code true}
	 * or consistently return {@code false}, provided no
	 * information used in {@code equals} comparisons on the
	 * objects is modified.
	 * <li>For any non-null reference value {@code x},
	 * {@code x.equals(null)} should return {@code false}.
	 * </ul>
	 * <p>
	 * The {@code equals} method for class {@code Object} implements
	 * the most discriminating possible equivalence relation on objects;
	 * that is, for any non-null reference values {@code x} and
	 * {@code y}, this method returns {@code true} if and only
	 * if {@code x} and {@code y} refer to the same object
	 * ({@code x == y} has the value {@code true}).
	 * <p>
	 * Note that it is generally necessary to override the {@code hashCode}
	 * method whenever this method is overridden, so as to maintain the
	 * general contract for the {@code hashCode} method, which states
	 * that equal objects must have equal hash codes.
	 *
	 * @param obj the reference object with which to compare.
	 * @return {@code true} if this object is the same as the obj
	 * argument; {@code false} otherwise.
	 * @see #hashCode()
	 * @see HashMap
	 */
	@Override
	public boolean equals(Object obj) {
		if(!Frame.class.isAssignableFrom(obj.getClass())) {
			if(String.class.isAssignableFrom(obj.getClass())) {
				return getIdentifier().equals(obj);
			}
			return false;
		}
		Frame f= (Frame) obj;
		return getIdentifier().equals(f.getIdentifier());
	}
}
