"""The local, deterministic rules engine for Animal Collector.

It owns every game value; JavaScript only renders snapshots and sends intents.
"""
import json
import hashlib
from datetime import datetime, timezone

RARITIES = ("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY")
CAPACITY = {rarity: 5 for rarity in RARITIES}
OUT_OF_BAG_MS = 24 * 60 * 60 * 1000
PROTECT_COST = 40
RANK_POINTS = {"COMMON": 20, "UNCOMMON": 45, "RARE": 90, "EPIC": 180, "LEGENDARY": 360}

DEFAULT = {"profile": "Исследователь", "points": 120, "xp": 0, "level": 1,
           "cards": [], "capacities": CAPACITY, "settings": {"mock_ai": True,
           "notifications": True, "animations": True, "sounds": False}, "trades": 0,
           "gallery_day": "", "gallery_count": 0, "next_id": 1, "claimed_achievements": []}
_state = {}

MOCK_RESULTS = [
 {"species":"Домашняя кошка","scientific":"Felis catus","family":"Кошачьи","breed":"British Shorthair","color":"голубой","age":"взрослая","confidence":78,"description":"Спокойная короткошёрстная домашняя кошка.","search":"British Shorthair","category":"Кошки","series":"Домашние кошки"},
 {"species":"Собака","scientific":"Canis lupus familiaris","family":"Псовые","breed":"Labrador Retriever","color":"палевый","age":"взрослая","confidence":81,"description":"Дружелюбная собака-компаньон.","search":"Labrador Retriever","category":"Собаки","series":"Породы собак"},
 {"species":"Сова","scientific":"Strigiformes","family":"Совиные","breed":"Лесная сова","color":"коричневый","age":"взрослая","confidence":72,"description":"Ночная птица с выразительными глазами.","search":"Owl","category":"Птицы","series":"Ночные животные"},
 {"species":"Лисица","scientific":"Vulpes vulpes","family":"Псовые","breed":"Рыжая лисица","color":"рыжий","age":"взрослая","confidence":76,"description":"Осторожный лесной хищник.","search":"Red fox","category":"Дикие животные","series":"Хищники"},
]

def _encode(value): return json.dumps(value, ensure_ascii=False, separators=(",", ":"))
def _decode(value):
    if not isinstance(value, str): raise ValueError("Некорректные данные действия")
    return json.loads(value)
def _now(): return int(datetime.now(timezone.utc).timestamp() * 1000)
def _today(): return datetime.now(timezone.utc).strftime("%Y-%m-%d")
def _rarity(score):
    if score < 100: return "COMMON"
    if score < 250: return "UNCOMMON"
    if score < 500: return "RARE"
    if score < 800: return "EPIC"
    return "LEGENDARY"
def _score(seed): return 5 + int(hashlib.sha256(seed.encode()).hexdigest()[:8], 16) % 996
def _new_bonus(card):
    cards = _state["cards"]
    bonus = 15
    if not any(c["species"] == card["species"] for c in cards): bonus += 35
    if not any(c["breed"] == card["breed"] for c in cards): bonus += 25
    if not any(c["color"] == card["color"] for c in cards): bonus += 10
    if card["rarity"] in ("EPIC", "LEGENDARY"): bonus += 20
    return bonus
def _expire():
    now = _now()
    for c in _state["cards"]:
        if c["state"] == "OUT_OF_BAG" and not c["protected"] and c.get("expires_at", 0) <= now:
            c["state"] = "LOST"
def _bag_counts():
    return {r: sum(c["rarity"] == r and c["state"] == "IN_BAG" for c in _state["cards"]) for r in RARITIES}
def _normalize(data):
    result = dict(DEFAULT)
    if isinstance(data, dict): result.update({k:v for k,v in data.items() if k in result})
    result["capacities"] = {r: max(0, int(result.get("capacities", {}).get(r, 5))) for r in RARITIES}
    result["settings"] = {**DEFAULT["settings"], **(result.get("settings") if isinstance(result.get("settings"), dict) else {})}
    result["cards"] = result["cards"] if isinstance(result["cards"], list) else []
    return result
