package org.simbrain.util.piccolo;

import com.Ostermiller.util.CSVParser;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Tiled {@code <data>} tag object representation.
 *
 * Cannot handle xml encoded data for now.
 */
@XStreamAlias("data")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = {"data"})
public class TiledData {

    /**
     * The encoding used to encode the tile layer data. When used, it can be “base64” and “csv” at the moment.
     */
    private String encoding;

    /**
     * The compression used to compress the tile layer data. Tiled supports “gzip” and “zlib”.
     */
    private String compression;

    /**
     * Raw content of the data.
     *
     * Not handling embedded image for now.
     */
    private char[] data;

    /**
     * The flat list of tile id from the raw data.
     */
    private transient List<Integer> gid = null;

    /**
     * Get the list of tile id.
     * Result is computed from raw data and stored in field on the first call.
     * After the first call this just returns the values stored in field.
     *
     * @return the list of tile id this data represents.
     */
    public List<Integer> getGid() {
        if (gid == null) {
            gid = new ArrayList<>();
            if ("csv".equals(encoding)) {
                CSVParser parser = new CSVParser(new CharArrayReader(data));
                String[][] allValues;
                try {
                    allValues = parser.getAllValues();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }

                for (String[] row : allValues) {
                    for (String cell : row) {
                        if (!cell.isEmpty()) { // trailing commas create extra empty elements. filter them out.
                            gid.add(Integer.parseInt(cell));
                        }
                    }
                }
            } else if ("base64".equals(encoding)) {

                // remove spaces and line breaks
                String tempData = String.valueOf(data).replaceAll("[ \n]", "");
                data = tempData.toCharArray();

                byte[] decoded = Base64.getDecoder().decode(String.valueOf(data));
                if ("gzip".equals(compression)) {
                    try (InputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(decoded))) {
                        decoded = inputStream.readAllBytes();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if ("zlib".equals(compression)) {
                    InputStream inputStream = new InflaterInputStream(new ByteArrayInputStream(decoded));
                    try {
                        decoded = inputStream.readAllBytes();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                for (int i = 0; i < decoded.length; i += 4) {
                    // interpreted as an array of unsigned 32-bit integers using little-endian byte ordering.
                    // see https://doc.mapeditor.org/en/stable/reference/tmx-map-format/#tmx-data
                    gid.add(decoded[i] | decoded[i + 1] << 8 | decoded[i + 2] << 16 | decoded[i + 3] << 24);
                }
            }
        }
        return gid;
    }

}
