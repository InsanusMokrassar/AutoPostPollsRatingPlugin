package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.commands

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.asPostId
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.utils.first
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.buildCommandFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private val enableRatingCommandRegex = Regex("enableRating")
private val disableRatingCommandRegex = Regex("disableRating")
private val reenableRatingCommandRegex = Regex("reenableRating")

internal fun CoroutineScope.enableEnableRatingCommand(
    ratingPlugin: MutableRatingPlugin,
    postsTable: PostsTable
): Job = launch {
    buildCommandFlow(
        enableRatingCommandRegex
    ).collectWithErrors {
        val repliedMessage = it.replyTo ?: return@collectWithErrors
        val postId = postsTable.findPost(repliedMessage.messageId)
        if (ratingPlugin.getPostRatings(postId).isEmpty()) {
            ratingPlugin.addRatingFor(postId)
        }
    }
}

internal fun CoroutineScope.enableDisableRatingCommand(
    ratingPlugin: MutableRatingPlugin,
    postsTable: PostsTable
): Job = launch {
    buildCommandFlow(
        disableRatingCommandRegex
    ).collectWithErrors {
        val repliedMessage = it.replyTo ?: return@collectWithErrors
        val postId = postsTable.findPost(repliedMessage.messageId)
        ratingPlugin.getPostRatings(postId).forEach { (ratingId, _) ->
            ratingPlugin.deleteRating(ratingId)
        }
    }
}

internal fun CoroutineScope.enableReenableRatingCommand(
    ratingPlugin: MutableRatingPlugin,
    postsTable: PostsTable
): Job = launch {
    buildCommandFlow(
        reenableRatingCommandRegex
    ).collectWithErrors {
        val repliedMessage = it.replyTo ?: return@collectWithErrors
        val postId = postsTable.findPost(repliedMessage.messageId)
        ratingPlugin.allocateRatingRemovedFlow().first { (ratingId, _) ->
            ratingId.asPostId == postId || ratingPlugin.getPostRatings(postId).isNotEmpty()
        }.collect {
            ratingPlugin.addRatingFor(postId)
        }
        ratingPlugin.getPostRatings(postId).forEach { (ratingId, _) ->
            ratingPlugin.deleteRating(ratingId)
        }
    }
}

