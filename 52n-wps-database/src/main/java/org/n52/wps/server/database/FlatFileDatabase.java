package org.n52.wps.server.database;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.server.database.configuration.DatabaseConfiguration;
import org.n52.wps.server.database.configuration.FlatFileDatabaseConfiguration;

import com.google.common.io.ByteStreams;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class FlatFileDatabase extends WipingDatabase {
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEXT_XML = "text/xml";
    private static final String ZIPPED_DATA_EXTENSION = ".data.gz";
    private static final String PLAIN_DATA_EXTENSION = ".data";
    private static final String PROPERTIES_EXTENSION = ".properties";
    private static final Logger LOGGER = LoggerFactory
            .getLogger(FlatFileDatabase.class);

    private Path requestPath;
    private Path responsePath;
    private Path rawDataPath;
    private boolean gzip;

    @Override
    public void init(DatabaseConfiguration conf)
            throws DatabaseInitializationException {
        if (!(conf instanceof FlatFileDatabaseConfiguration)) {
            throw new DatabaseInitializationException("Invalid database configuration");
        }
        FlatFileDatabaseConfiguration configuration
                = (FlatFileDatabaseConfiguration) conf;
        Path basePath = Paths.get(configuration.getPath());
        this.requestPath = createDirectory(basePath, "requests");
        this.responsePath = createDirectory(basePath, "responses");
        this.rawDataPath = createDirectory(basePath, "rawdata");
        this.gzip = configuration.isGZIP();
        super.init(configuration);
    }

    protected Path createDirectory(Path basePath, String directory)
            throws DatabaseInitializationException {
        Path file = basePath.resolve(directory);
        if (!Files.exists(file)) {
            LOGGER.debug("Creating directory {} for {}", file, directory);
            try {
                Files.createDirectories(file);
            } catch (IOException e) {
                throw new DatabaseInitializationException(
                        "Could not create direcory " + file, e);
            }

        } else if (!Files.isDirectory(file)) {
            throw new DatabaseInitializationException(
                    file + " exists, but is not a directory");
        } else {
            LOGGER.debug("Directory {} for {} already exists", file, directory);
        }
        return file;
    }

    protected Entry getResponseEntry(String id) {
        return new Entry(responsePath, id);
    }

    protected Entry getRequestEntry(String id) {
        return new Entry(requestPath, id);
    }

    protected Entry getRawDataEntry(String id) {
        return new Entry(rawDataPath, id);
    }

    @Override
    public void insertRequest(String id, InputStream request, boolean xml) {
        getRequestEntry(id).create(request, xml ? TEXT_XML : TEXT_PLAIN);
    }

    @Override
    public void insertResponse(String id, InputStream response) {
        getRequestEntry(id).create(response, TEXT_XML);
    }

    @Override
    public void updateResponse(String id, InputStream response) {
        getRequestEntry(id).replace(response);
    }

    @Override
    public void storeResponse(String id, InputStream response) {
        Entry e = getResponseEntry(id);
        if (e.exists()) {
            e.replace(response);
        } else {
            e.create(response, TEXT_XML);
        }
    }

    @Override
    public InputStream getRequest(String id) {
        return getRequestEntry(id).getDataStream();
    }

    @Override
    public InputStream getResponse(String id) {
        return getResponseEntry(id).getDataStream();
    }

    @Override
    public void storeRawData(String id, InputStream stream, String mimeType) {
        Entry e = getRawDataEntry(id);
        if (e.exists()) {
            e.replace(stream);
        } else {
            e.create(stream, mimeType);
        }
    }

    @Override
    public String getMimeTypeForResponse(String id) {
        return getResponseEntry(id).getMimeType();
    }

    @Override
    public long getContentLengthForResponse(String id) {
        return getResponseEntry(id).getSize();
    }

    @Override
    public File getRequestAsFile(String id) {
        return getRequestEntry(id).getDataFile();
    }

    @Override
    public File getResponseAsFile(String id) {
        return getResponseEntry(id).getDataFile();
    }

    @Override
    protected List<String> findOldRequests(long currentTime, long threshold) {
        return findOld(requestPath, currentTime, threshold);
    }

    @Override
    protected List<String> findOldResponses(long currentTime, long threshold) {
        return findOld(responsePath, currentTime, threshold);
    }

    @Override
    protected List<String> findOldComplexValues(long currentTime, long threshold) {
        return findOld(rawDataPath, currentTime, threshold);
    }

    @Override
    protected void deleteRequests(List<String> ids) {
        delete(requestPath, ids);
    }

    @Override
    protected void deleteResponses(List<String> ids) {
        delete(responsePath, ids);
    }

    @Override
    protected void deleteComplexValues(List<String> ids) {
        delete(rawDataPath, ids);
    }

    @Override
    public InputStream getRawData(String id) {
        return getRawDataEntry(id).getDataStream();
    }

    @Override
    public File getRawDataAsFile(String id) {
        return getRawDataEntry(id).getDataFile();
    }

    @Override
    public void insertRawData(String id, InputStream stream, String mimeType) {
        getRawDataEntry(id).create(stream, mimeType);
    }

    @Override
    public void updateRawData(String id, InputStream stream) {
        getRawDataEntry(id).replace(stream);
    }

    @Override
    public String getMimeTypeForRequest(String id) {
        return getRequestEntry(id).getMimeType();
    }

    @Override
    public String getMimeTypeForRawData(String id) {
        return getRawDataEntry(id).getMimeType();
    }

    @Override
    public long getContentLengthForRequest(String id) {
        return getRequestEntry(id).getSize();
    }

    @Override
    public long getContentLengthForRawData(String id) {
        return getRawDataEntry(id).getSize();
    }

    protected void delete(Path directory, List<String> ids) {
        if (ids != null) {
            for (String id : ids) {
                new Entry(directory, id).delete();
            }
        }
    }

    protected List<String> findOld(Path directory, long currentTime, long threshold) {
        final long time = currentTime - threshold;
        Filter<Path> filter = getPropertiesFileFilter(time);
        List<String> ids = new LinkedList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, filter)) {
            for (Path propertyFile : stream) {
                 String name = propertyFile.getFileName().toString();
                 ids.add(name.substring(0, name.length() - PROPERTIES_EXTENSION.length()));
            }
        } catch(IOException e) {
            LOGGER.error("Error reading directory contents", e);
        }
        return ids;
    }

    private Filter<Path> getPropertiesFileFilter(final long time) {
        return new OldPropertiesFileFilter(time);
    }

    protected class OldPropertiesFileFilter implements Filter<Path> {
        private final long time;

        protected OldPropertiesFileFilter(long time) {
            this.time = time;
        }
        @Override
        public boolean accept(Path entry)
                throws IOException {
            if (!entry.getFileName().toString().endsWith(PROPERTIES_EXTENSION)) {
                return false;
            }
            BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class);
            return attributes.isRegularFile() && attributes.creationTime().toMillis() < time;
        }
    }

    protected class Entry {
        public static final String MIME_TYPE_PROPERTY = "mimeType";
        private final boolean zipped;
        private final Path data;
        private final Path properities;
        private boolean exists;
        private final String id;

        protected Entry(Path parent, String id) {
            this.id = id;
            Path plainData = parent.resolve(id + PLAIN_DATA_EXTENSION);
            Path zippedData = parent.resolve(id + ZIPPED_DATA_EXTENSION);
            this.properities = parent.resolve(id + PROPERTIES_EXTENSION);
            if (Files.exists(plainData)) {
                this.zipped = false;
                this.exists = true;
            } else if (Files.exists(zippedData)) {
                this.zipped = true;
                this.exists = true;
            } else {
                this.zipped = gzip;
                this.exists = false;
            }
            this.data = zipped ? zippedData : plainData;
        }
        protected Path getPropertiesPath() {
            return this.properities;
        }

        protected Path getDataPath() {
            return this.data;
        }

        public File getDataFile() {
            if (this.zipped) {
                try {
                    Path file =  Files.createTempFile("id", ".tmp");
                    try (InputStream in = Files.newInputStream(getDataPath());
                         GZIPInputStream gzip = new GZIPInputStream(in);
                         OutputStream out = Files.newOutputStream(file)) {
                        ByteStreams.copy(gzip, out);
                    }
                    return file.toFile();
                } catch (IOException ex) {
                    LOGGER.error("Error copying gzipped data file " +
                                 getDataPath(), ex);
                    return null;
                }
            } else {
                return getDataPath().toFile();
            }
        }

        public InputStream getDataStream() {
            try {
                InputStream in = Files.newInputStream(getDataPath(),
                                                      StandardOpenOption.READ);
                if (this.zipped) {
                    in = new GZIPInputStream(in);
                }
                return in;
            } catch (IOException ex) {
                LOGGER.error("Error reading data file " + getDataPath(), ex);
                return null;
            }
        }

        protected Properties getProperties() {
            Properties properties = new Properties();
            Path p = getPropertiesPath();
            if (Files.exists(p)) {
                try (InputStream in = Files
                        .newInputStream(p, StandardOpenOption.READ)) {
                    properties.load(in);
                } catch (IOException e) {
                    LOGGER.info("Error loading properties file " + p, e);
                }
            }
            return properties;
        }

        public String getMimeType() {
            return getProperties().getProperty(MIME_TYPE_PROPERTY, null);
        }

        public long getSize() {
            try {
                if (this.exists && !this.zipped) {
                    return Files.size(this.data);
                }
            } catch (IOException ex) {
                LOGGER.error("Error determing the size of " + getDataPath(), ex);
            }
            return -1;
        }

        public boolean exists() {
            return this.exists;
        }

        public void delete() {
            delete(this.properities);
            delete(this.data);
            exists = false;
        }

        protected void delete(Path file) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                LOGGER.error("Error deleting file " + file, e);
            }
        }

        public void create(InputStream payload, String mimeType) {
            synchronized (this) {
                if (this.exists) {
                    throw new RuntimeException("File already exists!");
                }
                try {
                    Files.createFile(this.data);
                    Files.createFile(this.properities);
                    write(this.data, payload);
                    Properties p = new Properties();
                    p.put(MIME_TYPE_PROPERTY, mimeType);
                    try (OutputStream out = Files.newOutputStream(this.properities)) {
                        p.store(out, "");
                    }
                    this.exists = true;
                } catch (IOException ex) {
                    LOGGER.error("Error writing file " + this.data, ex);
                }
            }
        }

        public void replace(InputStream payload) {
            synchronized (this) {
                if (!this.exists) {
                    throw new RuntimeException("File does not exist");
                }
                try {
                    Path tmp = Files.createTempFile("file-database-", ".tmp");
                    write(tmp, payload);
                    Files.move(tmp, this.data, StandardCopyOption.REPLACE_EXISTING,
                                               StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException ex) {
                    LOGGER.error("Error writing file " + this.data, ex);
                }
            }
        }

        protected void write(Path destination, InputStream payload)
                throws IOException {
            try (InputStream in = payload) {
                if (this.zipped) {
                    try (OutputStream out = Files.newOutputStream(destination);
                            GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                        ByteStreams.copy(in, gzip);
                    }
                } else {
                    try (OutputStream out = Files.newOutputStream(destination)) {
                        ByteStreams.copy(in, out);
                    }
                }
            }
        }
    }
}
