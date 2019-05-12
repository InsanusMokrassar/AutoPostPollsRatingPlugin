package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.*
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.asRatingId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

internal class PollsRatingsTable : Table() {
    internal val ratingChangedChannel = BroadcastChannel<RatingPair>(Channel.CONFLATED)
    internal val ratingEnabledChannel = BroadcastChannel<PostIdRatingIdPair>(Channel.CONFLATED)
    internal val ratingDisabledChannel = BroadcastChannel<RatingPair>(Channel.CONFLATED)

    private val postIdColumn = integer("postId").primaryKey()

    /**
     * For case, of rating 56.23, will be stored 5623
     */
    private val ratingColumn = integer("rating").default(0)

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(this@PollsRatingsTable)
        }
    }

    operator fun contains(postId: PostId): Boolean = transaction {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() != null
    }

    fun enableRating(postId: PostId): Boolean = transaction {
        if (postId !in this@PollsRatingsTable) {
            insert {
                it[postIdColumn] = postId
            }
            postId in this@PollsRatingsTable
        } else {
            false
        }
    }.also {
        if (it) {
            ratingEnabledChannel.offer(postId to postId.asRatingId)
        }
    }

    fun upsertRating(postId: PostId, rating: Rating) = transaction {
        if (!updateRating(postId, rating)) {
            insert {
                it[postIdColumn] = postId
                it[ratingColumn] = rating.forDatabase
            }
            get(postId) == rating
        } else {
            true
        }
    }

    fun updateRating(postId: PostId, rating: Rating): Boolean = transaction {
        update(
            {
                postIdColumn.eq(postId)
            }
        ) {
            it[ratingColumn] = rating.forDatabase
        } > 0
    }.also {
        if (it) {
            ratingChangedChannel.offer(postId.asRatingId to rating)
        }
    }

    operator fun get(postId: PostId): Rating? = transaction {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() ?.get(ratingColumn) ?.let {
            it.asRating
        }
    }

    operator fun set(postId: PostId, rating: Rating): Boolean = upsertRating(postId, rating)

    fun disableRating(postId: PostId): Rating? = transaction {
        get(postId) ?.also {
            deleteWhere {
                postIdColumn.eq(postId)
            }
        }
    } ?.also {
        ratingDisabledChannel.offer(postId.asRatingId to it)
    }

    fun enabledRatings(): List<PostId> = transaction {
        selectAll().map {
            it[postIdColumn]
        }
    }
}
