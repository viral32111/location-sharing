package com.viral32111.locationsharing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Shared {

	public static String logTag = "LocationSharing"; // Can be anything, used by logcat to filter messages

	public static long OneMonth = 2592000;

	private static SharedPreferences credentials;
	private static final SecureRandom secureRandom = new SecureRandom();

	private static void setupCredentials( Activity activity ) {
		// https://developer.android.com/training/data-storage/shared-preferences
		credentials = activity.getSharedPreferences( "credentials", Context.MODE_PRIVATE );
	}

	// https://www.baeldung.com/java-password-hashing
	public static byte[] hashPassword( String password, byte[] salt ) throws NoSuchAlgorithmException, InvalidKeySpecException {
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
		KeySpec keySpec = new PBEKeySpec( password.toCharArray(), salt, 10000, 512 );

		return keyFactory.generateSecret( keySpec ).getEncoded();
	}

	// https://stackoverflow.com/a/13006907
	public static String bytesToHex( byte[] bytes ) {
		StringBuilder builder = new StringBuilder();

		for ( byte b : bytes ) builder.append( String.format( "%02x", b ) );

		return builder.toString();
	}

	public static void loginUser( Activity activity, int userIdentifier ) {
		if ( credentials == null ) setupCredentials( activity );

		// https://developer.android.com/training/data-storage/shared-preferences#WriteSharedPreference
		SharedPreferences.Editor credentialsEditor = credentials.edit();
		credentialsEditor.putInt( "userIdentifier", userIdentifier );
		credentialsEditor.apply();

		switchActivity( activity, HomeActivity.class );
	}

	public static void logoutUser( Activity activity ) {
		if ( credentials == null ) setupCredentials( activity );

		SharedPreferences.Editor credentialsEditor = credentials.edit();
		credentialsEditor.remove( "userIdentifier" );
		credentialsEditor.apply();

		switchActivity( activity, RegistrationActivity.class );
	}

	public static Integer getLoggedInUser( Activity activity ) {
		if ( credentials == null ) setupCredentials( activity );

		int userIdentifier = credentials.getInt( "userIdentifier", 0 );
		return ( userIdentifier == 0 ? null : userIdentifier );
	}

	// https://stackoverflow.com/a/7325248
	public static void switchActivity( Activity current, Class<?> destination ) {
		current.startActivity( new Intent( current, destination ) );
		current.finish();
	}

	// https://stackoverflow.com/a/7325248
	public static void switchActivityTemporarily( Activity current, Class<?> destination, int userIdentifier ) {
		current.startActivity( new Intent( current, destination )
				.putExtra( "userIdentifier", userIdentifier ) );
	}
	public static void switchActivityTemporarily( Activity current, Class<?> destination, int userIdentifier, int groupIdentifier ) {
		current.startActivity( new Intent( current, destination )
				.putExtra( "userIdentifier", userIdentifier )
				.putExtra( "groupIdentifier", groupIdentifier ) );
	}

	public static String prettyDateTime( Timestamp timestamp ) {
		return new SimpleDateFormat( "dd/MM/yyyy HH:mm", Locale.UK ).format( timestamp );
	}

	// https://stackoverflow.com/a/25903874
	public static String secondsToHumanReadable( long totalSeconds ) {
		long days = totalSeconds / 86400;
		long hours = ( totalSeconds % 86400 ) / 3600;
		long minutes = ( ( totalSeconds % 86400 ) % 3600 ) / 60;
		long seconds = ( ( totalSeconds % 86400 ) % 3600 ) % 60;

		return String.format( Locale.UK, "%dd %dm %dh %ds", days, hours, minutes, seconds );
	}

	// https://stackoverflow.com/a/12962615
	public static String positionToString( double position ) {
		return String.format( Locale.UK, "%-10f", position ).replace( ' ', '0' );
	}

	public static byte[] generateSalt() {
		byte[] salt = new byte[ 16 ];
		secureRandom.nextBytes( salt );
		return salt;
	}

	public static void getUserGroup( Activity activity, int userIdentifier, GenericCallback callback ) {
		Database.Query( "SELECT `Group` FROM Users WHERE Identifier = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					callback.OnCallback( results.getInt( "Group" ) );

				} else {
					Log.d( Shared.logTag, String.format( "Failed to get group for user %d from database", userIdentifier ) );
					activity.runOnUiThread( () -> Popup.Error( activity, R.string.popup_issue, false ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				activity.runOnUiThread( () -> Popup.Error( activity, R.string.popup_issue, false ) );
			}
		} );
	}

	public static void getGroupCreator( Activity activity, int groupIdentifier, GenericCallback callback ) {
		Database.Query( "SELECT Creator FROM Groups WHERE Identifier = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, groupIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					callback.OnCallback( results.getInt( "Creator" ) );

				} else {
					Log.d( Shared.logTag, String.format( "Failed to get creator for group %d from database", groupIdentifier ) );
					activity.runOnUiThread( () -> Popup.Error( activity, R.string.popup_issue, false ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				activity.runOnUiThread( () -> Popup.Error( activity, R.string.popup_issue, false ) );
			}
		} );
	}

	// https://stackoverflow.com/a/157202
	private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static String randomCapitalCharacters( int length ) {
		StringBuilder result = new StringBuilder();
		for ( int iteration = 0; iteration < length ; iteration++ ) result.append( alphabet.charAt( secureRandom.nextInt( alphabet.length() ) ) );
		return result.toString();
	}
}
