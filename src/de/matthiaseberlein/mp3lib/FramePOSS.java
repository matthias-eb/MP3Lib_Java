package de.matthiaseberlein.mp3lib;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;



/**
 * This Frame defines the Position that the Song is currently stopped at from the Music player. It  does so by using either the MPEG Fame number or the time in milliseconds.
 * The timestampformat defines wether the position is milliseconds or MPEG Frames and the position then is the corresponding number.
 */
public class FramePOSS extends Frame {
	byte timestampformat;
	static final String timestampformats[] = {"", "Absolute time, 32 bit sized, using MPEG frames as unit", "Absolute time, 32 bit sized, using milliseconds as unit"};
	static final String tsunits[] = {"", "MPEG frames", "Milliseconds"};
	int position;
	
	FramePOSS(int framesize, long startbyte, byte[] flags, File file) {
		super("POSS", framesize, startbyte, flags, file);
		RandomAccessFile raf=null;
		try {
			raf=new RandomAccessFile(file, "r");
			raf.seek(startbyte);
			timestampformat=raf.readByte();
			
			//read the actual Position. Is a 32 bit Integer. If there is a bigger sized Datatype in an upcoming ID3 Version, this needs to be changed to an if/else construct.
			position=raf.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	FramePOSS(long startbyte, InformationHolder data) {
		super("POSS", startbyte, data);
		position=-1;
		timestampformat=-1;
		setData(data);
		setValidity();
	}
	
	@Override
	public InformationHolder getData() {
		return new InformationHolder()
				       .with(MetaID.POSITION.toString(), new InformationHolder()
				                                                     .with(getIdentifier(), new InformationHolder()
						                                                                            .with(MetaID.POSITION.toString(), position)
						                                                                            .with(MetaID.SUB_TIMESTAMP_FORMAT.toString(), tsunits[timestampformat])));
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		if(isValid())
			raf.write(getBytes());
		else
			return false;
		return true;
	}
	
	/**
	 * Returns the bytes in the form that they are gonna be written in the File. The position is thus saved as individual bytes and gets shifted to the right position to place the value.
	 * @return a byte array ready to be written in a file. The length of this byte array can be used to determine the frame size. It does not necessarily match it since tha data can be altered.
	 */
	@Override
	protected byte[] getBytes() {
		byte ausg[] = new byte[(Integer.SIZE/8)+1];
		ausg[0]=timestampformat;
		int cnt=Integer.SIZE;
		for(int i=1;i<ausg.length;i++) {
			ausg[i] = (byte)((position>>cnt)&0xff);
			cnt-=8;
		}
		return ausg;
	}
	
	/**
	 * Sets one or multiple Information that the Frame saves. Data must be saved with the right key to be recognized as data for this frame.
	 * The class then checks the Data for Validity and, if valid, assigns the data to the correct attribute
	 * @param data an InformationHolder containing every datapart to be set to a new Value in this Frame. In this Frame only "Timestampformat" and "Position" can be changed.
	 * @return true if every assignment worked, false otherwise
	 */
	@Override
	public boolean setData(InformationHolder data) {
		boolean ret=true;
		MetaID id;
		for(InformationHolder.Information info: data) {
			id= MetaID.valueOf(info.getKey());
			switch(id) {
				case SUB_TIMESTAMP_FORMAT:
					if (Byte.class.isAssignableFrom(info.getInnerClass())) {
						if (info.getByte() < 0 || info.getByte() >= encodings.length)
							ret = false;
						else {
							timestampformat = info.getByte();
						}
					} else if(String.class.isAssignableFrom(info.getInnerClass())) {
						ArrayList<String> list = new ArrayList<>(Arrays.asList(timestampformats));
						if (list.indexOf(info.getString())==-1)
							ret=false;
						else {
							this.timestampformat = (byte) list.indexOf(info.getString());
						}
					}
					else
						ret=false;
					break;
				case POSITION:
					if(!Integer.class.isAssignableFrom(info.getInnerClass()))
						ret=false;
					else {
						if (info.getInt() >= 0) {
							position = info.getInt();
						}
						else
							ret = false;
					}
					break;
			}
		}
		setValidity();
		return ret;
	}
	
	@Override
	public boolean removeData(InformationHolder data) {
		return false;
	}
	
	@Override
	public boolean addData(InformationHolder data) {
		return false;
	}
	
	@Override
	protected String[] getFrameString() {
		return new String[]{"FrameID: "+getId(), "Timestampformat: "+timestampformats[timestampformat], "Position: "+position+" "+tsunits[timestampformat]};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		if(timestampformat!=1 && timestampformat!=2)
			invalidate();
		if(position<0)
			invalidate();
		else
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.POSITION.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.POSITION;
	}
}
