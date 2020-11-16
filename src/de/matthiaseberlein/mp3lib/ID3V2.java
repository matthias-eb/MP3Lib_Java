package de.matthiaseberlein.mp3lib;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class is for maintaining the integrity of the various Frame types that exist as well as reading the Information out of it,
 * changing said information and writing it to the file correctly as well as reordering the Frames if necessary or creating new ones.
 * If a frame is not known, a Proxy class will be created that simply saves the Frame bytes completely and can write them back to the file whenever necessary.
 * This class also makes sure that no Frame duplicates get saved where it isn't allowed.
 * Information read by the Frame classes get handed through this one to the upper classes using this.
 * Information changes get handed down to the right Frame class through this one as well.
 * Deleting frames and reordering as well as setting the new startbytes will get handled by this class as well.
 * Writing information back to the file will get initialized here for all frames. If frames got deleted, the new and old padding will get zeroed completely so remaining Framebytes won't confuse programs when reading.
 */
class ID3V2 {
	
	private boolean present;
	private long tagsize;
	private int extended_header_size;
	private double tag_version;
	private boolean flag_unsynchronization;
	private boolean flag_extended_header;
	private boolean flag_experimental;
	private boolean crc_data_present;
	private boolean valid;
	private long padding;
	
	private ArrayList<Frame> frames;    //Has every frame including pictureFrames in the Order of the File Structure
	
	/**
	 * Creates a new ID3V2 Tag without any Data but with a standardized size. Uses the flag_experimental as long as it is not fully tested yet.
	 */
	ID3V2() {
		present=false;
		frames=new ArrayList<>();
		flag_extended_header=false;
		flag_unsynchronization=false;
		flag_experimental=false;
		tagsize=1500;
		tag_version=2.3;
		crc_data_present=false;
		valid=false;
		padding=0;
	}
	
	/**
	 * Create the ID3V2 Tag from the Song information given. Flags are filled with default values and can be changed afterwards. ToDo flag control
	 * @param song the Song containing all the information to be saved in the Frames. Empty information will be ignored.
	 */
	ID3V2(Song song) {
		frames=new ArrayList<>();
		flag_extended_header=false;
		extended_header_size=0;
		flag_experimental=true;     //ToDo: Change this back to false once testing on the new MP3File is finished
		flag_unsynchronization=false;
		crc_data_present=false;
		tag_version=2.3;
		padding=0;
		valid=true;
		
		//ToDo: create Frames for the Songinformation
		createFramesFromData(song);
	}
	
