package org.n52.wps.server.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.server.database.configuration.DatabaseConfiguration;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class AbstractSQLDatabase extends WipingDatabase {
    private static final String TABLE_DEFINITION
            = "CREATE TABLE %s (ID VARCHAR(100) NOT NULL PRIMARY KEY, TIME TIMESTAMP, PAYLOAD CLOB, MIME_TYPE VARCHAR(100));";
    private static final String TABLE_REQUEST = "REQUEST";
    private static final String TABLE_RESPONSE = "RESPONSE";
    private static final String TABLE_RAW_DATA = "RAW_DATA";
    private static final String TEXT_XML = "text/xml";
    private static final String TEXT_PLAIN = "text/plain";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractSQLDatabase.class);

    private ConnectionSupplier connectionSupplier;

    @Override
    public void init(DatabaseConfiguration conf) throws DatabaseInitializationException {
        DatabaseConfiguration.Driver configuration = (DatabaseConfiguration.Driver) conf;
        loadDriver(configuration);

        Optional<String> username;
        Optional<String> password;
        if (configuration instanceof DatabaseConfiguration.Credentials) {
            DatabaseConfiguration.Credentials credential
                    = (DatabaseConfiguration.Credentials) configuration;
            username = credential.getUsername();
            password = credential.getPassword();
        } else {
            username = Optional.absent();
            password = Optional.absent();
        }
        Optional<String> connectionURL = checkJDBC(configuration);
        Optional<DataSource> datasource = checkJNDI(configuration);

        if (datasource.isPresent()) {
            LOGGER.info("Using JNDI connection");
            this.connectionSupplier = new DataSourceConnectionSupplier(datasource.get(), username, password);
        } else if (connectionURL.isPresent()) {
            LOGGER.info("Using JDBC connection URL {}", connectionURL.get());
            this.connectionSupplier = new JdbcUrlConnectionSupplier(connectionURL.get(), username, password);
        } else {
            throw new DatabaseInitializationException("No JNDI name or JDBC connection URL was supplied");
        }

        if (!createResultTable()) {
            throw new DatabaseInitializationException("Creating result table failed.");
        }
        super.init(configuration);
    }

    private Optional<String> checkJDBC(DatabaseConfiguration.Driver configuration) {
        if (configuration instanceof DatabaseConfiguration.JDBC) {
            DatabaseConfiguration.JDBC jdbc = (DatabaseConfiguration.JDBC) configuration;
            return Optional.of(jdbc.createConnectionURL());
        } else {
            return Optional.absent();
        }
    }

    private Optional<DataSource> checkJNDI(DatabaseConfiguration.Driver configuration)
            throws DatabaseInitializationException {
        if (configuration instanceof DatabaseConfiguration.JNDI) {
            DatabaseConfiguration.JNDI jndi = (DatabaseConfiguration.JNDI) configuration;
            String jndiName = "java:comp/env/jdbc/" + jndi.getJNDIName();
            try {
                InitialContext ctx = new InitialContext();
                return Optional.of((DataSource) ctx.lookup(jndiName));
            } catch (NamingException ex) {
                throw new DatabaseInitializationException(ex);
            }
        } else {
            return Optional.absent();
        }
    }

    private void loadDriver(DatabaseConfiguration.Driver configuration)
            throws DatabaseInitializationException {
        try {
            Class.forName(configuration.getDriverClass());
            LOGGER.debug("Connected to Database using {} driver", configuration .getDriverClass());
        } catch (ClassNotFoundException e) {
            LOGGER.error("Database driver cannot be loaded: " + configuration.getDriverClass(), e);
            throw new DatabaseInitializationException("The database class could not be loaded.", e);
        }
    }

    protected Connection getConnection()
            throws SQLException {
        try {
            LOGGER.info("Connecting to WPS database.");
            return this.connectionSupplier.get();
        } catch (SQLException e) {
            LOGGER.error("Could not connect to or create the database.", e);
            throw e;
        }
    }

    protected void returnConnection(Connection conn) {
        try {
            conn.close();
        } catch (SQLException ex) {
            LOGGER.error("Error closing connection", ex);
        }
    }

    private boolean createResultTable() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            return checkTable(conn, TABLE_REQUEST) &&
                   checkTable(conn, TABLE_RESPONSE) &&
                   checkTable(conn, TABLE_RAW_DATA);
        } catch (SQLException e) {
            LOGGER.error("Connection to the Derby database failed.", e);
        }
        return false;
    }

    private boolean existsTable(String name, DatabaseMetaData metaData)
            throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, name, new String[] {
            "TABLE" })) {
            return rs.next();
        }
    }

    private boolean checkTable(Connection conn, String name)
            throws SQLException {
        if (existsTable(name, conn.getMetaData())) {
            return true;
        } else {
            LOGGER.info("Table {} does not yet exist.", name);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(createTableDefinition(name));
                conn.commit();
            }
            if (existsTable(name, conn.getMetaData())) {
                LOGGER.info("Succesfully created table RESULTS.");
                return true;
            } else {
                LOGGER.error("Could not create table RESULTS.");
                return false;
            }
        }
    }

    @Override
    public void insertRequest(String id, InputStream request, boolean xml) {
        try {
            insertEntity(TABLE_REQUEST, id, request, xml ? TEXT_XML : TEXT_PLAIN);
        } catch (SQLException ex) {
            LOGGER.error("Error inserting entity", ex);
        }
    }

    @Override
    public void insertResponse(String id, InputStream response) {
        try {
            insertEntity(TABLE_RESPONSE, id, response, TEXT_XML);
        } catch (SQLException ex) {
            LOGGER.error("Error inserting entity", ex);
        }
    }

    @Override
    public void updateResponse(String id, InputStream response) {
        try {
            updateEntity(TABLE_RESPONSE, id, response);
        } catch (SQLException ex) {
            LOGGER.error("Error updating entity", ex);
        }
    }

    @Override
    public void storeResponse(String id, InputStream response) {
        storeEntity(TABLE_RESPONSE, id, response, TEXT_XML);
    }

    @Override
    public void storeRawData(String id, InputStream stream, String mimeType) {
        storeEntity(TABLE_RAW_DATA, id, stream, mimeType);
    }

    private void storeEntity(String table, String id, InputStream response,
                             String mimeType) {
        try {
            if (hasEntity(table, id)) {
                updateEntity(table, id, response);
            } else {
                insertEntity(table, id, response, mimeType);
            }
        } catch (SQLException ex) {
            LOGGER.error("Error storing entity", ex);
        }
    }

     @Override
    public InputStream getRawData(String id) {
        try {
            return getEntity(TABLE_RAW_DATA, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity", ex);
            return null;
        }
    }

    @Override
    public File getRawDataAsFile(String id) {
        try {
            return getEntityAsFile(TABLE_RAW_DATA, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity", ex);
            return null;
        }
    }

    @Override
    public void insertRawData(String id, InputStream stream, String mimeType) {
        try {
            insertEntity(TABLE_RAW_DATA, id, stream, mimeType);
        } catch (SQLException ex) {
            LOGGER.error("Error inserting entity", ex);
        }
    }

    @Override
    public void updateRawData(String id, InputStream stream) {
        try {
            updateEntity(TABLE_RAW_DATA, id, stream);
        } catch (SQLException ex) {
            LOGGER.error("Error updating entity", ex);
        }
    }

    @Override
    public String getMimeTypeForRequest(String id) {
        try {
            return getMimeType(TABLE_REQUEST, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity mimeType", ex);
            return null;
        }
    }

    @Override
    public String getMimeTypeForRawData(String id) {
        try {
            return getMimeType(TABLE_RAW_DATA, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity mimeType", ex);
            return null;
        }
    }

    @Override
    public long getContentLengthForRequest(String id) {
        try {
            return getContentLength(TABLE_REQUEST, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity length", ex);
            return -1;
        }
    }

    @Override
    public long getContentLengthForRawData(String id) {
        try {
            return getContentLength(TABLE_RAW_DATA, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity length", ex);
            return -1;
        }
    }


    @Override
    public InputStream getRequest(String id) {
        try {
            return getEntity(TABLE_REQUEST, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity", ex);
            return null;
        }
    }

    @Override
    public InputStream getResponse(String id) {
        try {
            return getEntity(TABLE_RESPONSE, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity", ex);
            return null;
        }
    }

    @Override
    public String getMimeTypeForResponse(String id) {
        try {
            return getMimeType(TABLE_RESPONSE, id);
        } catch (SQLException ex) {
            LOGGER.error("Error getting mimeType of entity", ex);
            return null;
        }
    }

    @Override
    public File getRequestAsFile(String id) {
        try {
            return getEntityAsFile(TABLE_REQUEST, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity as file", ex);
            return null;
        }
    }

    @Override
    public File getResponseAsFile(String id) {
        try {
            return getEntityAsFile(TABLE_RESPONSE, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity as file", ex);
            return null;
        }
    }

    @Override
    public long getContentLengthForResponse(String id) {
        try {
            return getContentLength(TABLE_RESPONSE, id);
        } catch (SQLException ex) {
            LOGGER.error("Error requesting entity length", ex);
            return -1;
        }
    }

    protected File getEntityAsFile(String table, String id)
            throws SQLException {
        InputStream e = getEntity(table, id);
        if (e == null) {
            return null;
        }
        try {
            File file = File.createTempFile(id, ".tmp");
            file.deleteOnExit();
            try (InputStream in = e;
                 OutputStream out = new FileOutputStream(file)) {
                ByteStreams.copy(in, out);
            }
            return file;
        } catch (IOException ex) {
            LOGGER.error("Error creating entity as file", ex);
            return null;
        }
    }

    protected long getContentLength(String table, String id)
            throws SQLException {
        return -1;
    }

    @Override
    protected void deleteComplexValues(List<String> ids) {
        try {
            deleteEntities(TABLE_RAW_DATA, ids);
        } catch (SQLException ex) {
            LOGGER.error("Could not delete old complex values", ex);
        }
    }

    @Override
    protected void deleteRequests(List<String> ids) {
        try {
            deleteEntities(TABLE_REQUEST, ids);
        } catch (SQLException ex) {
            LOGGER.error("Could not delete old requests", ex);
        }
    }

    @Override
    protected void deleteResponses(List<String> ids) {
        try {
            deleteEntities(TABLE_RESPONSE, ids);
        } catch (SQLException ex) {
            LOGGER.error("Could not delete old responses", ex);
        }
    }

    @Override
    protected List<String> findOldComplexValues(long currentTime, long threshold) {
        try {
            return findOldEntities(TABLE_RAW_DATA, currentTime, threshold);
        } catch (SQLException e) {
            LOGGER.error("Could not find old complex values", e);
            return null;
        }
    }

    @Override
    protected List<String> findOldRequests(long currentTime, long threshold) {
        try {
            return findOldEntities(TABLE_REQUEST, currentTime, threshold);
        } catch (SQLException e) {
            LOGGER.error("Could not find old requests", e);
            return null;
        }
    }

    @Override
    protected List<String> findOldResponses(long currentTime, long threshold) {
        try {
            return findOldEntities(TABLE_RESPONSE, currentTime, threshold);
        } catch (SQLException e) {
            LOGGER.error("Could not find old responses", e);
            return null;
        }
    }

    protected String createTableDefinition(String name) {
        return String.format(TABLE_DEFINITION, name);
    }

    private Timestamp currentTime() {
        return new Timestamp(System.currentTimeMillis());
    }

    protected void insertEntity(String table, String id, InputStream entity, String mimeType)
            throws SQLException {
        String sql = "INSERT INTO " + table + " VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement insert = conn.prepareStatement(sql)) {
            insert.setString(1, id);
            insert.setTimestamp(2, currentTime());
            insert.setAsciiStream(3, entity);
            insert.setString(4, mimeType);
            insert.executeUpdate();
            conn.commit();
        }
    }

    protected void updateEntity(String table, String id, InputStream entity)
            throws SQLException {
        String sql = "UPDATE " + table + " SET PAYLOAD = (?) WHERE ID = (?)";
        Connection conn = getConnection();
        try (PreparedStatement select = conn.prepareStatement(sql)) {
            select.setAsciiStream(1, entity);
            select.setString(2, id);
            select.executeUpdate();
        } finally {
            returnConnection(conn);
        }
    }

    protected boolean hasEntity(String table, String id)
            throws SQLException {
        String sql = "SELECT ID FROM " + table + " WHERE ID = (?)";
        Connection conn = getConnection();
        try (PreparedStatement select = conn.prepareStatement(sql)) {
            select.setString(1, id);
            try (ResultSet rs = select.executeQuery()) {
                return rs != null && rs.next();
            }
        } finally {
            returnConnection(conn);
        }
    }

    protected InputStream getEntity(String table, String id)
            throws SQLException {
        String sql = "SELECT PAYLOAD FROM " + table + " WHERE ID = (?)";
        Connection conn = getConnection();
        try (PreparedStatement select = conn.prepareStatement(sql)) {
            select.setString(1, id);
            try (ResultSet rs = select.executeQuery()) {
                if (rs == null || !rs.next()) {
                    return null;
                } else {
                    return rs.getAsciiStream(1);
                }
            }
        } finally {
            returnConnection(conn);
        }
    }

    protected String getMimeType(String table, String id)
            throws SQLException {
        String sql = "SELECT MIME_TYPE FROM " + table + " WHERE ID = (?)";
        Connection conn = getConnection();
        try (PreparedStatement select = conn.prepareStatement(sql)) {
            select.setString(1, id);
            try (ResultSet rs = select.executeQuery()) {
                if (rs == null || !rs.next()) {
                    return null;
                } else {
                    return rs.getString(1);
                }
            }
        } finally {
            returnConnection(conn);
        }
    }

    protected List<String> findOldEntities(String table, long currentTime,
                                           long threshold)
            throws SQLException {
        String sql = "SELECT ID FROM " + table + "WHERE TIME < (?)";
        Connection conn = getConnection();
        try {
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setTimestamp(1, new Timestamp(currentTime - threshold));
                try (ResultSet rs = st.executeQuery()) {
                    if (rs == null) {
                        return Collections.emptyList();
                    } else {
                        List<String> ids = new LinkedList<>();
                        while (rs.next()) {
                            ids.add(rs.getString(1));
                        }
                        return ids;
                    }
                }
            }
        } finally {
            returnConnection(conn);
        }
    }

    protected void deleteEntities(String table, List<String> ids)
            throws SQLException {
        String sql = "DELETE FROM " + table + " WHERE ID IN (?)";
        Connection conn = getConnection();
        try {
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setArray(1, conn.createArrayOf("STRING", ids.toArray()));
                st.executeUpdate();
            }
        } finally {
            returnConnection(conn);
        }
    }

    private static class DataSourceConnectionSupplier implements ConnectionSupplier {
        private final DataSource datasource;
        private final Optional<String> username;
        private final Optional<String> password;

        DataSourceConnectionSupplier(DataSource datasource,
                                     Optional<String> username,
                                     Optional<String> password) {
            this.datasource = datasource;
            this.username = username;
            this.password = password;
        }

        @Override
        public Connection get() throws SQLException {
            if (username.isPresent()) {
                return datasource.getConnection(username.get(), password.or(""));
            } else {
                return datasource.getConnection();
            }
        }
    }

    private static class JdbcUrlConnectionSupplier implements ConnectionSupplier {
        private final String connectionUrl;
        private final Optional<String> username;
        private final Optional<String> password;

        JdbcUrlConnectionSupplier(String connectionUrl,
                                  Optional<String> username,
                                  Optional<String> password) {
            this.connectionUrl = connectionUrl;
            this.username = username;
            this.password = password;
        }

        @Override
        public Connection get() throws SQLException {
            if (username.isPresent()) {
                return DriverManager.getConnection(connectionUrl, username.get(), password.or(""));
            } else {
                return DriverManager.getConnection(connectionUrl);
            }
        }
    }

    private interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

}