def _achievements():
    cards = _state["cards"]; active = [c for c in cards if c["state"] != "LOST"]
    definitions = [("Первое открытие", "Получите первую карточку", 1, 25, len(active)), ("Кошатник", "Соберите 5 кошек", 5, 50, sum(c["category"] == "Кошки" for c in active)), ("Собачник", "Соберите 5 собак", 5, 50, sum(c["category"] == "Собаки" for c in active)), ("Орнитолог", "Соберите 3 птицы", 3, 60, sum(c["category"] == "Птицы" for c in active)), ("Охотник за редкостью", "Найдите редкую карточку", 1, 40, sum(c["rarity"] in ("RARE","EPIC","LEGENDARY") for c in active)), ("Эпический момент", "Найдите EPIC", 1, 75, sum(c["rarity"] == "EPIC" for c in active)), ("Легенда", "Найдите LEGENDARY", 1, 150, sum(c["rarity"] == "LEGENDARY" for c in active)), ("Коллекционер", "Соберите 10 карточек", 10, 100, len(active)), ("Исследователь", "Откройте 5 видов", 5, 80, len({c["species"] for c in active})), ("Безумный коллекционер", "Соберите 50 карточек", 50, 250, len(active))]
    claimed = set(_state.get("claimed_achievements", []))
    return [{"name":n,"description":d,"target":t,"progress":min(p,t),"reward":r,"done":p>=t,"claimed":n in claimed} for n,d,t,r,p in definitions]
def _albums():
    cards = [c for c in _state["cards"] if c["state"] != "LOST"]
    return [{"name":n,"target":t,"progress":min(sum(c["category"] == n for c in cards),t)} for n,t in (("Кошки",50),("Собаки",50),("Птицы",30),("Дикие животные",40),("Насекомые",25))] + [{"name":"Редкие породы","target":20,"progress":min(sum(c["rarity"] in ("RARE","EPIC","LEGENDARY") for c in cards),20)}]
def get_state():
    _expire(); cards = _state["cards"]; counts = _bag_counts(); xp = max(0, int(_state["xp"])); level = 1 + xp // 100
    return _encode({**_state,"level":level,"xp_in_level":xp % 100,"xp_to_next":100-(xp%100),"bag": [{"rarity":r,"used":counts[r],"capacity":_state["capacities"][r]} for r in RARITIES],"albums":_albums(),"achievements":_achievements(),"series":[{"name":n,"progress":sum(c.get("series")==n and c["state"] != "LOST" for c in cards),"target":20} for n in ("Домашние кошки","Породы собак","Хищники","Ночные животные","Городские животные","Редкие окрасы","Птицы","Лесные жители")],"statistics":{"total":len(cards),"species":len({c["species"] for c in cards}),"breeds":len({c["breed"] for c in cards}),"colors":len({c["color"] for c in cards}),"max_score":max([c["score"] for c in cards],default=0),"lost":sum(c["state"]=="LOST" for c in cards),"protected":sum(c["protected"] for c in cards),"trades":_state["trades"]}})
def initialize(payload="{}"):
    global _state
    incoming = _decode(payload)
    _state = _normalize(incoming.get("state", incoming) if isinstance(incoming,dict) else {})
    return get_state()