	/**
	 * Reads the Frame information stored in an .mp3 File and saves the Frames in a List. ToDo: React to different flags. Right now, flags are completely ignored.
	 * Data will not be read if one of the following is true:
	 *      The Path does not point to a File or does not exist
	 *      The Path does not end with .mp3
	 *      The .mp3 File is not readable (it will try to get read Access tho)
	 * @param path the path the .mp3 File is at.
	 * @throws IOException
	 */
	ID3V2(String path) throws IOException {
		File mp3file=new File(path);
		if(!mp3file.canRead()) {
			if(!mp3file.setReadable(true)) {
				this.present=false;
				valid=false;
				return;
			}
		}
		RandomAccessFile raf=new RandomAccessFile(mp3file, "r");
		byte[] b=new byte[3];
		raf.read(b, 0, 3);
		if(!new String(b).equals("ID3")) {
			System.out.println("ID3 V2 Tag missing");
			present=false;
			return;
		}
		b=new byte[2];
		raf.read(b, 0, 2);
		tag_version=2;
		tag_version+=(double) b[0]/(double)10;
		tag_version+=(double) b[1] /(double)100;
		System.out.println("ID3 V"+tag_version+" Tag gefunden");
		
		frames=new ArrayList<>();
		
		byte flags=raf.readByte();
		this.flag_experimental=(flags>>5 & 1) == 1;
		this.flag_extended_header=(flags>>6 & 1) ==1;
		this.flag_unsynchronization=(flags>>7 & 1) ==1;
		System.out.println("Flags:\n\tUnsynchronization: "+flag_unsynchronization+"\n\tExtended Header: "+flag_extended_header+"\n\tExperimental: "+flag_experimental);
		
		b=new byte[4];
		raf.read(b, 0, 4);
		setTagSizeFromBytes(b);
		System.out.println("Tagsize (excluding the normal header): "+tagsize);
		if(flag_extended_header){
			//read extended Header
			extended_header_size=raf.readInt();
			crc_data_present=((raf.readByte()>>7)&1)==1;
			raf.readByte();
			if(crc_data_present){
				raf.read(b, 0, 4);
				System.out.println("raw crc Data: "+ bytesAsString(b));
				System.out.println("as String: "+new String(b));
			}
		}
		
		int framesize=0;
		Frame frame;
		//Frame reading starts
		while(raf.getFilePointer()<tagsize+10){
			b=new byte[4];
			raf.read(b, 0, 4);
			String frameId=new String(b);
			if(!Frame.isValidFrameId(frameId)) {
				if(Arrays.equals(frameId.getBytes(), new byte[]{0, 0, 0, 0})){
					System.out.println("Padding reached at Position "+raf.getFilePointer());
				}
				else
					System.out.println("False framesize or non-zeroed padding. Supposed Frame ID: "+frameId+" at Position "+raf.getFilePointer());
				System.out.println("Padding size: "+(tagsize+10-raf.getFilePointer())+" Bytes");
				break;
			}
			framesize=raf.readInt();
			b=new byte[2];
			raf.read(b, 0, 2);
			createFrameFromFile(frameId, framesize, raf.getFilePointer(), b, mp3file);
			raf.skipBytes(framesize);
		}
		System.out.println("ID3V2 Tag gelesen.\n");
	}
	
	public ID3V2(String path, boolean quickRead) {
	
	}
	
	/**
	 * This method will create a Frameclass for the Frame Header that got read in the constructor. If the frame ID is not known, a ProxyFrame is created and returned.
	 * Frames created will automatically read their Frame bodies and save their data.
	 * Successfully read Frames are automatically added to the frames List if they are not duplicates and valid.
	 * @param frameId   the Frame ID read from the Header. if this is null, null will be returned. Otherwise the Frame class for this id will be created or, if this id is not known, a Proxy class.
	 * @param framesize the size of the Frame. It tells, how many bytes are read until the end of the Frame is reached.
	 * @param startbyte The byte that the Frame class will start to read from.
	 * @param flags Two bytes containing the Flags for this Frame. The flags are going to be extracted in the Frame class.
	 * @param file The path to the .mp3 File to be read from.
	 * @return Returns a Proxy Frame if there is no matching FrameId or a matching Frame for this Id. Returns null if FrameId is null.
	 */
	private Frame createFrameFromFile(String frameId, int framesize, long startbyte, byte[] flags, File file) {
		if(frameId==null)
			return null;
		Frame frame = null;
		//ToDo: Frametype klassen erstellen und Informationen durchreichen
		switch(frameId){
			case "TALB":
				frame = new FrameTALB(framesize, startbyte, flags, file);
				break;
			case "APIC":
				frame=new FrameAPIC(framesize, startbyte, flags, file);
				break;
			case "TIT1":
				frame=new FrameTIT1(framesize, startbyte, flags, file);
				break;
			case "TIT2":
				frame=new FrameTIT2(framesize, startbyte, flags, file);
				break;
			case "TIT3":
				frame=new FrameTIT3(framesize, startbyte, flags, file);
				break;
			case "TYER":
				frame=new FrameTYER(framesize, startbyte, flags, file);
				break;
			case "TCON":
				frame=new FrameTCON(framesize, startbyte, flags, file);
				break;
			case "TCOP":
				frame=new FrameTCOP(framesize, startbyte, flags, file);
				break;
			case "TBPM":
				frame=new FrameTBPM(framesize, startbyte, flags, file);
				break;
			case "COMM":
				frame=new FrameCOMM(framesize, startbyte, flags, file);
				break;
			case "TPE1":
				frame=new FrameTPE1(framesize, startbyte, flags, file);
				break;
			case "TPE2":
				frame=new FrameTPE2(framesize, startbyte, flags, file);
				break;
			case "TPE3":
				frame=new FrameTPE3(framesize, startbyte, flags, file);
				break;
			case "TENC":
				frame=new FrameTENC(framesize, startbyte, flags, file);
				break;
			case "TDAT":
				frame=new FrameTDAT(framesize, startbyte, flags, file);
				break;
			case "TRCK":
				frame=new FrameTRCK(framesize, startbyte, flags, file);
				break;
			case "POSS":
				frame=new FramePOSS(framesize, startbyte, flags, file);
				break;
			default:
				frame=new FrameProxy(frameId, framesize, startbyte, flags, file);
				System.out.println("FrameProxy erstellt!");
		}
		if(!frames.contains(frame))
			frames.add(frame);
		return frame;
	}
	
