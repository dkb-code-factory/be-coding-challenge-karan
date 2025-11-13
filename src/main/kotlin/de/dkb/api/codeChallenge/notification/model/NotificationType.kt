package de.dkb.api.codeChallenge.notification.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Suppress("EnumEntryName")
enum class NotificationType {
    type1,
    type2,
    type3,
    type4,
    type5,
}

@Converter
class NotificationTypeSetConverter : AttributeConverter<MutableSet<NotificationType>, String> {

    override fun convertToDatabaseColumn(valueSet: MutableSet<NotificationType>?): String =
        valueSet.orEmpty()
            .joinToString(separator = ";") { it.name }

    override fun convertToEntityAttribute(databaseString: String?): MutableSet<NotificationType> =
        databaseString.orEmpty()
            .split(";")
            .map { NotificationType.valueOf(it) }
            .toMutableSet()
}