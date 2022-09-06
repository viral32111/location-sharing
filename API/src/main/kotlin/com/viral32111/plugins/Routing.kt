package com.viral32111.plugins

import com.google.gson.JsonSerializer
import com.viral32111.dao.dao
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Default routing
fun Application.setupRouting() {
	log.info( "Setting up default routing." )

	routing {

		// Hello World
		get( "/hello" ) {
			call.respondText( "Hello World!", ContentType.Text.Plain )
		}

		// Get all users - https://ktor.io/docs/interactive-website-add-persistence.html#update_routes
		get( "/users" ) {
			call.respond( mapOf( "users" to dao.allUsers() ) )
		}

		// Useful for testing in browsers
		get( "/favicon.ico" ) {
			call.respond( HttpStatusCode.Gone )
		}

	}
}
