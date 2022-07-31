package com.viral32111.locationsharing;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.util.LengthUnit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Bookmark {
	public int Identifier;
	public String Name;
	public int Radius; // Meters
	public LatLng Position;

	public Bookmark( int identifier, String name, int radius, double latitude, double longitude ) {
		Identifier = identifier;
		Name = name;
		Radius = radius;
		Position = new LatLng( latitude, longitude );
	}

	// https://github.com/JavadocMD/simplelatlng/wiki#distance-between-two-points
	public boolean IsWithinRangeOf( double latitude, double longitude ) {
		LatLng theirPosition = new LatLng( latitude, longitude );

		double distanceInMeters = com.javadocmd.simplelatlng.LatLngTool.distance( this.Position, theirPosition, LengthUnit.METER );

		return ( distanceInMeters <= this.Radius );
	}

	public static void Fetch( int userIdentifier, BookmarkCallback callback ) {
		ArrayList<Bookmark> bookmarks = new ArrayList<>();

		Database.Query( "SELECT Identifier, Name, Radius, CAST( AES_DECRYPT( Latitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Latitude, CAST( AES_DECRYPT( Longitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Longitude FROM Bookmarks WHERE User = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				Database.InsertEncryptionKey( statement, new int[] { 1, 2 } );
				statement.setInt( 3, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				while ( results.next() ) {
					int identifier = results.getInt( "Identifier" );
					String name = results.getString( "Name" );
					int radius = results.getInt( "Radius" );
					double latitude = results.getDouble( "Latitude" );
					double longitude = results.getDouble( "Longitude" );

					bookmarks.add( new Bookmark( identifier, name, radius, latitude, longitude ) );
				}

				callback.OnComplete( bookmarks );
			}

			@Override
			public void OnException( Exception exception ) {
				callback.OnException( exception );
			}
		} );
	}
}
