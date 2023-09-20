package com.chromasgaming.plugins

import com.chromasgaming.UserSession
import com.chromasgaming.ktweet.oauth2.TwitterOauth2Authentication
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.html.ButtonType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.nav
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul
import kotlinx.serialization.ExperimentalSerializationApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureRouting() {
    routing {

        get("/login") {
            call.respondRedirect("https://twitter.com/i/oauth2/authorize?response_type=code&client_id=${System.getProperty("clientId")}&redirect_uri=http://127.0.0.1:8080/callback&scope=tweet.write%20tweet.read%20users.read%20offline.access&state=state&code_challenge=challenge&code_challenge_method=plain")
        }

        get("/callback") {
            val state = call.request.queryParameters["state"]
            val code = call.request.queryParameters["code"]
            if (code != null) {
                val oAuth2 = TwitterOauth2Authentication().getBearerToken(
                    code = code,
                    clientId = System.getProperty("clientId"),
                    clientSecret = System.getProperty("clientSecret"),
                    redirectUri = "http://127.0.0.1:8080/callback",
                    pkce = "challenge"
                )

                val expired = LocalDateTime.now().plusSeconds(oAuth2.expiresIn!!.toLong())
                val session = UserSession(oAuth2.accessToken, expired.toEpochSecond(ZoneOffset.UTC), oAuth2.refreshToken!!)
                call.sessions.set(session)
                respondWithOAuth2Html(call, session)
            }
        }

        get("/") {
            val userSession = call.sessions.get<UserSession>()
            if(userSession != null) {
                if(userSession.expiresIn < Instant.now().epochSecond) {
                    val oAuth2 = TwitterOauth2Authentication().useRefreshToken(
                        clientId = System.getProperty("clientId"),
                        clientSecret = System.getProperty("clientSecret"),
                        refreshToken = userSession.refreshToken
                    )

                    val expired = LocalDateTime.now().plusSeconds(oAuth2.expiresIn!!.toLong())
                    val session =
                        UserSession(oAuth2.accessToken, expired.toEpochSecond(ZoneOffset.UTC), oAuth2.refreshToken!!)
                    call.sessions.set(session)
                    respondWithOAuth2Html(call, session)
                } else {
                    respondWithOAuth2Html(call, userSession)
                }
            } else {
                call.respondHtml {
                    body {
                        p {
                            a("/login") { +"Login with Twitter" }
                        }
                    }
                }
            }
        }
    }
}

suspend fun respondWithOAuth2Html(call: ApplicationCall, session: UserSession) {
    call.respondHtml {
        head {
            link(href = "https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css", rel = "stylesheet") {
                attributes.putAll(mapOf("integrity" to "sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC",
                    "crossorigin" to "anonymous"))
            } //
        }
        body {
            nav(classes = "navbar navbar-expand-lg navbar-light bg-light") {
                a(href = "#", classes = "navbar-brand") {
                    text("My Website")
                }
                button(type = ButtonType.button, classes = "navbar-toggler") {
                    attributes.putAll(mapOf("data-toggle" to "collapse", "data-target" to "#navbarNav", "aria-controls" to "navbarNav", "aria-expanded" to "false", "aria-label" to "Toggle navigation"))
                    span(classes = "navbar-toggler-icon")
                }
                div(classes = "collapse navbar-collapse") {
                    id = "navbarNav"
                    ul(classes = "navbar-nav") {
                        li(classes = "nav-item") {
                            a(href = "#", classes = "nav-link") {
                                text("Home")
                            }
                        }
                        li(classes = "nav-item") {
                            a(href = "#about", classes = "nav-link") {
                                text("About")
                            }
                        }
                    }
                }
            }

            // Display the OAuth2 object
            div(classes = "container") {
                div(classes = "row") {
                    div(classes = "col-md-12") {
                        h2 { text("OAuth2 Object") }

                        table(classes = "table table-striped") {
                            thead {
                                tr {
                                    th { text("Field") }
                                    th { text("Value") }
                                }
                            }
                            tbody {
                                for (field in UserSession::class.java.declaredFields) {
                                    field.trySetAccessible()
                                    tr {
                                        td { text(field.name) }
                                        if(field.name == "expiresIn")
                                            td { text(Instant.ofEpochSecond(field.get(session).toString().toLong()).atZone(ZoneId.of("UTC")).toLocalDateTime().toString()) }
                                        else
                                            td { text(field.get(session).toString()) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
