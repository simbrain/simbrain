package org.simbrain.workspace;

import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;

/**
 * This class implements an output stream filter for writing files in the
 * ZIP file format. Includes support for both compressed and uncompressed
 * entries.
 *
 * @author  David Connelly
 * @version 1.31, 12/19/03
 */
public
class Zip extends DeflaterOutputStream {
    /*
     * Header signatures
     */
    static long LOCSIG = 0x04034b50L;   // "PK\003\004"
    static long EXTSIG = 0x08074b50L;   // "PK\007\008"
    static long CENSIG = 0x02014b50L;   // "PK\001\002"
    static long ENDSIG = 0x06054b50L;   // "PK\005\006"

    /*
     * Header sizes in bytes (including signatures)
     */
    static final int LOCHDR = 30;   // LOC header size
    static final int EXTHDR = 16;   // EXT header size
    static final int CENHDR = 46;   // CEN header size
    static final int ENDHDR = 22;   // END header size

    /*
     * Local file (LOC) header field offsets
     */
    static final int LOCVER = 4;    // version needed to extract
    static final int LOCFLG = 6;    // general purpose bit flag
    static final int LOCHOW = 8;    // compression method
    static final int LOCTIM = 10;   // modification time
    static final int LOCCRC = 14;   // uncompressed file crc-32 value
    static final int LOCSIZ = 18;   // compressed size
    static final int LOCLEN = 22;   // uncompressed size
    static final int LOCNAM = 26;   // filename length
    static final int LOCEXT = 28;   // extra field length

    /*
     * Extra local (EXT) header field offsets
     */
    static final int EXTCRC = 4;    // uncompressed file crc-32 value
    static final int EXTSIZ = 8;    // compressed size
    static final int EXTLEN = 12;   // uncompressed size

    /*
     * Central directory (CEN) header field offsets
     */
    static final int CENVEM = 4;    // version made by
    static final int CENVER = 6;    // version needed to extract
    static final int CENFLG = 8;    // encrypt, decrypt flags
    static final int CENHOW = 10;   // compression method
    static final int CENTIM = 12;   // modification time
    static final int CENCRC = 16;   // uncompressed file crc-32 value
    static final int CENSIZ = 20;   // compressed size
    static final int CENLEN = 24;   // uncompressed size
    static final int CENNAM = 28;   // filename length
    static final int CENEXT = 30;   // extra field length
    static final int CENCOM = 32;   // comment length
    static final int CENDSK = 34;   // disk number start
    static final int CENATT = 36;   // internal file attributes
    static final int CENATX = 38;   // external file attributes
    static final int CENOFF = 42;   // LOC header offset

    /*
     * End of central directory (END) header field offsets
     */
    static final int ENDSUB = 8;    // number of entries on this disk
    static final int ENDTOT = 10;   // total number of entries
    static final int ENDSIZ = 12;   // central directory size in bytes
    static final int ENDOFF = 16;   // offset of first CEN header
    static final int ENDCOM = 20;   // zip file comment length
    
    private ZipEntry entry;
    private Vector entries = new Vector();
    private Hashtable names = new Hashtable();
    private CRC32 crc = new CRC32();
    private long written = 0;
    private long locoff = 0;
    private String comment;
    private int method = DEFLATED;
    private boolean finished;

    private boolean closed = false;
    
    /**
     * Compressor for this stream.
     */
    protected Deflater def;

    /**
     * Output buffer for writing compressed data.
     */
    protected byte[] buf;
   
    /**
     * Indicates that the stream has been closed.
     */

    private boolean closed = false;

    /**
     * Creates a new output stream with the specified compressor and
     * buffer size.
     * @param out the output stream
     * @param def the compressor ("deflater")
     * @param size the output buffer size
     * @exception IllegalArgumentException if size is <= 0
     */
    public DeflaterOutputStream(OutputStream out, Deflater def, int size) {
        super(out);
        if (out == null || def == null) {
            throw new NullPointerException();
        } else if (size <= 0) {
            throw new IllegalArgumentException("buffer size <= 0");
        }
        this.def = def;
        buf = new byte[size];
    }