	/**
	 * This method creates several new Frames, for each Information given by the Song class one Frame in the order of frameorder.
	 * This method will automatically assign a startbyte and calculate the Framesize.
	 * If the data is null, the Method will return without doing anything. If a generated Frame did not set its valid flag, it will be added but the Frames cannot be written.
	 * Duplicate Frames are dropped.
	 * If the Frame is valid and no duplicate, it will be added to the frames list.
	 * @param data  The Data containing the song information. Cannot be null.
	 */
	private void createFramesFromData(Song data) {
		if(data==null)
			return;
		data.getAllData();
	}
	
	/**
	 * This method is for debugging purposes. It makes a few bytes of data visible in binary form contained in a String.
	 * @param barray
	 * @return A String containing the bits grouped as bytes
	 */
	private String bytesAsString(byte[] barray) {
		StringBuilder s= new StringBuilder();
		for (byte aBarray : barray) {
			for (int j = 7; j >= 0; j--) {
				s.append(aBarray >> j & 1);
			}
			s.append(" ");
		}
		return s.toString();
	}
	
	/**
	 * Writes the current List of frames to the given file after checking its integrity and validity.
	 * @param path the path to write the frames to.
	 * @return Returns true if the frames got written successfully.
	 */
	boolean write(String path) throws IOException {
		long position=10;
		//Check if every frame is valid and the sizes and startbytes are correct
		for(Frame frame: frames){
			if(frame.getStartbyte()!=position) {
				System.out.println("frame "+frame.getId()+" is either at the wrong position or the size of the earlier was not correct.");
				//Log.e("Nachricht","frame "+frame.id+" is either at the wrong position or the size of the earlier was not correct.");
				return false;
			}
			if(!frame.isValid()) {
				System.out.println("frame " + frame.getId() + " is not valid.");
				//Log.e("Nachricht", "frame " + frame.id + " is not valid.");
				return false;
			}
			position+=frame.getFramesize();
		}
		//actually write the data
		RandomAccessFile raf=new RandomAccessFile(new File(path), "rw");
		for(Frame frame: frames){
			frame.write(raf);
		}
		return true;
	}
	
	/**
	 * Returns the Size of the ID3V2 Tag in bytes
	 * @return
	 */
	long getTagsize() {
		return tagsize;
	}
	
	/**
	 * Removes a frame completely. Excludes certain frames that have to be present logically like Songtitle. recalculates the startbytes of all following Frames and assigns them.
	 * @param identifier The idnetifier that uniquely identifies the frame to be removed.
	 * @return true if deleting the frame was successful.
	 */
	boolean removeFrame(String identifier){
		for(Frame f: frames){
			if(f.getIdentifier().equals(identifier))
				return frames.remove(f);
		}
		return false;
	}
	
	/**
	 * If a frame got altered or removed, the frame startbytes after the current one need to be fixed. This method addresses that.
	 */
	private void fixFrameStartbytes(){
		if(frames.size()==0)
			return;
		Frame frame=frames.get(0);
		if(flag_extended_header)
			frame.setStartbyte(20+extended_header_size);
		else
			frame.setStartbyte(20);
		long startbyte=frame.getStartbyte()+frame.getFramesize()+10;    //10 Bytes for next Frame Header
		for(int i=1; i<frames.size(); i++) {
			frame=frames.get(i);
			frame.setStartbyte(startbyte);
			startbyte+=frame.getFramesize()+10;
		}
	}
	
