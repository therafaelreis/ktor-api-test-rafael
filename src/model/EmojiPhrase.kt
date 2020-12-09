package com.therafaelreis.model

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import java.io.Serializable

data class EmojiPhrase(val id: Int,
                       val userId: String,
                       val emoji: String,
                       val phrase: String): Serializable

object EmojiPharses: IntIdTable(){
    val user: Column<String> = varchar("user_id", 20).index()
    val emoji: Column<String> = varchar("emoji", 255)
    val phrase: Column<String> = varchar("phrase", 255)
}
