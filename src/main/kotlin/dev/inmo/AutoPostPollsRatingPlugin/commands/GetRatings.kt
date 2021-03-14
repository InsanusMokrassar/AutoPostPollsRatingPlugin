package dev.inmo.AutoPostPollsRatingPlugin.commands

import dev.inmo.AutoPostPollsRatingPlugin.sumBy
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.Rating
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.RatingPlugin
import dev.inmo.AutoPostTelegramBot.utils.commands.buildCommandFlow
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.formatting.boldMarkdown
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ParseMode.Markdown
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

private val getRatingsRegex = Regex("(^ratings$)|(^availableRatings$)|(^getRatings$)")

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

            messageBuilder.append("\n```\n")
            ratingsTable.asSequence().sortedBy {
                it.key
            }.toList().forEach { (rating, count) ->
                messageBuilder.append("$rating: $count\n")
            }
            messageBuilder.append("\n```\n")

            messageBuilder.append("Ratings average: ${averageRatings.values.average()};\n")
            messageBuilder.append("Ratings count:   ${averageRatings.size};\n")

            executor.executeUnsafe(
                SendTextMessage(
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
