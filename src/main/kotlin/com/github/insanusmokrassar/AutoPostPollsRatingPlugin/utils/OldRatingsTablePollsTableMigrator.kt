package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.utils

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.asPostId
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsRatingsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.DatabaseConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.load

object OldRatingsTablePollsTableMigrator {
    @JvmStatic
    fun main(vararg args: String) {
        val dbConfig = load(args[0], DatabaseConfig.serializer())
        dbConfig.connect()

        val oldRatingPlugin = RatingPlugin()
        val pollsRatingsTable = PollsRatingsTable()

        val ratings = oldRatingPlugin.postsLikesTable.getRateRange(null, null)

        ratings.forEach { (ratingId, rating) ->
            pollsRatingsTable[ratingId.asPostId] = rating
        }
    }
}