/**
 * eobjects.org MetaModel
 * Copyright (C) 2010 eobjects.org
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.metamodel.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;

import org.eobjects.metamodel.MetaModelException;
import org.eobjects.metamodel.QueryPostprocessDataContext;
import org.eobjects.metamodel.UpdateScript;
import org.eobjects.metamodel.UpdateableDataContext;
import org.eobjects.metamodel.data.DataSet;
import org.eobjects.metamodel.query.FilterItem;
import org.eobjects.metamodel.schema.Column;
import org.eobjects.metamodel.schema.Table;
import org.eobjects.metamodel.util.FileHelper;
import org.eobjects.metamodel.util.FileResource;
import org.eobjects.metamodel.util.Func;
import org.eobjects.metamodel.util.Resource;
import org.eobjects.metamodel.util.UrlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

/**
 * DataContext implementation for reading CSV files.
 */
public final class CsvDataContext extends QueryPostprocessDataContext implements UpdateableDataContext {

    private static final Logger logger = LoggerFactory.getLogger(CsvDataContext.class);

    private final Object WRITE_LOCK = new Object();

    private final Resource _resource;
    private final CsvConfiguration _configuration;
    private final boolean _writable;

    /**
     * Constructs a CSV DataContext based on a file
     * 
     * @param file
     * @param configuration
     */
    public CsvDataContext(File file, CsvConfiguration configuration) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("CsvConfiguration cannot be null");
        }
        _resource = new FileResource(file);
        _configuration = configuration;
        _writable = true;
    }

    public CsvDataContext(Resource resource, CsvConfiguration configuration) {
        if (resource == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("CsvConfiguration cannot be null");
        }
        _resource = resource;
        _configuration = configuration;
        _writable = !resource.isReadOnly();
    }

    /**
     * Constructs a CSV DataContext based on a {@link URL}
     * 
     * @param url
     * @param configuration
     */
    public CsvDataContext(URL url, CsvConfiguration configuration) {
        _resource = new UrlResource(url);
        _configuration = configuration;
        _writable = false;
    }

    /**
     * Constructs a CSV DataContext based on a file
     * 
     * @param file
     */
    public CsvDataContext(File file) {
        this(file, new CsvConfiguration());
    }

    /**
     * Constructs a CSV DataContext based on an {@link InputStream}
     * 
     * @param inputStream
     * @param configuration
     */
    public CsvDataContext(InputStream inputStream, CsvConfiguration configuration) {
        File file = createFileFromInputStream(inputStream, configuration.getEncoding());
        _configuration = configuration;
        _writable = false;
        _resource = new FileResource(file);
    }

    /**
     * @deprecated use {@link #CsvDataContext(File, CsvConfiguration)} instead.
     */
    @Deprecated
    public CsvDataContext(File file, char separatorChar) {
        this(file, separatorChar, CsvConfiguration.DEFAULT_QUOTE_CHAR);
    }

    /**
     * @deprecated use {@link #CsvDataContext(File, CsvConfiguration)} instead.
     */
    @Deprecated
    public CsvDataContext(File file, char separatorChar, char quoteChar) {
        this(file, new CsvConfiguration(CsvConfiguration.DEFAULT_COLUMN_NAME_LINE, FileHelper.DEFAULT_ENCODING,
                separatorChar, quoteChar, CsvConfiguration.DEFAULT_ESCAPE_CHAR));
    }

    /**
     * @deprecated use {@link #CsvDataContext(File, CsvConfiguration)} instead.
     */
    @Deprecated
    public CsvDataContext(File file, char separatorChar, char quoteChar, String encoding) {
        this(file, new CsvConfiguration(CsvConfiguration.DEFAULT_COLUMN_NAME_LINE, encoding, separatorChar, quoteChar,
                CsvConfiguration.DEFAULT_ESCAPE_CHAR));
    }

    /**
     * @deprecated use {@link #CsvDataContext(URL, CsvConfiguration)} instead.
     */
    @Deprecated
    public CsvDataContext(URL url, char separatorChar, char quoteChar) {
        this(url, separatorChar, quoteChar, FileHelper.DEFAULT_ENCODING);
    }

    /**
     * @deprecated use {@link #CsvDataContext(URL, CsvConfiguration)} instead.
     */
    @Deprecated
    public CsvDataContext(URL url, char separatorChar, char quoteChar, String encoding) {
        this(url, new CsvConfiguration(CsvConfiguration.DEFAULT_COLUMN_NAME_LINE, encoding, separatorChar, quoteChar,
                CsvConfiguration.DEFAULT_ESCAPE_CHAR));
    }

    /**
     * @deprecated use {@link #CsvDataContext(InputStream, CsvConfiguration)}
     *             instead.
     */
    @Deprecated
    public CsvDataContext(InputStream inputStream, char separatorChar, char quoteChar) {
        this(inputStream, new CsvConfiguration(CsvConfiguration.DEFAULT_COLUMN_NAME_LINE, FileHelper.DEFAULT_ENCODING,
                separatorChar, quoteChar, CsvConfiguration.DEFAULT_ESCAPE_CHAR));
    }

    /**
     * @deprecated use {@link #CsvDataContext(InputStream, CsvConfiguration)}
     *             instead.
     */
    @Deprecated
    public CsvDataContext(InputStream inputStream, char separatorChar, char quoteChar, String encoding) {
        this(inputStream, new CsvConfiguration(CsvConfiguration.DEFAULT_COLUMN_NAME_LINE, encoding, separatorChar,
                quoteChar, CsvConfiguration.DEFAULT_ESCAPE_CHAR));
    }

    /**
     * Gets the CSV configuration used
     * 
     * @return a CSV configuration
     */
    public CsvConfiguration getConfiguration() {
        return _configuration;
    }

    /**
     * Gets the CSV file being read
     * 
     * @return a file
     * 
     * @deprecated use {@link #getResource()} instead.
     */
    @Deprecated
    public File getFile() {
        if (_resource instanceof FileResource) {
            return ((FileResource) _resource).getFile();
        }
        return null;
    }

    /**
     * Gets the resource that is being read from.
     * 
     * @return
     */
    public Resource getResource() {
        return _resource;
    }

    private static File createFileFromInputStream(InputStream inputStream, String encoding) {
        final File file;
        final File tempDir = FileHelper.getTempDir();

        File fileCandidate = null;
        boolean usableName = false;
        int index = 0;

        while (!usableName) {
            index++;
            fileCandidate = new File(tempDir, "metamodel" + index + ".csv");
            usableName = !fileCandidate.exists();
        }
        file = fileCandidate;

        final BufferedWriter writer = FileHelper.getBufferedWriter(file, encoding);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            file.createNewFile();
            file.deleteOnExit();

            boolean firstLine = true;

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (firstLine) {
                    firstLine = false;
                } else {
                    writer.write('\n');
                }
                writer.write(line);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.safeClose(writer, reader);
        }

        return file;
    }

    @Override
    protected Number executeCountQuery(Table table, List<FilterItem> whereItems, boolean functionApproximationAllowed) {
        if (!functionApproximationAllowed) {
            return null;
        }

        return _resource.read(new Func<InputStream, Number>() {
            @Override
            public Number eval(InputStream inputStream) {
                try {
                    final long length = _resource.getSize();
                    // read up to 5 megs of the file and approximate number of
                    // lines
                    // based on that.

                    final int sampleSize = (int) Math.min(length, 1024 * 1024 * 5);
                    final int chunkSize = Math.min(sampleSize, 1024 * 1024);

                    int readSize = 0;
                    int newlines = 0;
                    int carriageReturns = 0;
                    byte[] byteBuffer = new byte[chunkSize];
                    char[] charBuffer = new char[chunkSize];

                    while (readSize < sampleSize) {
                        final int read = inputStream.read(byteBuffer);
                        if (read == -1) {
                            break;
                        } else {
                            readSize += read;
                        }

                        Reader reader = getReader(byteBuffer, _configuration.getEncoding());
                        reader.read(charBuffer);
                        for (char c : charBuffer) {
                            if ('\n' == c) {
                                newlines++;
                            } else if ('\r' == c) {
                                carriageReturns++;
                            }
                        }
                    }

                    int lines = Math.max(newlines, carriageReturns);

                    logger.info("Found {} lines breaks in {} bytes", lines, sampleSize);

                    long approxCount = (long) (lines * length / sampleSize);
                    return approxCount;
                } catch (IOException e) {
                    logger.error("Unexpected error during COUNT(*) approximation", e);
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    private Reader getReader(byte[] byteBuffer, String encoding) throws UnsupportedEncodingException {
        try {
            return new InputStreamReader(new ByteArrayInputStream(byteBuffer), encoding);
        } catch (UnsupportedEncodingException e1) {
            // this may happen on more exotic encodings, but since this reader
            // is only meant for finding newlines, we'll try again with UTF8
            try {
                return new InputStreamReader(new ByteArrayInputStream(byteBuffer), "UTF8");
            } catch (UnsupportedEncodingException e2) {
                throw e1;
            }
        }
    }

    @Override
    public DataSet materializeMainSchemaTable(Table table, Column[] columns, int maxRows) {
        final int lineNumber = _configuration.getColumnNameLineNumber();
        final CSVReader reader = createCsvReader(lineNumber);
        final int columnCount = table.getColumnCount();
        final boolean failOnInconsistentRowLength = _configuration.isFailOnInconsistentRowLength();
        if (maxRows < 0) {
            return new CsvDataSet(reader, columns, null, columnCount, failOnInconsistentRowLength);
        } else {
            return new CsvDataSet(reader, columns, maxRows, columnCount, failOnInconsistentRowLength);
        }
    }

    protected CSVReader createCsvReader(int skipLines) {
        final Reader fileReader = FileHelper.getReader(_resource.read(), _configuration.getEncoding());
        final CSVReader csvReader = new CSVReader(fileReader, _configuration.getSeparatorChar(),
                _configuration.getQuoteChar(), _configuration.getEscapeChar(), skipLines);
        return csvReader;
    }

    @Override
    protected CsvSchema getMainSchema() throws MetaModelException {
        CsvSchema schema = new CsvSchema(getMainSchemaName(), this);
        if (_resource.isExists()) {
            schema.setTable(new CsvTable(schema));
        }
        return schema;
    }

    @Override
    protected String getMainSchemaName() {
        return _resource.getName();
    }

    protected boolean isWritable() {
        return _writable;
    }

    private void checkWritable() {
        if (!isWritable()) {
            throw new IllegalStateException(
                    "This CSV DataContext is not writable, as it based on a read-only resource.");
        }
    }

    @Override
    public void executeUpdate(UpdateScript update) {
        checkWritable();
        CsvUpdateCallback callback = new CsvUpdateCallback(this);
        synchronized (WRITE_LOCK) {
            try {
                update.run(callback);
            } finally {
                callback.close();
            }
        }
    }
}