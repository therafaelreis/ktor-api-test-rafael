package com.therafaelreis.routes

import com.therafaelreis.apiUser
import com.therafaelreis.model.EPSession
import com.therafaelreis.model.EmojiPhrase
import com.therafaelreis.model.User
import com.therafaelreis.redirect
import com.therafaelreis.repository.Repository
import com.therafaelreis.securityCode
import com.therafaelreis.verifyCode
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.freemarker.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import java.lang.IllegalArgumentException

const val PHRASES = "/phrases"

@Location(PHRASES)
class Phrases

fun Route.phrases(repository: Repository, hash : (String) -> String) {

    get<Phrases> {
        val user = call.sessions.get<EPSession>()?.let { repository.user(it.userId) }
        if(user == null){
            call.redirect(Signin())
        }else{
            val phrases = repository.phrases(user.userId)
            val date = System.currentTimeMillis()
            val code = call.securityCode(date, user, hash)
            call.respond(
                FreeMarkerContent(
                    template = "phrases.ftl",
                    model =  mapOf(
                        "phrases" to phrases,
                        "user" to user,
                        "date" to date,
                        "code" to code
                    ),
                    etag = user.userId
                )
            )
        }

    }

    post<Phrases> {
        val user = call.sessions.get<EPSession>()?.let{ repository.user(it.userId)}

        val params = call.receiveParameters()
        val date = params["date"]?.toLongOrNull() ?: return@post call.redirect(it)
        val code = params["code"] ?: return@post call.redirect(it)
        val action = params["action"] ?: throw IllegalArgumentException("Missing parameter: action")

        if(user == null || !call.verifyCode(date, user, code, hash)){
            call.redirect(Signin())
        }

        when (action) {
            "delete" -> {
                val id = params["id"] ?: throw IllegalArgumentException("Missing parameter: id")
                repository.remove(id)
            }
            "add" -> {
                val emoji = params["emoji"] ?: throw IllegalArgumentException("Missing parameter: emoji")
                val phrase = params["phrase"] ?: throw IllegalArgumentException("Missing parameter: phrase")
                user?.let{localUser ->
                    repository.add(localUser.userId, emoji, phrase)
                }

            }
        }

        call.redirect(Phrases())
            
    }
}