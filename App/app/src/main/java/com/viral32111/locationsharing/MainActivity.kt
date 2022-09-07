package com.viral32111.locationsharing

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

	override fun onCreate( savedInstanceState: Bundle? ) {

		super.onCreate( savedInstanceState )
		setContentView( R.layout.activity_main )

		/*supportActionBar?.title = "Location Sharing"
		supportActionBar?.subtitle = "Login to your account"*/

		supportActionBar?.title = "Login to your account"

	}

	override fun onCreateOptionsMenu( menu: Menu ): Boolean {
		super.onCreateOptionsMenu( menu )

		val inflater = menuInflater
		inflater.inflate( R.menu.menu_options, menu )

		return true
	}

}
