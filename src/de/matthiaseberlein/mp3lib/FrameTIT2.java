package de.matthiaseberlein.mp3lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The 'Title/Songname/Content description' frame is the actual name of the piece (e.g. "Adagio", "Hurricane Donna").
 */
public class FrameTIT2 extends Frame {
	
	private byte encoding;
	private String title;
	
	FrameTIT2(int framesize, long startbyte, byte[] flags, File file) {
		super("TIT2", framesize, startbyte, flags, file);
		RandomAccessFile raf=null;
		BufferedReader br=null;
		try {
			
			raf=new RandomAccessFile(file,"r");
			raf.seek(startbyte);
			
			encoding=raf.readByte();
			if(encoding==1) //UTF-16LE funktioniert nicht als Ersatz für UCS-2, daher wird statdessen UTF-8 gelesen und dann jedes nullbyte entfernt.
				br=new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1));
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
			title = String.valueOf(buffer);
			
			if(encoding==1) {
				System.out.println("UCS 2 Test: " + title);
			}
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
			title=new String(cleanUCS_2(title.getBytes()));
		}
		else {
			if(title.charAt(title.length()-1)==0)
				title=title.substring(0, title.length()-1);
		}
	}
	
	FrameTIT2(long startbyte, InformationHolder data) {
		super("TIT2", startbyte, data);
		encoding=0;
		title=null;
		setData(data);
		setValidity();
	}
	
	@Override
	public InformationHolder getData() {
		return new InformationHolder()
				       .with(MetaID.TITLE.toString(), new InformationHolder()
						                                      .with(getIdentifier(), new InformationHolder()
								                                                             .with(MetaID.TITLE.toString(), title)
								                                                             .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	protected byte[] getBytes() {
		byte b[]=new byte[1+title.getBytes().length];
		b[0]=encoding;
		byte bytes[]=title.getBytes();
		System.arraycopy(bytes, 0, b, 1, bytes.length);
		return b;
	}
	
	@Override
	public boolean setData(InformationHolder data) {
		boolean ret=true;
		boolean sizeChanged=false;
		MetaID id;
		for(InformationHolder.Information info : data) {
			id= MetaID.valueOf(info.getKey());
			switch(id) {
				case TITLE:
					if(String.class.isAssignableFrom(info.getInnerClass())) {
						title = info.getString();
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
	public boolean removeData(InformationHolder data) {
		return false;
	}
	
	@Override
	public boolean addData(InformationHolder data) {
		return false;
	}
	
	@Override
	protected String[] getFrameString() {
		return new String[]{"Frame ID: "+getId(), "Encoding: "+encodings[encoding], "Title: "+title};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(title==null)
			invalidate();
		else if(title.length()==0)
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.TITLE.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.TITLE;
	}
}
