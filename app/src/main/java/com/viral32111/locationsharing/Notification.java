package com.viral32111.locationsharing;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.core.app.NotificationCompat;

import java.util.Random;

public class Notification {
	private static final String channelID = "Location Sharing App";

	private static final Random random = new Random();
	private static NotificationManager notificationManager;

	public static void Setup( Activity activity, int channelTitleId, int channelDescriptionId ) {
		NotificationChannel notificationChannel = new NotificationChannel( channelID, activity.getString( channelTitleId ), NotificationManager.IMPORTANCE_DEFAULT );
		notificationChannel.setDescription( activity.getString( channelDescriptionId) );

		notificationManager = activity.getSystemService( NotificationManager.class );
		notificationManager.createNotificationChannel( notificationChannel );
	}

	public static int Show( Activity activity, int titleId, String text ) {
		if ( notificationManager == null ) return -1;

		int notificationId = random.nextInt( 1024 );

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder( activity, channelID );
		notificationBuilder.setSmallIcon( R.drawable.notification_icon );
		notificationBuilder.setContentTitle( activity.getString( titleId ) );
		notificationBuilder.setContentText( text );
		notificationBuilder.setPriority( NotificationCompat.PRIORITY_DEFAULT );

		notificationManager.notify( notificationId, notificationBuilder.build() );

		return notificationId;
	}
}
