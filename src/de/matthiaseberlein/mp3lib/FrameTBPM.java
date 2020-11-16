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
 * The 'BPM' frame contains the number of beats per minute in the mainpart of the audio. The BPM is an integer and represented as a numerical string.
 */
class FrameTBPM extends Frame {
	private byte encoding;
	private int bpm;
	
	FrameTBPM(int framesize, long startbyte, byte[] flags, File file) {
		super("TBPM", framesize, startbyte, flags, file);
		RandomAccessFile raf=null;
		BufferedReader br=null;
		String s=null;
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
		if(encoding==1){
			s=new String(cleanUCS_2(s.getBytes()));
		}
		else {
			if(s.charAt(s.length()-1)==0)
				s=s.substring(0, s.length()-1);
		}
		
		//Convert the read Numerical String to int
		try {
			bpm = Integer.parseInt(s);
		} catch(NumberFormatException nfe){
			nfe.printStackTrace();
		}
	}
	
	FrameTBPM(long startbyte, InformationHolder data) {
		super("TBPM", startbyte, data);
		encoding=0;
		bpm=-1;
		setData(data);
		setValidity();
	}
	
	@Override
	InformationHolder getData() {
		return new InformationHolder()
				       .with(MetaID.BPM.toString(), new InformationHolder()
						                                    .with(getIdentifier(), new InformationHolder()
								                                                           .with(MetaID.BPM.toString(), bpm)
								                                                           .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
	}
	
	@Override
	boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	byte[] getBytes() {
		byte b[] = new byte[1+String.valueOf(bpm).length()];
		b[0]=encoding;
		byte bytes[]=String.valueOf(bpm).getBytes();
		System.arraycopy(bytes, 0, b, 1, bytes.length);
		return b;
	}
	
	@Override
	boolean setData(InformationHolder data) {
		boolean ret=true;
		boolean sizeChanged=false;
		MetaID id;
		for(InformationHolder.Information info : data) {
			id= MetaID.valueOf(info.getKey());
			switch(id) {
				case BPM:
					if(info.getInt()>0) {
						bpm = info.getInt();
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
						ArrayList<String> list = new ArrayList<>(Arrays.asList(encodings));
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
	boolean removeData(InformationHolder data) {
		return false;
	}
	
	@Override
	boolean addData(InformationHolder data) {
		return false;
	}
	
	@Override
	String[] getFrameString() {
		return new String[]{"Frame ID: "+getId(), "Encoding: "+encodings[encoding], "Beats per Minute: "+bpm};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(bpm<=0)
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.BPM.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.BPM;
	}
}
