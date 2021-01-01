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

public class FrameCOMM extends Frame {
	private byte encoding;
	private Comment comment;
	
	FrameCOMM(int framesize, long startbyte, byte[] flags, File file) {
		super("COMM", framesize, startbyte, flags, file);
		RandomAccessFile raf=null;
		BufferedReader br=null;
		String comment="";
		String content_descr="";
		LanguageCode language = null;
		String language_code = null;
		try {
			
			raf=new RandomAccessFile(file,"r");
			raf.seek(startbyte);
			
			encoding=raf.readByte();
			if(encoding==1) //UTF-16LE funktioniert nicht als Ersatz für UCS-2, daher wird das ganze einfach von Hand gemacht indem UTF-8 gelesen wird und dann die 0 Bytes raus geworfen werden.
				br=new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("ISO-8859-1")));
			else
				br=new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName(encodings[encoding])));
			
			br.skip(startbyte + 1);
			CharBuffer buffer=CharBuffer.allocate(3);
			int read_bytes = br.read(buffer);
			if(read_bytes == 0)
				System.err.println("No Bytes were read from the input! Unknown error");
			else if(read_bytes == -1)
				System.err.println("Error: End of File was reached while reading.");
			buffer.position(0);

			language_code = String.valueOf(buffer);
			try {
				language = LanguageCode.valueOf(language_code);    //Language is not encoded. Everything after it is.
			} catch (IllegalArgumentException e) {
				System.out.println("Frame COMM: Language not known: " + language_code);
				language = LanguageCode.oth;
			}
			buffer.clear();
			
			buffer=CharBuffer.allocate(framesize-4);
			read_bytes = br.read(buffer);
			if(read_bytes == 0)
				System.err.println("No Bytes were read from the input! Unknown error");
			else if(read_bytes == -1)
				System.err.println("Error: End of File was reached while reading.");
			buffer.position(0);
			int b0=0;
			for(int i=0;i<buffer.length();i++){
				if(buffer.charAt(i)==0){
					b0=i;
					break;
				}
			}
			String s=String.valueOf(buffer);
			content_descr=s.substring(0, b0);
			if(s.length() > b0+1) {
				if (s.charAt(b0 + 1) == 0)
					comment = s.substring(b0 + 2);
				else
					comment = s.substring(b0 + 1);
			}
			else
				comment="";
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
			content_descr=new String(cleanUCS_2(content_descr.getBytes()));
			comment=new String(cleanUCS_2(comment.getBytes()));
		}
		else {
			if(content_descr.length()>0) {
				if (content_descr.charAt(content_descr.length() - 1) == 0) {
					content_descr = content_descr.substring(0, content_descr.length() - 1);
				}
			}
			if(comment.length()>0) {
				if (comment.charAt(comment.length() - 1) == 0)
					comment = comment.substring(0, comment.length() - 1);
			}
		}
		this.comment=new Comment(comment, content_descr, language);
	}
	
	FrameCOMM(long startbyte, InformationHolder data) {
		super("COMM", startbyte, data);
		encoding=0;
		comment=new Comment();
		setData(data);
		setValidity();
	}
	
	@Override
	public InformationHolder getData() {
		InformationHolder holder=new InformationHolder()
				                         .with(getPurpose().toString(), new InformationHolder()
						                                                        .with(getIdentifier(), new InformationHolder()
								                                                                               .with(MetaID.COMMENT.toString(), comment)
								                                                                               .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
		return holder;
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		if(isValid())
			raf.write(getBytes());
		else
			return false;
		return true;
	}
	
	@Override
	protected byte[] getBytes() {
		String content_descr=this.comment.getContent_description();
		String comment=this.comment.getComment();
		byte[] language=this.comment.getLanguage().getValue().getBytes();
		byte[] ret=new byte[1+3+content_descr.getBytes().length+1+comment.getBytes().length]; //1 Byte Encoding, 3 Bytes language, content descr. + 1 zero Byte and the comment itself
		ret[0]=encoding;
		ret[1]=language[0];
		ret[2]=language[1];
		ret[3]=language[2];
		int cnt=4;
		for(int i=0;i<content_descr.getBytes().length;i++,cnt++) {
			ret[cnt]=content_descr.getBytes()[i];
		}
		ret[cnt]=0;
		cnt++;
		for(int i=0;i<comment.getBytes().length;i++,cnt++){
			ret[cnt]=comment.getBytes()[i];
		}
		return ret;
	}
	
	@Override
	public boolean setData(InformationHolder data) {
		boolean ret=true;
		boolean sizeChanged=false;
		data.setPosition(0);
		MetaID id;
		for(InformationHolder.Information info : data) {
			id= MetaID.valueOf(info.getKey());
			switch(id) {
				case COMMENT:
					if(String.class.isAssignableFrom(info.getInnerClass())) {
						comment.setComment(info.getString());
						sizeChanged=true;
					}
					else if(Comment.class.isAssignableFrom(info.getInnerClass())){
						comment=(Comment) info.getObject();
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
				case SUB_CONTENT_DESCRIPTION:
					if(String.class.isAssignableFrom(info.getInnerClass())) {
						comment.setContent_description(info.getString());
						sizeChanged=true;
					}
					else ret=false;
					break;
				case SUB_LANGUAGE:
					if(LanguageCode.class.isAssignableFrom(info.getInnerClass())) {
						comment.setLanguage((LanguageCode) info.getObject());
					}
					else if(String.class.isAssignableFrom(info.getInnerClass())){
						LanguageCode language;
						try{
							language= LanguageCode.valueOf(info.getString());
						} catch(IllegalArgumentException e) {
							language = LanguageCode.getLanguageCodeByValue(info.getString());
							if(language==null)
								ret=false;
						}
						if(language!=null)
							comment.setLanguage(language);
					}
					else
						ret=false;
					break;
			}
		}
		if(sizeChanged){
			recalculateFrameSize();
			setValidity();
		}
		return ret;
	}
	
	@Override
	public boolean removeData(InformationHolder data) {
		invalidate();
		return true;
	}
	
	@Override
	public boolean addData(InformationHolder data) {
		return false;
	}
	
	@Override
	protected String[] getFrameString() {
		return new String[]{"Frame ID: "+getId(), "Encoding: "+encodings[encoding], "Language: "+comment.getLanguage().getValue(), "Content description: "+comment.getContent_description(), "Comment: "+comment.getComment()};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		if(comment.getLanguage()==null)
			invalidate();
		else if(comment.getComment()==null)
			invalidate();
		else if(encoding>=encodings.length || encoding<0)
			invalidate();
	}
	
	@Override
	String getIdentifier() {
		return comment.getIdentifier();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.COMMENT;
	}
}
