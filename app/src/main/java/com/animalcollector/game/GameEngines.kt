package com.animalcollector.game

import com.animalcollector.domain.*
import kotlin.math.roundToInt

/** Rules stay local: recognition never supplies a score or currency value. */
object RarityRules {
    val species = mapOf("Домашняя кошка" to 95, "Лиса" to 310, "Волк" to 520, "Сова" to 420, "Лабрадор-ретривер" to 135, "Немецкая овчарка" to 175)
    val breeds = mapOf("Британская короткошёрстная" to 75, "Мейн-кун" to 180, "Персидская" to 160, "Лабрадор-ретривер" to 65, "Немецкая овчарка" to 95)
    val colors = mapOf("обычный" to 12, "серебристый" to 68, "белый" to 38, "рыжий" to 31, "чёрный" to 29)
}
class RarityScoreCalculator {
    fun calculate(seed: String, result: AnimalRecognitionResult): Int {
        val base = RarityRules.species[result.species] ?: 120
        val breed = result.breed?.let { RarityRules.breeds[it] } ?: 0
        val color = result.color?.let { RarityRules.colors[it] } ?: 10
        val variation = (seed.hashCode().toUInt().toLong() % 73).toInt() + 5
        var score = (base + breed + color + variation).coerceIn(5, 999)
        if (score % 10 == 0) score += 3
        return score.coerceAtMost(999)
    }
}
class SlotPriceCalculator { fun price(score: Int): Int { var p = (score * 1.08).roundToInt() + 1; if (p % 10 == 0) p += 2; return p } }
class BagManager(private val capacity: MutableMap<Rarity, Int> = Rarity.entries.associateWith { 5 }.toMutableMap()) {
    fun canStore(rarity: Rarity, cards: Collection<AnimalCard>) = cards.count { it.storageState == StorageState.IN_BAG && it.rarity == rarity } < (capacity[rarity] ?: 0)
    fun buySlot(rarity: Rarity, points: Int, score: Int): Int? { val price = SlotPriceCalculator().price(score); if (points < price) return null; capacity[rarity] = (capacity[rarity] ?: 0) + 1; return points - price }
}
class CardExpirationManager { fun expire(cards: List<AnimalCard>, now: Long): AnimalCard? = cards.filter { it.storageState == StorageState.OUT_OF_BAG && !it.isProtected && (it.expiresAt ?: Long.MAX_VALUE) <= now }.minByOrNull { it.expiresAt ?: Long.MAX_VALUE } }
class DailyUploadManager { fun canUpload(day: String, storedDay: String?, uploads: Int) = storedDay != day || uploads < 2; fun nextCount(day: String, storedDay: String?, uploads: Int) = if (storedDay == day) uploads + 1 else 1 }
class ResearchManager { fun award(progress: ResearchProgress, newSpecies: Boolean, newBreed: Boolean, newColor: Boolean): ResearchProgress { val gained = 10 + (if (newSpecies) 55 else 0) + (if (newBreed) 30 else 0) + (if (newColor) 15 else 0); val xp = progress.xp + gained; return progress.copy(level = 1 + xp / 150, xp = xp, totalDiscoveries = progress.totalDiscoveries + 1, uniqueSpecies = progress.uniqueSpecies + if(newSpecies)1 else 0, uniqueBreeds = progress.uniqueBreeds + if(newBreed)1 else 0, uniqueColors = progress.uniqueColors + if(newColor)1 else 0) } }
class AchievementManager { fun newlyUnlocked(existing: Set<String>, cards: List<AnimalCard>, level: Int): Set<String> = buildSet { if(cards.isNotEmpty()) add("first_discovery"); if(cards.any { it.rarity >= Rarity.RARE }) add("rare_hunter"); if(cards.any { it.rarity == Rarity.EPIC }) add("epic_moment"); if(cards.any { it.rarity == Rarity.LEGENDARY }) add("legend"); if(cards.size >= 50) add("collector"); if(level >= 10) add("researcher") } - existing }
class TradeManager { private val completed = mutableSetOf<String>(); fun complete(transaction: TradeTransaction): Boolean { if(transaction.id in completed || transaction.state != TradeState.CONFIRMED) return false; completed += transaction.id; return true } }
