package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.commands.*
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.commands.enableDisableRatingCommand
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.commands.enableEnableRatingCommand
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsRatingsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.asReference
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

internal typealias VariantTransformer = (String) -> Rating?

@Serializable
class PollRatingPlugin(
    @Serializable(RatingsVariantsSerializer::class)
    private val ratingVariants: RatingsVariants,
    private val text: String = "How do you like it?",
    private val autoAttach: Boolean = false,
    private val variantsRatings: Boolean = false
) : MutableRatingPlugin {
    @Transient
    private val pollsRatingsTable = PollsRatingsTable()
    @Transient
    private val pollsMessagesTable = PollsMessagesTable()

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(executor, baseConfig, pluginManager)
        val ratingsTransformers: MutableList<VariantTransformer> = mutableListOf()
        val fullRatingVariants: RatingsVariants = ratingVariants.asSequence().associate { (originalText, rating) ->
            "$originalText ($rating)".also { fullText ->
                ratingsTransformers.add {
                    if (it == fullText || it == originalText) {
                        rating
                    } else {
                        null
                    }
                }
            } to rating
        }

        NewDefaultCoroutineScope(8).apply {
            enableAutoremovingOfPolls(
                executor,
                baseConfig.sourceChatId,
                this@PollRatingPlugin,
                pollsMessagesTable
            )

            enableAutoremovingOnPostRemoved(
                this@PollRatingPlugin,
                PostsTable
            )

            enableAutoaddingOfPolls(
                executor,
                baseConfig.sourceChatId,
                this@PollRatingPlugin,
                (if (variantsRatings) {
                    fullRatingVariants
                } else {
                    ratingVariants
                }).keys.toList(),
                text,
                pollsMessagesTable,
                PostsMessagesTable
            )

            enableRatingUpdatesByPolls(
                ratingsTransformers,
                pollsRatingsTable,
                pollsMessagesTable
            )

            enableEnableRatingCommand(
                this@PollRatingPlugin,
                PostsTable
            )

            enableDisableRatingCommand(
                this@PollRatingPlugin,
                PostsTable
            )

            enableGetRatingsCommand(
                executor.asReference(),
                this@PollRatingPlugin
            )

            if (autoAttach) {
                enableAutoEnablingOfPolls(
                    PostsTable,
                    this@PollRatingPlugin
                )
            }
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
