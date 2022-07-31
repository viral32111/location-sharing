package com.viral32111.locationsharing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class HomeActivity extends AppCompatActivity {

	// Standard
	private final Activity myself = this;

	// Database
	private Integer userIdentifier;

	// UI
	private MapView mapView;
	private TextView navigationHeaderNameTextView;
	private TextView navigationHeaderGroupTextView;

	// Misc
	private final int permissionRequestCode = 1; // Can be any number
	private final String mapViewStateBundleName = "mapBundle";
	private ActionBarDrawerToggle drawerToggle;
	private LocationManager locationManager;
	private final int mapViewZoomLevel = 17;
	private final ArrayList<Marker> currentLocationMarkers = new ArrayList<>();
	private final ArrayList<Marker> currentBookmarkMarkers = new ArrayList<>();
	private final ArrayList<Circle> currentBookmarkCircles = new ArrayList<>();
	private final int mapRefreshDelay = 10000; // 60000; // 1 minute
	private final HashMap<Integer, com.javadocmd.simplelatlng.LatLng> lastKnownUserPositions = new HashMap<>();
	private final HashMap<Integer, Bookmark> lastKnownUserBookmark = new HashMap<>();

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_home );

		DrawerLayout drawerLayout = findViewById( R.id.homeDrawerLayout );
		NavigationView navigationView = findViewById( R.id.homeNavigationView );
		mapView = findViewById( R.id.homeMapView );

		// https://stackoverflow.com/a/38418531
		View navigationHeaderView = navigationView.getHeaderView( 0 );
		navigationHeaderNameTextView = navigationHeaderView.findViewById( R.id.navigationHeaderNameTextView );
		navigationHeaderGroupTextView = navigationHeaderView.findViewById( R.id.navigationHeaderGroupTextView );

		// https://developer.android.com/guide/navigation/navigation-ui#add_a_navigation_drawer
		// https://www.geeksforgeeks.org/navigation-drawer-in-android/
		drawerToggle = new ActionBarDrawerToggle( this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close );
		drawerLayout.addDrawerListener( drawerToggle );
		drawerToggle.syncState();
		Objects.requireNonNull( getSupportActionBar() ).setDisplayHomeAsUpEnabled( true );

		// https://stackoverflow.com/a/42297548
		navigationView.setNavigationItemSelectedListener( ( MenuItem item ) -> {
			String itemTitle = String.valueOf( item.getTitle() );
			Log.d( Shared.logTag, String.format( "Switching to activity: %s...", itemTitle ) );

			// Yourself
			if ( itemTitle.equals( getString( R.string.navigation_menu_yourself_history ) ) ) Shared.switchActivityTemporarily( this, YourselfHistoryActivity.class, userIdentifier );
			else if ( itemTitle.equals( getString( R.string.navigation_menu_yourself_bookmarks ) ) ) Shared.switchActivityTemporarily( this, YourselfBookmarksActivity.class, userIdentifier );
			else if ( itemTitle.equals( getString( R.string.navigation_menu_yourself_events ) ) ) Shared.switchActivityTemporarily( this, YourselfEventsActivity.class, userIdentifier );

			// Group
			else if ( itemTitle.equals( getString( R.string.navigation_menu_group_members ) ) ) Shared.getUserGroup( this, userIdentifier, ( groupIdentifier ) -> {
				if ( groupIdentifier > 0 ) Shared.switchActivityTemporarily( this, GroupMembersActivity.class, userIdentifier, groupIdentifier );
				else runOnUiThread( () -> Popup.Error( this, R.string.popup_group_impossible, false ) );
			} );
			else if ( itemTitle.equals( getString( R.string.navigation_menu_group_invites ) ) ) Shared.getUserGroup( this, userIdentifier, ( groupIdentifier ) -> {
				if ( groupIdentifier > 0 ) Shared.getGroupCreator( myself, groupIdentifier, ( creatorIdentifier ) -> {
					if ( creatorIdentifier == userIdentifier ) Shared.switchActivityTemporarily( this, GroupInviteActivity.class, userIdentifier, groupIdentifier );
					else runOnUiThread( () -> Popup.Error( this, R.string.popup_group_permission, false ) );
				} );

				else runOnUiThread( () -> Popup.Error( this, R.string.popup_group_impossible, false ) );
			} );
			else if ( itemTitle.equals( getString( R.string.navigation_menu_group_manage ) ) ) Shared.getUserGroup( this, userIdentifier, ( groupIdentifier ) -> {
				if ( groupIdentifier > 0 ) Shared.getGroupCreator( myself, groupIdentifier, ( creatorIdentifier ) -> {
					if ( creatorIdentifier == userIdentifier ) Shared.switchActivityTemporarily( this, GroupManageActivity.class, userIdentifier, groupIdentifier );
					else runOnUiThread( () -> Popup.Error( this, R.string.popup_group_permission, false ) );
				} );

				else runOnUiThread( () -> Popup.Error( this, R.string.popup_group_impossible, false ) );
			} );

			// Misc
			else if ( itemTitle.equals( getString( R.string.navigation_menu_misc_account ) ) ) Shared.switchActivityTemporarily( this, MiscManageActivity.class, userIdentifier );

			// Default to closing the drawer
			else drawerLayout.closeDrawer( GravityCompat.START );

			return true;
		} );

		userIdentifier = Shared.getLoggedInUser( this );
		if ( userIdentifier == null ) {
			Log.d( Shared.logTag, "User not logged in?" );
			Shared.logoutUser( this );
		}

		updateNavigationHeader();

		locationManager = ( LocationManager ) getSystemService( Context.LOCATION_SERVICE );

		if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
			Popup.Error( this, R.string.activity_home_popup_provider_location_failure, true );
			return;
		}

		Notification.Setup( this, R.string.notification_channel_name, R.string.notification_channel_description );

		// https://developers.google.com/maps/documentation/android-sdk/start
		// https://github.com/googlemaps/android-samples/blob/main/ApiDemos/java/app/src/gms/java/com/example/mapdemo/RawMapViewDemoActivity.java
		Bundle mapBundle = null;
		if ( savedInstanceState != null ) mapBundle = savedInstanceState.getBundle( mapViewStateBundleName );

		mapView.onCreate( mapBundle );
		mapView.getMapAsync( map -> {
			Log.d( Shared.logTag, "Map is ready!" );

			map.setMapType( GoogleMap.MAP_TYPE_NORMAL );

			// https://developers.google.com/maps/documentation/android-sdk/reference/com/google/android/libraries/maps/GoogleMap.OnMapLongClickListener
			// https://developers.google.com/maps/documentation/android-sdk/reference/com/google/android/libraries/maps/GoogleMap.OnInfoWindowLongClickListener
			map.setOnMapLongClickListener( this::createBookmarkDialog );
			map.setOnInfoWindowLongClickListener( ( marker ) -> createBookmarkDialog( marker.getPosition() ) );

			startLocationUpdates();
		} );

		// https://stackoverflow.com/a/11434143
		Handler handler = new Handler();
		handler.postDelayed( new Runnable() {
			@Override
			public void run() {
				updateLocationIndicators(); // Also runs events

				handler.postDelayed( this, mapRefreshDelay );
			}
		}, mapRefreshDelay ); // 1 minute
	}

	private void updateNavigationHeader() {
		// https://www.w3schools.com/mysql/mysql_join.asp
		Database.Query( "SELECT Users.Name AS UserName, Groups.Name AS GroupName FROM Users LEFT JOIN Groups ON Users.Group = Groups.Identifier WHERE Users.Identifier = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				if ( results.next() ) {
					Log.d( Shared.logTag, String.format( "Found user %d in database", userIdentifier ) );

					String userName = results.getString( "UserName" );
					String groupName = results.getString( "GroupName" );

					runOnUiThread( () -> {
						// https://stackoverflow.com/a/6200841

						navigationHeaderNameTextView.setTypeface( Typeface.defaultFromStyle( Typeface.BOLD ) );
						navigationHeaderNameTextView.setText( userName );

						if ( groupName != null ) {
							navigationHeaderGroupTextView.setTypeface( Typeface.defaultFromStyle( Typeface.NORMAL ) );
							navigationHeaderGroupTextView.setText( groupName );
						} else {
							navigationHeaderGroupTextView.setTypeface( Typeface.defaultFromStyle( Typeface.ITALIC ) );
							navigationHeaderGroupTextView.setText( R.string.navigation_header_group_none );
						}
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Could not find user %d in database", userIdentifier ) );
					Shared.logoutUser( myself );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	@SuppressLint( "MissingPermission" ) // Dumb code does not realise it already has permission
	private void startLocationUpdates() {
		// https://developer.android.com/training/location/permissions
		// https://developer.android.com/training/permissions/requesting
		if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_DENIED ) {
			Log.d( Shared.logTag, "Requesting location permissions..." );

			ActivityCompat.requestPermissions( this, new String[] {
					Manifest.permission.ACCESS_COARSE_LOCATION,
					Manifest.permission.ACCESS_FINE_LOCATION
			}, permissionRequestCode );

			return;
		} else {
			Log.d( Shared.logTag, "Location permissions previously granted." );
		}

		// https://stackoverflow.com/a/42218626
		Log.d( Shared.logTag, "Starting location updates..." );
		locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 10000, 10, location -> { // TODO: Set the minimum time to 5 minutes
			String newLatitude = Shared.positionToString( location.getLatitude() );
			String newLongitude = Shared.positionToString( location.getLongitude() );

			mapView.getMapAsync( map -> map.moveCamera( CameraUpdateFactory.newLatLngZoom( new LatLng( location.getLatitude(), location.getLongitude() ), mapViewZoomLevel ) ) );

			// Encrypted column length: 16*(floor(LENGTH/16)+1), 16*(floor(10/16)+1)=16
			Database.Query( "SELECT Identifier, CAST( AES_DECRYPT( Latitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Latitude, CAST( AES_DECRYPT( Longitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Longitude FROM History WHERE User = ? ORDER BY Recorded DESC LIMIT 1;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					Database.InsertEncryptionKey( statement, new int[] { 1, 2 } );
					statement.setInt( 3, userIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( ResultSet results ) throws SQLException {
					if ( results.next() ) {
						String latestLatitude = Shared.positionToString( results.getDouble( "Latitude" ) );
						String latestLongitude = Shared.positionToString( results.getDouble( "Longitude" ) );

						if ( latestLatitude.equals( newLatitude ) && latestLongitude.equals( newLongitude ) ) {
							Log.d( Shared.logTag, String.format( "Not adding location history entry: %s, %s because it is identical to the latest one", newLatitude, newLongitude ) );
							updateLocationIndicators();
							return;
						}
					} else {
						Log.d( Shared.logTag, "No previous location history entries" );
					}

					Database.Query( "INSERT INTO History ( User, Latitude, Longitude ) VALUES ( ?, AES_ENCRYPT( ?, UNHEX( SHA2( ?, 512 ) ) ), AES_ENCRYPT( ?, UNHEX( SHA2( ?, 512 ) ) ) );", new DatabaseCallback() {
						@Override
						public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
							statement.setInt( 1, userIdentifier );

							statement.setString( 2, newLatitude );
							statement.setString( 4, newLongitude );

							Database.InsertEncryptionKey( statement, new int[] { 3, 5 } );

							return statement;
						}

						@Override
						public void OnComplete( int resultCount, ResultSet results ) throws SQLException {
							if ( resultCount == 1 && results.next() ) {
								Log.d( Shared.logTag, String.format( "New history entry: %s, %s with identifier %d", newLatitude, newLongitude, results.getInt( "Identifier" ) ) );

								runOnUiThread( HomeActivity.this::updateLocationIndicators );

							} else {
								Log.d( Shared.logTag, String.format( "Failed to add history entry: %s, %s", newLatitude, newLongitude ) );
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

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} );
		} );
	}

	// https://stackoverflow.com/a/49809415
	@Override
	public void onRequestPermissionsResult( int requestCode, @NonNull String[] requestedPermissions, @NonNull int[] userGrantResults ) {
		super.onRequestPermissionsResult( requestCode, requestedPermissions, userGrantResults );

		if ( requestCode != permissionRequestCode ) return;

		for ( int index = 0; index < requestedPermissions.length; index++ ) {
			String requestedPermission = requestedPermissions[ index ];
			boolean isGranted = ( userGrantResults[ index ] == PackageManager.PERMISSION_GRANTED );

			if ( requestedPermission.equals( Manifest.permission.ACCESS_COARSE_LOCATION ) ) {
				if ( isGranted ) {
					Log.d( Shared.logTag, "Permission granted for approximate location" );
				} else {
					Log.d( Shared.logTag, "Permission denied for approximate location" );
					Popup.Error( this, R.string.activity_home_popup_permission_location_approximate_failure, true );
					break;
				}
			}

			if ( requestedPermission.equals( Manifest.permission.ACCESS_FINE_LOCATION ) ) {
				if ( isGranted ) {
					Log.d( Shared.logTag, "Permission granted for exact location" );

					startLocationUpdates();
				} else {
					Log.d( Shared.logTag, "Permission denied for exact location" );
					Popup.Error( this, R.string.activity_home_popup_permission_location_exact_failure, true );
					break;
				}
			}
		}
	}

	private void updateLocationIndicators() {
		Log.d( Shared.logTag, "Updating location indicators..." );

		for ( Marker marker : currentLocationMarkers ) marker.remove();
		currentLocationMarkers.clear();

		Shared.getUserGroup( myself, userIdentifier, ( groupIdentifier ) -> {

			// User is in a group...
			// NOTE: Same query from the group members activity
			if ( groupIdentifier > 0 ) Database.Query( "SELECT Users.Identifier, Users.Name, CAST( AES_DECRYPT( History.Latitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Latitude, CAST( AES_DECRYPT( History.Longitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Longitude, History.Recorded FROM Users LEFT JOIN History ON History.Identifier = ( SELECT History.Identifier FROM History WHERE History.User = Users.Identifier ORDER BY History.Recorded DESC LIMIT 1 ) LEFT JOIN Groups ON Groups.Identifier = Users.Group WHERE Groups.Identifier = ?;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					Database.InsertEncryptionKey( statement, new int[] { 1, 2 } );
					statement.setInt( 3, groupIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( ResultSet results ) throws SQLException {
					while ( results.next() ) {
						int identifier = results.getInt( "Identifier" );
						String name = results.getString( "Name" );
						double latitude = results.getDouble( "Latitude" );
						double longitude = results.getDouble( "Longitude" );
						Timestamp recorded = results.getTimestamp( "Recorded" );

						if ( recorded == null ) continue; // This user has no location history entries yet

						Log.d( Shared.logTag, String.format( "Last group location for %d (%s) is %f, %f since %d", identifier, name, latitude, longitude, recorded.getTime() ) );
						runOnUiThread( () -> addLocationIndicator( identifier, name, latitude, longitude, recorded ) );
					}
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} );

			// User is not in a group...
			else Database.Query( "SELECT Users.Identifier, Users.Name, CAST( AES_DECRYPT( History.Latitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Latitude, CAST( AES_DECRYPT( History.Longitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Longitude, History.Recorded FROM Users LEFT JOIN History ON History.Identifier = ( SELECT History.Identifier FROM History WHERE History.User = Users.Identifier ORDER BY History.Recorded DESC LIMIT 1 ) WHERE Users.Identifier = ?", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					Database.InsertEncryptionKey( statement, new int[] { 1, 2 } );
					statement.setInt( 3, userIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( ResultSet results ) throws SQLException {
					if ( results.next() ) {
						int identifier = results.getInt( "Identifier" );
						String name = results.getString( "Name" );
						double latitude = results.getDouble( "Latitude" );
						double longitude = results.getDouble( "Longitude" );
						Timestamp recorded = results.getTimestamp( "Recorded" );

						if ( recorded == null ) return; // This user has no location history entries yet

						Log.d( Shared.logTag, String.format( "Last user location for %d (%s) is %f, %f since %d", identifier, name, latitude, longitude, recorded.getTime() ) );
						runOnUiThread( () -> addLocationIndicator( identifier, name, latitude, longitude, recorded ) );
					} else {
						Log.d( Shared.logTag, String.format( "Failed to get last location for user %d", userIdentifier ) );
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

		runEvents();
	}

	private void addLocationIndicator( int identifier, String name, double latitude, double longitude, Timestamp recorded ) {
		LatLng position = new LatLng( latitude, longitude );

		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position( position );
		markerOptions.title( name );
		markerOptions.snippet( String.format( "Since %s", Shared.prettyDateTime( recorded ) ) );
		markerOptions.flat( false );
		markerOptions.visible( true );
		markerOptions.draggable( false );
		markerOptions.alpha( 1.0f );

		if ( identifier == userIdentifier ) {
			markerOptions.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_RED ) );
		} else {
			markerOptions.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_ORANGE ) );
		}

		mapView.getMapAsync( map -> {
			Marker marker = map.addMarker( markerOptions );
			currentLocationMarkers.add( marker );

			Log.d( Shared.logTag, String.format( "Added location indicator at %f, %f for user %s", latitude, longitude, name ) );
		} );
	}

	private void createBookmarkDialog( LatLng position ) {
		View bookmarkCreateView = getLayoutInflater().inflate( R.layout.dialog_bookmark_create, findViewById( R.id.homeConstraintLayout ), false );

		EditText bookmarkNameEditText = bookmarkCreateView.findViewById( R.id.dialogBookmarkCreateNameEditText );
		EditText bookmarkRadiusEditText = bookmarkCreateView.findViewById( R.id.dialogBookmarkCreateRadiusEditText );

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( this );
		dialogBuilder.setTitle( R.string.dialog_bookmark_create_title );
		dialogBuilder.setMessage( String.format( getString( R.string.dialog_bookmark_create_description ), position.latitude, position.longitude ) );
		dialogBuilder.setView( bookmarkCreateView );
		dialogBuilder.setPositiveButton( R.string.dialog_bookmark_create_button_positive, ( dialog, which ) -> {
			String bookmarkName = bookmarkNameEditText.getText().toString();
			int bookmarkRadius;

			if ( bookmarkName.isEmpty() || bookmarkName.length() > 32 ) {
				Log.d( Shared.logTag, "Bookmark name is empty, or too long." );
				Popup.Error( this, R.string.dialog_bookmark_create_error_name_invalid, false );
				return;
			}

			try {
				bookmarkRadius = Integer.parseInt( bookmarkRadiusEditText.getText().toString() );
			} catch ( NumberFormatException exception ) {
				Log.d( Shared.logTag, "Bookmark radius is invalid." );
				Popup.Error( myself, R.string.dialog_bookmark_create_error_radius_invalid, false );
				return;
			}

			if ( bookmarkRadius <= 0 || bookmarkRadius > 100 ) {
				Log.d( Shared.logTag, "Bookmark radius is too low or too high." );
				Popup.Error( this, R.string.dialog_bookmark_create_error_radius_range, false );
				return;
			}

			Database.Query( "INSERT INTO Bookmarks ( User, Name, Radius, Latitude, Longitude ) VALUES ( ?, ?, ?, AES_ENCRYPT( ?, UNHEX( SHA2( ?, 512 ) ) ), AES_ENCRYPT( ?, UNHEX( SHA2( ?, 512 ) ) ) );", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					Database.InsertEncryptionKey( statement, new int[] { 5, 7 } );

					statement.setInt( 1, userIdentifier );

					statement.setString( 2, bookmarkName );
					statement.setInt( 3, bookmarkRadius );

					statement.setString( 4, Shared.positionToString( position.latitude ) );
					statement.setString( 6, Shared.positionToString( position.longitude ) );

					return statement;
				}

				@Override
				public void OnComplete( int resultCount, ResultSet results ) {
					if ( resultCount == 1 ) {
						Log.d( Shared.logTag, String.format( "User created bookmark %s with radius %d", bookmarkName, bookmarkRadius ) );

						runOnUiThread( () -> {
							Popup.Success( myself, R.string.dialog_bookmark_create_popup_success );
							updateBookmarkIndicators();
						} );
					} else {
						Log.d( Shared.logTag, String.format( "Failed to create bookmark %s with radius %d", bookmarkName, bookmarkRadius ) );
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

		AlertDialog bookmarkDialog = dialogBuilder.create();
		bookmarkDialog.setCanceledOnTouchOutside( true );
		bookmarkDialog.show();
	}

	private void updateBookmarkIndicators() {
		Log.d( Shared.logTag, "Updating bookmark indicators..." );

		for ( Marker marker : currentBookmarkMarkers ) marker.remove();
		currentBookmarkMarkers.clear();

		for ( Circle marker : currentBookmarkCircles ) marker.remove();
		currentBookmarkCircles.clear();

		Bookmark.Fetch( userIdentifier, new BookmarkCallback() {
			@Override
			public void OnComplete( ArrayList<Bookmark> bookmarks ) {
				Log.d( Shared.logTag, String.format( "Fetched %d bookmarks for user %d", bookmarks.size(), userIdentifier ) );

				for ( Bookmark bookmark : bookmarks ) {
					runOnUiThread( () -> addBookmarkIndicator( bookmark ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.d( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	private void addBookmarkIndicator( Bookmark bookmark ) {
		// Convert from JavadocMC SimpleLatLng to Google Android Maps SDK LatLng
		LatLng position = new LatLng( bookmark.Position.getLatitude(), bookmark.Position.getLongitude() );

		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position( position );
		markerOptions.title( bookmark.Name );
		markerOptions.snippet( String.format( Locale.UK, "%f, %f", bookmark.Position.getLatitude(), bookmark.Position.getLongitude() ) );
		markerOptions.flat( false );
		markerOptions.visible( true );
		markerOptions.draggable( false );
		markerOptions.alpha( 1.0f );
		markerOptions.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_MAGENTA ) );

		CircleOptions circleOptions = new CircleOptions();
		circleOptions.center( position );
		circleOptions.radius( bookmark.Radius );
		circleOptions.fillColor( 0x11F102F9 );
		circleOptions.strokeColor( 0x44F102F9 );
		circleOptions.strokeWidth( 5 );
		circleOptions.visible( true );

		mapView.getMapAsync( map -> {
			Marker marker = map.addMarker( markerOptions );
			Circle circle = map.addCircle( circleOptions );
			currentBookmarkMarkers.add( marker );
			currentBookmarkCircles.add( circle );

			Log.d( Shared.logTag, String.format( "Added bookmark indicator %s at %f, %f", bookmark.Name, bookmark.Position.getLatitude(), bookmark.Position.getLongitude() ) );
		} );
	}

	private void runEvents() {
		Shared.getUserGroup( myself, userIdentifier, ( groupIdentifier ) -> {
			if ( groupIdentifier <= 0 ) {
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_group_impossible, true ) );
				return;
			}

			Database.Query( "SELECT Users.Identifier, Users.Name, CAST( AES_DECRYPT( History.Latitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Latitude, CAST( AES_DECRYPT( History.Longitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Longitude, History.Recorded FROM Users LEFT JOIN History ON History.Identifier = ( SELECT History.Identifier FROM History WHERE History.User = Users.Identifier ORDER BY History.Recorded DESC LIMIT 1 ) LEFT JOIN Groups ON Groups.Identifier = Users.Group WHERE Groups.Identifier = ?;", new DatabaseCallback() {
				@Override
				public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
					Database.InsertEncryptionKey( statement, new int[] { 1, 2 } );
					statement.setInt( 3, groupIdentifier );
					return statement;
				}

				@Override
				public void OnComplete( ResultSet results ) throws SQLException {
					ArrayList<User> members = new ArrayList<>();

					while ( results.next() ) {
						int memberIdentifier = results.getInt( "Identifier" );
						String memberName = results.getString( "Name" );
						double memberLatitude = results.getDouble( "Latitude" );
						double memberLongitude = results.getDouble( "Longitude" );
						Timestamp memberRecorded = results.getTimestamp( "Recorded" );

						if ( memberRecorded == null ) continue;

						members.add( new User( memberIdentifier, memberName, memberLatitude, memberLongitude, memberRecorded ) );
					}

					Bookmark.Fetch( userIdentifier, new BookmarkCallback() {
						@Override
						public void OnComplete( ArrayList<Bookmark> bookmarks ) {
							Log.d( Shared.logTag, String.format( "Fetched %d bookmarks for user %d", bookmarks.size(), userIdentifier ) );

							Event.Fetch( userIdentifier, new EventCallback() {
								@Override
								public void OnComplete( ArrayList<Event> events ) {
									Log.d( Shared.logTag, String.format( "Got %d events for user %d", events.size(), userIdentifier ) );

									for ( Event event : events ) {
										Log.d( Shared.logTag, String.format( "Event %d, %s, %s, %s", event.Identifier, event.Condition, event.TargetName, event.Bookmark.Name ) );

										if ( event.Condition.equals( "Arrives" ) ) {
											Log.d( Shared.logTag, "This is arrival!" );

											for ( User member : members ) {
												Log.d( Shared.logTag, String.format( "Member %d, %s", member.Identifier, member.Name ) );

												if ( member.Identifier == event.TargetIdentifier ) {
													Log.d( Shared.logTag, "Is the target member!" );

													com.javadocmd.simplelatlng.LatLng lastKnownPosition = lastKnownUserPositions.get( member.Identifier );
													Bookmark lastKnownBookmark = lastKnownUserBookmark.get( member.Identifier );

													if ( lastKnownPosition == null || ( lastKnownPosition.getLatitude() != member.Position.getLatitude() || lastKnownPosition.getLongitude() != member.Position.getLongitude() ) ) {
														Log.d( Shared.logTag, "Last known position was null, or has changed!" );

														if ( lastKnownBookmark == null || ( lastKnownBookmark.Identifier != event.Bookmark.Identifier ) ) {
															Log.d( Shared.logTag, "Last known bookmark was null, or has changed!" );

															if ( event.Bookmark.IsWithinRangeOf( member.Position.getLatitude(), member.Position.getLongitude() ) ) {
																Log.d( Shared.logTag, String.format( "Member is now within bookmark %s", event.Bookmark.Name ) );

																if ( event.Action.equals( "Notification" ) ) {
																	runOnUiThread( () -> Notification.Show( myself, R.string.notification_arrive_title, String.format( getString( R.string.notification_arrive_text ), member.Name, event.Bookmark.Name ) ) );
																}
															}
														}
													}
												} else {
													Log.d( Shared.logTag, "Not the target member..." );
												}
											}

										} else if ( event.Condition.equals( "Leaves" ) ) {
											Log.d( Shared.logTag, "This is leaving!" );

											for ( User member : members ) {
												Log.d( Shared.logTag, String.format( "Member %d, %s", member.Identifier, member.Name ) );

												if ( member.Identifier == event.TargetIdentifier ) {
													Log.d( Shared.logTag, "Is the target member!" );

													com.javadocmd.simplelatlng.LatLng lastKnownPosition = lastKnownUserPositions.get( member.Identifier );
													Bookmark lastKnownBookmark = lastKnownUserBookmark.get( member.Identifier );

													if ( lastKnownPosition == null || ( lastKnownPosition.getLatitude() != member.Position.getLatitude() || lastKnownPosition.getLongitude() != member.Position.getLongitude() ) ) {
														Log.d( Shared.logTag, "Last known position was null, or has changed!" );

														if ( lastKnownBookmark != null && lastKnownBookmark.Identifier == event.Bookmark.Identifier ) {
															Log.d( Shared.logTag, String.format( "Last known bookmark is bookmark %s", event.Bookmark.Name ) );

															if ( !event.Bookmark.IsWithinRangeOf( member.Position.getLatitude(), member.Position.getLongitude() ) ) {
																Log.d( Shared.logTag, String.format( "Member is now outside bookmark %s", event.Bookmark.Name ) );

																if ( event.Action.equals( "Notification" ) ) {
																	runOnUiThread( () -> Notification.Show( myself, R.string.notification_leave_title, String.format( getString( R.string.notification_leave_text ), member.Name, event.Bookmark.Name ) ) );
																}
															}
														}
													}
												} else {
													Log.d( Shared.logTag, "Not the target member..." );
												}
											}
										}
									}

									for ( User member : members ) {
										lastKnownUserPositions.put( member.Identifier, member.Position );
										Log.d( Shared.logTag, String.format( "LAST KNOWN USER POSITION FOR %d NOW %f, %f", member.Identifier, member.Position.getLatitude(), member.Position.getLongitude() ) );

										Bookmark bookmarkMatch = null;
										for ( Bookmark bookmark : bookmarks ) {
											if ( bookmark.IsWithinRangeOf( member.Position.getLatitude(), member.Position.getLongitude() ) ) {
												bookmarkMatch = bookmark;
												break;
											}
										}
										lastKnownUserBookmark.put( member.Identifier, bookmarkMatch );
										if ( bookmarkMatch != null ) {
											Log.d( Shared.logTag, String.format( "LAST KNOWN USER BOOKMARK FOR %d NOW %s", member.Identifier, bookmarkMatch.Name ) );
										} else {
											Log.d( Shared.logTag, String.format( "LAST KNOWN USER BOOKMARK FOR %d NOW NULL", member.Identifier ) );
										}

									}
								}

								@Override
								public void OnException( Exception exception ) {
									Log.d( Shared.logTag, exception.getMessage() );
									runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
								}
							} );
						}

						@Override
						public void OnException( Exception exception ) {
							Log.d( Shared.logTag, exception.getMessage() );
							runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
						}
					} );
				}

				@Override
				public void OnException( Exception exception ) {
					Log.e( Shared.logTag, exception.getMessage() );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			} );
		} );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		if ( drawerToggle.onOptionsItemSelected( item ) ) return true;
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onStart() {
		updateNavigationHeader();

		super.onStart();
		mapView.onStart();
	}

	@Override
	protected void onResume() {
		Log.d( Shared.logTag, "Activity resumed" );

		super.onResume();

		mapView.onResume();
		mapView.getMapAsync( map -> updateBookmarkIndicators() );
	}

	@Override
	protected void onPause() {
		Log.d( Shared.logTag, "Activity paused" );

		super.onPause();
		mapView.onPause();
	}

	@Override
	protected void onStop() {
		Log.d( Shared.logTag, "Activity stopped" );

		super.onStop();
		mapView.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d( Shared.logTag, "Activity destroyed" );

		super.onDestroy();
		// TODO: mapView.onDestroy();
	}

	@Override
	protected void onSaveInstanceState( @NonNull Bundle bundle ) {
		Log.d( Shared.logTag, "Activity state saved" );

		super.onSaveInstanceState( bundle );

		Bundle mapBundle = bundle.getBundle( mapViewStateBundleName );
		if ( mapBundle == null ) {
			mapBundle = new Bundle();
			bundle.putBundle( mapViewStateBundleName, mapBundle );
		}

		mapView.onSaveInstanceState( mapBundle );
	}

	@Override
	public void onLowMemory() {
		Log.d( Shared.logTag, "Activity low on memory" );

		super.onLowMemory();
		mapView.onLowMemory();
	}
}