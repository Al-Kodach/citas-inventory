package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
enum class UnitOfMeasure(val displayName: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    BOTTLE("Bottle"),
    BOX("Box"),
    PACK("Pack"),
    PIECE("Piece"),
    GRAM("Gram"),
    KILOGRAM("Kilogram"),
    LITRE("Litre"),
    MILLILITRE("Millilitre");

    companion object {
        fun fromString(value: String?): UnitOfMeasure {
            if (value == null) return PIECE
            return entries.find { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            } ?: PIECE
        }
    }
}
