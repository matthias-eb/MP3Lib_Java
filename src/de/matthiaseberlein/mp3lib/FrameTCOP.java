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
 * The 'Copyright message' frame, which must begin with a year and a space character (making five characters), is intended for the copyright holder of the original sound,
 * not the audio file itself. The absence of this frame means only that the copyright information is unavailable or has been removed, and must not be interpreted to mean that the sound is public domain.
 * Every time this field is displayed the field must be preceded with "Copyright © "
 */
class FrameTCOP extends Frame {
	private byte encoding;
	private String copyrigthMsg;
	
	FrameTCOP(int framesize, long startbyte, byte[] flags, File file) {
		super("TCOP", framesize, startbyte, flags, file);
		RandomAccessFile raf=null;
		BufferedReader br=null;
		try {
			
			raf=new RandomAccessFile(file,"r");
			raf.seek(startbyte);
			
			encoding=raf.readByte();
			if(encoding==1) //UTF-16LE funktioniert nicht als Ersatz für UCS-2, daher wird das ganze einfach von Hand gemacht indem ISO 8859-1 gelesen wird und dann die 0 Bytes raus geworfen werden.
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
			copyrigthMsg = String.valueOf(buffer);
			buffer.clear();
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				assert raf != null;
				raf.close();
				assert br != null;
				br.close();
			} catch (IOException | NullPointerException e) {
				e.printStackTrace();
			}
		}
		if(encoding==1){
			copyrigthMsg=new String(cleanUCS_2(copyrigthMsg.getBytes()));
		}
		else {
			if(copyrigthMsg.charAt(copyrigthMsg.length()-1)==0)
				copyrigthMsg=copyrigthMsg.substring(0, copyrigthMsg.length()-1);
		}
	}
	
	FrameTCOP(long startbyte, InformationHolder data) {
		super("TCOP", startbyte, data);
		encoding=0;
		copyrigthMsg=null;
		setData(data);
		setValidity();
	}
	
	@Override
	InformationHolder getData() {
		return new InformationHolder()
					   .with(MetaID.COPYRIGHT.toString(), new InformationHolder()
							                                      .with(getIdentifier(), new InformationHolder()
									                                                             .with(MetaID.COPYRIGHT.toString(), copyrigthMsg)
									                                                             .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
	}
	
	@Override
	boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	byte[] getBytes() {
		byte b[]=new byte[copyrigthMsg.length()+1];
		int cnt=0;
		b[cnt]=encoding;
		cnt++;
		for(int i=0;i<copyrigthMsg.getBytes().length;i++,cnt++){
			b[cnt]=copyrigthMsg.getBytes()[i];
		}
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
				case COPYRIGHT:
					if(String.class.isAssignableFrom(info.getInnerClass())) {
						copyrigthMsg = info.getString();
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
		return new String[]{"Frame Id: "+getId(), "Encoding: "+encodings[encoding], "Copyright © "+copyrigthMsg};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(copyrigthMsg==null)
			invalidate();
		else if(copyrigthMsg.length()==0)
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.COPYRIGHT.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.COPYRIGHT;
	}
}
