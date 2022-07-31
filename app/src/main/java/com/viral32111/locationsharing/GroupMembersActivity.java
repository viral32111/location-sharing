package com.viral32111.locationsharing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class GroupMembersActivity extends AppCompatActivity {

	// Standard
	private final Activity myself = this;

	// Database
	private int userIdentifier;
	private int groupIdentifier;
	private int groupCreatorIdentifier;

	// UI
	private TextView administratorTextView;

	// Dynamic Layouts
	private LinearLayout linearLayout;
	private LayoutInflater layoutInflater;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_group_members );

		administratorTextView = findViewById( R.id.groupMembersAdministratorTextView );

		linearLayout = findViewById( R.id.groupMembersLinearLayout );
		layoutInflater = getLayoutInflater();

		userIdentifier = getIntent().getIntExtra( "userIdentifier", 0 );
		groupIdentifier = getIntent().getIntExtra( "groupIdentifier", 0 );

		Database.Query( "SELECT Users.Identifier, Users.Name FROM Users LEFT JOIN Groups ON Groups.Creator = Users.Identifier WHERE Groups.Identifier = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, groupIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					groupCreatorIdentifier = results.getInt( "Identifier" );
					String groupCreatorName = results.getString( "Name" );

					Log.d( Shared.logTag, String.format( "Got creator %d for group %d", groupCreatorIdentifier, groupIdentifier ) );

					runOnUiThread( () -> {
						administratorTextView.setText( getString( R.string.activity_group_members_textview_administrator, groupCreatorName ) );
						updateMembers();
					} );

				} else {
					Log.d( Shared.logTag, String.format( "Failed to get creator for group %d", groupIdentifier ) );
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

	private void updateMembers() {
		linearLayout.removeAllViews();

		// https://stackoverflow.com/a/24442655
		Database.Query( "SELECT Users.Name, Users.Identifier, CAST( AES_DECRYPT( History.Latitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Latitude, CAST( AES_DECRYPT( History.Longitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Longitude, History.Recorded FROM Users LEFT JOIN History ON History.Identifier = ( SELECT History.Identifier FROM History WHERE History.User = Users.Identifier ORDER BY History.Recorded DESC LIMIT 1 ) LEFT JOIN Groups ON Groups.Identifier = Users.Group WHERE Groups.Identifier = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				Database.InsertEncryptionKey( statement, new int[] { 1, 2 } );
				statement.setInt( 3, groupIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				while ( results.next() ) {
					String userName = results.getString( "Name" );
					int userIdentifier = results.getInt( "Identifier" );
					double latitude = results.getDouble( "Latitude" );
					double longitude = results.getDouble( "Longitude" );
					//Timestamp recorded = results.getTimestamp( "Recorded" );

					runOnUiThread( () -> createMembersEntry( userName, userIdentifier, latitude, longitude ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	private void createMembersEntry( String memberName, int memberIdentifier, double latitude, double longitude ) {
		View membersEntry = layoutInflater.inflate( R.layout.activity_group_members_entry, linearLayout, false );

		TextView nameTextView = membersEntry.findViewById( R.id.groupMembersEntryNameTextView );
		TextView locationTextView = membersEntry.findViewById( R.id.groupMembersEntryLocationTextView );
		Button deleteButton = membersEntry.findViewById( R.id.groupMembersEntryDeleteButton );

		String locationText = "Unknown";
		if ( latitude != 0 && longitude != 0 ) locationText = String.format( Locale.UK, "%.4f, %.4f", latitude, longitude );

		nameTextView.setText( String.format( getString( R.string.activity_group_members_textview_entry_name ), memberName ) );
		locationTextView.setText( String.format( getString( R.string.activity_group_members_textview_entry_location ), locationText ) );

		if ( memberIdentifier == groupCreatorIdentifier ) {
			deleteButton.setVisibility( View.INVISIBLE );
			deleteButton.setEnabled( false );
		}

		deleteButton.setOnClickListener( ( view ) -> {
			if ( userIdentifier != groupCreatorIdentifier  ) {
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_group_permission, false ) );
				return;
			}

			Popup.Confirm( myself, R.string.activity_group_members_popup_entry_kick_confirm, () -> Database.Query( "UPDATE Users SET `Group` = NULL WHERE `Group` = ? AND Identifier = ?;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					statement.setInt( 1, groupIdentifier );
					statement.setInt( 2, memberIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( int resultCount ) {
					if ( resultCount == 1 ) {
						Log.d( Shared.logTag, String.format( "Removed member %s from group %d", memberIdentifier, groupIdentifier ) );

						runOnUiThread( () -> {
							Popup.Success( myself, R.string.activity_group_members_popup_entry_kick_done );
							updateMembers();
						} );
					} else {
						Log.d( Shared.logTag, String.format( "Failed to remove member %d from group %d", memberIdentifier, groupIdentifier ) );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
					}
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} ) );
		} );

		linearLayout.addView( membersEntry );
	}

}