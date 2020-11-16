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

class FrameTYER extends Frame {
	private byte encoding;
	private String year;
	
	FrameTYER(int framesize, long startbyte, byte[] flags, File file) {
		super("TYER", framesize, startbyte, flags, file);
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
			year= String.valueOf(buffer);
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
			year=new String(cleanUCS_2(year.getBytes()));
		}
		else {
			if(year.charAt(year.length()-1)==0)
				year=year.substring(0, year.length()-1);
		}
	}
	
	FrameTYER(long startbyte, InformationHolder data) {
		super("TYER", startbyte, data);
		encoding=0;
		year=null;
		setData(data);
		setValidity();
	}
	
	@Override
	public InformationHolder getData() {
		return new InformationHolder()
				       .with(MetaID.YEAR.toString(), new InformationHolder()
						                                     .with(getIdentifier(), new InformationHolder()
								                                                            .with(MetaID.YEAR.toString(), year)
								                                                            .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	protected byte[] getBytes() {
		byte[] b = new byte[1+year.length()];
		b[0]=encoding;
		for(int i=1;i<b.length;i++) {
			b[i]=year.getBytes()[i-1];
		}
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
				case YEAR:
					if(String.class.isAssignableFrom(info.getInnerClass())) {
						try {
							Integer.parseInt(info.getString());
							year = info.getString();
							sizeChanged=true;
						} catch (NumberFormatException nfe) {
							System.out.println("Error: Could not set year: not a Number");
							nfe.printStackTrace();
						}
					}
					else if(Integer.class.isAssignableFrom(info.getInnerClass())) {
						year=""+info.getInt();
						sizeChanged=true;
					}
					else if(Byte.class.isAssignableFrom(info.getInnerClass())) {
						year=""+info.getByte();
						sizeChanged=true;
					}
					else if(Long.class.isAssignableFrom(info.getInnerClass())) {
						year=""+info.getLong();
						sizeChanged=true;
					}
					else ret=false;
					break;
				case SUB_ENCODING:
					if (Byte.class.isAssignableFrom(info.getInnerClass())) {
						if (info.getByte() < 0 || info.getByte() >= encodings.length)
							ret = false;
						else
							encoding = info.getByte();
					} else if(String.class.isAssignableFrom(info.getInnerClass())) {
						ArrayList<String> list = new ArrayList<>(Arrays.asList(encodings));
						if (list.indexOf(info.getString())==-1)
							ret=false;
						else
							this.encoding = (byte) list.indexOf(info.getString());
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
		return new String[]{"FrameID: "+getId(), "Encoding: "+encodings[encoding], "Year: "+year};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(year==null)
			invalidate();
		else {
			try {
				Integer.parseInt(year);
			} catch (NumberFormatException nfe) {
				invalidate();
			}
		}
	}
	
	@Override
	String getIdentifier() {
		return MetaID.YEAR.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.YEAR;
	}
}
