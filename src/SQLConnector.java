import java.sql.*;

public class SQLConnector {
    PreparedStatement stm;
    ResultSet res;
    Connection conn;

    SQLConnector(String dbname, String acc, String pw) throws SQLException {
        this.conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/" + dbname + "?serversTimeZone=UTC",
                acc, pw
        );

    }

    public void setStatement(String st) throws SQLException {
        this.stm = this.conn.prepareStatement(st);
        stm.clearParameters();
    }

    // if stm(PreparedStatement) has "?", this may be used helpfully
    public void setStrings(String[] args) throws SQLException {
        for (int i = 1, z = args.length; i <= z; i++) {
            stm.setString(i, args[i]);
        }
        stm.executeUpdate();
    }

    public ResultSet getResultSet() throws SQLException {
        this.res = this.stm.executeQuery();
        return res;
    }

    public void update() throws SQLException {
        // don't call this method if the statement is "SELECT"
        if (this.stm.toString().charAt(0) == 'S') {
            throw new SQLException("Can't call execute() for SELECT statement");
        }

        stm.executeUpdate();
    }

    public void update(String s) throws SQLException {
        // don't call this method if the statement is "SELECT"
        if (s.toString().charAt(0) == 'S') {
            throw new SQLException("Can't call execute() for SELECT statement");
        }

        stm.executeUpdate(s);
    }

}
