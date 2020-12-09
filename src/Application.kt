package com.therafaelreis

import com.therafaelreis.api.login
import com.therafaelreis.api.phrasesApi
import com.therafaelreis.model.EPSession
import com.therafaelreis.model.User
import com.therafaelreis.repository.DatabaseFactory
import com.therafaelreis.repository.EmojiPhrasesRepositoryImpl
import com.therafaelreis.routes.*
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import java.net.URI
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(DefaultHeaders)

    install(StatusPages) {
        exception<Throwable> { e ->
            call.respondText(
                e.localizedMessage,
                ContentType.Text.Plain, HttpStatusCode.InternalServerError
            )
        }
    }

    install(ContentNegotiation) {
        gson()
    }

    // initialize freemarker template
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    install(Locations)
    install(Sessions) {
        cookie<EPSession>("SESSION") {
            transform(SessionTransportTransformerMessageAuthentication(hashKey))
        }
    }
    val hash = { s: String -> hash(s) }

    DatabaseFactory.init()

    val repository = EmojiPhrasesRepositoryImpl()
    val jwtService = JWTService()

    install(Authentication){
        jwt("jwt") {
            verifier(jwtService.verifier)
            realm = "emojihphrases app"

            validate {
                val payload = it.payload
                val claim = payload.getClaim("id")
                val claimString = claim.asString()
                val user = repository.userById(claimString)
                user
            }
        }
    }

    routing {
        static("/static") {
            resources("images")
        }
        index(repository)
        about(repository)
        signin(repository, hash)
        signout()
        signup(repository, hash)

        // api
        phrases(repository = repository, hash = hash)
        login(repository = repository, jwtService = jwtService)
        phrasesApi(repository = repository)
    }
}

const val API_VERSION = "/api/v1"
suspend fun ApplicationCall.redirect(location: Any) {
    respondRedirect(application.locations.href(location))
}

fun ApplicationCall.referrerHost() = request.header(HttpHeaders.Referrer)?.let { URI.create(it).host }

fun ApplicationCall.securityCode(date: Long, user: User, hash: (String) -> String) =
    hash("$date:${user.userId}:${request.host()}:${referrerHost()}")

fun ApplicationCall.verifyCode(date: Long, user: User, code: String, hash: (String) -> String) =
    securityCode(date, user, hash) == code
            && (System.currentTimeMillis() - date).let {
        it > 0 && it < TimeUnit.MILLISECONDS.convert(
            2,
            TimeUnit.HOURS
        )
    }

val ApplicationCall.apiUser
    get() = authentication.principal<User>()

