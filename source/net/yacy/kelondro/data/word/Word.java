// Word.java
// (C) 2008 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 26.03.2008 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
// LICENSE
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.yacy.kelondro.data.word;

import java.util.Collection;
import java.util.Locale;

import net.yacy.cora.order.Base64Order;
import net.yacy.cora.order.Digest;
import net.yacy.cora.storage.ARC;
import net.yacy.cora.storage.ConcurrentARC;
import net.yacy.cora.storage.HandleSet;
import net.yacy.cora.util.SpaceExceededException;
import net.yacy.kelondro.index.RowHandleSet;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.util.Bitfield;
import net.yacy.kelondro.util.MemoryControl;


public class Word {


    /**
     * this is the lenght(12) of the hash key that is used:<br>
     * - for seed hashes (this Object)<br>
     * - for word hashes (IndexEntry.wordHashLength)<br>
     * - for L-URL hashes (plasmaLURL.urlHashLength)<br><br>
     * these hashes all shall be generated by base64.enhancedCoder
     */
    public static final int commonHashLength = 12;

    private static final int hashCacheSize = Math.max(20000, Math.min(200000, (int) (MemoryControl.available() / 40000L)));
    private static ARC<String, byte[]> hashCache = null;
    static {
        try {
            hashCache = new ConcurrentARC<String, byte[]>(hashCacheSize, Math.max(32, 4 * Runtime.getRuntime().availableProcessors()));
            Log.logInfo("Word", "hashCache.size = " + hashCacheSize);
        } catch (final OutOfMemoryError e) {
            hashCache = new ConcurrentARC<String, byte[]>(1000, Math.max(8, 2 * Runtime.getRuntime().availableProcessors()));
            Log.logInfo("Word", "hashCache.size = " + 1000);
        }
    }
    /*
    private static ConcurrentHashMap<String, byte[]> hashCache = null;
    static {
        hashCache = new ConcurrentHashMap<String, byte[]>();
    }
    */

    // object carries statistics for words and sentences
    public  int      count;       // number of occurrences
    public  int      posInText;   // unique handle, is initialized with word position (excluding double occurring words)
    public  int      posInPhrase; // position of word in phrase
    public  int      numOfPhrase; // number of phrase. 'normal' phrases begin with number 100
    public  Bitfield flags;       // the flag bits for each word

    public Word(final int handle, final int pip, final int nop) {
        this.count = 1;
        this.posInText = handle;
        this.posInPhrase = pip;
        this.numOfPhrase = nop;
        this.flags = null;
    }

    public void inc() {
        this.count++;
    }

    public int occurrences() {
        return this.count;
    }

    @Override
    public String toString() {
        // this is here for debugging
        return "{count=" + this.count + ", posInText=" + this.posInText + ", posInPhrase=" + this.posInPhrase + ", numOfPhrase=" + this.numOfPhrase + "}";
    }

    // static methods
    public static byte[] word2hash(final StringBuilder word) {
        return word2hash(word.toString());
    }

    private final static byte lowByte = Base64Order.alpha_enhanced[0];
    private final static byte highByte = Base64Order.alpha_enhanced[Base64Order.alpha_enhanced.length - 1];

    public static boolean isPrivate(byte[] hash) {
        return hash[0] == highByte && hash[1] == highByte && hash[2] == highByte && hash[3] == highByte && hash[4] == highByte;
    }

    // create a word hash
    public static final byte[] word2hash(final String word) {
    	final String wordlc = word.toLowerCase(Locale.ENGLISH);
    	byte[] h = hashCache.get(wordlc);
        if (h != null) return h;
        // calculate the hash
    	h = Base64Order.enhancedCoder.encodeSubstring(Digest.encodeMD5Raw(wordlc), commonHashLength);
    	while (h[0] == highByte && h[1] == highByte && h[2] == highByte && h[3] == highByte && h[4] == highByte) {
    	    // ensure that word hashes do not start with hash '_____' which is a key for an extra hash range for private usage on the local peer
    	    // statistically we are inside this loop only every 2^^30 calls of word2hash (which means almost never)
    	    System.arraycopy(h, 1, h, 0, commonHashLength - 1);
    	    h[commonHashLength - 1] = lowByte;
    	}
        assert h[2] != '@';
        if (MemoryControl.shortStatus()) {
            hashCache.clear();
        } else {
            //hashCache.putIfAbsent(wordlc, h); // prevent expensive MD5 computation and encoding
            hashCache.insertIfAbsent(wordlc, h); // prevent expensive MD5 computation and encoding
        }
        return h;
    }

    public final static byte PRIVATE_TYPE_COPY = 'C';     // used for a private local copy of the index
    public final static byte PRIVATE_TYPE_PHONETIC = 'K'; // used for ColognePhonetics

    public static final byte[] hash2private(final byte[] hash, byte privateType) {
        byte[] p = new byte[commonHashLength];
        p[0] = highByte; p[1] = highByte; p[2] = highByte; p[3] = highByte; p[4] = highByte; p[5] = privateType;
        System.arraycopy(hash, 0, p, 6, commonHashLength - 6); // 36 bits left for private hashes should be enough
        return p;
    }

    public static final HandleSet words2hashesHandles(final Collection<String> words) {
        final HandleSet hashes = new RowHandleSet(WordReferenceRow.urlEntryRow.primaryKeyLength, WordReferenceRow.urlEntryRow.objectOrder, words.size());
        for (final String word: words)
            try {
                hashes.put(word2hash(word));
            } catch (final SpaceExceededException e) {
                Log.logException(e);
                return hashes;
            }
        return hashes;
    }

    public static final HandleSet words2hashesHandles(final String[] words) {
        final HandleSet hashes = new RowHandleSet(WordReferenceRow.urlEntryRow.primaryKeyLength, WordReferenceRow.urlEntryRow.objectOrder, words.length);
        for (final String word: words)
            try {
                hashes.put(word2hash(word));
            } catch (final SpaceExceededException e) {
                Log.logException(e);
                return hashes;
            }
        return hashes;
    }
}
