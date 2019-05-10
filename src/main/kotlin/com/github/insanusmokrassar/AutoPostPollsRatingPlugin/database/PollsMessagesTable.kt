package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Rating
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.PollIdentifier
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

internal typealias MessageIdToPollIdPair = Pair<MessageIdentifier, PollIdentifier>

internal class PollsMessagesTable : Table() {
    private val postIdColumn = integer("postId").primaryKey()
    private val messageIdColumn = long("messageId").uniqueIndex()
    private val pollIdColumn = text("pollId")

    operator fun contains(postId: PostId): Boolean = transaction {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() != null
    }

    fun registerPoll(postId: PostId, messageId: MessageIdentifier, pollId: PollIdentifier): Boolean = transaction {
        if (postId !in this@PollsMessagesTable) {
            insert {
                it[postIdColumn] = postId
                it[messageIdColumn] = messageId
                it[pollIdColumn] = pollId
            }[postIdColumn] == postId
        } else {
            false
        }
    }

    operator fun get(postId: PostId): MessageIdToPollIdPair? = transaction {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() ?.let {
            it[messageIdColumn] to it[pollIdColumn]
        }
    }

    operator fun get(pollId: PollIdentifier): PostId? = transaction {
        select {
            pollIdColumn.eq(pollId)
        }.firstOrNull() ?.get(postIdColumn)
    }

    fun unregisterPoll(postId: PostId): MessageIdToPollIdPair? = transaction {
        get(postId) ?.also {
            deleteWhere {
                postIdColumn.eq(postId)
            }
        }
    }

    fun registeredPolls(): List<PostId> = transaction {
        selectAll().map {
            it[postIdColumn]
        }
    }
}
