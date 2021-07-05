package dev.inmo.AutoPostPollsRatingPlugin

import dev.inmo.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.*
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.requests.DeleteMessage
import dev.inmo.tgbotapi.requests.StopPoll
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow

internal fun CoroutineScope.enableAutoremovingOnPostRemoved(
    ratingPlugin: MutableRatingPlugin,
    postsTable: PostsBaseInfoTable
): Job = launch {
    postsTable.getAll().also { posts ->
        ratingPlugin.getRegisteredPosts().filter {
            it !in posts
        }.forEach {
            ratingPlugin.getPostRatings(it).forEach {
                ratingPlugin.deleteRating(it.first)
            }
        }
    }
    postsTable.postRemovedChannel.asFlow().collectWithErrors {
        ratingPlugin.getPostRatings(it).forEach {
            ratingPlugin.deleteRating(it.first)
        }
    }
}

internal fun CoroutineScope.enableAutoremovingOfPolls(
    executor: RequestsExecutor,
    chatId: ChatId,
    ratingPlugin: RatingPlugin,
    pollsMessagesTable: PollsMessagesTable,
    closeIfUnsuccess: Boolean = true
): Job = launch {
    ratingPlugin.allocateRatingRemovedFlow().collectWithErrors {
        removePoll(executor, chatId, closeIfUnsuccess, pollsMessagesTable, it)
    }
}

private suspend fun removePoll(
    executor: RequestsExecutor,
    chatId: ChatId,
    closeIfUnsuccess: Boolean,
    pollsMessagesTable: PollsMessagesTable,
    ratingPair: RatingPair
) {
    val pollInfo = pollsMessagesTable[ratingPair.first.asPostId] ?: return@removePoll

    executor.executeUnsafe(
        DeleteMessage(
            chatId,
            pollInfo.first
        )
    ) ?: if (closeIfUnsuccess) {
        executor.executeUnsafe(
            StopPoll(
                chatId,
                pollInfo.first
            )
        )
    }

    pollsMessagesTable.unregisterPoll(ratingPair.first.asPostId)
}
