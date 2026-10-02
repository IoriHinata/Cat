package com.animalcollector.domain

import java.util.UUID

enum class Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
    companion object { fun fromScore(score: Int) = when (score) { in 5..99 -> COMMON; in 100..249 -> UNCOMMON; in 250..499 -> RARE; in 500..799 -> EPIC; else -> LEGENDARY } }
}
enum class StorageState { IN_BAG, OUT_OF_BAG, TRADED, LOST }
enum class AnimalFamily { CAT, DOG, BIRD, WILD, INSECT, OTHER }
data class AnimalRecognitionResult(val species: String, val scientificName: String?, val family: AnimalFamily, val breed: String?, val color: String?, val confidence: Int, val description: String, val wikipediaQuery: String)
data class AnimalCard(val id: String = UUID.randomUUID().toString(), val ownerId: String = "local-player", val name: String, val imageUri: String, val recognition: AnimalRecognitionResult, val score: Int, val rarity: Rarity = Rarity.fromScore(score), val createdAt: Long = System.currentTimeMillis(), val expiresAt: Long? = null, val isProtected: Boolean = false, val isDuplicate: Boolean = false, val storageState: StorageState = StorageState.IN_BAG)
data class ResearchProgress(val level: Int = 1, val xp: Int = 0, val totalDiscoveries: Int = 0, val uniqueSpecies: Int = 0, val uniqueBreeds: Int = 0, val uniqueColors: Int = 0)
data class TradeTransaction(val id: String = UUID.randomUUID().toString(), val nonce: String = UUID.randomUUID().toString(), val timestamp: Long = System.currentTimeMillis(), val state: TradeState = TradeState.CREATED)
enum class TradeState { CREATED, OFFERED, ACCEPTED, CONFIRMED, COMPLETED, CANCELLED, FAILED }
