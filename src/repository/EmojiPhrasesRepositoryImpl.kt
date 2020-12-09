package com.therafaelreis.repository

import com.therafaelreis.model.EmojiPharses
import com.therafaelreis.model.EmojiPhrase
import com.therafaelreis.model.User
import com.therafaelreis.model.Users
import com.therafaelreis.repository.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.IllegalArgumentException

class EmojiPhrasesRepositoryImpl : Repository {

    override suspend fun add(userId: String, emojiValue: String, phraseValue: String) =
        dbQuery {
            val insertStatement = EmojiPharses.insert {
                it[user] = userId
                it[emoji] = emojiValue
                it[phrase] = phraseValue
            }

            val result = insertStatement.resultedValues?.get(0)

            if (result != null) {
                toEmojiPhrase(result)
            } else {
                null
            }
        }

    override suspend fun phrase(id: Int): EmojiPhrase? = dbQuery {
        EmojiPharses.select {
            (EmojiPharses.id eq id)
        }.mapNotNull { toEmojiPhrase(it) }
            .singleOrNull()
    }

    override suspend fun phrase(id: String): EmojiPhrase? {
        return phrase(id.toInt())
    }

    override suspend fun phrases(userId: String): List<EmojiPhrase> = dbQuery {
        EmojiPharses.select {
            EmojiPharses.user eq userId
        }.mapNotNull {
            toEmojiPhrase(it)
        }
    }

    override suspend fun remove(id: Int): Boolean {
        if (phrase(id) == null) {
            throw IllegalArgumentException("No phrase found for id $id.")
        }

        return dbQuery {
            EmojiPharses.deleteWhere { EmojiPharses.id eq id } > 0
        }
    }

    override suspend fun remove(id: String): Boolean {
        return remove(id.toInt())
    }

    override suspend fun clear() {
        return dbQuery {
            EmojiPharses.deleteAll()
        }
    }

    override suspend fun userById(id: String): User? = dbQuery {
        Users.select { Users.id.eq(id) }
            .map {
                User(
                    userId = id,
                    email = it[Users.email],
                    displayName = it[Users.displayName],
                    passwordHash = it[Users.passwordHash]
                )
            }.singleOrNull()
    }

    override suspend fun createUser(user: User) {
        transaction {
            Users.insert {
                it[id] = user.userId
                it[displayName] = user.displayName
                it[passwordHash] = user.passwordHash
                it[email] = user.email
            }
        }
    }

    override suspend fun user(userId: String, hash: String?): User? {
        val user = dbQuery {
            Users.select {
                Users.id eq userId
            }.mapNotNull {
                toUser(it)
            }.singleOrNull()
        }

        return when {
            user == null -> null
            hash == null -> user
            user.passwordHash == hash -> user
            else -> null
        }
    }

    override suspend fun userByEmail(email: String): User? = dbQuery {
        Users.select {
            Users.email.eq(email)
        }.map {
            User(
                userId = it[Users.id],
                email = email,
                displayName = it[Users.displayName],
                passwordHash = it[Users.passwordHash]
            )
        }.singleOrNull()
    }


    private fun toEmojiPhrase(row: ResultRow): EmojiPhrase =
        EmojiPhrase(
            id = row[EmojiPharses.id].value,
            userId = row[EmojiPharses.user],
            emoji = row[EmojiPharses.emoji],
            phrase = row[EmojiPharses.phrase]
        )

    private fun toUser(row: ResultRow): User =
        User(
            userId = row[Users.id],
            email = row[Users.email],
            passwordHash = row[Users.passwordHash],
            displayName = row[Users.displayName]
        )
}