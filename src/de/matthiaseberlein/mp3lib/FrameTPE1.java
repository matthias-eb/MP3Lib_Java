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
 * The 'Lead artist(s)/Lead performer(s)/Soloist(s)/Performing group' is used for the main artist(s). They are seperated with the "/" character.
 */
class FrameTPE1 extends Frame {
	
	private byte encoding;
	private ArrayList<String> artists;
	
	FrameTPE1(int framesize, long startbyte, byte[] flags, File file) {
		super("TPE1", framesize, startbyte, flags, file);
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
			String s = String.valueOf(buffer);
			if(encoding==1){
				s=new String(cleanUCS_2(s.getBytes()));
			}
			artists=new ArrayList<>();
			artists.addAll(Arrays.asList(s.split("/")));
			buffer.clear();
			if(encoding!=1) {
				for (int i = 0; i < artists.size(); i++) {
					if (artists.get(i).charAt(artists.get(i).length() - 1) == 0)
						artists.set(i, artists.get(i).substring(0, artists.get(i).length() - 1));
				}
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
	
	FrameTPE1(long startbyte, InformationHolder data) {
		super("TPE1", startbyte, data);
		encoding=0;
		artists=new ArrayList<>();
		setData(data);
		setValidity();
	}
	
	@Override
	public InformationHolder getData() {
		return new InformationHolder()
				       .with(MetaID.ARTISTS.toString(), new InformationHolder()
						                                        .with(getIdentifier(), new InformationHolder()
								                                                               .with(MetaID.ARTISTS.toString(), new ArrayList<String>(artists))
								                                                               .with(MetaID.SUB_ENCODING.toString(), encodings[encoding])));
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		return false;
	}
	
	@Override
	protected byte[] getBytes() {
		int artbytesize=0;
		if(artists.size()>0) {
			for (String s : artists) {
				artbytesize += s.getBytes().length + 1;
			}
			artbytesize--;
		}
		byte ret[]=new byte[1+artbytesize];
		int cnt=0;
		ret[cnt]=encoding;
		cnt++;
		for(String s: artists){
			for(int i=0; i<s.getBytes().length; i++,cnt++){
				ret[cnt]=s.getBytes()[i];
			}
			if(cnt<ret.length) {
				ret[cnt] = '/';
				cnt++;
			}
		}
		return ret;
	}
	
	@Override
	public boolean setData(InformationHolder data) {
		boolean ret=true;
		boolean sizeChanged=false;
		MetaID id;
		for(InformationHolder.Information info : data) {
			id= MetaID.valueOf(info.getKey());
			switch(id) {
				case ARTISTS:
					if(ArrayList.class.isAssignableFrom(info.getInnerClass())) {
						if(info.<String>getList().contains("")) {
							ArrayList<String> list = info.<String>getList();
							while (list.contains("")){
								list.remove("");
							}
							if(list.size()==0)
								ret=false;
							else {
								artists.clear();
								ret=artists.addAll(list);
							}
						} else {
							artists.clear();
							ret = artists.addAll(info.<String>getList());
							sizeChanged = true;
						}
					}
					else if(String.class.isAssignableFrom(info.getInnerClass())) {
						if(info.getString().equals(""))
							ret=false;
						else {
							artists.clear();
							ret = artists.add(info.getString());
							sizeChanged = true;
						}
					}
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
		boolean ret=true;
		boolean altered=false;
		MetaID id;
		for(InformationHolder.Information info : data) {
			id= MetaID.valueOf(info.getKey());
			if (id == MetaID.ARTISTS) {
				if (ArrayList.class.isAssignableFrom(info.getInnerClass())) {
					if(info.<String>getList().contains("")) {
						ArrayList<String> removes = info.<String>getList();
						while (removes.contains("")){
							removes.remove("");
						}
						if(removes.size()==0)
							ret=false;
						else
							ret = artists.removeAll(removes);
					}
					else {
						ret = artists.removeAll(info.<String>getList());
						altered = true;
					}
				} else if (String.class.isAssignableFrom(info.getInnerClass())) {
					if(info.getString().equals(""))
						ret=false;
					else {
						ret = artists.remove(info.getString());
						altered = true;
					}
				}
			}
		}
		if(altered) {
			recalculateFrameSize();
			setValidity();
		}
		return ret;
	}
	
	@Override
	public boolean addData(InformationHolder data) {
		boolean ret=true;
		boolean altered=false;
		MetaID id;
		for(InformationHolder.Information info : data) {
			id= MetaID.valueOf(info.getKey());
			if (id == MetaID.ARTISTS) {
				if (ArrayList.class.isAssignableFrom(info.getInnerClass())) {
					if(info.<String>getList().contains("")) {
						ArrayList<String> adds = info.<String>getList();
						while (adds.contains("")){
							adds.remove("");
						}
						if(adds.size()==0)
							ret=false;
						else
							ret = artists.addAll(adds);
					}
					else {
						ret = artists.addAll(info.<String>getList());
						altered = true;
					}
				} else if (String.class.isAssignableFrom(info.getInnerClass())) {
					if(info.getString().equals(""))
						ret=false;
					else {
						ret = artists.add(info.getString());
						altered = true;
					}
				}
			}
		}
		if(altered) {
			recalculateFrameSize();
			setValidity();
		}
		return ret;
	}
	
	@Override
	protected String[] getFrameString() {
		String artistrepresentation="";
		for(String s: artists){
			artistrepresentation+="\n\t"+s;
		}
		return new String[]{"Frame ID: "+getId(), "Encoding: "+encodings[encoding], "Artists: "+artistrepresentation};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		
		if(encoding<0 || encoding>=encodings.length)
			invalidate();
		else if(artists==null)
			invalidate();
		else if(artists.size()==0)
			invalidate();
		else {
			for (String artist : artists) {
				if (artist == null) {
					invalidate();
					return;
				}
				else if(artist.length()==0) {
					artists.remove(artist);
					if(artists.size()==0) {
						invalidate();
						return;
					}
				}
			}
		}
	}
	
	@Override
	String getIdentifier() {
		return MetaID.ARTISTS.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.ARTISTS;
	}
}
