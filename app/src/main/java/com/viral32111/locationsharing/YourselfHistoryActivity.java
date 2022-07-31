package com.viral32111.locationsharing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class YourselfHistoryActivity extends AppCompatActivity {

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
		setContentView( R.layout.activity_yourself_history );

		// https://stackoverflow.com/a/3952629
		/*ScrollView scrollView = findViewById( R.id.yourselfHistoryScrollView );
		scrollView.setOnScrollChangeListener( ( view, newScrollX, newScrollY, oldScrollX, oldScrollY ) -> {
			Log.d( Shared.logTag, String.format( "scrolling was %d, now %d", oldScrollY, newScrollY ) );

			// https://stackoverflow.com/a/12428154
			// TODO: Check if history entry is visible, and if it is activate the Map
		} );*/

		linearLayout = findViewById( R.id.yourselfHistoryLinearLayout );
		layoutInflater = getLayoutInflater();

		userIdentifier = getIntent().getIntExtra( "userIdentifier", 0 );

		Bookmark.Fetch( userIdentifier, new BookmarkCallback() {
			@Override
			public void OnComplete( ArrayList<Bookmark> bookmarks ) {
				Log.d( Shared.logTag, String.format( "Fetched %d bookmarks for user %d", bookmarks.size(), userIdentifier ) );

				updateHistoryEntries( bookmarks );
			}

			@Override
			public void OnException( Exception exception ) {
				Log.d( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	private void updateHistoryEntries( ArrayList<Bookmark> bookmarks ) {
		Database.Query( "SELECT CAST( AES_DECRYPT( Latitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Latitude, CAST( AES_DECRYPT( Longitude, UNHEX( SHA2( ?, 512 ) ) ) AS DOUBLE ) AS Longitude, Recorded FROM History WHERE User = ? ORDER BY Recorded DESC;", new DatabaseCallback() {
			@Override
			public PreparedStatement OnPopulate( PreparedStatement statement ) throws SQLException {
				Database.InsertEncryptionKey( statement, new int[] { 1, 2 } );
				statement.setInt( 3, userIdentifier );
				return statement;
			}

			@Override
			public void OnComplete( ResultSet results ) throws SQLException {
				while ( results.next() ) {
					double latitude = results.getDouble( "Latitude" );
					double longitude = results.getDouble( "Longitude" );
					Timestamp recorded = results.getTimestamp( "Recorded" );

					Log.d( Shared.logTag, String.format( "Location history entry at %f, %f", latitude, longitude ) );

					Bookmark bookmarkMatch = null;
					for ( Bookmark bookmark : bookmarks ) {
						Log.d( Shared.logTag, String.format( "Bookmark %s at %f, %f", bookmark.Name, bookmark.Position.getLatitude(), bookmark.Position.getLongitude() ) );

						if ( bookmark.IsWithinRangeOf( latitude, longitude ) ) {
							Log.d( Shared.logTag, String.format( "Bookmark %s matches %f, %f", bookmark.Name, latitude, longitude ) );
							bookmarkMatch = bookmark;
							break;
						} else {
							Log.d( Shared.logTag, String.format( "Bookmark %s does not match %f, %f", bookmark.Name, latitude, longitude ) );
						}
					}

					final Bookmark bookmarkMatchFinal = bookmarkMatch; // Effectively final
					runOnUiThread( () -> createHistoryEntry( recorded, latitude, longitude, bookmarkMatchFinal ) );
				}
			}

			@Override
			public void OnException( Exception exception ) {
				Log.e( Shared.logTag, exception.getMessage() );
				runOnUiThread( () -> Popup.Error( myself, R.string.popup_issue, false ) );
			}
		} );
	}

	// https://stackoverflow.com/a/41500409
	private void createHistoryEntry( Timestamp recorded, double latitude, double longitude, Bookmark bookmark ) {
		LatLng position = new LatLng( latitude, longitude );

		View historyEntry = layoutInflater.inflate( R.layout.activity_yourself_history_entry, linearLayout, false );

		TextView dateName = historyEntry.findViewById( R.id.yourselfHistoryEntryDateNameTextView );
		MapView mapView = historyEntry.findViewById( R.id.yourselfHistoryEntryPreviewMapView );

		if ( bookmark != null ) {
			dateName.setText( String.format( getString( R.string.activity_yourself_history_entry_datebookmark ), Shared.prettyDateTime( recorded ), bookmark.Name ) );
		} else {
			dateName.setText( String.format( getString( R.string.activity_yourself_history_entry_dateposition ), Shared.prettyDateTime( recorded ), latitude, longitude ) );
		}

		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position( position );
		markerOptions.flat( false );
		markerOptions.visible( true );
		markerOptions.draggable( false );
		markerOptions.alpha( 1.0f );
		markerOptions.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_RED ) );

		mapView.onCreate( null );
		mapView.onStart();
		mapView.onResume();

		mapView.getMapAsync( map -> {
			map.setMapType( GoogleMap.MAP_TYPE_TERRAIN );

			// https://stackoverflow.com/a/61146384
			UiSettings mapSettings = map.getUiSettings();
			mapSettings.setZoomControlsEnabled( false );
			mapSettings.setZoomGesturesEnabled( false );
			mapSettings.setCompassEnabled( false );
			mapSettings.setScrollGesturesEnabled( false );
			mapSettings.setScrollGesturesEnabledDuringRotateOrZoom( false );
			mapSettings.setMyLocationButtonEnabled( false );

			map.addMarker( markerOptions );

			map.moveCamera( CameraUpdateFactory.newLatLngZoom( position, 18 ) );
		} );

		linearLayout.addView( historyEntry );
	}

}