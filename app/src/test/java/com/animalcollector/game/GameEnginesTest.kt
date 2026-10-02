package com.animalcollector.game

import com.animalcollector.domain.*
import org.junit.Assert.*
import org.junit.Test

class GameEnginesTest {
 private fun result()=AnimalRecognitionResult("Домашняя кошка","Felis catus",AnimalFamily.CAT,"Британская короткошёрстная","серебристый",80,"","Cat")
 @Test fun scoreIsStableBoundedAndNotStandardRound(){ val engine=RarityScoreCalculator(); val s=engine.calculate("same-id",result()); assertEquals(s,engine.calculate("same-id",result())); assertTrue(s in 5..1000); assertNotEquals(0,s%10) }
 @Test fun ranksHaveSpecifiedBoundaries(){ assertEquals(Rarity.COMMON,Rarity.fromScore(5));assertEquals(Rarity.UNCOMMON,Rarity.fromScore(100));assertEquals(Rarity.RARE,Rarity.fromScore(250));assertEquals(Rarity.EPIC,Rarity.fromScore(500));assertEquals(Rarity.LEGENDARY,Rarity.fromScore(800)) }
 @Test fun slotPurchaseNeedsPoints(){ val bag=BagManager(); assertNull(bag.buySlot(Rarity.RARE,10,347)); assertEquals(124,bag.buySlot(Rarity.RARE,500,347)) }
 @Test fun uploadLimitResetsEachDay(){ val m=DailyUploadManager();assertTrue(m.canUpload("2026-01-02","2026-01-01",2));assertFalse(m.canUpload("2026-01-01","2026-01-01",2));assertEquals(1,m.nextCount("n","o",2)) }
 @Test fun expirationSkipsProtected(){ val e=CardExpirationManager(); val old=System.currentTimeMillis()-1; val exposed=AnimalCard(name="x",imageUri="",recognition=result(),score=50,storageState=StorageState.OUT_OF_BAG,expiresAt=old); val safe=exposed.copy(id="safe",isProtected=true); assertEquals(exposed.id,e.expire(listOf(safe,exposed),System.currentTimeMillis())?.id) }
 @Test fun researchAndAchievementAreNotRepeated(){ val p=ResearchManager().award(ResearchProgress(),true,true,true);assertEquals(1,p.level);val cards=listOf(AnimalCard(name="x",imageUri="",recognition=result(),score=300)); val a=AchievementManager().newlyUnlocked(emptySet(),cards,p.level);assertTrue("first_discovery" in a);assertTrue(AchievementManager().newlyUnlocked(a,cards,p.level).isEmpty()) }
 @Test fun transactionCannotCompleteTwice(){ val t=TradeTransaction(state=TradeState.CONFIRMED);val m=TradeManager();assertTrue(m.complete(t));assertFalse(m.complete(t)) }
}
