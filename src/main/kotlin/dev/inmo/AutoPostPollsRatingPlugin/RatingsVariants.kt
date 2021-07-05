package dev.inmo.AutoPostPollsRatingPlugin

import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.Rating
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.internal.*

typealias RatingsVariants = Map<String, Rating>

@Serializer(Map::class)
object RatingsVariantsSerializer : KSerializer<RatingsVariants> by MapSerializer(
    String.serializer(),
    Float.serializer()
)
