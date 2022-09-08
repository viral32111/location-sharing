package com.viral32111.locationsharing

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class SetupActivity : AppCompatActivity() {

	override fun onCreate( savedInstanceState: Bundle? ) {

		super.onCreate( savedInstanceState )
		setContentView( R.layout.activity_setup )

		val registerButton = findViewById<Button>( R.id.setupRegisterButton )
		val loginButton = findViewById<Button>( R.id.setupLoginButton )
		val guestButton = findViewById<Button>( R.id.setupGuestButton )
		val serverButton = findViewById<Button>( R.id.setupServerButton )

		loginButton.setOnClickListener {

			startActivity( Intent( this, LoginActivity::class.java ) )

			overridePendingTransition( R.anim.slide_in_from_right, R.anim.slide_out_to_left )

		}

		serverButton.setOnClickListener {

			startActivity( Intent( this, ServerActivity::class.java ) )

			overridePendingTransition( R.anim.slide_in_from_right, R.anim.slide_out_to_left )

		}

	}

}
