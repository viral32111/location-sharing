package com.viral32111.locationsharing;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.mysql.jdbc.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YourselfEventsActivity extends AppCompatActivity {

	// TODO: For now the only action is sending the user a notification, but more could be added in the future

	// Standard
	private final Activity myself = this;

	// Database
	private int userIdentifier;
	private final HashMap<String, Integer> memberIdentifierLookup = new HashMap<>();
	private final HashMap<String, Integer> bookmarkIdentifierLookup = new HashMap<>();

	// Dynamic Layouts
	private LinearLayout linearLayout;
	private LayoutInflater layoutInflater;

	// Misc
	private final Pattern enumSetPattern = Pattern.compile( "^enum\\((.+)\\)$" );
	private final Pattern stringPattern = Pattern.compile( "^'(.+)'$" );

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_yourself_events );

		linearLayout = findViewById( R.id.yourselfEventsLinearLayout );
		layoutInflater = getLayoutInflater();

		userIdentifier = getIntent().getIntExtra( "userIdentifier", 0 );

		updateEvents();
	}

	private void updateEvents() {
		linearLayout.removeAllViews();

		Database.Query( "SELECT Events.Identifier, Events.Condition, 'Notification' AS Action, Events.Target, Users.Name AS TargetName, Bookmarks.Name AS BookmarkName FROM Events INNER JOIN Bookmarks ON Bookmarks.Identifier = Events.Bookmark INNER JOIN Users ON Users.Identifier = Events.Target WHERE Bookmarks.User = ?;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				while ( results.next() ) {
					int eventIdentifier = results.getInt( "Identifier" );
					String eventCondition = results.getString( "Condition" );
					String eventAction = results.getString( "Action" );
					int targetIdentifier = results.getInt( "Target" );
					String targetName = results.getString( "TargetName" );
					String bookmarkName = results.getString( "BookmarkName" );

					runOnUiThread( () -> createEventEntry( eventIdentifier, targetName, bookmarkName, eventCondition, eventAction ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	private void createEventEntry( int eventIdentifier, String targetName, String bookmarkName, String eventCondition, String eventAction ) {
		View eventEntry = layoutInflater.inflate( R.layout.activity_yourself_events_entry, linearLayout, false );

		TextView targetTextView = eventEntry.findViewById( R.id.yourselfEventsEntryTargetTextView );
		TextView locationTextView = eventEntry.findViewById( R.id.yourselfEventsEntryLocationTextView );
		TextView conditionTextView = eventEntry.findViewById( R.id.yourselfEventsEntryConditionTextView );
		TextView actionTextView = eventEntry.findViewById( R.id.yourselfEventsEntryActionTextView );
		Button deleteButton = eventEntry.findViewById( R.id.yourselfEventsEntryDeleteButton );

		targetTextView.setText( String.format( getString( R.string.activity_yourself_events_entry_textview_target ), targetName ) );
		locationTextView.setText( String.format( getString( R.string.activity_yourself_events_entry_textview_location ), bookmarkName ) );
		conditionTextView.setText( String.format( getString( R.string.activity_yourself_events_entry_textview_condition ), eventCondition ) );

		if ( eventAction.equals( "Notification" ) ) {
			actionTextView.setText( String.format( getString( R.string.activity_yourself_events_entry_textview_action ), "Send me a notification" ) );
		} else {
			actionTextView.setText( String.format( getString( R.string.activity_yourself_events_entry_textview_action ), "?" ) );
		}

		deleteButton.setOnClickListener( ( view ) -> Database.Query( "DELETE FROM Events WHERE Identifier = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, eventIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( int resultCount ) {
				if ( resultCount == 1 ) {
					Log.d( Shared.logTag, String.format( "Deleted event %d", eventIdentifier ) );

					runOnUiThread( () -> {
						Popup.Success( myself, R.string.activity_yourself_events_popup_deleted );
						updateEvents();
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to delete event %d", eventIdentifier ) );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} ) );

		linearLayout.addView( eventEntry );
	}

	public void OnCreateButtonClick( View view ) {
		View eventCreateView = getLayoutInflater().inflate( R.layout.dialog_event_create, findViewById( R.id.yourselfEventsConstraintLayout ), false );

		Spinner eventCreateTargetSpinner = eventCreateView.findViewById( R.id.dialogEventCreateTargetSpinner );
		Spinner eventCreateLocationSpinner = eventCreateView.findViewById( R.id.dialogEventCreateLocationSpinner );
		Spinner eventCreateConditionSpinner = eventCreateView.findViewById( R.id.dialogEventCreateConditionSpinner );
		Spinner eventCreateActionSpinner = eventCreateView.findViewById( R.id.dialogEventCreateActionSpinner );

		addMembersToSpinner( eventCreateTargetSpinner );
		addBookmarksToSpinner( eventCreateLocationSpinner );
		addConditionsToSpinner( eventCreateConditionSpinner );
		addActionsToSpinner( eventCreateActionSpinner );

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( this );
		dialogBuilder.setTitle( R.string.dialog_event_create_title );
		dialogBuilder.setMessage( getString( R.string.dialog_event_create_description ) );
		dialogBuilder.setView( eventCreateView );
		dialogBuilder.setPositiveButton( R.string.dialog_event_create_button_positive, ( dialog, which ) -> {
			String targetChoice = String.valueOf( eventCreateTargetSpinner.getSelectedItem() );
			String locationChoice = String.valueOf( eventCreateLocationSpinner.getSelectedItem() );
			String conditionChoice = String.valueOf( eventCreateConditionSpinner.getSelectedItem() );
			String actionChoice = String.valueOf( eventCreateActionSpinner.getSelectedItem() );

			if ( StringUtils.isNullOrEmpty( targetChoice ) ) {
				Log.d( Shared.logTag, "Invalid choice for event target" );
				Popup.Error( this, R.string.activity_yourself_events_popup_target_invalid, false );
				return;
			}

			if ( StringUtils.isNullOrEmpty( locationChoice ) ) {
				Log.d( Shared.logTag, "Invalid choice for event location" );
				Popup.Error( this, R.string.activity_yourself_events_popup_location_invalid, false );
				return;
			}

			if ( StringUtils.isNullOrEmpty( conditionChoice ) ) {
				Log.d( Shared.logTag, "Invalid choice for event condition" );
				Popup.Error( this, R.string.activity_yourself_events_popup_condition_invalid, false );
				return;
			}

			if ( StringUtils.isNullOrEmpty( actionChoice ) ) {
				Log.d( Shared.logTag, "Invalid choice for event action" );
				Popup.Error( this, R.string.activity_yourself_events_popup_action_invalid, false );
				return;
			}

			Integer targetIdentifier = memberIdentifierLookup.get( targetChoice );
			Integer locationIdentifier = bookmarkIdentifierLookup.get( locationChoice );

			if ( targetIdentifier == null ) {
				Log.d( Shared.logTag, String.format( "Target %s does not exist in lookup map", targetChoice ) );
				Popup.Error( this, R.string.popup_issue, false );
				return;
			}

			if ( locationIdentifier == null ) {
				Log.d( Shared.logTag, String.format( "Location %s does not exist in lookup map", targetChoice ) );
				Popup.Error( this, R.string.popup_issue, false );
				return;
			}

			Database.Query( "INSERT INTO Events ( `Condition`, Bookmark, Target ) VALUES ( ?, ?, ? );", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					statement.setString( 1, conditionChoice );
					statement.setInt( 2, locationIdentifier );
					statement.setInt( 3, targetIdentifier );

					return statement;
				}

				@Override
				public void OnComplete( int resultCount, ResultSet results ) {
					if ( resultCount == 1 ) {
						Log.d( Shared.logTag, String.format( "Created new event when %d matches %s at %d", targetIdentifier, conditionChoice, locationIdentifier ) );

						runOnUiThread( () -> {
							Popup.Success( myself, R.string.activity_yourself_events_popup_create );
							updateEvents();
						} );
					} else {
						Log.d( Shared.logTag, String.format( "Failed to create new event when %d matches %s at %d", targetIdentifier, conditionChoice, locationIdentifier ) );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
					}
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} );
		} );

		AlertDialog eventDialog = dialogBuilder.create();
		eventDialog.setCanceledOnTouchOutside( true );
		eventDialog.show();
	}

	private void populateSpinner( Spinner spinner, ArrayList<String> entries ) {
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>( myself, android.R.layout.simple_spinner_dropdown_item );
		spinner.setAdapter( spinnerAdapter );

		spinnerAdapter.addAll( entries );

		spinnerAdapter.notifyDataSetChanged();
	}

	private void addMembersToSpinner( Spinner spinner ) {
		ArrayList<String> members = new ArrayList<>();

		Shared.getUserGroup( myself, userIdentifier, ( groupIdentifier ) -> {
			if ( groupIdentifier <= 0 ) {
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_group_impossible, true ) );
				return;
			}

			Database.Query( "SELECT Users.Identifier, Users.Name FROM Users LEFT JOIN Groups ON Groups.Identifier = Users.Group WHERE Groups.Identifier = ?;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					statement.setInt( 1, groupIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( ResultSet results ) throws SQLException {
					while ( results.next() ) {
						int userIdentifier = results.getInt( "Identifier" );
						String userName = results.getString( "Name" );

						memberIdentifierLookup.put( userName, userIdentifier );
						members.add( userName );
					}

					runOnUiThread( () -> populateSpinner( spinner, members ) );
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} );
		} );
	}

	private void addBookmarksToSpinner( Spinner spinner ) {
		ArrayList<String> bookmarks = new ArrayList<>();

		Bookmark.Fetch( userIdentifier, new BookmarkCallback() {
			@Override
			public void OnComplete( ArrayList<Bookmark> bookmarksResult ) {
				for ( Bookmark bookmark : bookmarksResult ) {
					bookmarkIdentifierLookup.put( bookmark.Name, bookmark.Identifier );
					bookmarks.add( bookmark.Name );
				}

				runOnUiThread( () -> populateSpinner( spinner, bookmarks ) );
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	private void addConditionsToSpinner( Spinner spinner ) {
		ArrayList<String> conditions = new ArrayList<>();

		// https://stackoverflow.com/a/11429272
		Database.Query( "SHOW COLUMNS FROM Events WHERE Field = 'Condition';", new DatabaseCallback() {
			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					String columnType = results.getString( "Type" );

					Matcher enumSetMatches = enumSetPattern.matcher( columnType );
					if ( enumSetMatches.find() ) {
						String enumSetMatch = enumSetMatches.group( 1 );

						if ( enumSetMatch == null ) {
							Log.e( Shared.logTag, "Could not fetch first group of matched enum set regular expression." );
							runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
							return;
						}

						String[] enumSetValues = enumSetMatch.split( "," );
						for ( String enumSetValue : enumSetValues ) {
							Matcher stringMatches = stringPattern.matcher( enumSetValue );

							if ( stringMatches.find() ) {
								String stringMatch = stringMatches.group( 1 );

								if ( stringMatch == null ) {
									Log.e( Shared.logTag, "Could not fetch first group of matched string regular expression." );
									runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
									return;
								}

								conditions.add( stringMatch );
							} else {
								Log.e( Shared.logTag, String.format( "Column type result %s did not match string regular expression.", columnType ) );
								runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
							}
						}
					} else {
						Log.e( Shared.logTag, String.format( "Column type result %s did not match enum set regular expression.", columnType ) );
						runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
					}

					runOnUiThread( () -> populateSpinner( spinner, conditions ) );
				} else {
					Log.e( Shared.logTag, "Could not fetch information for Condition column in Events table." );
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

	private void addActionsToSpinner( Spinner spinner ) {
		ArrayList<String> actions = new ArrayList<>();
		actions.add( "Notification" );

		populateSpinner( spinner, actions );
	}
}