package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Rating

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
internal inline fun <T> Iterable<T>.sumBy(selector: (T) -> Rating): Rating {
    var sum: Rating = 0F
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
