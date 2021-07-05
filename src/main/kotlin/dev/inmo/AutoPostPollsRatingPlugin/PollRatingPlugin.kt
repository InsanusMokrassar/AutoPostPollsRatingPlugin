package dev.inmo.AutoPostPollsRatingPlugin

import dev.inmo.AutoPostPollsRatingPlugin.commands.*
import dev.inmo.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import dev.inmo.AutoPostPollsRatingPlugin.database.PollsRatingsTable
import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.base.plugins.PluginManager
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.*
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.AutoPostTelegramBot.utils.SafeLazy
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.utils.extensions.asReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val scope = NewDefaultCoroutineScope(8)
    @Transient
    private val pollsRatingsTable = SafeLazy<PollsRatingsTable>(scope)
    @Transient
    private val pollsMessagesTable = SafeLazy<PollsMessagesTable>(scope)

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

        val postsTable = baseConfig.postsTable
        val postsMessagesTable = baseConfig.postsMessagesTable

        pollsRatingsTable.set(PollsRatingsTable(baseConfig.databaseConfig.database))
        pollsMessagesTable.set(PollsMessagesTable(baseConfig.databaseConfig.database))

        NewDefaultCoroutineScope(8).apply {
            enableAutoremovingOfPolls(
                executor,
                baseConfig.sourceChatId,
                this@PollRatingPlugin,
                pollsMessagesTable.get()
            )

            enableAutoremovingOnPostRemoved(
                this@PollRatingPlugin,
                postsTable
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
                pollsMessagesTable.get(),
                postsMessagesTable
            )

            enableRatingUpdatesByPolls(
                ratingsTransformers,
                pollsRatingsTable.get(),
                pollsMessagesTable.get()
            )

            enableEnableRatingCommand(
                this@PollRatingPlugin,
                postsTable
            )

            enableDisableRatingCommand(
                this@PollRatingPlugin,
                postsTable
            )

            enableGetRatingsCommand(
                executor.asReference(),
                this@PollRatingPlugin
            )

            if (autoAttach) {
                enableAutoEnablingOfPolls(
                    postsTable,
                    this@PollRatingPlugin
                )
            }
        }

        val postsIds = postsTable.getAll()
        getRegisteredPosts().filter {
            it !in postsIds
        }.forEach {
            getPostRatings(it).forEach { (ratingId, _) ->
                deleteRating(ratingId)
            }
        }
    }

    override suspend fun allocateRatingAddedFlow(): Flow<PostIdRatingIdPair> = pollsRatingsTable.get()
        .ratingEnabledFlow
        .asSharedFlow()

    override suspend fun allocateRatingChangedFlow(): Flow<RatingPair> = pollsRatingsTable.get()
        .ratingChangedFlow
        .asSharedFlow()

    override suspend fun allocateRatingRemovedFlow(): Flow<RatingPair> = pollsRatingsTable.get()
        .ratingDisabledFlow
        .asSharedFlow()

    override suspend fun getPostRatings(postId: PostId): List<RatingPair> = listOfNotNull(
        pollsRatingsTable.get()[postId] ?.let {
            postId.asRatingId to it
        }
    )

    override suspend fun getRatingById(ratingId: RatingId): Rating? = pollsRatingsTable.get()[ratingId.asPostId]

    override suspend fun getRegisteredPosts(): List<PostId> = pollsRatingsTable.get().enabledRatings()

    override suspend fun resolvePostId(ratingId: RatingId): PostId? {
        val asPostId = ratingId.asPostId
        return if (asPostId in pollsRatingsTable.get()) {
            asPostId
        } else {
            null
        }
    }

    override suspend fun addRatingFor(postId: PostId): RatingId? = pollsRatingsTable.get().enableRating(
        postId
    ).let {
        if (it) {
            postId.asRatingId
        } else {
            null
        }
    }

    override suspend fun deleteRating(ratingId: RatingId) {
        pollsRatingsTable.get().disableRating(ratingId.asPostId)
    }
}
