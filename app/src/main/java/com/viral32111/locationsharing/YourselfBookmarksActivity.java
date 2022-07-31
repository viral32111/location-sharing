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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class YourselfBookmarksActivity extends AppCompatActivity {

	// Standard
	private final Activity myself = this;

	// Database
	private int userIdentifier;

	// Dynamic Layouts
	private LinearLayout linearLayout;
	private LayoutInflater layoutInflater;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_yourself_bookmarks );

		linearLayout = findViewById( R.id.yourselfBookmarksLinearLayout );
		layoutInflater = getLayoutInflater();

		userIdentifier = getIntent().getIntExtra( "userIdentifier", 0 );

		updateBookmarks();
	}

	private void updateBookmarks() {
		linearLayout.removeAllViews();

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

					runOnUiThread( () -> createBookmarkEntry( identifier, name, radius, latitude, longitude ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	private void createBookmarkEntry( int identifier, String name, int radius, Double latitude, Double longitude ) {
		LatLng position = new LatLng( latitude, longitude );

		View historyEntry = layoutInflater.inflate( R.layout.activity_yourself_bookmarks_entry, linearLayout, false );

		TextView nameTextView = historyEntry.findViewById( R.id.yourselfBookmarksEntryNameTextView );
		Button deleteButton = historyEntry.findViewById( R.id.yourselfBookmarksEntryDeleteButton );
		MapView mapView = historyEntry.findViewById( R.id.yourselfBookmarksEntryPreviewMapView );

		nameTextView.setText( String.format( getString( R.string.activity_yourself_bookmarks_entry_textview_name ), name ) );

		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position( position );
		markerOptions.flat( false );
		markerOptions.visible( true );
		markerOptions.draggable( false );
		markerOptions.alpha( 1.0f );
		markerOptions.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_MAGENTA ) );

		CircleOptions circleOptions = new CircleOptions();
		circleOptions.center( position );
		circleOptions.radius( radius );
		circleOptions.fillColor( 0x11F102F9 );
		circleOptions.strokeColor( 0x44F102F9 );
		circleOptions.strokeWidth( 5 );
		circleOptions.visible( true );

		mapView.onCreate( null );
		mapView.onStart();
		mapView.onResume();

		mapView.getMapAsync( map -> {
			map.setMapType( GoogleMap.MAP_TYPE_TERRAIN );

			UiSettings mapSettings = map.getUiSettings();
			mapSettings.setZoomControlsEnabled( false );
			mapSettings.setZoomGesturesEnabled( false );
			mapSettings.setCompassEnabled( false );
			mapSettings.setScrollGesturesEnabled( false );
			mapSettings.setScrollGesturesEnabledDuringRotateOrZoom( false );
			mapSettings.setMyLocationButtonEnabled( false );

			map.addMarker( markerOptions );
			map.addCircle( circleOptions );

			map.moveCamera( CameraUpdateFactory.newLatLngZoom( position, 18 ) );
		} );

		deleteButton.setOnClickListener( ( view ) -> Database.Query( "DELETE FROM Bookmarks WHERE Identifier = ? AND User = ? LIMIT 1;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				statement.setInt( 1, identifier );
				statement.setInt( 2, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( int resultCount ) {
				if ( resultCount == 1 ) {
					Log.d( Shared.logTag, String.format( "Deleted bookmark %d for user %d", identifier, userIdentifier ) );

					runOnUiThread( () -> {
						Popup.Success( myself, R.string.activity_yourself_bookmarks_popup_deleted );
						updateBookmarks();
					} );
				} else {
					Log.d( Shared.logTag, String.format( "Failed to delete bookmark %d for group %d", identifier, userIdentifier ) );
					runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} ) );

		linearLayout.addView( historyEntry );
	}

}