package com.chromasgaming

import com.chromasgaming.ktweet.models.OAuth
import com.chromasgaming.ktweet.models.OAuth2
import com.chromasgaming.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

val applicationHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
}

fun main() {
    embeddedServer(io.ktor.server.cio.CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module(httpClient: HttpClient = applicationHttpClient) {
    install(Sessions) {
        cookie<UserSession>("user_session")
    }
    configureRouting()
}


data class UserSession(val accessToken: String, val expiresIn: Long, val refreshToken: String)