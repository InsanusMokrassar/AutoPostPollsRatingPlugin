package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Rating
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.*

typealias RatingsVariants = Map<String, Rating>

@Serializer(Map::class)
object RatingsVariantsSerializer : KSerializer<RatingsVariants> by LinkedHashMapSerializer(
    StringSerializer,
    FloatSerializer
)
