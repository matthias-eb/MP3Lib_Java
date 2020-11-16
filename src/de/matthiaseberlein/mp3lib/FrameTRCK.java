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

class FrameTRCK extends Frame {
	private byte encoding;
	private int tracknumber;
	/**
	 * Can be either -1 or any number above the Track number, it being above zero.
	 */
	private int totalTracks;
	FrameTRCK(int framesize, long startbyte, byte[] flags, File file) {
		super("TRCK", framesize, startbyte, flags, file);
		RandomAccessFile raf=null;
		BufferedReader br=null;
		String s;
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
			s = String.valueOf(buffer);
			buffer.clear();
			
			if(encoding==1){
				s=new String(cleanUCS_2(s.getBytes()));
			}
			else {
				if(s.charAt(s.length()-1)==0)
					s=s.substring(0, s.length()-1);
			}
			
			try {
				if (s.contains("/")) {
					String s2[] = s.split("/");
					tracknumber = Integer.parseInt(s2[0]);
					totalTracks = Integer.parseInt(s2[1]);
				} else {
					tracknumber = Integer.parseInt(s);
					totalTracks = -1;
				}
			} catch(NumberFormatException nfe){
				tracknumber=-1;
				totalTracks=-1;
				setValidity();
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
	
	FrameTRCK(long startbyte, InformationHolder data) {
		super("TRCK", startbyte, data);
		encoding=0;
		tracknumber=-1;
		totalTracks=-1;
		setData(data);
		setValidity();
	}
	
	@Override
	InformationHolder getData() {
		return new InformationHolder().with(MetaID.TRACK_NUMBER.toString(), new InformationHolder().with(getIdentifier(), new InformationHolder()
				                                                                                                                  .with(MetaID.TRACK_NUMBER.toString(), tracknumber)
				                                                                                                                  .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])
				                                                                                                                  .with(MetaID.SUB_TOTAL_TRACKS.toString(), totalTracks)));
	}
	
	@Override
	boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	byte[] getBytes() {
		String s;
		if(totalTracks!=-1)
			s=tracknumber+"/"+totalTracks;
		else
			s=""+tracknumber;
		byte ausg[] =new byte[1+s.getBytes().length];
		ausg[0]=encoding;
		System.arraycopy(s.getBytes(), 0, ausg, 1, s.getBytes().length);
		return ausg;
	}
	
	@Override
	boolean setData(InformationHolder data) {
		boolean ret=true;
		boolean sizeChanged=false;
		int tracknumber=this.tracknumber;
		int totalTracks=this.totalTracks;
		MetaID id;
		for(InformationHolder.Information info : data) {
			id= MetaID.valueOf(info.getKey());
			switch(id) {
				case TRACK_NUMBER:
					if (Integer.class.isAssignableFrom(info.getInnerClass())) {
						tracknumber = info.getInt();
						sizeChanged = true;
					} else if (Byte.class.isAssignableFrom(info.getInnerClass())) {
						tracknumber = info.getByte();
						sizeChanged = true;
					}
					else ret=false;
					break;
				case SUB_TOTAL_TRACKS:
					if(Integer.class.isAssignableFrom(info.getInnerClass())) {
						totalTracks = info.getInt();
						sizeChanged=true;
					}
					else if(Byte.class.isAssignableFrom(info.getInnerClass())) {
						totalTracks = info.getByte();
						sizeChanged=true;
					}
					else
						ret=false;
					break;
				case SUB_ENCODING:
					if(Byte.class.isAssignableFrom(info.getInnerClass())) {
						if (info.getByte() < 0 || info.getByte() >= encodings.length)
							ret = false;
						else
							encoding = info.getByte();
					}
					else if(String.class.isAssignableFrom(info.getInnerClass())) {
						ArrayList<String> list = new ArrayList<>(Arrays.asList(encodings));
						if (list.indexOf(info.getString())==-1)
							ret=false;
						else
							this.encoding = (byte) list.indexOf(info.getString());
					}
					else ret=false;
					break;
			}
		}
		if(totalTracks<tracknumber && totalTracks!=-1)
			return false;
		else {
			this.tracknumber = tracknumber;
			this.totalTracks=totalTracks;
			if(sizeChanged) {
				recalculateFrameSize();
				setValidity();
			}
			return ret;
		}
		
		
	}
	
	@Override
	boolean removeData(InformationHolder data) {
		return false;
	}
	
	@Override
	boolean addData(InformationHolder data) {
		return false;
	}
	
	@Override
	String[] getFrameString() {
		if(totalTracks!=-1)
			return new String[] {"Frame ID: "+getId(), "Encoding: "+encodings[encoding], "Tracknumber: "+tracknumber, "Total Tracks: "+totalTracks};
		return new String[] {"Frame ID: "+getId(), "Encoding: "+encodings[encoding], "Tracknumber: "+tracknumber};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(tracknumber<=0)
			invalidate();
		else if(totalTracks!=-1 && totalTracks<tracknumber)
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.TRACK_NUMBER.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.TRACK_NUMBER;
	}
}
