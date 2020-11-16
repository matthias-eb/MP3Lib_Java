package de.matthiaseberlein.mp3lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

class InformationHolder implements Iterable<InformationHolder.Information>, Iterator<InformationHolder.Information> {
    private int position;
    private ArrayList<Information> list;

    InformationHolder(){
        position=0;
        list=new ArrayList<>();
    }
    InformationHolder with(String key, Object data){
        if(!list.contains(new Information(key)))
            list.add(new Information(key, data));
        return this;
    }

    InformationHolder join(InformationHolder holder) {
        if(holder==null)
            return this;
        holder.setPosition(0);
        for(Information info : holder) {
            boolean present=false;
            for(Information i2: list) {
                if(i2.compareTo(info)==0)
                    present=true;

            }
            if(!present) {
                list.add(info);
            }
        }
        holder.setPosition(0);
        return this;
    }

    char getChar(String key) {
        return getInformation(key).getChar();
    }
    String getString(String key){
        return getInformation(key).getString();
    }
    byte getByte(String key) {
        return getInformation(key).getByte();
    }
    short getShort(String key) {
        return getInformation(key).getShort();
    }
    int getInt(String key) {
        return getInformation(key).getInt();
    }
    double getDouble(String key) {
        return getInformation(key).getDouble();
    }
    long getLong(String key) {
        return getInformation(key).getLong();
    }

    <T> ArrayList<T> getList(String key) {
        return getInformation(key).getList();
    }
    InformationHolder getHolder(String key) {return getInformation(key).getHolder();}
    Object getObject(String key) {
        return getInformation(key).getObject();
    }
    
    public void setPosition(int position) {
        if(list.size()>position)
            this.position = position;
    }

    private Information getInformation(String key){
        if(list.size()==0)
            return null;
        Information obj=new Information(key);
        for(Information info : list) {
            if(info.equals(obj)) {
                return info;
            }
        }
        return null;
    }
    
    public int size() {
        return list.size();
    }
    
    public boolean contains(String key) {
        return list.contains(new Information(key));
    }
	
	public boolean remove(String key) {
		return list.remove(new Information(key));
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
		String s="[Holder Size: "+list.size()+" Items: ";
		for(Information info : list){
			s+=info.toString();
		}
		s+="]";
		return s;
	}
	
	/**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Information> iterator() {
    	setPosition(0);
        return this;
    }
    
    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return position < list.size();
    }
    
    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public Information next() {
        Information ausg = null;
        if(list.size()>position) {
            ausg = list.get(position);
            position++;
        }
        else
            throw new NoSuchElementException("There is no further Information in this Holder.");
        return ausg;
    }
    
    class Information implements Comparable<Information> {
        private String key;
        private Class datatype;
        private Object data;

        Information(String key){
            this.key=key;
        }

        Information(String key, Object data){
            this.key=key;
            datatype=data.getClass();
            this.data=data;
        }

        String getKey() {
            return key;
        }

        String getClassName() {
            return datatype.getName();
        }
        
        Class getInnerClass(){return datatype;}

        char getChar() {
            if(Character.class.isAssignableFrom(data.getClass()))
                return (char) data;
            return 0;
        }
        String getString(){
            if(String.class.isAssignableFrom(data.getClass()))
                return (String) data;
            try{
            	String s= (String) data;
            } catch(ClassCastException e){
            	e.printStackTrace();
            }
            return null;
        }
        byte getByte() {
            if(Byte.class.isAssignableFrom(data.getClass()))
                return (byte) data;
            return 0;
        }
        short getShort() {
            if(Short.class.isAssignableFrom(data.getClass()))
                return (short) data;
            return 0;
        }
        int getInt() {
            if(Integer.class.isAssignableFrom(data.getClass()))
                return (int) data;
            return 0;
        }
        double getDouble() {
            if(Double.class.isAssignableFrom(data.getClass()))
                return (double) data;
            return 0;
            
        }
        long getLong() {
            if(Long.class.isAssignableFrom(data.getClass()))
                return (long) data;
            return 0;
        }

        <T> ArrayList<T> getList() {
            ArrayList<T> test=new ArrayList<>();
            if(test.getClass().isAssignableFrom(data.getClass()))
                return (ArrayList<T>) data;
            return null;
        }
        InformationHolder getHolder() {
            if(InformationHolder.class.isAssignableFrom(data.getClass()))
                return (InformationHolder) data;
            return null;
        }
        Object getObject() {
            return data;
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
		    return "{" + key + "|" + data.toString() + "}";
	    }
	
	    /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         *
         * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
         * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
         * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
         * <tt>y.compareTo(x)</tt> throws an exception.)
         *
         * <p>The implementor must also ensure that the relation is transitive:
         * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
         * <tt>x.compareTo(z)&gt;0</tt>.
         *
         * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
         * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
         * all <tt>z</tt>.
         *
         * <p>It is strongly recommended, but <i>not</i> strictly required that
         * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
         * class that implements the <tt>Comparable</tt> interface and violates
         * this condition should clearly indicate this fact.  The recommended
         * language is "Note: this class has a natural ordering that is
         * inconsistent with equals."
         *
         * <p>In the foregoing description, the notation
         * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
         * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
         * <tt>0</tt>, or <tt>1</tt> according to whether the value of
         * <i>expression</i> is negative, zero or positive.
         *
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         */
        @Override
        public int compareTo(Information o) {
            return key.compareTo(o.key);
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
            if(obj==null)
                return false;
            Information info;
            if(Information.class.isAssignableFrom(obj.getClass())) {
	            info= (Information) obj;
	            return key.equals(info.getKey());
            }
            else if(String.class.isAssignableFrom(obj.getClass())){
            	String s= (String) obj;
            	return key.equals(s);
            }
            return false;
        }
    }
}
