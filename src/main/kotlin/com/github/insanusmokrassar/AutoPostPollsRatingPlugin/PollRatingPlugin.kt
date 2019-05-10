package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsRatingsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.sql.SchemaUtils

class PollRatingPlugin : MutableRatingPlugin {
    private val pollsRatingsTable = PollsRatingsTable()
    private val pollsMessagesTable = PollsMessagesTable()

    init {
        SchemaUtils.createMissingTablesAndColumns(
            pollsMessagesTable,
            pollsRatingsTable
        )
    }

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(executor, baseConfig, pluginManager)
        NewDefaultCoroutineScope().apply {
            enableAutoremovingOfPolls(
                executor,
                baseConfig.sourceChatId,
                this@PollRatingPlugin,
                pollsMessagesTable
            )
        }
    }

    override suspend fun allocateRatingAddedFlow(): Flow<PostIdRatingIdPair> = pollsRatingsTable
        .ratingEnabledChannel
        .asFlow()

    override suspend fun allocateRatingChangedFlow(): Flow<RatingPair> = pollsRatingsTable
        .ratingChangedChannel
        .asFlow()

    override suspend fun allocateRatingRemovedFlow(): Flow<RatingPair> = pollsRatingsTable
        .ratingDisabledChannel
        .asFlow()

    override suspend fun getPostRatings(postId: PostId): List<RatingPair> = listOfNotNull(
        pollsRatingsTable[postId] ?.let {
            postId.asRatingId to it
        }
    )

    override suspend fun getRatingById(ratingId: RatingId): Rating? = pollsRatingsTable[ratingId.asPostId]

    override suspend fun getRegisteredPosts(): List<PostId> = pollsRatingsTable.enabledRatings()

    override suspend fun resolvePostId(ratingId: RatingId): PostId? {
        val asPostId = ratingId.asPostId
        return if (asPostId in pollsRatingsTable) {
            asPostId
        } else {
            null
        }
    }

    override suspend fun addRatingFor(postId: PostId): RatingId? = pollsRatingsTable.enableRating(
        postId
    ).let {
        if (it) {
            postId.asRatingId
        } else {
            null
        }
    }

    override suspend fun deleteRating(ratingId: RatingId) {
        pollsRatingsTable.disableRating(ratingId.asPostId)
    }
}
