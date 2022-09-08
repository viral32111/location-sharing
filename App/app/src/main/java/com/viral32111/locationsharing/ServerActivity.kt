package com.viral32111.locationsharing

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class ServerActivity : AppCompatActivity() {

	override fun onCreate( savedInstanceState: Bundle? ) {

		super.onCreate( savedInstanceState )
		setContentView( R.layout.activity_server )

		val switchButton = findViewById<Button>( R.id.serverSwitchButton )
		val backButton = findViewById<Button>( R.id.serverBackButton )

		backButton.setOnClickListener {

			finish()

			overridePendingTransition( R.anim.slide_in_from_left, R.anim.slide_out_to_right )

		}

	}

	override fun onBackPressed() {

		super.onBackPressed()

		overridePendingTransition( R.anim.slide_in_from_left, R.anim.slide_out_to_right )

	}

}
