package com.viral32111.locationsharing;

import android.app.Activity;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

public class Popup {

	// https://developer.android.com/guide/topics/ui/dialogs#DialogFragment
	private static AlertDialog Show( Activity activity, String title, @StringRes int messageId ) {
		AlertDialog dialog = new AlertDialog.Builder( activity )
			.setTitle( title )
			.setMessage( messageId )
			.setPositiveButton( "Okay", null )
			.create();

		dialog.setCanceledOnTouchOutside( true ); // https://stackoverflow.com/a/8384124
		dialog.show();

		return dialog;
	}

	private static AlertDialog Show( Activity activity, String title, String message ) {
		AlertDialog dialog = new AlertDialog.Builder( activity )
				.setTitle( title )
				.setMessage( message )
				.setPositiveButton( "Okay", null )
				.create();

		dialog.setCanceledOnTouchOutside( true );
		dialog.show();

		return dialog;
	}

	public static void Error( Activity activity, @StringRes int messageId, boolean closeAfterwards ) {
		AlertDialog dialog = Show( activity, "Error", messageId );
		if ( closeAfterwards ) dialog.setOnDismissListener( ( dialogInterface ) -> activity.finishAndRemoveTask() );

	}

	public static AlertDialog Success( Activity activity, @StringRes int messageId ) {
		return Show( activity, "Success", messageId );
	}

	public static void Success( Activity activity, String message ) {
		Show( activity, "Success", message );
	}

	/*public static AlertDialog Notice( Activity activity, @StringRes int messageId ) {
		return Show( activity, "Notice", messageId );
	}*/

	public static void Confirm( Activity activity, @StringRes int messageId, PopupCallback callback ) {
		// https://stackoverflow.com/a/19288365

		AlertDialog dialog = new AlertDialog.Builder( activity )
			.setTitle( "Confirm" )
			.setMessage( messageId )
			.setPositiveButton( "Yes", ( dialogInterface, componentId ) -> callback.OnAccept() )
			.setNegativeButton( "No", ( dialogInterface, componentId ) -> callback.OnDecline() )
			.create();

		dialog.setCanceledOnTouchOutside( true );
		dialog.setOnDismissListener( dialogInterface -> callback.OnDismiss() );

		dialog.show();
	}

}
