"use strict";

const status = document.querySelector("#status");
const errorBox = document.querySelector("#error");
let pyodide;

function showError(stage, error) {
  const detail = error instanceof Error ? error.message : String(error);
  errorBox.hidden = false;
  errorBox.textContent = `Ошибка: ${stage}.\n${detail}`;
  status.textContent = "Движок недоступен";
  console.error(stage, error);
}

// The only JS ↔ Python adapter. Arguments and return values are JSON strings.
async function callPython(name, ...args) {
  let fn;
  let result;
  try {
    fn = pyodide.globals.get(name);
    if (!fn) throw new Error(`Python API '${name}' не экспортирован`);
    result = await fn(...args);
    return typeof result === "string" ? result : result.toString();
  } catch (error) {
    showError(`вызов Python-функции ${name}`, error);
    throw error;
  } finally {
    // Pyodide globals and non-primitive return values are PyProxy objects.
    // Release them after converting the JSON response to avoid leaking proxies.
    if (result && typeof result.destroy === "function") result.destroy();
    if (fn && typeof fn.destroy === "function") fn.destroy();
  }
}

function parseState(json) {
  try { return JSON.parse(json); }
  catch (error) { showError("десериализация JSON состояния", error); throw error; }
}

function render(state) {
  document.querySelector("#profile").textContent = state.profile;
  document.querySelector("#profile-name").value = state.profile;
  document.querySelector("#points").textContent = `${state.points} Points`;
  document.querySelector("#level").textContent = `Уровень ${state.level}`;
  document.querySelector("#cards").replaceChildren(...state.cards.map((card) => {
    const item = document.createElement("li"); item.textContent = `${card.name} · ${card.species}`; return item;
  }));
}

async function persistState() {
  try {
    localStorage.setItem("animal-collector-state", await callPython("export_state"));
  } catch (_) { /* The bridge already displays the stage-specific error. */ }
}

async function dispatch(action) {
  render(parseState(await callPython("dispatch", JSON.stringify(action))));
  await persistState();
}

window.onNativeCameraPermission = (granted) => {
  status.textContent = granted ? "Камера разрешена Android" : "Доступ к камере не предоставлен";
};

async function boot() {
  try {
    status.textContent = "Загрузка Pyodide…";
    pyodide = await loadPyodide({ indexURL: "https://cdn.jsdelivr.net/pyodide/v0.27.0/full/" });
  } catch (error) { showError("загрузка Pyodide", error); return; }
  let source;
  try {
    status.textContent = "Получение Python-исходника…";
    const response = await fetch("engine.py");
    if (!response.ok) throw new Error(`engine.py: HTTP ${response.status}`);
    source = await response.text();
  } catch (error) { showError("получение Python-исходника", error); return; }
  try {
    status.textContent = "Runtime-компиляция Python…";
    await pyodide.runPythonAsync(source);
  } catch (error) { showError("runtime-компиляция и исполнение Python", error); return; }
  try {
    const saved = localStorage.getItem("animal-collector-state") || "{}";
    render(parseState(await callPython("initialize", saved)));
    document.querySelector("#game").hidden = false;
    status.textContent = "Готово: Python работает в Pyodide";
  } catch (_) { return; }
}

document.querySelector("#add-card").addEventListener("click", () => dispatch({ type: "add_mock_card" }));
document.querySelector("#profile-form").addEventListener("submit", (event) => { event.preventDefault(); dispatch({ type: "rename_profile", name: document.querySelector("#profile-name").value }); });
document.querySelector("#camera").addEventListener("click", () => {
  if (window.NativeDevice) window.NativeDevice.requestCameraPermission();
  else status.textContent = "Android API камеры доступен только внутри приложения";
});
window.addEventListener("pagehide", async () => {
  await persistState();
});
boot();