	/**
	 * Lists all Frame IDs possible and returns them
	 * @return a String Array of all possible Frame IDs
	 */
	String[] listFrameIDs(){
		return (String[]) Frame.frametitles.keySet().toArray();
	}
	
	/**
	 * Method for calculating the Tag size read as bytes from the File. Should be used only if the Tag gets read from an existing File.
	 * Sets the Tagsize of this frame using the bytes read directly from the file. The 4 byte's highest bit is always zero to avoid false syncsignals. From the ID3.org site:
	 * The ID3v2 tag size is encoded with four bytes where the most significant bit (bit 7) is set to zero in every byte, making a total of 28 bits. The zeroed bits are ignored, so a 257 bytes long tag is represented as $00 00 02 01.
	 * @param b the 4 bytes that together build the tag size
	 */
	private void setTagSizeFromBytes(byte[] b){
		tagsize=0;
		int cnt=0;
		for(int j=3;j>=0;j--) {
			for (int i = 0; i < 7 ; i++) {
				tagsize = tagsize | ((long)((b[j]>>i) & 1)<<cnt);
				cnt++;
			}
		}
	}
	
	/**
	 * Removes one or multiple parts from the List of data of a frame or a whole Frame. A frame can get deleted through sending a String as Identifier in the purpose holder.
	 * One or multiple parts of the List get deleted by packing an ArrayList in the appropriate identifier Holder and Purpose Holder with the right Key.
	 * If after removing the data the Frame is invalid (by removing every Part of the Main List it contained), it will get deleted.
	 * @param purpose the Data which should be removed from the Frame. For a documentation on how this should look like, go into the correct Frame class to find out.
	 * @return false if the frame does not have a list of data or if the data or there was no IDENTIFIER Key or the identifier was not known.
	 */
	boolean removeData(InformationHolder purpose){
		for(InformationHolder.Information identifier : purpose){
			if(InformationHolder.class.isAssignableFrom(identifier.getInnerClass())){
				for(Frame f: frames){
					if(f.getIdentifier().equals(identifier.getKey())) {
						boolean ret = f.removeData(identifier.getHolder());
						if(ret){
							if(!f.isValid())
								ret=removeFrame(identifier.getKey());
						}
						return ret;
					}
				}
			}
			else if(String.class.isAssignableFrom(identifier.getInnerClass())){
				return removeFrame(identifier.getString());
			}
		}
		return false;
	}
	
	/**
	 * Adds one or multiple parts to the List of purpose of a specific type of Frame, identified through the Keys inside the purpose with their respective Holders of Data.
	 * @param purposeholder the purpose that contains one or multiple Identifiers with the Data to be added.
	 * @return false if there is no frame possessing that identifier or if the addData Method returned false.
	 */
	boolean addData(MetaID purpose, InformationHolder purposeholder) {
		boolean ret=true;
		for(InformationHolder.Information identifier : purposeholder) {
			boolean frame_found=false;
			for(Frame f: frames){
				if(f.getIdentifier().equals(identifier.getKey())){
					frame_found=true;
					if(ret)
						ret=f.addData(identifier.getHolder());
					else
						f.addData(identifier.getHolder());
				}
			}
			if(!frame_found)
				addFrame(purpose, purposeholder);
		}
		return ret;
	}
	
	/**
	 * Calls the setData Method of every single Frame and hands over the data. If the Frame uses a key specified in the data, it will set the Value that it contains if possible.
	 * @param data  The data to be set. For a documentation of its form, take a look at InformationHolder.
	 * @return true if the Data could be added or the Data was not used by the Frames. False if at least one setData Method returned false.
	 */
	boolean setDataToAll(InformationHolder data) {
		boolean ret=true;
		for(Frame f: frames) {
			if(ret)
				ret=f.setData(data);
			else
				f.setData(data);
		}
		return ret;
	}
	
