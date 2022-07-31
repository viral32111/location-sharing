package com.viral32111.locationsharing;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

public class RegistrationActivity extends AppCompatActivity {

	// Standard
	private final Activity myself = this;

	// UI
	private EditText nameEditText;
	private EditText passwordEditText;
	private EditText passwordConfirmEditText;
	private EditText inviteCodeEditText;
	private CheckBox tempAccountCheckBox;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_registration );

		nameEditText = findViewById( R.id.miscManageNameEditText );
		passwordEditText = findViewById( R.id.registrationPasswordEditText );
		passwordConfirmEditText = findViewById( R.id.registrationPasswordConfirmEditText );
		inviteCodeEditText = findViewById( R.id.registrationInviteCodeEditText );
		tempAccountCheckBox = findViewById( R.id.registrationTempAccountCheckBox );

		try {
			// https://developer.android.com/guide/topics/manifest/meta-data-element
			Bundle bundle = getPackageManager().getApplicationInfo( getPackageName(), PackageManager.GET_META_DATA ).metaData;
			String connectionUri = bundle.getString( "com.viral32111.locationsharing.DATABASE.URI" );
			String userName = bundle.getString( "com.viral32111.locationsharing.DATABASE.USERNAME" );
			String userPassword = bundle.getString( "com.viral32111.locationsharing.DATABASE.PASSWORD" );
			String encryptionKey = bundle.getString( "com.viral32111.locationsharing.ENCRYPTION.KEY" );

			Database.Initialise( connectionUri, userName, userPassword, encryptionKey );
			Log.d( Shared.logTag, "Database initialised" );
		} catch ( PackageManager.NameNotFoundException exception ) {
			Log.e( Shared.logTag, exception.getMessage() );
			Popup.Error( this, R.string.popup_issue, true );
			return;
		}

		if ( Shared.getLoggedInUser( this ) != null ) {
			Log.d( Shared.logTag, "User already logged in, switching to home..." );
			Shared.switchActivity( this, HomeActivity.class );
		}
	}

	public void onLoginButtonClick( View view ) {
		String userName = String.valueOf( nameEditText.getText() );
		String userPasswordAttempt = String.valueOf( passwordEditText.getText() );
		String userPasswordConfirmation = String.valueOf( passwordConfirmEditText.getText() );
		String groupInviteCode = String.valueOf( inviteCodeEditText.getText() );
		boolean isTemporaryAccount = tempAccountCheckBox.isChecked();

		if ( userName.isEmpty() || userName.length() < 3 || userName.length() > 32 ) {
			Log.d( Shared.logTag, "Name is empty or outside of character limit" );
			Popup.Error( this, R.string.popup_name_invalid, false );
			return;
		}

		if ( userPasswordAttempt.isEmpty() || userPasswordAttempt.length() < 8 ) {
			Log.d( Shared.logTag, "Password is empty or below minimum character limit" );
			Popup.Error( this, R.string.popup_password_invalid, false );
			return;
		}

		if ( !userPasswordConfirmation.equals( userPasswordAttempt ) ) {
			Log.d( Shared.logTag, "Passwords do not match" );
			Popup.Error( this, R.string.popup_password_mismatch, false );
			return;
		}

		if ( !groupInviteCode.isEmpty() ) {
			Log.d( Shared.logTag, "Attempt to use group invite code on login" );
			Popup.Error( this, R.string.activity_registration_popup_invitecode_unusable, false );
			return;
		}

		if ( isTemporaryAccount ) {
			Log.d( Shared.logTag, "Attempt to use temporary account on login" );
			Popup.Error( this, R.string.activity_registration_popup_tempaccount_unusable, false );
			return;
		}

		Database.Query( "SELECT Identifier, Password, Salt FROM Users WHERE Name = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setString( 1, userName );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					Log.d( Shared.logTag, String.format( "Found user '%s' in database", userName ) );

					int userIdentifier = results.getInt( "Identifier" );
					byte[] userPasswordHashed = results.getBytes( "Password" );
					byte[] userSalt = results.getBytes( "Salt" );

					byte[] userPasswordAttemptHashed;

					try {
						userPasswordAttemptHashed = Shared.hashPassword( userPasswordAttempt, userSalt );
						Log.d( Shared.logTag, Shared.bytesToHex( userPasswordAttemptHashed ) );
					} catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
						Log.e( Shared.logTag, "No such hashing algorithm, or invalid key specification" );
						// https://stackoverflow.com/a/9815528
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
						return;
					}

					if ( !Arrays.equals( userPasswordAttemptHashed, userPasswordHashed ) ) {
						Log.d( Shared.logTag, "Password is incorrect" );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_password_incorrect, false ) );
						return;
					}

					Shared.loginUser( myself, userIdentifier );
				} else {
					Log.d( Shared.logTag, String.format( "No user exists with name '%s'", userName ) );
					runOnUiThread( () -> Popup.Error( myself, R.string.activity_registration_popup_name_unknown, false ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	public void onRegisterButtonClick( View view ) {
		String userName = String.valueOf( nameEditText.getText() );
		String userPassword = String.valueOf( passwordEditText.getText() );
		String userPasswordConfirmation = String.valueOf( passwordConfirmEditText.getText() );
		String inviteCode = String.valueOf( inviteCodeEditText.getText() );
		boolean isTemporaryAccount = tempAccountCheckBox.isChecked();

		if ( userName.isEmpty() || userName.length() < 3 || userName.length() > 32 ) {
			Log.d( Shared.logTag, "Username is empty or outside of character limit" );
			Popup.Error( this, R.string.popup_name_invalid, false );
			return;
		}

		if ( !isTemporaryAccount && ( userPassword.isEmpty() || userPassword.length() < 8 ) ) {
			Log.d( Shared.logTag, "Password is empty or below minimum character limit" );
			Popup.Error( this, R.string.popup_password_invalid, false );
			return;
		}

		if ( !isTemporaryAccount && ( !userPasswordConfirmation.equals( userPassword ) ) ) {
			Log.d( Shared.logTag, "Passwords do not match" );
			Popup.Error( this, R.string.popup_password_mismatch, false );
			return;
		}

		if ( isTemporaryAccount && !userPassword.isEmpty() ) {
			Log.d( Shared.logTag, "Attempt to provide password in temporary account mode" );
			Popup.Error( this, R.string.activity_registration_popup_password_tempaccount, false );
			return;
		}

		if ( !inviteCode.isEmpty() ) {
			Database.Query( "SELECT `Group` AS Identifier FROM Invites WHERE Code = ?;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					statement.setString( 1, inviteCode );

					return statement;
				}

				@Override
				public void OnComplete( ResultSet results ) throws SQLException {
					if ( results.next() ) {
						int groupIdentifier = results.getInt( "Identifier" );
						Log.d( Shared.logTag, String.format( "Found group %d from invite code %s", groupIdentifier, inviteCode ) );

						registerUser( userName, userPassword, isTemporaryAccount, groupIdentifier );
					} else {
						Log.d( Shared.logTag, String.format( "Failed to find group using invite code %s", inviteCode ) );
						runOnUiThread( () -> Popup.Error( myself, R.string.activity_misc_manage_popup_group_join_code_invalid, false ) );
					}
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} );
		} else {
			registerUser( userName, userPassword, isTemporaryAccount, 0 );
		}
	}

	private void registerUser( String userName, String userPassword, boolean isTemporaryAccount, int groupIdentifier ) {
		Database.Query( "INSERT INTO Users ( Name, Password, Salt, `Group` ) VALUES ( ?, ?, ?, ? );", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setString( 1, userName );

				byte[] userSalt = Shared.generateSalt();
				statement.setBytes( 3, userSalt );

				if ( !isTemporaryAccount ) {
					try {
						byte[] userPasswordHashed = Shared.hashPassword( userPassword, userSalt );
						statement.setBytes( 2, userPasswordHashed );

						Log.d( Shared.logTag, String.format( "Hashed password: %s, with salt: %s", Shared.bytesToHex( userPasswordHashed ), Shared.bytesToHex( userSalt ) ) );
					} catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
						Log.e( Shared.logTag, "No such hashing algorithm, or invalid key specification" );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
					}
				} else {
					// https://www.techieshah.com/2019/11/jdbc-preparedstatement-how-to-set-null.html
					statement.setNull( 2, Types.BINARY );
				}

				if ( groupIdentifier != 0 ) {
					statement.setInt( 4, groupIdentifier );
				} else {
					statement.setNull( 4, Types.INTEGER );
				}

				return statement;
			}

			@Override
			public void OnComplete( int resultCount, ResultSet results ) throws SQLException {
				if ( resultCount == 1 ) {
					Log.d( Shared.logTag, "Account created" );

					if ( isTemporaryAccount ) {
						if ( results.next() ) {
							int userIdentifier = results.getInt( "Identifier" );
							Log.d( Shared.logTag, String.format( "Got latest user identifier: %d", userIdentifier ) );
							Shared.loginUser( myself, userIdentifier );
						} else {
							Log.d( Shared.logTag, "Could not get latest user identifier" );
							runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
						}
					} else {
						runOnUiThread( () -> Popup.Success( myself, R.string.activity_registration_popup_register_success ) );
					}
				} else {
					Log.d( Shared.logTag, "Account was not created" );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}
}
