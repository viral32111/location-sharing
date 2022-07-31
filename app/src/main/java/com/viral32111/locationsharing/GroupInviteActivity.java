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
import java.sql.Timestamp;
import java.util.Date;

public class GroupInviteActivity extends AppCompatActivity {

	// Standard
	private final Activity myself = this;

	// Database
	private int userIdentifier;
	private int groupIdentifier;

	// Dynamic Layouts
	private LinearLayout linearLayout;
	private LayoutInflater layoutInflater;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_group_invite );

		linearLayout = findViewById( R.id.groupInviteLinearLayout );
		layoutInflater = getLayoutInflater();

		userIdentifier = getIntent().getIntExtra( "userIdentifier", 0 );
		groupIdentifier = getIntent().getIntExtra( "groupIdentifier", 0 );

		updateInvites();
	}

	private void updateInvites() {
		linearLayout.removeAllViews();

		Database.Query( "SELECT Code, Created FROM Invites WHERE `Group` = ? ORDER BY Created DESC;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, groupIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				while ( results.next() ) {
					String inviteCode = results.getString( "Code" );
					Timestamp inviteCreated = results.getTimestamp( "Created" );

					runOnUiThread( () -> createInviteEntry( inviteCode, inviteCreated ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	private void createInviteEntry( String inviteCode, Timestamp inviteCreated ) {
		View inviteEntry = layoutInflater.inflate( R.layout.activity_group_invite_entry, linearLayout, false );

		TextView codeTextView = inviteEntry.findViewById( R.id.groupInviteEntryCodeTextView );
		TextView createdTextView = inviteEntry.findViewById( R.id.groupInviteEntryCreatedTextView );
		TextView expiresTextView = inviteEntry.findViewById( R.id.groupInviteEntryExpiresTextView );
		Button deleteButton = inviteEntry.findViewById( R.id.groupInviteEntryDeleteButton );

		Timestamp accountExpires = new Timestamp( inviteCreated.getTime() + ( Shared.OneMonth * 1000L ) );
		long remainingTime = ( accountExpires.getTime() - new Date().getTime() ) / 1000L;

		codeTextView.setText( String.format( getString( R.string.activity_group_invite_textview_entry_code ), inviteCode ) );
		createdTextView.setText( String.format( getString( R.string.activity_group_invite_textview_entry_created ), Shared.prettyDateTime( inviteCreated ) ) );
		expiresTextView.setText( String.format( getString( R.string.activity_group_invite_textview_entry_expires ), Shared.secondsToHumanReadable( remainingTime ) ) );

		deleteButton.setOnClickListener( ( view ) -> Database.Query( "DELETE FROM Invites WHERE `Group` = ? AND Code = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, groupIdentifier );
				statement.setString( 2, inviteCode );
				return statement;
			}

			@Override
			public void OnComplete( int resultCount ) {
				if ( resultCount == 1 ) {
					Log.d( Shared.logTag, String.format( "Deleted invite %s for group %d", inviteCode, groupIdentifier ) );

					runOnUiThread( () -> {
						Popup.Success( myself, R.string.activity_group_invite_popup_deleted );
						updateInvites();
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to delete invite %s for group %d", inviteCode, groupIdentifier ) );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} ) );

		linearLayout.addView( inviteEntry );
	}

	public void OnCreateButtonClick( View view ) {
		String inviteCode = Shared.randomCapitalCharacters( 8 );

		Database.Query( "INSERT INTO Invites ( `Group`, Code ) VALUES ( ?, ? );", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, groupIdentifier );
				statement.setString( 2, inviteCode );
				return statement;
			}

			@Override
			public void OnComplete( int resultCount, ResultSet results ) throws SQLException {
				if ( resultCount == 1 && results.next() ) {
					Log.d( Shared.logTag, String.format( "Created invite %d (%s) for group %d", results.getInt( "Identifier" ), inviteCode, groupIdentifier ) );

					runOnUiThread( () -> {
						Popup.Success( myself, String.format( getString( R.string.activity_group_invite_popup_created ), inviteCode ) );
						updateInvites();
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to create invite %s for group %d", inviteCode, groupIdentifier ) );
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