	/**
	 * For developing purposes. Shows the current frames List and its start- and endbytes.
	 */
	void showTagRepresentation(){
		for(Frame f: frames){
			System.out.print(f.getFrameView());
			//Log.v("Nachricht", f.getFrameView());
		}
	}
	
	/**
	 * Selfexplanatory. Returns the current Frame Order as an ArrayList of Identifiers.
	 * @return a list of Identifiers showing the Order the Frames are in.
	 */
	ArrayList<String> getCurrentFrameOrder() {
		ArrayList<String> list=new ArrayList<>();
		for(Frame f: frames) {
			list.add(f.getIdentifier());
		}
		return list;
	}
	
	/**
	 * Returns the Data belonging to this Identifier in an InformationHolder representing the wanted Purpose. It then contains one or multiple InformationHolders
	 * with the Identifiers as keys.
	 * @param purpose Obtainable in Song.MetaID
	 * @return the InformationHolder(s) identified by the Purpose or an empty InformationHolder.
	 */
	public InformationHolder getDataByPurpose(MetaID purpose) {
		InformationHolder holder=new InformationHolder();
		for(Frame f: frames) {
			if(f.getPurpose().equals(purpose)) {
				InformationHolder data = f.getData();
				data.setPosition(0);
				holder.join(data.next().getHolder());
			}
		}
		if(holder.size()==0)
			return null;
		return holder;
	}
	
	/**
	 * Returns the specified Identifier as an InformationHolder. Top level is the Identifier itself, so the containing Informations are those of the Frame itself.
	 * @param identifier The identifier to be returned.
	 * @return An InfomationHolder containing all Information of the Frame. If none was found with this ID, an empty Holder will be returned.
	 */
	public InformationHolder getDataByIdentifier(String identifier){
		for(Frame f: frames) {
			if(f.getIdentifier().equals(identifier)){
				InformationHolder data=f.getData();
				data.setPosition(0);
				data=data.next().getHolder();
				data.setPosition(0);
				return data.next().getHolder();
			}
		}
		return new InformationHolder();
	}
	