    /**
     * Creates a new output stream with the specified compressor and
     * a default buffer size.
     * @param out the output stream
     * @param def the compressor ("deflater")
     */
    public DeflaterOutputStream(OutputStream out, Deflater def) {
    this(out, def, 512);
    }

    boolean usesDefaultDeflater = false;

    /**
     * Creates a new output stream with a default compressor and buffer size.
     * @param out the output stream
     */
    public DeflaterOutputStream(OutputStream out) {
    this(out, new Deflater());
        usesDefaultDeflater = true;
    }

    /**
     * Writes a byte to the compressed output stream. This method will
     * block until the byte can be written.
     * @param b the byte to be written
     * @exception IOException if an I/O error has occurred
     */
    public void write(int b) throws IOException {
        byte[] buf = new byte[1];
    buf[0] = (byte)(b & 0xff);
    write(buf, 0, 1);
    }

    /**
     * Writes an array of bytes to the compressed output stream. This
     * method will block until all the bytes are written.
     * @param b the data to be written
     * @param off the start offset of the data
     * @param len the length of the data
     * @exception IOException if an I/O error has occurred
     */
    public void write(byte[] b, int off, int len) throws IOException {
    if (def.finished()) {
        throw new IOException("write beyond end of stream");
    }
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
        throw new IndexOutOfBoundsException();
    } else if (len == 0) {
        return;
    }
    if (!def.finished()) {
        def.setInput(b, off, len);
        while (!def.needsInput()) {
        deflate();
        }
    }
    }

    /**
     * Finishes writing compressed data to the output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     * @exception IOException if an I/O error has occurred
     */
    public void finish() throws IOException {
    if (!def.finished()) {
        def.finish();
        while (!def.finished()) {
        deflate();
        }
    }
    }

    /**
     * Writes remaining compressed data to the output stream and closes the
     * underlying stream.
     * @exception IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (!closed) {
            finish();
            if (usesDefaultDeflater)
                def.end();
            out.close();
            closed = true;
        }
    }

    /**
     * Writes next block of compressed data to the output stream.
     * @throws IOException if an I/O error has occurred
     */
    protected void deflate() throws IOException {
    int len = def.deflate(buf, 0, buf.length);
    if (len > 0) {
        out.write(buf, 0, len);
    }
    }
    
    /**
     * Check to make sure that this stream has not been closed
     */
    private void ensureOpen() throws IOException {
    if (closed) {
        throw new IOException("Stream closed");
        }
    }
    /**
     * Compression method for uncompressed (STORED) entries.
     */
    public static final int STORED = ZipEntry.STORED;

    /**
     * Compression method for compressed (DEFLATED) entries.
     */
    public static final int DEFLATED = ZipEntry.DEFLATED;

    /**
     * Creates a new ZIP output stream.
     * @param out the actual output stream
     */
    public Zip(OutputStream out) {
    super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
        usesDefaultDeflater = true;
    }

    /**
     * Sets the ZIP file comment.
     * @param comment the comment string
     * @exception IllegalArgumentException if the length of the specified
     *        ZIP file comment is greater than 0xFFFF bytes
     */
    public void setComment(String comment) {
        if (comment != null && comment.length() > 0xffff/3 
                                           && getUTF8Length(comment) > 0xffff) {
        throw new IllegalArgumentException("ZIP file comment too long.");
    }
    this.comment = comment;
    }

    /**
     * Sets the default compression method for subsequent entries. This
     * default will be used whenever the compression method is not specified
     * for an individual ZIP file entry, and is initially set to DEFLATED.
     * @param method the default compression method
     * @exception IllegalArgumentException if the specified compression method
     *        is invalid
     */
    public void setMethod(int method) {
    if (method != DEFLATED && method != STORED) {
        throw new IllegalArgumentException("invalid compression method");
    }
    this.method = method;
    }

    /**
     * Sets the compression level for subsequent entries which are DEFLATED.
     * The default setting is DEFAULT_COMPRESSION.
     * @param level the compression level (0-9)
     * @exception IllegalArgumentException if the compression level is invalid
     */
    public void setLevel(int level) {
    def.setLevel(level);
    }

    /**
     * Begins writing a new ZIP file entry and positions the stream to the
     * start of the entry data. Closes the current entry if still active.
     * The default compression method will be used if no compression method
     * was specified for the entry, and the current time will be used if
     * the entry has no set modification time.
     * @param e the ZIP entry to be written
     * @exception ZipException if a ZIP format error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void putNextEntry(ZipEntry e) throws IOException {
    ensureOpen();
    if (entry != null) {
        closeEntry();   // close previous entry
    }
    if (e.time == -1) {
        e.setTime(System.currentTimeMillis());
    }
    if (e.method == -1) {
        e.method = method;  // use default method
    }
    switch (e.method) {
    case DEFLATED:
        if (e.size == -1 || e.csize == -1 || e.crc == -1) {
        // store size, compressed size, and crc-32 in data descriptor
        // immediately following the compressed entry data
        e.flag = 8;
        } else if (e.size != -1 && e.csize != -1 && e.crc != -1) {
        // store size, compressed size, and crc-32 in LOC header
        e.flag = 0;
        } else {
        throw new ZipException(
            "DEFLATED entry missing size, compressed size, or crc-32");
        }
        e.version = 20;
        break;
    case STORED:
        // compressed size, uncompressed size, and crc-32 must all be
        // set for entries using STORED compression method
        if (e.size == -1) {
        e.size = e.csize;
        } else if (e.csize == -1) {
        e.csize = e.size;
        } else if (e.size != e.csize) {
        throw new ZipException(
            "STORED entry where compressed != uncompressed size");
        }
        if (e.size == -1 || e.crc == -1) {
        throw new ZipException(
            "STORED entry missing size, compressed size, or crc-32");
        }
        e.version = 10;
        e.flag = 0;
        break;
    default:
        throw new ZipException("unsupported compression method");
    }
    e.offset = written;
    if (names.put(e.name, e) != null) {
        throw new ZipException("duplicate entry: " + e.name);
    }
        writeLOC(e);
    entries.addElement(e);
    entry = e;
    }

    /**
     * Closes the current ZIP entry and positions the stream for writing
     * the next entry.
     * @exception ZipException if a ZIP format error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void closeEntry() throws IOException {
    ensureOpen();
    ZipEntry e = entry;
    if (e != null) {
        switch (e.method) {
        case DEFLATED:
        def.finish();
        while (!def.finished()) {
            deflate();
        }
        if ((e.flag & 8) == 0) {
            // verify size, compressed size, and crc-32 settings
            if (e.size != def.getBytesRead()) {
            throw new ZipException(
                "invalid entry size (expected " + e.size +
                " but got " + def.getBytesRead() + " bytes)");
            }
            if (e.csize != def.getBytesWritten()) {
            throw new ZipException(
                "invalid entry compressed size (expected " +
                e.csize + " but got " + def.getBytesWritten() + " bytes)");
            }
            if (e.crc != crc.getValue()) {
            throw new ZipException(
                "invalid entry CRC-32 (expected 0x" +
                Long.toHexString(e.crc) + " but got 0x" +
                Long.toHexString(crc.getValue()) + ")");
            }
        } else {
            e.size  = def.getBytesRead();
            e.csize = def.getBytesWritten();
            e.crc = crc.getValue();
            writeEXT(e);
        }
        def.reset();
        written += e.csize;
        break;
        case STORED:
        // we already know that both e.size and e.csize are the same
        if (e.size != written - locoff) {
            throw new ZipException(
            "invalid entry size (expected " + e.size +
            " but got " + (written - locoff) + " bytes)");
        }
        if (e.crc != crc.getValue()) {
            throw new ZipException(
             "invalid entry crc-32 (expected 0x" +
             Long.toHexString(e.crc) + " but got 0x" +
             Long.toHexString(crc.getValue()) + ")");
        }
        break;
        default:
        throw new InternalError("invalid compression method");
        }
        crc.reset();
        entry = null;
    }
    }

    /**
     * Writes an array of bytes to the current ZIP entry data. This method
     * will block until all the bytes are written.
     * @param b the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public synchronized void write(byte[] b, int off, int len)
    throws IOException
    {
    ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
        throw new IndexOutOfBoundsException();
    } else if (len == 0) {
        return;
    }

    if (entry == null) {
        throw new ZipException("no current ZIP entry");
    }
    switch (entry.method) {
    case DEFLATED:
        super.write(b, off, len);
        break;
    case STORED:
        written += len;
        if (written - locoff > entry.size) {
        throw new ZipException(
            "attempt to write past end of STORED entry");
        }
        out.write(b, off, len);
        break;
    default:
        throw new InternalError("invalid compression method");
    }
    crc.update(b, off, len);
    }

    /**
     * Finishes writing the contents of the ZIP output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O exception has occurred
     */
    public void finish() throws IOException {
    ensureOpen();
    if (finished) {
        return;
    }
    if (entry != null) {
        closeEntry();
    }
    if (entries.size() < 1) {
        throw new ZipException("ZIP file must have at least one entry");
    }
    // write central directory
    long off = written;
    Enumeration e = entries.elements();
    while (e.hasMoreElements()) {
        writeCEN((ZipEntry)e.nextElement());
    }
    writeEND(off, written - off);
    finished = true;
    }

    /**
     * Closes the ZIP output stream as well as the stream being filtered.
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (!closed) {
            super.close();
            closed = true;
        }
    }

    /*
     * Writes local file (LOC) header for specified entry.
     */
    private void writeLOC(ZipEntry e) throws IOException {
    writeInt(LOCSIG);       // LOC header signature
    writeShort(e.version);      // version needed to extract
    writeShort(e.flag);         // general purpose bit flag
    writeShort(e.method);       // compression method
    writeInt(e.time);           // last modification time
    if ((e.flag & 8) == 8) {
        // store size, uncompressed size, and crc-32 in data descriptor
        // immediately following compressed entry data
        writeInt(0);
        writeInt(0);
        writeInt(0);
    } else {
        writeInt(e.crc);        // crc-32
        writeInt(e.csize);      // compressed size
        writeInt(e.size);       // uncompressed size
    }
    byte[] nameBytes = getUTF8Bytes(e.name);
    writeShort(nameBytes.length);
    writeShort(e.extra != null ? e.extra.length : 0);
    writeBytes(nameBytes, 0, nameBytes.length);
    if (e.extra != null) {
        writeBytes(e.extra, 0, e.extra.length);
    }
    locoff = written;
    }

    /*
     * Writes extra data descriptor (EXT) for specified entry.
     */
    private void writeEXT(ZipEntry e) throws IOException {
    writeInt(EXTSIG);       // EXT header signature
    writeInt(e.crc);        // crc-32
    writeInt(e.csize);      // compressed size
    writeInt(e.size);       // uncompressed size
    }

    /*
     * Write central directory (CEN) header for specified entry.
     * REMIND: add support for file attributes
     */
    private void writeCEN(ZipEntry e) throws IOException {
    writeInt(CENSIG);       // CEN header signature
    writeShort(e.version);      // version made by
    writeShort(e.version);      // version needed to extract
    writeShort(e.flag);     // general purpose bit flag
    writeShort(e.method);       // compression method
    writeInt(e.time);       // last modification time
    writeInt(e.crc);        // crc-32
    writeInt(e.csize);      // compressed size
    writeInt(e.size);       // uncompressed size
    byte[] nameBytes = getUTF8Bytes(e.name);
    writeShort(nameBytes.length);
    writeShort(e.extra != null ? e.extra.length : 0);
    byte[] commentBytes;
    if (e.comment != null) {
        commentBytes = getUTF8Bytes(e.comment);
        writeShort(commentBytes.length);
    } else {
        commentBytes = null;
        writeShort(0);
    }
    writeShort(0);          // starting disk number
    writeShort(0);          // internal file attributes (unused)
    writeInt(0);            // external file attributes (unused)
    writeInt(e.offset);     // relative offset of local header
    writeBytes(nameBytes, 0, nameBytes.length);
    if (e.extra != null) {
        writeBytes(e.extra, 0, e.extra.length);
    }
    if (commentBytes != null) {
        writeBytes(commentBytes, 0, commentBytes.length);
    }
    }

    /*
     * Writes end of central directory (END) header.
     */
    private void writeEND(long off, long len) throws IOException {
    writeInt(ENDSIG);       // END record signature
    writeShort(0);          // number of this disk
    writeShort(0);          // central directory start disk
    writeShort(entries.size()); // number of directory entries on disk
    writeShort(entries.size()); // total number of directory entries
    writeInt(len);          // length of central directory
    writeInt(off);          // offset of central directory
    if (comment != null) {      // zip file comment
        byte[] b = getUTF8Bytes(comment);
        writeShort(b.length);
        writeBytes(b, 0, b.length);
    } else {
        writeShort(0);
    }
    }

    /*
     * Writes a 16-bit short to the output stream in little-endian byte order.
     */
    private void writeShort(int v) throws IOException {
    OutputStream out = this.out;
    out.write((v >>> 0) & 0xff);
    out.write((v >>> 8) & 0xff);
    written += 2;
    }

    /*
     * Writes a 32-bit int to the output stream in little-endian byte order.
     */
    private void writeInt(long v) throws IOException {
    OutputStream out = this.out;
    out.write((int)((v >>>  0) & 0xff));
    out.write((int)((v >>>  8) & 0xff));
    out.write((int)((v >>> 16) & 0xff));
    out.write((int)((v >>> 24) & 0xff));
    written += 4;
    }

    /*
     * Writes an array of bytes to the output stream.
     */
    private void writeBytes(byte[] b, int off, int len) throws IOException {
    super.out.write(b, off, len);
    written += len;
    }

    /*
     * Returns the length of String's UTF8 encoding.
     */
    static int getUTF8Length(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i); 
            if (ch <= 0x7f) {
                count++;
            } else if (ch <= 0x7ff) {
                count += 2;
            } else {
                count += 3;
            }
        }
        return count;
    }

    /*
     * Returns an array of bytes representing the UTF8 encoding
     * of the specified String.
     */
    private static byte[] getUTF8Bytes(String s) {
    char[] c = s.toCharArray();
    int len = c.length;
    // Count the number of encoded bytes...
    int count = 0;
    for (int i = 0; i < len; i++) {
        int ch = c[i];
        if (ch <= 0x7f) {
        count++;
        } else if (ch <= 0x7ff) {
        count += 2;
        } else {
        count += 3;
        }
    }
    // Now return the encoded bytes...
    byte[] b = new byte[count];
    int off = 0;
    for (int i = 0; i < len; i++) {
        int ch = c[i];
        if (ch <= 0x7f) {
        b[off++] = (byte)ch;
        } else if (ch <= 0x7ff) {
        b[off++] = (byte)((ch >> 6) | 0xc0);
        b[off++] = (byte)((ch & 0x3f) | 0x80);
        } else {
        b[off++] = (byte)((ch >> 12) | 0xe0);
        b[off++] = (byte)(((ch >> 6) & 0x3f) | 0x80);
        b[off++] = (byte)((ch & 0x3f) | 0x80);
        }
    }
    return b;
    }
}