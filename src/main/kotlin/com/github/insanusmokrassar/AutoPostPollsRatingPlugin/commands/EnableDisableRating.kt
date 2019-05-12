package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.buildCommandFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

private val enableRatingCommandRegex = Regex("enableRating")

internal fun CoroutineScope.enableEnableRatingCommand(
    ratingPlugin: MutableRatingPlugin,
    postsMessagesTable: PostsMessagesTable
): Job = launch {
    buildCommandFlow(
        enableRatingCommandRegex
    ).collect {
        val repliedMessage = it.replyTo ?: return@collect
        val postId = postsMessagesTable.findPostByMessageId(repliedMessage.messageId) ?: return@collect
        if (ratingPlugin.getPostRatings(postId).isEmpty()) {
            ratingPlugin.addRatingFor(postId)
        }
    }
}

