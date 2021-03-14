package dev.inmo.AutoPostPollsRatingPlugin.database

import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.PollIdentifier
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

internal typealias MessageIdToPollIdPair = Pair<MessageIdentifier, PollIdentifier>

internal class PollsMessagesTable(
    private val database: Database
) : Table() {
    private val postIdColumn = integer("postId").uniqueIndex()
    private val messageIdColumn = long("messageId").uniqueIndex()
    private val pollIdColumn = text("pollId")

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(this@PollsMessagesTable)
        }
    }

    operator fun contains(postId: PostId): Boolean = transaction(database) {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() != null
    }

    fun registerPoll(postId: PostId, messageId: MessageIdentifier, pollId: PollIdentifier): Boolean = transaction(database) {
        if (postId !in this@PollsMessagesTable) {
            insert {
                it[postIdColumn] = postId
                it[messageIdColumn] = messageId
                it[pollIdColumn] = pollId
            }
            postId in this@PollsMessagesTable
        } else {
            false
        }
    }

    operator fun get(postId: PostId): MessageIdToPollIdPair? = transaction(database) {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() ?.let {
            it[messageIdColumn] to it[pollIdColumn]
        }
    }

    operator fun get(pollId: PollIdentifier): PostId? = transaction(database) {
        select {
            pollIdColumn.eq(pollId)
        }.firstOrNull() ?.get(postIdColumn)
    }

    fun unregisterPoll(postId: PostId): MessageIdToPollIdPair? = transaction(database) {
        get(postId) ?.also {
            deleteWhere {
                postIdColumn.eq(postId)
            }
        }
    }

    fun registeredPolls(): List<PostId> = transaction(database) {
        selectAll().map {
            it[postIdColumn]
        }
    }
}
