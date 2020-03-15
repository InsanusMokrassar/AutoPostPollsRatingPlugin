package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.StopPoll
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect

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
