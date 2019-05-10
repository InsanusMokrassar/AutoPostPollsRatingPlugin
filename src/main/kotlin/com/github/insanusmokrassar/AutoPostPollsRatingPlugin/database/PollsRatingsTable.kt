package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Rating
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class PollsRatingsTable : Table() {
    private val postIdColumn = integer("postId").primaryKey()

    /**
     * Percentage * 100
     *
     * For case, of rating 56.23, will be stored 5623
     */
    private val ratingColumn = integer("rating").default(0)

    operator fun contains(postId: PostId): Boolean = transaction {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() != null
    }

    fun enableRating(postId: PostId): Boolean = transaction {
        if (postId !in this@PollsRatingsTable) {
            insert {
                it[postIdColumn] = postId
            }[postIdColumn] == postId
        } else {
            false
        }
    }

    fun updateRating(postId: PostId, rating: Rating): Boolean = transaction {
        update(
            {
                postIdColumn.eq(postId)
            }
        ) {
            it[ratingColumn] = (rating * 1000).toInt()
        } > 0
    }

    operator fun get(postId: PostId): Rating? = transaction {
        select {
            postIdColumn.eq(postId)
        }.firstOrNull() ?.get(ratingColumn) ?.let {
            it.toFloat() / 1000
        }
    }

    fun disableRating(postId: PostId): Rating? = transaction {
        get(postId) ?.also {
            deleteWhere {
                postIdColumn.eq(postId)
            }
        }
    }

    fun enabledRatings(): List<PostId> = transaction {
        selectAll().map {
            it[postIdColumn]
        }
    }
}
