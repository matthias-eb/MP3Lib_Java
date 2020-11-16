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
import java.util.Date;

/**
 * The 'Date' frame is a numeric string in the DDMM format containing the date for the recording. This field is always four characters long.
 */
class FrameTDAT extends Frame {
	
	private byte encoding;
	private byte month;
	private byte day;
	
	FrameTDAT(int framesize, long startbyte, byte[] flags, File file) {
		super("TDAT", framesize, startbyte, flags, file);
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
			String date = String.valueOf(buffer);
			buffer.clear();
			if(encoding==1){
				date=new String(cleanUCS_2(date.getBytes()));
			}
			
			try{
				day=Byte.parseByte(date.substring(0, 2));
				month=Byte.parseByte(date.substring(2));
			} catch(NumberFormatException nfe) {
				nfe.printStackTrace();
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
	
	FrameTDAT(long startbyte, InformationHolder data) {
		super("TDAT", startbyte, data);
		encoding=0;
		day=0;
		month=0;
		setData(data);
		setValidity();
	}
	
	@Override
	InformationHolder getData() {
		return new InformationHolder()
				       .with(MetaID.DATE.toString(), new InformationHolder()
						                                     .with(getIdentifier(), new InformationHolder()
								                                                            .with(MetaID.DATE.toString(), day+"."+month+".")
								                                                            .with(MetaID.SUB_DAY.toString(), day)
								                                                            .with(MetaID.SUB_MONTH.toString(), month)
								                                                            .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
	}
	
	@Override
	boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	byte[] getBytes() {
		byte b[] = new byte[5];
		b[0]=encoding;
		String d=String.valueOf(day);
		String m=String.valueOf(month);
		if(d.length()==1)
			d="0"+d;
		if(m.length()==1){
			m="0"+m;
		}
		byte bytes[]= (d + m).getBytes();
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
				case DATE:
					if(Date.class.isAssignableFrom(info.getInnerClass())) {
						this.day= (byte) ((Date)info.getObject()).getDate();
						this.month=(byte) ((Date)info.getObject()).getMonth();
					}
					else if(String.class.isAssignableFrom(info.getInnerClass())){
						try {
							String[] dt = info.getString().split(".");
							this.day = Byte.parseByte(dt[0]);
							this.month = Byte.parseByte(dt[1]);
						} catch(NumberFormatException | ArrayIndexOutOfBoundsException e){
							e.printStackTrace();
							ret=false;
						}
					}
					else ret=false;
					break;
				case SUB_DAY:
					if(Byte.class.isAssignableFrom(info.getInnerClass())) {
						if (info.getByte() <= 31 && info.getByte() > 0) {
							day = info.getByte();
							sizeChanged=true;
						}
						else ret=false;
					}
					else if(Integer.class.isAssignableFrom(info.getInnerClass())) {
						if(info.getInt()<=31 && info.getInt() > 0) {
							day = (byte) info.getInt();
							sizeChanged = true;
						}
						else ret=false;
					}
					else if(Long.class.isAssignableFrom(info.getInnerClass())) {
						if(info.getLong()<=31 && info.getLong() > 0) {
							day = (byte) info.getLong();
							sizeChanged = true;
						}
						else ret=false;
					}
					else ret=false;
					break;
				case SUB_MONTH:
					if(Byte.class.isAssignableFrom(info.getInnerClass())) {
						if(info.getByte()<=12 && info.getByte() > 0) {
							month = info.getByte();
							sizeChanged=true;
						}
						else ret=false;
					}
					else if(Integer.class.isAssignableFrom(info.getInnerClass())) {
						if(info.getInt()<=12 && info.getInt() > 0) {
							month = (byte) info.getInt();
							sizeChanged = true;
						}
						else ret=false;
					}
					else if(Long.class.isAssignableFrom(info.getInnerClass())) {
						if(info.getLong()<=12 && info.getLong() > 0) {
							month = (byte) info.getLong();
							sizeChanged = true;
						}
						else ret=false;
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
		return new String[]{"FrameID: "+getId(), "Encoding: "+encodings[encoding], "Date: "+day+"."+month+"."};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(day>31 || day < 1)
			invalidate();
		else if(month>12 || month<1)
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.DATE.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.DATE;
	}
}
