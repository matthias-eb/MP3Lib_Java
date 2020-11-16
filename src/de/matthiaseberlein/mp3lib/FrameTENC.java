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

public class FrameTENC extends Frame {
	private byte encoding;
	private String encoder;
	FrameTENC(int framesize, long startbyte, byte[] flags, File file) {
		super("TENC", framesize, startbyte, flags, file);
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
			encoder = String.valueOf(buffer);
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
			encoder =new String(cleanUCS_2(encoder.getBytes()));
		}
		else {
			if(encoder.charAt(encoder.length()-1)==0)
				encoder = encoder.substring(0, encoder.length()-1);
		}
	}
	
	FrameTENC(long startbyte, InformationHolder data) {
		super("TENC", startbyte, data);
		encoding=0;
		encoder=null;
		setData(data);
		setValidity();
	}
	
	@Override
	public InformationHolder getData() {
		return new InformationHolder()
					   .with(MetaID.ENCODER.toString(), new InformationHolder()
							                                         .with(getIdentifier(), new InformationHolder()
									                                                                .with(MetaID.ENCODER.toString(), encoder)
									                                                                .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	protected byte[] getBytes() {
		byte[] b=new byte[encoder.length()+1];
		b[0]=encoding;
		System.arraycopy(encoder.getBytes(), 0, b, 0, encoder.getBytes().length);
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
				case ENCODER:
					if(String.class.isAssignableFrom(info.getInnerClass())) {
						encoder = info.getString();
						sizeChanged = true;
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
		return new String[]{"FrameID: "+getId(), "Encoding: "+encodings[encoding], "Encoder: "+ encoder};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(encoder==null)
			invalidate();
		else if(encoder.length()==0)
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.ENCODER.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.ENCODER;
	}
}
