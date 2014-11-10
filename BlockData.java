package LZ77;

/**
 * Speichert die Informationen eines LZ77-Blockes.
 * Offset : Wie viele Zeichen vorher eine Übereinstimmung statt findet.
 * charsIn : Wie viele Zeichen die Überinstimmung umfasst.
 * nectChat : Welches das nächste Zeichen ist.
 * @author Patrick
 */
public class BlockData implements Comparable<BlockData> {

	public int offset;
	public int charsIn;
	public char nextChar;

	public BlockData() {
		offset = 0;
		charsIn = 0;
	}

	public BlockData(int o, int cIn, char nCh) {
		offset = o;
		charsIn = cIn;
		nextChar = nCh;
	}

	@Override
	public int compareTo(BlockData o) {
		return this.charsIn < o.charsIn ? -1 : (this.charsIn > o.charsIn ? 1
				: 0);
	}

	boolean isEmpty() {
		return charsIn <= 0;
	}

	@Override
	public String toString() {
		return "[" + offset + "," + charsIn + ","
				+ Character.toString(nextChar) + "]";
	}
}
