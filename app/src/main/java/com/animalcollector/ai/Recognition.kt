package com.animalcollector.ai
import android.net.Uri
import com.animalcollector.domain.*
import kotlinx.coroutines.delay

interface AnimalRecognitionService { suspend fun recognize(image: Uri): AnimalRecognitionResult }
class MockAnimalRecognitionService: AnimalRecognitionService {
    private val samples = listOf(
        AnimalRecognitionResult("Домашняя кошка", "Felis catus", AnimalFamily.CAT, "Британская короткошёрстная", "серебристый", 78, "Спокойная домашняя кошка с плотной шерстью.", "British Shorthair"),
        AnimalRecognitionResult("Домашняя кошка", "Felis catus", AnimalFamily.CAT, "Мейн-кун", "рыжий", 83, "Крупная домашняя кошка с пушистой шерстью.", "Maine Coon"),
        AnimalRecognitionResult("Лабрадор-ретривер", "Canis lupus familiaris", AnimalFamily.DOG, "Лабрадор-ретривер", "обычный", 81, "Дружелюбная собака-компаньон.", "Labrador Retriever"),
        AnimalRecognitionResult("Сова", null, AnimalFamily.BIRD, null, "чёрный", 62, "Ночная птица с выразительными глазами.", "Owl"),
        AnimalRecognitionResult("Лиса", "Vulpes vulpes", AnimalFamily.WILD, null, "рыжий", 71, "Осторожный лесной хищник.", "Red fox"))
    override suspend fun recognize(image: Uri): AnimalRecognitionResult { delay(700); return samples[(image.toString().hashCode().toUInt().toLong() % samples.size).toInt()] }
}
class RemoteAnimalRecognitionService: AnimalRecognitionService { override suspend fun recognize(image: Uri): AnimalRecognitionResult = throw UnsupportedOperationException("Подключите провайдера AI в настройках") }
