package com.viral32111.locationsharing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {

	private static String connectionUri = "";
	private static String userName = "";
	private static String userPassword = "";

	private static String encryptionKey = "";

	public static void Initialise( String uri, String name, String password, String key ) {
		connectionUri = uri;
		userName = name;
		userPassword = password;
		encryptionKey = key;
	}

	public static void Query( String sql, DatabaseCallback callback ) {
		if ( connectionUri.isEmpty() || userName.isEmpty() || userPassword.isEmpty() ) {
			callback.OnException( new Exception( "Database class has not yet been initialised!" ) );
			return;
		}

		Thread queryThread = new Thread( () -> {
			try {
				// https://www.mysqltutorial.org/mysql-jdbc-tutorial/
				Connection connection = DriverManager.getConnection( "jdbc:mysql://" + connectionUri, userName, userPassword );

				PreparedStatement statement = callback.OnPopulate( connection.prepareStatement( sql ) );

				if ( sql.startsWith( "SELECT" ) || sql.startsWith( "SHOW" )  ) {
					callback.OnComplete( statement.executeQuery() );
				} else if ( sql.startsWith( "INSERT" ) ) {
					int resultCount = statement.executeUpdate();

					// https://stackoverflow.com/a/5432844
					ResultSet results = connection.createStatement().executeQuery( "SELECT LAST_INSERT_ID() AS Identifier;" );

					callback.OnComplete( resultCount, results );
				} else {
					callback.OnComplete( statement.executeUpdate() );
				}

				connection.close();
			} catch ( SQLException exception ) {
				callback.OnException( exception );
			}
		}, "DatabaseQuery" );

		queryThread.start();
	}

	public static void InsertEncryptionKey( PreparedStatement statement, int position ) throws SQLException {
		statement.setString( position, encryptionKey );
	}

	public static void InsertEncryptionKey( PreparedStatement statement, int[] positions ) throws SQLException {
		for ( int position : positions ) statement.setString( position, encryptionKey );
	}

}
