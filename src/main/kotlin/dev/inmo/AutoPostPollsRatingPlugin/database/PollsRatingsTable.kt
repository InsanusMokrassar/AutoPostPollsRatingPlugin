package dev.inmo.AutoPostPollsRatingPlugin.database

import dev.inmo.AutoPostPollsRatingPlugin.*
import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

internal class PollsRatingsTable(
    private val database: Database
) : Table() {
    internal val ratingChangedFlow = MutableSharedFlow<RatingPair>(0, Int.MAX_VALUE, BufferOverflow.SUSPEND)
    internal val ratingEnabledFlow = MutableSharedFlow<PostIdRatingIdPair>(0, Int.MAX_VALUE, BufferOverflow.SUSPEND)
    internal val ratingDisabledFlow = MutableSharedFlow<RatingPair>(0, Int.MAX_VALUE, BufferOverflow.SUSPEND)

    private val postIdColumn = integer("postId")
    override val primaryKey: PrimaryKey = PrimaryKey(postIdColumn)

    /**
     * For case, of rating 56.23, will be stored 5623
     */
    private val ratingColumn = integer("rating").default(0)

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(this@PollsRatingsTable)
        }
    }

    operator fun contains(postId: PostId): Boolean = transaction(database) {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() != null
    }

    fun enableRating(postId: PostId): Boolean = transaction(database) {
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
            ratingEnabledFlow.tryEmit(postId to postId.asRatingId)
        }
    }

    fun upsertRating(postId: PostId, rating: Rating) = transaction(database) {
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

    fun updateRating(postId: PostId, rating: Rating): Boolean = transaction(database) {
        update(
            {
                postIdColumn.eq(postId)
            }
        ) {
            it[ratingColumn] = rating.forDatabase
        } > 0
    }.also {
        if (it) {
            ratingChangedFlow.tryEmit(postId.asRatingId to rating)
        }
    }

    operator fun get(postId: PostId): Rating? = transaction(database) {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() ?.get(ratingColumn) ?.let {
            it.asRating
        }
    }

    operator fun set(postId: PostId, rating: Rating): Boolean = upsertRating(postId, rating)

    fun disableRating(postId: PostId): Rating? = transaction(database) {
        get(postId) ?.also {
            deleteWhere {
                postIdColumn.eq(postId)
            }
        }
    } ?.also {
        ratingDisabledFlow.tryEmit(postId.asRatingId to it)
    }

    fun enabledRatings(): List<PostId> = transaction(database) {
        selectAll().map {
            it[postIdColumn]
        }
    }
}
