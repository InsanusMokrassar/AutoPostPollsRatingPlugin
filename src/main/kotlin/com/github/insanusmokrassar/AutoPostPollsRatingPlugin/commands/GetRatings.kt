package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.commands

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.sumBy
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Rating
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.buildCommandFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.Markdown
import com.github.insanusmokrassar.TelegramBotAPI.utils.boldMarkdown
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

private val getRatingsRegex = Regex("(^rating$)|(^availableRatings$)|(^getRatings$)")

internal fun CoroutineScope.enableGetRatingsCommand(
    executorWR: WeakReference<RequestsExecutor>,
    ratingPlugin: RatingPlugin
): Job = launch {
    buildCommandFlow(
        getRatingsRegex
    ).collectWithErrors {
        executorWR.get() ?.let { executor ->
            val averageRatings = ratingPlugin.getRegisteredPosts().associate {
                val ratings = ratingPlugin.getPostRatings(it)
                val averageRating: Rating = ratings.sumBy { it.second } / ratings.size
                it to averageRating
            }

            val messageBuilder = StringBuilder()
            messageBuilder.append("Ratings:".boldMarkdown()).append("\n")

            val ratingsTable: MutableMap<Rating, Int> = HashMap()
            averageRatings.forEach { (_, rating) ->
                ratingsTable[rating] = ratingsTable[rating] ?.plus(1) ?: 1
            }

            ratingsTable.forEach { (rating, count) ->
                messageBuilder.append("\t$rating: $count")
            }

            messageBuilder.append("Average rating: ${ratingsTable.keys.average()}; Ratings count: ${averageRatings.size}")

            executor.executeUnsafe(
                SendMessage(
                    it.chat.id,
                    messageBuilder.toString(),
                    Markdown,
                    disableNotification = true,
                    replyToMessageId = it.messageId
                )
            )
        }
    }
}
