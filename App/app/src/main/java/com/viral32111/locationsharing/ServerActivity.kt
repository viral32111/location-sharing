package com.viral32111.locationsharing

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch

class ServerActivity : AppCompatActivity() {

	override fun onCreate( savedInstanceState: Bundle? ) {
		super.onCreate( savedInstanceState )
		setContentView( R.layout.activity_server )

		val serverUrlEditText = findViewById<EditText>( R.id.serverUrlEditText )
		val switchButton = findViewById<Button>( R.id.serverSwitchButton )
		val backButton = findViewById<Button>( R.id.serverBackButton )

		val theServerUrl = "https://guatemala-desktop-maryland-sq.trycloudflare.com"

		val offlinePreferences = getSharedPreferences( getString( R.string.config_sharedpreferences_offline ), Context.MODE_PRIVATE )
		with ( offlinePreferences.edit() ) {
			putString( getString( R.string.config_sharedpreferences_offline_serverurl ), theServerUrl )
			apply()
		}

		val serverUrl = offlinePreferences.getString( getString( R.string.config_sharedpreferences_offline_serverurl ), theServerUrl )!!
		serverUrlEditText.setText( serverUrl )

		switchButton.setOnClickListener {
			lifecycleScope.launch {
				val httpClient = HttpClient( CIO )

				val response = httpClient.get( serverUrl )
				println( response.status )
				println( response.requestTime )
				println( response.responseTime )

				httpClient.close()
			}
		}

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
