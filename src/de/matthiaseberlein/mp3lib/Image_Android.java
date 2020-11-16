package de.matthiaseberlein.mp3lib;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;


public class Image_Android extends Image {
//ToDO: Add method body of method that converts the image into a 32 by 32 pixel image

	
	private Bitmap.CompressFormat mime_type;
	private Bitmap image;

	
	public Image_Android(String mime_type, byte picture_type, byte imagebytes[]) {
		super(mime_type, picture_type, imagebytes);
		this.setMime_type(mime_type);
		this.image = BitmapFactory.decodeByteArray(imagebytes, 0, imagebytes.length);
	}
	
	/**
	 * Returns the Mime Type (JPEG, PNG or WEBP) as lowercase Strings.
	 * @return
	 */
	@Override
	public String getMime_type() {
		return mime_type.name();
	}


	@Override
	public boolean setMime_type(String mime_type) {
		if(mime_type.equals("-->") | mime_type.equals("image/")) {
			this.mime_type = null;
			return true;
		}
		else if(mime_type.contains("image/")) {
			this.mime_type = Bitmap.CompressFormat.valueOf(mime_type.substring(mime_type.indexOf('/')+1).toUpperCase());
			return true;
		}
		System.out.println("Mime Type:"+mime_type);
		return false;
	}
	
	public byte getPicture_Type_Byte() {
		return picture_type;
	}
	

	
	/**
	 * This Method returns a byte array containing everything belonging to the Image as a bytearray that can be directly written to the file.
	 * @return
	 */
	byte [] getCompressedBytes(int quality) {    //ToDo: add the Description and Mime Type etc to the writeable bytes
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		image.compress(mime_type, quality, outputStream);
		outputStream.toByteArray();
		return outputStream.toByteArray();
	}
	
	/**
	 * Returns only the Image as ByteArrayOutputStream compressed to the Format specified in mime_type. Supported Formats are specified in {@linkplain Bitmap.CompressFormat Bitmap.CompressFormat}
	 * @return ByteArrayOutputStream modified by {@linkplain Bitmap#compress(Bitmap.CompressFormat, int, OutputStream) Bitmap.compress()}
	 */
	@Override
	public byte [] getImage_bytes() {
		if(image != null) {
			image_bytes=getCompressedBytes(100);
		}
		return image_bytes;
	}

	@Override
	public void createFileIconFromImage() {

	}

	public Canvas getCanvas() {
		Canvas canvas=new Canvas();
		canvas.setBitmap(image);
		return canvas;
	}
	
	public boolean setImage(Bitmap image) {
		if(image==null)
			return false;
		this.image=image;
		return true;
	}
	
	public Bitmap getImage() {
		return image;
	}
	
	/**
	 * Returns a string representation of the object. In general, the
	 * {@code toString} method returns a string that
	 * "textually represents" this object. The result should
	 * be a concise but informative representation that is easy for a
	 * person to read.
	 * It is recommended that all subclasses override this method.
	 * <p>
	 * The {@code toString} method for class {@code Object}
	 * returns a string consisting of the name of the class of which the
	 * object is an instance, the at-sign character `{@code @}', and
	 * the unsigned hexadecimal representation of the hash code of the
	 * object. In other words, this method returns a string equal to the
	 * value of:
	 * <blockquote>
	 * <pre>
	 * getClass().getName() + '@' + Integer.toHexString(hashCode())
	 * </pre></blockquote>
	 *
	 * @return a string representation of the object.
	 */
	@Override
	public String toString() {
		if(image==null)
			return "No Image saved.";
		return "Mime Type: "+mime_type+"Image Bytes: "+image.getByteCount();
	}

	@Override
	boolean isValid() {
		if(picture_type>=picture_types.size() || picture_type<0)
			return false;
		return true;
	}

	@Override
	String getIdentifier() {
		if(picture_type==1 || picture_type==2){
			return picture_types.get(picture_type).hashCode()+"";
		}
		return (""+(picture_types.get(picture_type).hashCode())+(mime_type.hashCode()));
	}
	
	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <p>
	 * The {@code equals} method implements an equivalence relation
	 * on non-null object references:
	 * <ul>
	 * <li>It is <i>reflexive</i>: for any non-null reference value
	 * {@code x}, {@code x.equals(x)} should return
	 * {@code true}.
	 * <li>It is <i>symmetric</i>: for any non-null reference values
	 * {@code x} and {@code y}, {@code x.equals(y)}
	 * should return {@code true} if and only if
	 * {@code y.equals(x)} returns {@code true}.
	 * <li>It is <i>transitive</i>: for any non-null reference values
	 * {@code x}, {@code y}, and {@code z}, if
	 * {@code x.equals(y)} returns {@code true} and
	 * {@code y.equals(z)} returns {@code true}, then
	 * {@code x.equals(z)} should return {@code true}.
	 * <li>It is <i>consistent</i>: for any non-null reference values
	 * {@code x} and {@code y}, multiple invocations of
	 * {@code x.equals(y)} consistently return {@code true}
	 * or consistently return {@code false}, provided no
	 * information used in {@code equals} comparisons on the
	 * objects is modified.
	 * <li>For any non-null reference value {@code x},
	 * {@code x.equals(null)} should return {@code false}.
	 * </ul>
	 * <p>
	 * The {@code equals} method for class {@code Object} implements
	 * the most discriminating possible equivalence relation on objects;
	 * that is, for any non-null reference values {@code x} and
	 * {@code y}, this method returns {@code true} if and only
	 * if {@code x} and {@code y} refer to the same object
	 * ({@code x == y} has the value {@code true}).
	 * <p>
	 * Note that it is generally necessary to override the {@code hashCode}
	 * method whenever this method is overridden, so as to maintain the
	 * general contract for the {@code hashCode} method, which states
	 * that equal objects must have equal hash codes.
	 *
	 * @param obj the reference object with which to compare.
	 * @return {@code true} if this object is the same as the obj
	 * argument; {@code false} otherwise.
	 * @see #hashCode()
	 * @see HashMap
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		
		if (!Image_Android.class.isAssignableFrom(obj.getClass())) {
			if(String.class.isAssignableFrom(obj.getClass())) {
				String s= (String) obj;
				if(s.equals(getIdentifier()))
					return true;
			}
			return false;
		}
		
		final Image_Android img = (Image_Android) obj;
		if(this.getIdentifier().equals(img.getIdentifier()))
			return true;
		return false;
	}
}
