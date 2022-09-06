package com.viral32111.dao

import com.viral32111.models.*
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*

object DatabaseFactory {

	fun initialize( serverAddress: String, serverPort: Int, userName: String, userPassword: String, databaseName: String ) {

		// "Connect" to MySQL database - https://ktor.io/docs/interactive-website-add-persistence.html#connect_db
		// https://github.com/JetBrains/Exposed/wiki/DataBase-and-DataSource#datasource
		Database.connect( "jdbc:mysql://${serverAddress}:${serverPort}/${databaseName}",
			driver = "com.mysql.cj.jdbc.Driver",
			user = userName,
			password = userPassword
		)

		// Create tables - https://ktor.io/docs/interactive-website-add-persistence.html#create_table
		transaction {
			SchemaUtils.create( Users )
		}

	}

	// Executes a query - https://ktor.io/docs/interactive-website-add-persistence.html#queries
	suspend fun <T> databaseQuery( block: suspend () -> T ): T = newSuspendedTransaction( Dispatchers.IO ) {
		block()
	}

}