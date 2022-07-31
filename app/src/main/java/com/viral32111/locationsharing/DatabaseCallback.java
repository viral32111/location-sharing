package com.viral32111.locationsharing;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DatabaseCallback {
	
	default PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
		return statement;
	};

	default void OnComplete( ResultSet results ) throws SQLException { }
	default void OnComplete( int resultCount, ResultSet results ) throws SQLException { };
	default void OnComplete( int resultCount ) { };

	void OnException( Exception exception );
	
}
