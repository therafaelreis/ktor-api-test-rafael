package com.therafaelreis.api

import com.therafaelreis.API_VERSION
import com.therafaelreis.api.request.PhrasesApiRequest
import com.therafaelreis.apiUser
import com.therafaelreis.model.EPSession
import com.therafaelreis.repository.Repository
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*

const val PHRASE_API_ENDPOINT = "$API_VERSION/phrases"

@Location(PHRASE_API_ENDPOINT)
class PhrasesApi

fun Route.phrasesApi(repository: Repository) {
    authenticate("jwt") {

        get<PhrasesApi> {
            val user = call.sessions.get<EPSession>()?.let { repository.user(it.userId) }
            user?.let {
                call.respond(repository.phrases(user.userId))
            }
        }

        post<PhrasesApi> {
            val user = call.apiUser
            user?.let {
                try {
                    val request = call.receive<PhrasesApiRequest>()
                    val phrase =
                        repository.add(userId = user.userId, emojiValue = request.emoji, phraseValue = request.phrase)

                    if(phrase != null){
                        call.respond(phrase)
                    }else{
                        call.respondText("Invalid data received", status = HttpStatusCode.InternalServerError)
                    }
                }catch (e: Throwable){
                    call.respondText("Invalid data received", status = HttpStatusCode.BadRequest)
                }
            }
        }

    }
}
