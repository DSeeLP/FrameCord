/*
 * Created by Dirk on 22.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(Version.Serializer::class)
data class Version(
    val major: Int,
    val minor: Int? = null,
    val patch: Int? = null,
    val identifier: Identifier = Identifier.RELEASE,
    val build: String? = null
) {

    object Serializer : KSerializer<Version> {
        override fun deserialize(decoder: Decoder): Version = parse(decoder.decodeString())

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("version", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Version) {
            encoder.encodeString(value.toString())
        }
    }


    init {
        val lazyMessage = { "Major, minor, patch version must be higher or equals 0" }
        check(major >= 0, lazyMessage)
        if (minor != null) check(minor >= 0, lazyMessage)
        if (patch != null) check(patch >= 0, lazyMessage)
        if (major == 0 && (minor == 0 || minor == null) && (patch == 0 || patch == null)) throw IllegalStateException("The version must at least be 0.0.1")
        if (build != null) {
            if (!build.checkString()) throw IllegalStateException("The version build must not contain an minus, plus or point")
            val bn = build.toIntOrNull()
            if (bn != null && bn <= 0) throw IllegalStateException("The version build number must be higher than 0")
        }
    }

    @Serializable(Identifier.Serializer::class)
    class Identifier private constructor(val displayName: String, val isRedundant: Boolean, vararg names: String) {
        val names: Array<String> = names.toList().toTypedArray()

        companion object {

            private val map = mutableMapOf<String, Identifier>()
            operator fun get(name: String) = name.uppercase().let { upperName ->
                (map.filter { it.key == upperName || it.value.names.contains(upperName) }.values.firstOrNull()
                    ?: map[upperName])
                    ?: throw IllegalStateException("Version identifier with display name ${name.uppercase()} couldn't be found")
            }

            fun register(displayName: String, isRedundant: Boolean = false, vararg names: String): Identifier {
                val identifier =
                    Identifier(displayName.uppercase(), isRedundant, *names.map { it.uppercase() }.toTypedArray())
                map[displayName.uppercase()] = identifier
                return identifier
            }

            val RELEASE = register("RELEASE", true, "RELEASE")
            val RELEASE_CANDIDATE = register("RC", false, "RC")
            val SNAPSHOT = register("SNAPSHOT", false, "SNAPSHOT")
            val BETA = register("BETA", false, "BETA")
            val ALPHA = register("ALPHA", false, "ALPHA")
        }

        object Serializer : KSerializer<Identifier> {
            override fun deserialize(decoder: Decoder): Identifier = get(decoder.decodeString())

            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("identifier", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: Identifier) {
                encoder.encodeString(value.displayName)
            }

        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Identifier) return false

            if (displayName != other.displayName) return false
            if (isRedundant != other.isRedundant) return false
            if (!names.contentEquals(other.names)) return false

            return true
        }

        override fun hashCode(): Int = displayName.hashCode()
    }

    override fun toString(): String = toString(false)

    fun toString(displayRedundantIdentifiers: Boolean): String = buildString {
        append(major)
        if (minor != null) append(".$minor")
        if (patch != null) append(".$patch")
        if (build != null) append("+$build")
        val identifierString = "-${identifier.displayName}"
        if (identifier.isRedundant) {
            if (displayRedundantIdentifiers) append(identifierString)
        } else append(identifierString)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Version) return false

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false
        if (identifier != other.identifier) return false
        if (build != other.build) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + (minor ?: 0)
        result = 31 * result + (patch ?: 0)
        result = 31 * result + identifier.hashCode()
        result = 31 * result + (build?.hashCode() ?: 0)
        return result
    }


    companion object {
        fun parse(version: String): Version {
            val versionSplitted = version.split('.')
            val buildSplitted = version.split('+')
            val identifierSplitted = version.split('-')
            fun String.removeStrings(buildNumber: Boolean = true, identifier: Boolean = true): String {
                var result = this
                if (buildSplitted.size > 1 && buildNumber) result =
                    result.replace("+${buildSplitted[1].replace("-${identifierSplitted[1]}", "")}", "")
                if (identifierSplitted.size > 1 && identifier) result =
                    result.replace("-${identifierSplitted[1].replace("+${buildSplitted[1]}", "")}", "")
                return result
            }

            val major =
                if (versionSplitted.size > 1) versionSplitted[0].removeStrings()
                    .toInt() else TODO("Handle case when version doesn't contains any points")
            val minor = if (versionSplitted.size >= 2) versionSplitted[1].removeStrings().toInt() else null
            val patch = if (versionSplitted.size >= 3) versionSplitted[2].removeStrings().toInt() else null
            val build = if (buildSplitted.size == 2) buildSplitted[1].removeStrings(false) else null
            val identifier =
                if (identifierSplitted.size == 2) identifierSplitted[1].removeStrings(identifier = false) else null
            return Version(major, minor, patch, identifier?.let { Identifier[it] } ?: Identifier.RELEASE, build)
        }

        private val stringConditions = arrayOf<(String) -> Boolean>(
            { !it.contains('-') },
            { !it.contains('+') },
            { !it.contains('.') },
        )

        private fun String.checkString(): Boolean = stringConditions.all { it.invoke(this) }
    }
}