def dispatch(payload):
    global _state
    action = _decode(payload); kind = action.get("type") if isinstance(action,dict) else None
    if kind == "rename_profile":
        name = str(action.get("name", "")).strip()
        if not name: raise ValueError("Введите имя исследователя")
        _state["profile"] = name[:40]
    elif kind in ("gallery_check", "gallery_start"):
        if _state["gallery_day"] != _today(): _state["gallery_day"], _state["gallery_count"] = _today(), 0
        if _state["gallery_count"] >= 2: raise ValueError("Сегодня доступно только 2 загрузки фотографий. Попробуйте завтра.")
        if kind == "gallery_start": _state["gallery_count"] += 1
    elif kind == "create_card":
        result = action.get("result", {}); name = str(action.get("name", "")).strip(); image = str(action.get("image", ""))
        required = ("species","scientific","family","breed","color","confidence","description","category","series")
        if not name: raise ValueError("Назовите животное перед созданием карточки")
        if not isinstance(result,dict) or not all(result.get(k) is not None for k in required): raise ValueError("Результат распознавания неполный")
        cid = int(_state["next_id"]); score = _score(f"{cid}:{name}:{result['species']}:{_now()}"); rarity = _rarity(score); counts = _bag_counts()
        card = {"id":cid,"number":cid,"name":name[:50],"image":image,"score":score,"rarity":rarity,"state":"IN_BAG" if counts[rarity] < _state["capacities"][rarity] else "OUT_OF_BAG","protected":False,"created_at":_now(),"expires_at":0,**{k:result.get(k,"") for k in required},"age":result.get("age","") ,"search":result.get("search",result["species"])}
        if card["state"] == "OUT_OF_BAG": card["expires_at"] = _now() + OUT_OF_BAG_MS
        bonus = _new_bonus(card); _state["cards"].append(card); _state["next_id"] = cid + 1; _state["points"] += RANK_POINTS[rarity]; _state["xp"] += bonus
    elif kind == "buy_slot":
        rarity = action.get("rarity")
        if rarity not in RARITIES: raise ValueError("Неизвестный ранг")
        slot_cost = RANK_POINTS[rarity] * 2
        if _state["points"] < slot_cost: raise ValueError("Недостаточно лапок для покупки слота")
        _state["points"] -= slot_cost; _state["capacities"][rarity] += 1
    elif kind == "protect":
        card = next((c for c in _state["cards"] if c["id"] == action.get("id")), None)
        if not card: raise ValueError("Карточка не найдена")
        if _state["points"] < PROTECT_COST: raise ValueError("Недостаточно лапок для защиты карточки")
        if not card["protected"]: _state["points"] -= PROTECT_COST; card["protected"] = True
    elif kind == "settings":
        key = action.get("key")
        if key not in _state["settings"]: raise ValueError("Неизвестная настройка")
        _state["settings"][key] = bool(action.get("value"))
    elif kind == "claim_achievement":
        name = str(action.get("name", ""))
        achievement = next((item for item in _achievements() if item["name"] == name), None)
        if not achievement or not achievement["done"]: raise ValueError("Задание ещё не выполнено")
        if achievement["claimed"]: raise ValueError("Награда за это задание уже получена")
        _state.setdefault("claimed_achievements", []).append(name)
        _state["points"] += achievement["reward"]
    elif kind == "receive_trade_card":
        remote = action.get("card")
        if not isinstance(remote, dict) or not remote.get("species") or not remote.get("name"):
            raise ValueError("Получены неполные данные карточки")
        pay = max(0, int(action.get("pay_paws", 0)))
        if _state["points"] < pay: raise ValueError("Недостаточно лапок для этой сделки")
        cid = int(_state["next_id"]); score = max(5, min(1000, int(remote.get("score", 5)))); rarity = _rarity(score); counts = _bag_counts()
        card = {"id":cid,"number":cid,"name":str(remote["name"])[:50],"image":str(remote.get("image", "")),"score":score,"rarity":rarity,"state":"IN_BAG" if counts[rarity] < _state["capacities"][rarity] else "OUT_OF_BAG","protected":False,"created_at":_now(),"expires_at":0,"species":str(remote["species"]),"scientific":str(remote.get("scientific", "")),"family":str(remote.get("family", "")),"breed":str(remote.get("breed", "")),"color":str(remote.get("color", "")),"confidence":int(remote.get("confidence", 0)),"description":str(remote.get("description", "")),"category":str(remote.get("category", "Другие")),"series":str(remote.get("series", "")),"age":str(remote.get("age", "")),"search":str(remote.get("search", remote["species"]))}
        if card["state"] == "OUT_OF_BAG": card["expires_at"] = _now() + OUT_OF_BAG_MS
        _state["cards"].append(card); _state["next_id"] = cid + 1; _state["points"] -= pay
    elif kind == "sell_card":
        card = next((item for item in _state["cards"] if item["id"] == action.get("id")), None)
        if not card or card["state"] == "LOST": raise ValueError("Карточка недоступна для продажи")
        _state["cards"].remove(card); _state["points"] += max(1, RANK_POINTS[card["rarity"]] // 4)
    elif kind == "clear":
        _state = _normalize({})
    else: raise ValueError("Это действие сейчас недоступно")
    return get_state()
def mock_results(seed=""):
    offset = int(hashlib.sha256(str(seed).encode()).hexdigest()[:2],16) % len(MOCK_RESULTS)
    return _encode([MOCK_RESULTS[(offset+i)%len(MOCK_RESULTS)] | {"confidence": max(8, MOCK_RESULTS[(offset+i)%len(MOCK_RESULTS)]["confidence"] - i*31)} for i in range(3)])
def export_state(): return _encode({"version":2,"state":_state})
def import_state(payload): return initialize(payload)
