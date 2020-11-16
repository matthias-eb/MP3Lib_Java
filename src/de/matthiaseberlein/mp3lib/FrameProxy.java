package de.matthiaseberlein.mp3lib;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class FrameProxy extends Frame {
	
	byte[] data;
	FrameProxy(String id, int framesize, long startbyte, byte[] flags, File file) {
		super(id, framesize, startbyte, flags, file);
		RandomAccessFile raf=null;
		try {
			raf=new RandomAccessFile(file,"r");
			raf.seek(startbyte);
			
			data=new byte[framesize];
			raf.read(data, 0, framesize);
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public InformationHolder getData() {
		return null;
	}
	
	@Override
	public boolean write(RandomAccessFile raf) throws IOException {
		raf.write(data);
		return true;
	}
	
	@Override
	protected byte[] getBytes() {
		return data;
	}
	
	@Override
	public boolean setData(InformationHolder data) {
		return false;
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
		return new String[]{"PROXYFRAME!", "Frame ID: "+getId(), "Frame size: "+getFramesize()};
	}
	
	@Override
	void setValidity() {
		super.setValidity();
		if(!isValid())
			return;
		invalidate();
	}
	
	@Override
	String getIdentifier() {
		return MetaID.NONE.toString();
	}
	
	@Override
	MetaID getPurpose() {
		return MetaID.NONE;
	}
}