	/**
	 * Sets the data contained in holder to one or multiple Frames. The Frames that get called are identified by the structural design of the holder:
	 * the holder can contain multiple InformationHolders itself which identify the purpose. Inside the purpose Holders are the identifiers that identify the individual frames.
	 * If the frame is not found it will be added with addFrame() using the purpose and the inner identification holder.
	 * @param holder the Data to be replaced. Must be built like described above.
	 * @return false if at least one setData Method or addFrame method returned false. True otherwise.
	 */
	boolean setData(InformationHolder holder) {
		boolean ret=true;
		boolean present;
		for(InformationHolder.Information purpose: holder){
			InformationHolder identifiers=purpose.getHolder();
			if(identifiers!=null) {
				for (InformationHolder.Information identifier : identifiers) {
					InformationHolder frameData=identifier.getHolder();
					if(frameData!=null) {
						present=false;
						for(Frame f: frames) {
							if(f.equals(identifier.getKey())) { //This works since Frame has the equals Method overloaded in such a way that, if the object to be compared to is a String, it will get compared to the identifier directly.
								if(ret) {
									InformationHolder h = f.getData();  //Helper Variable that saves that last Data from f to reset it if the Frame Data was duplicate (For example two Picture Type 1s).
									h.getHolder(f.getPurpose().toString()).setPosition(0);
									ret = f.setData(frameData);
									if(!f.getData().getHolder(f.getPurpose().toString()).contains(h.getHolder(f.getPurpose().toString()).next().getKey())){
										byte cnt=0;
										for(int i=0;i<frames.size();i++){
											Frame ff=frames.get(i);
											if(ff.getIdentifier().equals(f.getIdentifier())){
												cnt++;
												if(cnt>1){
													System.err.println("Error: Duplicate Frame Data.");
													ret=false;
													h.getHolder(f.getPurpose().toString()).setPosition(0);
													f.setData(h.getHolder(f.getPurpose().toString()).next().getHolder());
													break;
												}
											}
										}
									}
								}
								else
									f.setData(frameData);
								present=true;
								
								break;  //Break since there can be only one frame with matching Identifier
							}
						}
						if(!present){
							if(ret)
								ret=addFrame(MetaID.valueOf(purpose.getKey()), frameData);
							else
								addFrame(MetaID.valueOf(purpose.getKey()), frameData);
						}
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * Creates the correct frame for the purpose(The Song id attributes) and saves the Data in it. If the Frame is not a duplicate, it will be added to the end of the frames with the right startbyte.
	 * Returns false if one of the parameters is null or if the Frame was a duplicate or the ID was not recognized. Returns true otherwise.
	 * @param holder the Information that the Frame should be filled with. for a detailed description, look into the documentation of the Frame the ID belongs to.
	 * @param purpose The ID that corresponds to the Frame that should be created and added. Can be found in the Song class.
	 * @return true if the id was correct and the frame was no duplicate and thus added to the frames, false otherwise.
	 */
	private boolean addFrame(MetaID purpose, InformationHolder holder) {
		if(holder==null) {
			System.out.println("Error: Information holder was null.");
			return false;
		}
		if(purpose==null) {
			System.out.println("Error: purpose was null.");
			return false;
		}
		Frame f=null;
		long startbyte=frames.get(frames.size()-1).getStartbyte()+frames.get(frames.size()-1).getFramesize()+10;
		MetaID id= purpose;
		switch(id) {
			case ALBUM:
				f=new FrameTALB(startbyte, holder);
				break;
			case ARTISTS:
				f=new FrameTPE1(startbyte, holder);
				break;
			case BPM:
				f=new FrameTBPM(startbyte, holder);
				break;
			case COMMENT:
				f=new FrameCOMM(startbyte, holder);
				break;
			case CONDUCTOR:
				f=new FrameTPE3(startbyte, holder);
				break;
			case CONTENT_GROUP:
				f=new FrameTIT1(startbyte, holder);
				break;
			case COPYRIGHT:
				f=new FrameTCOP(startbyte, holder);
				break;
			case DATE:
				f=new FrameTDAT(startbyte, holder);
				break;
			case ENCODER:
				f=new FrameTENC(startbyte, holder);
				break;
			case GENRES:
				f=new FrameTCON(startbyte, holder);
				break;
			case IMAGE:
				f=new FrameAPIC(startbyte, holder);
				break;
			case PERFORMER_INFO:
				f=new FrameTPE2(startbyte, holder);
				break;
			case POSITION:
				f=new FramePOSS(startbyte, holder);
				break;
			case SUBTITLE:
				f=new FrameTIT3(startbyte, holder);
				break;
			case TITLE:
				f=new FrameTIT2(startbyte, holder);
				break;
			case TRACK_NUMBER:
				f=new FrameTRCK(startbyte, holder);
				break;
			case YEAR:
				f=new FrameTYER(startbyte, holder);
				break;
			
			default:
				System.out.println("ID "+purpose+" was not recognized. Maybe the implementation is still missing?");
				return false;
		}
		if(f.isValid()) {
			if (!frames.contains(f)) {
				frames.add(f);
				return true;
			}
			else
				System.out.println("The Frame "+f.getId()+" was a duplicate.");
		}
		else
			System.out.println("Frame "+f.getId()+" was invalid.");
		return false;
	}
	
	InformationHolder getAllData() {
		InformationHolder holder=new InformationHolder();
		for(Frame f: frames) {
			if(f.getData()!=null) {
				holder.join(f.getData());
			}
		}
		return holder;
	}
	
	boolean isMultiFrame(String purpose) {
		MetaID id=MetaID.valueOf(purpose);
		switch (id){
			case IMAGE:
			case COMMENT:
				return true;
			default:
				return false;
		}
	}
	boolean isListFrame(String purpose){
		MetaID id=MetaID.valueOf(purpose);
		switch (id){
			case ARTISTS:
			case GENRES:
				return true;
			default:
				return false;
		}
	}
}
