package com.viral32111.locationsharing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

public class MiscManageActivity extends AppCompatActivity {

	// Standard
	private final Activity myself = this;

	// Database
	private int userIdentifier;

	// UI
	private TextView registeredTextView;
	private TextView expiresTextView;
	private EditText nameEditText;
	private EditText passwordOldEditText;
	private EditText passwordNewEditText;
	private EditText passwordConfirmEditText;
	private EditText groupEditText;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_misc_manage );

		registeredTextView = findViewById( R.id.miscManageRegisteredTextView );
		expiresTextView = findViewById( R.id.miscManageExpiresTextView );
		nameEditText = findViewById( R.id.miscManageNameEditText );
		passwordOldEditText = findViewById( R.id.miscManagePasswordOldEditText );
		passwordNewEditText = findViewById( R.id.miscManagePasswordNewEditText );
		passwordConfirmEditText = findViewById( R.id.miscManagePasswordConfirmEditText );
		groupEditText = findViewById( R.id.miscManageGroupEditText );

		// https://stackoverflow.com/a/30166602
		userIdentifier = getIntent().getIntExtra( "userIdentifier", 0 );

		// https://www.w3schools.com/SQL/func_mysql_isnull.asp
		Database.Query( "SELECT Registered, ISNULL( Password ) AS Temporary FROM Users WHERE Identifier = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					Log.d( Shared.logTag, String.format( "Found user %d in database", userIdentifier ) );

					Timestamp userRegistered = results.getTimestamp( "Registered" );
					boolean isTemporary = results.getBoolean( "Temporary" );

					runOnUiThread( () -> registeredTextView.setText( String.format( getString( R.string.activity_misc_manage_textview_registered ), Shared.prettyDateTime( userRegistered ) ) ) );

					if ( isTemporary ) {
						Timestamp accountExpires = new Timestamp( userRegistered.getTime() + ( Shared.OneMonth * 1000L ) );
						long remainingTime = ( accountExpires.getTime() - new Date().getTime() ) / 1000L;

						runOnUiThread( () -> {
							expiresTextView.setVisibility( View.VISIBLE );
							expiresTextView.setText( String.format( getString( R.string.activity_misc_manage_textview_expires ), Shared.secondsToHumanReadable( remainingTime ) ) );
						} );
					}
				} else {
					Log.d( Shared.logTag, String.format( "Could not find user %d in database", userIdentifier ) );
					// TODO: Shared.logoutUser()
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	public void onNameButtonClick( View view ) {
		String userName = String.valueOf( nameEditText.getText() );

		if ( userName.isEmpty() || userName.length() < 3 || userName.length() > 32 ) {
			Log.d( Shared.logTag, "Name is empty or outside of character limit" );
			Popup.Error( this, R.string.popup_name_invalid, false );
			return;
		}

		Database.Query( "UPDATE Users SET Name = ? WHERE Identifier = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setString( 1, userName );
				statement.setInt( 2, userIdentifier );

				return statement;
			}

			@Override
			public void OnComplete( int resultCount ) {
				if ( resultCount == 1 ) {
					Log.d( Shared.logTag, String.format( "Name updated for user %d", userIdentifier ) );
					runOnUiThread( () -> Popup.Success( myself, R.string.activity_misc_manage_popup_name_changed ) );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to update name for user %d", userIdentifier ) );
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

	public void onPasswordButtonClick( View view ) {
		String userPasswordOld = String.valueOf( passwordOldEditText.getText() );
		String userPasswordNew = String.valueOf( passwordNewEditText.getText() );
		String userPasswordConfirmation = String.valueOf( passwordConfirmEditText.getText() );

		if ( ( userPasswordOld.isEmpty() || userPasswordOld.length() < 8 ) || ( userPasswordNew.isEmpty() || userPasswordNew.length() < 8 ) ) {
			Log.d( Shared.logTag, "Password is empty or below minimum character limit" );
			Popup.Error( this, R.string.popup_password_invalid, false );
			return;
		}

		if ( !userPasswordConfirmation.equals( userPasswordNew ) ) {
			Log.d( Shared.logTag, "Passwords do not match" );
			Popup.Error( this, R.string.popup_password_mismatch, false );
			return;
		}

		Database.Query( "SELECT Password, Salt FROM Users WHERE Identifier = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					byte[] userPasswordCurrentHashed = results.getBytes( "Password" );
					byte[] userSalt = results.getBytes( "Salt" );

					if ( userPasswordCurrentHashed == null ) {
						Log.d( Shared.logTag, "Attempt to change password for temporary account" );
						runOnUiThread( () -> Popup.Error( myself, R.string.activity_misc_manage_popup_password_temporary, false ) );
						return;
					}

					byte[] userPasswordOldHashed;

					try {
						userPasswordOldHashed = Shared.hashPassword( userPasswordOld, userSalt );
						Log.d( Shared.logTag, Shared.bytesToHex( userPasswordOldHashed ) );
					} catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
						Log.e( Shared.logTag, "No such hashing algorithm, or invalid key specification" );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
						return;
					}

					if ( !Arrays.equals( userPasswordOldHashed, userPasswordCurrentHashed ) ) {
						Log.d( Shared.logTag, "Password is incorrect" );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_password_incorrect, false ) );
						return;
					}

					Database.Query( "UPDATE Users SET Password = ? WHERE Identifier = ?;", new DatabaseCallback() {
						@Override
						public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
							statement.setInt( 2, userIdentifier );

							try {
								byte[] userPasswordNewHashed = Shared.hashPassword( userPasswordNew, userSalt );
								Log.d( Shared.logTag, Shared.bytesToHex( userPasswordNewHashed ) );

								statement.setBytes( 1, userPasswordNewHashed );
							} catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
								Log.e( Shared.logTag, "No such hashing algorithm, or invalid key specification" );
								runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
							}

							return statement;
						}

						@Override
						public void OnComplete( int resultCount ) {
							if ( resultCount == 1 ) {
								Log.d( Shared.logTag, String.format( "Password updated for user %d", userIdentifier ) );
								runOnUiThread( () -> Popup.Success( myself, R.string.activity_misc_manage_popup_password_changed ) );
							} else {
								Log.d( Shared.logTag, String.format( "Failed to update password for user %d", userIdentifier ) );
								runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
							}
						}

						@Override
						public void OnException( Exception exception ) {
							Log.e( Shared.logTag, exception.getMessage() );
							runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
						}
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to get credentials from database for user %d", userIdentifier ) );
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

	public void onGroupJoinButtonClick( View view ) {
		String inviteCode = String.valueOf( groupEditText.getText() );

		if ( inviteCode.isEmpty() ) {
			Log.d( Shared.logTag, "Invite code is empty" );
			Popup.Error( this, R.string.activity_misc_manage_popup_group_join_code_empty, false );
			return;
		}

		// Requires string because group is a MySQL keyword
		Database.Query( "SELECT `Group` FROM Users WHERE Identifier = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					int groupIdentifier = results.getInt( "Group" );

					if ( groupIdentifier != 0 ) {
						Log.d( Shared.logTag, "User is already in group" );
						runOnUiThread( () -> Popup.Error( myself, R.string.activity_misc_manage_popup_group_impossible, false ) );
						return;
					}

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

								Database.Query( "UPDATE Users SET `Group` = ? WHERE Identifier = ?;", new DatabaseCallback() {
									@Override
									public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
										statement.setInt( 1, groupIdentifier );
										statement.setInt( 2, userIdentifier );

										return statement;
									}

									@Override
									public void OnComplete( int resultCount ) {
										if ( resultCount == 1 ) {
											Log.d( Shared.logTag, String.format( "User %d group set to %d", userIdentifier, groupIdentifier ) );
											runOnUiThread( () -> Popup.Success( myself, R.string.activity_misc_manage_popup_group_join_success ) );
										} else {
											Log.d( Shared.logTag, String.format( "Failed to join group %d for user %d", groupIdentifier, userIdentifier ) );
											runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
										}
									}

									@Override
									public void OnException( Exception exception ) {
										Log.e( Shared.logTag, exception.getMessage() );
										runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
									}
								} );
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
					Log.d( Shared.logTag, String.format( "Failed to get group for user %d from database", userIdentifier ) );
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

	public void onGroupCreateButtonClick( View view ) {
		String groupName = String.valueOf( groupEditText.getText() );

		if ( groupName.isEmpty() ) {
			Log.d( Shared.logTag, "Group name is empty" );
			Popup.Error( this, R.string.popup_group_create_name_invalid, false );
			return;
		}

		Database.Query( "SELECT `Group` FROM Users WHERE Identifier = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					int groupIdentifier = results.getInt( "Group" );

					if ( groupIdentifier != 0 ) {
						Log.d( Shared.logTag, "User is already in group" );
						runOnUiThread( () -> Popup.Error( myself, R.string.activity_misc_manage_popup_group_impossible, false ) );
						return;
					}

					Database.Query( "INSERT INTO Groups ( Name, Creator ) VALUES ( ?, ? );", new DatabaseCallback() {
						@Override
						public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
							statement.setString( 1, groupName );
							statement.setInt( 2, userIdentifier );

							return statement;
						}

						@Override
						public void OnComplete( int resultCount, ResultSet results ) throws SQLException {
							if ( resultCount == 1 && results.next() ) {
								int groupIdentifier = results.getInt( "Identifier" );
								Log.d( Shared.logTag, String.format( "Group %d created", groupIdentifier ) );

								Database.Query( "UPDATE Users SET `Group` = ? WHERE Identifier = ?;", new DatabaseCallback() {
									@Override
									public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
										statement.setInt( 1, groupIdentifier );
										statement.setInt( 2, userIdentifier );

										return statement;
									}

									@Override
									public void OnComplete( int resultCount ) {
										if ( resultCount == 1 ) {
											Log.d( Shared.logTag, String.format( "User %d group set to %d", userIdentifier, groupIdentifier ) );
											runOnUiThread( () -> Popup.Success( myself, R.string.activity_misc_manage_popup_group_create_success ) );
										} else {
											Log.d( Shared.logTag, String.format( "Failed to assign group %d for user %d", groupIdentifier, userIdentifier ) );
											runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
										}
									}

									@Override
									public void OnException( Exception exception ) {
										Log.e( Shared.logTag, exception.getMessage() );
										runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
									}
								} );
							} else {
								Log.d( Shared.logTag, "Failed to create group" );
								runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
							}
						}

						@Override
						public void OnException( Exception exception ) {
							Log.e( Shared.logTag, exception.getMessage() );
							runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
						}
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to get group for user %d from database", userIdentifier ) );
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

	// TODO: Make it so they cant leave the group if they own it
	public void onGroupLeaveButtonClick( View view ) {
		Database.Query( "SELECT `Group` FROM Users WHERE Identifier = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					int groupIdentifier = results.getInt( "Group" );

					if ( groupIdentifier == 0 ) {
						Log.d( Shared.logTag, "Attempt to leave group when not in one" );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_group_impossible, false ) );
						return;
					}

					Database.Query( "UPDATE Users SET `Group` = NULL WHERE Identifier = ?;", new DatabaseCallback() {
						@Override
						public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
							statement.setInt( 1, userIdentifier );

							return statement;
						}

						@Override
						public void OnComplete( int resultCount ) {
							if ( resultCount == 1 ) {
								Log.d( Shared.logTag, String.format( "Left group for user %d", userIdentifier ) );
								runOnUiThread( () -> Popup.Error( myself, R.string.activity_misc_manage_popup_group_leave_success, false ) );
							} else {
								Log.d( Shared.logTag, String.format( "Failed to leave group for user %d", userIdentifier ) );
								runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
							}
						}

						@Override
						public void OnException( Exception exception ) {
							Log.e( Shared.logTag, exception.getMessage() );
							runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
						}
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to get group for user %d from database", userIdentifier ) );
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

	public void onPermanentButtonClick( View view ) {
		String userPasswordNew = String.valueOf( passwordNewEditText.getText() );
		String userPasswordConfirmation = String.valueOf( passwordConfirmEditText.getText() );

		if ( userPasswordNew.isEmpty() || userPasswordNew.length() < 8 ) {
			Log.d( Shared.logTag, "Password is empty or below minimum character limit" );
			Popup.Error( this, R.string.popup_password_invalid, false );
			return;
		}

		if ( !userPasswordConfirmation.equals( userPasswordNew ) ) {
			Log.d( Shared.logTag, "Passwords do not match" );
			Popup.Error( this, R.string.popup_password_mismatch, false );
			return;
		}

		Database.Query( "SELECT Password, Salt FROM Users WHERE Identifier = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					byte[] userPasswordCurrentHashed = results.getBytes( "Password" );
					byte[] userSalt = results.getBytes( "Salt" );

					if ( userPasswordCurrentHashed != null ) {
						Log.d( Shared.logTag, "Attempt to convert to permanent account when already permanent" );
						runOnUiThread( () -> Popup.Error( myself, R.string.activity_misc_manage_popup_permanent_already, false ) );
						return;
					}

					Database.Query( "UPDATE Users SET Password = ? WHERE Identifier = ?;", new DatabaseCallback() {
						@Override
						public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
							statement.setInt( 2, userIdentifier );

							try {
								byte[] userPasswordNewHashed = Shared.hashPassword( userPasswordNew, userSalt );
								Log.d( Shared.logTag, Shared.bytesToHex( userPasswordNewHashed ) );

								statement.setBytes( 1, userPasswordNewHashed );
							} catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
								Log.e( Shared.logTag, "No such hashing algorithm, or invalid key specification" );
								runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
							}

							return statement;
						}

						@Override
						public void OnComplete( int resultCount ) {
							if ( resultCount == 1 ) {
								Log.d( Shared.logTag, String.format( "User %d converted to permanent account", userIdentifier ) );
								runOnUiThread( () -> Popup.Success( myself, R.string.activity_misc_manage_popup_permanent_changed ) );
							} else {
								Log.d( Shared.logTag, String.format( "Failed to convert user %d to permanent account", userIdentifier ) );
								runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
							}
						}

						@Override
						public void OnException( Exception exception ) {
							Log.e( Shared.logTag, exception.getMessage() );
							runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
						}
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to get credentials from database for user %d", userIdentifier ) );
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

	public void onDeleteButtonClick( View view ) {
		Popup.Confirm( this, R.string.activity_misc_manage_popup_delete_confirm, () -> Database.Query( "DELETE FROM Users WHERE Identifier = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( int resultCount ) {
				if ( resultCount == 1 ) {
					Log.d( Shared.logTag, String.format( "User %d deleted", userIdentifier ) );
					Shared.logoutUser( myself );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to delete user %d", userIdentifier ) );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} ) );
	}
}