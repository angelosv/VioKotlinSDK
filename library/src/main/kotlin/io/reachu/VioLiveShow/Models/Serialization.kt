package io.reachu.liveshow.models

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.reachu.sdk.core.helpers.JsonUtils
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Ensures the shared Jackson mapper understands java.time instants before
 * any of the migrated models rely on JSON decoding.
 */
object LiveShowJacksonConfigurator {
    init {
        JsonUtils.mapper.registerModule(JavaTimeModule())
        JsonUtils.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}

/**
 * kotlinx.serialization helper used by the migrated models to match the Swift
 * ISO-8601 date handling semantics.
 */
object InstantIso8601Serializer : KSerializer<Instant> {
    private val formatter = DateTimeFormatter.ISO_INSTANT

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InstantISO8601", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatter.format(value))
    }
}
