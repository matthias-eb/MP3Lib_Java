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
 * This Class is able to read the APIC Frame and provides access to its saved Picture. the Encoding, MimeType, PictureType and description are all changeable with the Image itself.
 * Like all Frame classes, this class can write the Frame back to the mp3 File and recalculate its size if needed.
 */
public class FrameAPIC extends Frame {

	private static final boolean using_Android=false;

	private byte encoding;
	private Image image;
	private String description;
	
	FrameAPIC(int framesize, long startbyte, byte[] flags, File file) {
		super("APIC", framesize, startbyte, flags, file);
		RandomAccessFile raf = null;
		BufferedReader br=null;
		byte[] b = null;
		try {
			raf = new RandomAccessFile(file, "r");
			raf.seek(startbyte);
			
			byte enc = raf.readByte();
			encoding = enc;
			
			if(encoding==1) //UTF-16LE funktioniert nicht als Ersatz für UCS-2, daher wird das ganze einfach von Hand gemacht indem ISO-8859-1 gelesen wird und dann der Schrott raus geworfen wird.
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
			
			String s = String.valueOf(buffer);
			buffer.position(0);
			
			int index_0 = s.indexOf(0);
			String mime_type = s.substring(0, index_0);
			if (mime_type.length() == 0) {
				mime_type = "image/";
			}
			byte picture_type = s.getBytes()[index_0 + 1];
			s=s.substring(index_0+2);
			index_0=0;
			String description;
			if(encoding==1){
				do{
					index_0=s.indexOf(0, index_0+1);
				} while(s.charAt(index_0+1)!=0);
				description=s.substring(0, index_0);
				description=new String(cleanUCS_2(description.getBytes()));
			}
			else {
				index_0=s.indexOf(0, index_0);
				description=s.substring(0, index_0);
			}
			this.description = description;
			
			//Remove the leading zero(s)
			int cnt=0;
			s=s.substring(index_0+1);
			while(s.charAt(0)==0 && cnt<2) {
				s = s.substring(1);
				cnt++;
			}

			image = new Image(mime_type, picture_type, s.getBytes());
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				raf.close();
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	FrameAPIC(long startbyte, InformationHolder data){
		super("APIC", startbyte, data);
		encoding=0;
		image=null;
		setData(data);
		setValidity();
	}
	
	@Override
	InformationHolder getData() {
		InformationHolder holder=new InformationHolder()
				                         .with(getPurpose().toString(), new InformationHolder()
						                                                        .with(getIdentifier(), new InformationHolder()
								                                                                               .with(MetaID.IMAGE.toString(), image)
								                                                                               .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
		return holder;
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		setValidity();
		if(isValid())
			raf.write(getBytes());
		else
			return false;
		return true;
	}
	
	@Override
	protected byte[] getBytes() {
		int pos=0;
		byte imagebytes[] = image.getImage_bytes();
		String mime_type_output="image/"+image.getMime_type().toLowerCase();
		//1 Byte encoding + mimetype Bytes + 1 zero Byte + 1 Picture Type Byte + Description + 1 or 2 zero Bytes (up to programmer) + imagebytes
		byte ausg[] = new byte[1+mime_type_output.getBytes().length+1+1+description.getBytes().length+1+imagebytes.length];
		ausg[pos]=encoding;
		pos++;
		System.arraycopy(mime_type_output.getBytes(), 0, ausg, pos, mime_type_output.getBytes().length);
		pos+=mime_type_output.getBytes().length;
		ausg[pos]=0;
		pos++;
		ausg[pos]=image.getPicture_typeAsByte();
		pos++;
		System.arraycopy(description.getBytes(), 0, ausg, pos, description.getBytes().length);
		pos+=description.getBytes().length;
		ausg[pos]=0;
		pos++;
		System.arraycopy(imagebytes, 0, ausg, pos, imagebytes.length);
		return ausg;
	}
	
	@Override
	public boolean setData(InformationHolder data) {
		boolean ret=true;
		boolean sizeChanged=false;
		MetaID id;
		data.setPosition(0);
		for(InformationHolder.Information info : data){
			id= MetaID.valueOf(info.getKey());
			switch(id) {
				case SUB_ENCODING:
					if(Byte.class.isAssignableFrom(info.getInnerClass())) {
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
					} else
						ret=false;
					break;
				case IMAGE:
					try {
						image = (Image) info.getObject();
						sizeChanged=true;
					} catch(ClassCastException ex){
						ret=false;
					}
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
		return new String[]{"Frame ID: "+getId(), "Text Encoding: "+encodings[encoding], "MIME Type: "+image.getMime_type(), "Picture Type: "+image.getPicture_type(), "Picture Description: "+description};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		else if(!image.isValid()) {
			invalidate();
		}
		// The description has a maximum length of 64 characters, but may be empty.
		else if(description.length() > 64)
			invalidate();
		else if(encoding>encodings.length || encoding<0) {
			invalidate();
		}
		else if(image==null) {
			invalidate();
		}
	}
	
	@Override
	String getIdentifier() {
		try {
			return image.getIdentifier() + description.hashCode();
		} catch(NullPointerException | IndexOutOfBoundsException e) {
			if(description!=null)
				return description.hashCode()+"";
			return image.getImage_bytes().hashCode()+"";
		}
	}
	
	boolean contentDescriptorEquals(FrameAPIC apic){
		return apic.image.equals(image);
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.IMAGE;
	}
	
	Image getImage() {
		return image;
	}
}
