package org.petos.packagemanager.server;
import java.sql.*;

public class SQLConnector {

public static void main(String[] args) {
      try{
	    Class.forName("org.hsqldb.jdbc.JDBCDriver" );
	    Connection c = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/xdb", "server", "UnixOneLove");
	    Statement statement = c.createStatement( );
	    ResultSet resultSet = statement.executeQuery("SELECT * FROM LICENCES");
	    while (resultSet.next()) {
		  System.out.println(resultSet.getString("name") + " " + resultSet.getString("password"));
	    }
      } catch (SQLException e) {
	    throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
	    throw new RuntimeException(e);
      }
}

}
