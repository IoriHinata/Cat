"""Runtime game engine executed by Pyodide, not compiled into Android DEX."""

import json

_state = {"profile": "Исследователь", "points": 120, "level": 1, "cards": []}


def _decode(payload):
    if not isinstance(payload, str):
        raise ValueError("Payload must be a JSON string")
    return json.loads(payload)


def _encode(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def initialize(payload="{}"):
    """Initialize only through JSON so the UI never receives the mutable state."""
    global _state
    saved = _decode(payload)
    if isinstance(saved, dict) and isinstance(saved.get("state"), dict):
        _state = saved["state"]
    return get_state()


def dispatch(payload):
    """Apply a single validated UI intent and return a snapshot."""
    action = _decode(payload)
    if not isinstance(action, dict):
        raise ValueError("Action must be an object")
    kind = action.get("type")
    if kind == "rename_profile":
        name = str(action.get("name", "")).strip()
        if not name:
            raise ValueError("Profile name cannot be empty")
        _state["profile"] = name[:40]
    elif kind == "add_mock_card":
        cards = _state.setdefault("cards", [])
        card_number = len(cards) + 1
        cards.append({"id": card_number, "name": action.get("name") or f"Находка #{card_number}", "species": "Кошка"})
        _state["points"] = int(_state.get("points", 0)) + 25
    else:
        raise ValueError(f"Unknown action: {kind}")
    return get_state()


def get_state():
    return _encode(_state)


def export_state():
    return _encode({"version": 1, "state": _state})


def import_state(payload):
    return initialize(payload)
