package com.viral32111.locationsharing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.mysql.jdbc.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GroupManageActivity extends AppCompatActivity {

	// Standard
	private final Activity myself = this;

	// Database
	private int userIdentifier;
	private int groupIdentifier;

	// UI
	private EditText nameEditText;
	private Spinner ownershipSpinner;
	ArrayAdapter<String> spinnerAdapter;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_group_manage );

		nameEditText = findViewById( R.id.groupManageNameEditText );
		ownershipSpinner = findViewById( R.id.groupManageOwnershipSpinner );

		userIdentifier = getIntent().getIntExtra( "userIdentifier", 0 );
		groupIdentifier = getIntent().getIntExtra( "groupIdentifier", 0 );

		// https://stackoverflow.com/a/8755749
		spinnerAdapter = new ArrayAdapter<>( myself, android.R.layout.simple_spinner_dropdown_item );
		ownershipSpinner.setAdapter( spinnerAdapter );

		updateMembersInSpinner();
	}

	private void updateMembersInSpinner() {
		spinnerAdapter.clear();

		Database.Query( "SELECT Identifier, Name FROM Users WHERE `Group` = ? AND Identifier != ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, groupIdentifier );
				statement.setInt( 2, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				while ( results.next() ) spinnerAdapter.add( results.getString( "Name" ) );
				runOnUiThread( spinnerAdapter::notifyDataSetChanged );
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	public void onNameButtonClick( View view ) {
		String groupName = String.valueOf( nameEditText.getText() );

		if ( groupName.isEmpty() ) {
			Log.d( Shared.logTag, "Group name is empty" );
			Popup.Error( this, R.string.popup_group_create_name_invalid, false );
			return;
		}

		Database.Query( "UPDATE Groups SET Name = ? WHERE Identifier = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setString( 1, groupName );
				statement.setInt( 2, groupIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( int resultCount ) {
				if ( resultCount == 1 ) {
					Log.d( Shared.logTag, String.format( "Group %d renamed to %s", groupIdentifier, groupName ) );
					runOnUiThread( () -> Popup.Success( myself, R.string.activity_group_manage_popup_name_changed ) );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to rename group %d to %s", groupIdentifier, groupName ) );
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

	public void onInviteButtonClick( View view ) {
		Shared.getGroupCreator( myself, groupIdentifier, ( creatorIdentifier ) -> {
			if ( creatorIdentifier != userIdentifier ) runOnUiThread( () -> Popup.Error( myself, R.string.popup_group_permission, false ) );
			else runOnUiThread( () -> Popup.Confirm( myself, R.string.activity_group_manage_popup_invites_confirm, () -> Database.Query( "DELETE FROM Invites WHERE `Group` = ?;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					statement.setInt( 1, groupIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( int resultCount ) {
					if ( resultCount == 1 ) {
						Log.d( Shared.logTag, String.format( "Invites for group %d deleted", groupIdentifier ) );
						runOnUiThread( () -> Popup.Success( myself, R.string.activity_group_manage_popup_invites_done ) );
					} else {
						Log.d( Shared.logTag, String.format( "No invites for group %d", groupIdentifier ) );
						//runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
					}
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} ) ) );
		} );
	}

	public void onMembersButtonClick( View view ) {
		Shared.getGroupCreator( myself, groupIdentifier, ( creatorIdentifier ) -> {
			if ( creatorIdentifier != userIdentifier ) runOnUiThread( () -> Popup.Error( myself, R.string.popup_group_permission, false ) );
			else runOnUiThread( () -> Popup.Confirm( myself, R.string.activity_group_manage_popup_members_confirm, () -> Database.Query( "UPDATE Users SET `Group` = NULL WHERE `Group` = ? AND Identifier != ?;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					statement.setInt( 1, groupIdentifier );
					statement.setInt( 2, userIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( int resultCount ) {
					if ( resultCount == 1 ) {
						Log.d( Shared.logTag, String.format( "All members removed from group %d", groupIdentifier ) );
						runOnUiThread( () -> {
							Popup.Success( myself, R.string.activity_group_manage_popup_members_done );
							updateMembersInSpinner();
						} );
					} else {
						Log.d( Shared.logTag, String.format( "No members for group %d", groupIdentifier ) );
						//runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
					}
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} ) ) );
		} );
	}

	public void onOwnershipButtonClick( View view ) {
		String memberChoice = String.valueOf( ownershipSpinner.getSelectedItem() );

		if ( StringUtils.isNullOrEmpty( memberChoice ) ) {
			Log.d( Shared.logTag, "Invalid choice for new group owner" );
			Popup.Error( this, R.string.activity_group_manage_popup_ownership_invalid, false );
			return;
		}

		// TO-DO: Confirm the selected member is still in the group

		// https://stackoverflow.com/a/11588758
		Shared.getGroupCreator( myself, groupIdentifier, ( creatorIdentifier ) -> {
			if ( creatorIdentifier != userIdentifier ) runOnUiThread( () -> Popup.Error( myself, R.string.popup_group_permission, false ) );
			else runOnUiThread( () -> Popup.Confirm( myself, R.string.activity_group_manage_popup_ownership_confirm, () -> Database.Query( "UPDATE Groups INNER JOIN ( SELECT Users.Identifier FROM Users WHERE Users.Name = ? ) AS Users ON Groups.Identifier = ? SET Groups.Creator = Users.Identifier;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					statement.setString( 1, memberChoice );
					statement.setInt( 2, groupIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( int resultCount ) {
					if ( resultCount == 1 ) {
						Log.d( Shared.logTag, String.format( "Ownership of group %d changed to %s", groupIdentifier, memberChoice ) );
						runOnUiThread( () -> Popup.Success( myself, R.string.activity_group_manage_popup_ownership_changed ).setOnDismissListener( ( $ ) -> finish() ) );
					} else {
						Log.d( Shared.logTag, String.format( "Could not change ownership of group %d", groupIdentifier ) );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
					}
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} ) ) );
		} );
	}

	public void onDeleteButtonClick( View view ) {
		Shared.getGroupCreator( myself, groupIdentifier, ( creatorIdentifier ) -> {
			if ( creatorIdentifier != userIdentifier ) runOnUiThread( () -> Popup.Error( myself, R.string.popup_group_permission, false ) );
			else runOnUiThread( () -> Popup.Confirm( myself, R.string.activity_group_manage_popup_delete_confirm, () -> Database.Query( "DELETE FROM Groups WHERE Identifier = ? LIMIT 1;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					statement.setInt( 1, groupIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( int resultCount ) {
					if ( resultCount == 1 ) {
						Log.d( Shared.logTag, String.format( "Group %d deleted", groupIdentifier ) );
						runOnUiThread( () -> finish() );
					} else {
						Log.d( Shared.logTag, String.format( "Failed to delete group %d", groupIdentifier ) );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
					}
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} ) ) );
		} );
	}

}