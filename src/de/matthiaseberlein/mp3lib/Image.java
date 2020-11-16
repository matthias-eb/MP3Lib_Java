package de.matthiaseberlein.mp3lib;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

class Image {
    List<String> picture_types= Arrays.asList("Other",
            "32*32 pixels 'file icon' (PNG only)",
            "Other file icon", "Cover(front)",
            "Cover(back)", "Leaflet page",
            "Media (e.g. label side of CD",
            "Lead artist/lead performer/soloist",
            "Artist/Performer",
            "Conductor",
            "Band/Orchestra",
            "Composer",
            "Lyricist/Text writer",
            "Recording Location",
            "During recording",
            "During performance",
            "Movie/video screen capture",
            "A bright coloured fish",
            "Illustration",
            "Band/Artist logotype",
            "Publisher/Studio logotype"
    );
    byte [] image_bytes;
    byte picture_type;
    String mime_type;

    BufferedImage bufferedImage;


    public Image(String mime_type, byte picture_type, byte[] imagebytes) {
        this.setMime_type(mime_type);
        this.picture_type = picture_type;
        this.image_bytes = imagebytes;

        try {
            if(mime_type.equals("-->"))
                bufferedImage = ImageIO.read(new URL(new String(imagebytes)));
            else
                bufferedImage = ImageIO.read(new ByteArrayInputStream(image_bytes));
        } catch (IOException e) {
            System.out.println("No Image found in Image bytes:\n\t"+e.getMessage());
        }
    }

    /**
     * Returns the Mime Type (for example image/jpeg, image/png or image/webp) as lowercase Strings.
     * @return A String containing either "-->" as an indicator for a link or a regular Mime type preceded by a "image/"
     */
    public String getMime_type() {
        return mime_type;
    }

    public boolean setMime_type(String mime_type){
        if(mime_type.startsWith("image/") || mime_type.equals("-->")) {
            this.mime_type = mime_type;
            return true;
        }
        return false;
    }

    public String getPicture_type() {
        return picture_types.get(picture_type);
    }

    byte getPicture_typeAsByte() {return picture_type;}

    public boolean setPicture_type(String picture_type) {
        if(!picture_types.contains(picture_type))
            return false;
        if(picture_types.indexOf(picture_type)==1) {
            mime_type = "image/png";
            createFileIconFromImage();
        }
        this.picture_type=(byte) picture_types.indexOf(picture_type);
        return true;
    }

    byte[] getImage_bytes() {
        return image_bytes;
    }

    /**
     * Scales the Image into a 32 by 32 Image after the picture type is set to file icon
     */
    void createFileIconFromImage() {
        // Create the output Image with the correct size
        BufferedImage outputImage = new BufferedImage(32, 32, bufferedImage.getType());

        // Scale current Image to new Image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(bufferedImage, 0, 0, 32, 32, null);
        g2d.dispose();

        bufferedImage = outputImage;
    }

    /**
     * Checks the validity of every attribute contained in this class.
     * The allowed values stem directly from the  ID3V2_3 guidelines and read as follows:
     *
     * This frame contains a picture directly related to the audio file. Image format is the MIME type and subtype for the image.
     * In the event that the MIME media type name is omitted, "image/" will be implied. The "image/png" or "image/jpeg" picture format should be used when interoperability is wanted.
     * Description is a short description of the picture, represented as a terminated textstring. The description has a maximum length of 64 characters, but may be empty.
     * There may be several pictures attached to one file, each in their individual "APIC" frame, but only one with the same content descriptor. There may only be one picture with the picture type declared as picture type $01 and $02 respectively.
     * There is the possibility to put only a link to the image file by using the 'MIME type' "-->" and having a complete URL instead of picture data. The use of linked files should however be used sparingly since there is the risk of separation of files.
     *
     * @return false if any of the Attributes contains a value that is not in the range of allowed values or if any combination of values is not allowed. Returns true otherwise.
     */
    boolean isValid() {
        if (image_bytes.length == 0)
            return false;
        if (!mime_type.startsWith("image/") && !mime_type.equals("-->"))
            return false;
        return true;
    }

    /**
     * Returns a unique Hash that identifies this Image.
     * @return a unique Identifier for this image
     */
    String getIdentifier() {
        if(picture_type==1 || picture_type==2){
            return picture_types.get(picture_type).hashCode()+"";
        }
        return (""+(picture_types.get(picture_type).hashCode())+(mime_type.hashCode()));
    }

    @Override
    public String toString() {
        if(image_bytes.length==0)
            return "No Image saved.";
        return "Mime Type: "+mime_type+"Image Bytes: "+image_bytes.length;
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

        if (!Image.class.isAssignableFrom(obj.getClass())) {
            if(String.class.isAssignableFrom(obj.getClass())) {
                String s= (String) obj;
                if(s.equals(getIdentifier()))
                    return true;
            }
            return false;
        }

        final Image img = (Image) obj;
        if(this.getIdentifier().equals(img.getIdentifier()))
            return true;
        return false;
    }
}
