package com.viral32111.locationsharing;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Event {
	public int Identifier;
	public String Condition;
	public String Action;

	public int TargetIdentifier;
	public String TargetName;

	public Bookmark Bookmark;

	public Event( int eventIdentifier, String eventCondition, String eventAction, int targetIdentifier, String targetName, int bookmarkIdentifier, String bookmarkName, int bookmarkRadius, double bookmarkLatitude, double bookmarkLongitude ) {
		Identifier = eventIdentifier;
		Condition = eventCondition;
		Action = eventAction;

		TargetIdentifier = targetIdentifier;
		TargetName = targetName;

		Bookmark = new Bookmark( bookmarkIdentifier, bookmarkName, bookmarkRadius, bookmarkLatitude, bookmarkLongitude );
	}

	public static void Fetch( int userIdentifier, EventCallback callback ) {
		ArrayList<Event> events = new ArrayList<>();

		Database.Query( "SELECT Events.Identifier AS EventIdentifier, Events.Condition AS EventCondition, 'Notification' AS EventAction, Events.Target AS TargetIdentifier, Users.Name AS TargetName, Events.Bookmark AS BookmarkIdentifier, Bookmarks.Name AS BookmarkName, Bookmarks.Radius AS BookmarkRadius, CAST( AES_DECRYPT( Latitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS BookmarkLatitude, CAST( AES_DECRYPT( Longitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS BookmarkLongitude FROM Events INNER JOIN Bookmarks ON Bookmarks.Identifier = Events.Bookmark INNER JOIN Users ON Users.Identifier = Events.Target WHERE Bookmarks.User = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				Database.InsertEncryptionKey( statement, new int[] { 1, 2 } );
				statement.setInt( 3, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				while ( results.next() ) {
					int eventIdentifier = results.getInt( "EventIdentifier" );
					String eventCondition = results.getString( "EventCondition" );
					String eventAction = results.getString( "EventAction" );

					int targetIdentifier = results.getInt( "TargetIdentifier" );
					String targetName = results.getString( "TargetName" );

					int bookmarkIdentifier = results.getInt( "BookmarkIdentifier" );
					String bookmarkName = results.getString( "BookmarkName" );
					int bookmarkRadius = results.getInt( "BookmarkRadius" );
					double bookmarkLatitude = results.getDouble( "BookmarkLatitude" );
					double bookmarkLongitude = results.getDouble( "BookmarkLongitude" );

					events.add( new Event( eventIdentifier, eventCondition, eventAction, targetIdentifier, targetName, bookmarkIdentifier, bookmarkName, bookmarkRadius, bookmarkLatitude, bookmarkLongitude ) );
				}

				callback.OnComplete( events );
			}

			@Override
			public void OnException( Exception exception ) {
				callback.OnException( exception );
			}
		} );
	}
}
