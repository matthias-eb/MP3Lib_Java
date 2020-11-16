package de.matthiaseberlein.mp3lib;


import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

class SongData implements Iterable<SongData>, Iterator<SongData>{
	private int position;
	private PurposeID purpose;
	private String frameIdentifier;
	private HashMap<MetaID, Object> metadata;
	/**
	 * Returns an iterator over elements of type {@code T}.
	 *
	 * @return an Iterator.
	 */

	@Override
	public Iterator<SongData> iterator() {
		position=0;
		return this;
	}
	
	/**
	 * Performs the given action for each element of the {@code Iterable}
	 * until all elements have been processed or the action throws an
	 * exception.  Unless otherwise specified by the implementing class,
	 * actions are performed in the order of iteration (if an iteration order
	 * is specified).  Exceptions thrown by the action are relayed to the
	 * caller.
	 *
	 * @param action The action to be performed for each element
	 * @throws NullPointerException if the specified action is null
	 * @since 1.8
	 */
	@Override
	public void forEach(Consumer<? super SongData> action) {
	
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
		return false;
	}
	
	/**
	 * Returns the next element in the iteration.
	 *
	 * @return the next element in the iteration
	 */
	@Override
	public SongData next() {
		return null;
	}
}
