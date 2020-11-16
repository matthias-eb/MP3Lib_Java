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
 * This class shows the music Group to which this content belongs, for example "Piano Concerto" or "Life concert", or as the Documentation sais:
 * The 'Content group description' frame is used if the sound belongs to a larger category of sounds/music. For example, classical music is often sorted in different musical sections (e.g. "Piano Concerto", "Weather - Hurricane").
 */
public class FrameTIT1 extends Frame {
	private byte encoding;
	private String contentgroup;
	
	FrameTIT1(int framesize, long startbyte, byte[] flags, File file) {
		super("TIT1", framesize, startbyte, flags, file);
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
			contentgroup = String.valueOf(buffer);
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
			contentgroup=new String(cleanUCS_2(contentgroup.getBytes()));
		}
		else{
			if(contentgroup.charAt(contentgroup.length()-1)==0)
				contentgroup=contentgroup.substring(0, contentgroup.length()-1);
		}
	}
	
	FrameTIT1(long startbyte, InformationHolder data) {
		super("TIT1", startbyte, data);
		contentgroup=null;
		encoding=0;
		setData(data);
		setValidity();
	}
	
	@Override
	public InformationHolder getData() {
		return new InformationHolder()
				       .with(MetaID.CONTENT_GROUP.toString(), new InformationHolder()
						                                              .with(getIdentifier(), new InformationHolder()
								                                                                     .with(MetaID.CONTENT_GROUP.toString(), contentgroup)
								                                                                     .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	protected byte[] getBytes() {
		byte b[]=new byte[1+contentgroup.getBytes().length];
		b[0]=encoding;
		System.arraycopy(contentgroup.getBytes(), 0, b, 0, contentgroup.getBytes().length);
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
				case CONTENT_GROUP:
					if(String.class.isAssignableFrom(info.getInnerClass())) {
						contentgroup = info.getString();
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
		return new String[]{"Frame ID: "+getId(), "Encoding: "+encodings[encoding], "Content Group: "+ contentgroup};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(contentgroup==null)
			invalidate();
		else if(contentgroup.length()==0)
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.CONTENT_GROUP.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.CONTENT_GROUP;
	}
}
