const DIRECTIONS = ["NORTH", "SOUTH", "EAST", "WEST"];
const LANE = { NORTH: 42.5, SOUTH: 57.5, EAST: 42.5, WEST: 57.5 };
const STOP_START = 35;
const STOP_END = 65;
const QUEUE_LIMIT_PER_LANE = 6;
const CAR_POOL_LIMIT = 30;

const intersectionEl = document.getElementById("intersection");
const ui = {
    runningState: document.getElementById("runningState"),
    greenLight: document.getElementById("greenLight"),
    carsPassed: document.getElementById("carsPassed"),
    threadCount: document.getElementById("threadCount"),
    qN: document.getElementById("qN"),
    qS: document.getElementById("qS"),
    qE: document.getElementById("qE"),
    qW: document.getElementById("qW"),
    speedRange: document.getElementById("speedRange"),
    speedLabel: document.getElementById("speedLabel"),
    playBtn: document.getElementById("playBtn"),
    resetBtn: document.getElementById("resetBtn")
};
const lightEls = {
    NORTH: document.getElementById("lightN"),
    SOUTH: document.getElementById("lightS"),
    EAST: document.getElementById("lightE"),
    WEST: document.getElementById("lightW")
};

const carsById = new Map();
const carPool = [];
let currentSpeed = Number(ui.speedRange.value);
let speedDebounce = null;
let pendingSpeed = null;
let isRunning = false;
let reconnectTimer = null;

ui.playBtn.addEventListener("click", () => {
    const endpoint = isRunning ? "pause" : "start";
    ui.playBtn.disabled = true;

    post(`/api/simulation/${endpoint}`).finally(() => {
        ui.playBtn.disabled = false;
    });
});

ui.resetBtn.addEventListener("click", () => {
    clearCars();
    post("/api/simulation/reset");
});

ui.speedRange.addEventListener("input", () => {
    currentSpeed = Number(ui.speedRange.value);
    pendingSpeed = currentSpeed;
    ui.speedLabel.textContent = `${currentSpeed.toFixed(1)}x`;

    clearTimeout(speedDebounce);
    speedDebounce = setTimeout(() => {
        post("/api/simulation/speed", { speed: currentSpeed });
    }, 150);
});

connectSocket();
loadState();

function connectSocket() {
    const protocol = location.protocol === "https:" ? "wss" : "ws";
    const socket = new WebSocket(`${protocol}://${location.host}/ws/simulation`);

    socket.addEventListener("message", (event) => {
        renderState(JSON.parse(event.data));
    });

    socket.addEventListener("close", () => {
        clearTimeout(reconnectTimer);
        reconnectTimer = setTimeout(connectSocket, 1200);
    });
}

function renderState(state) {
    updateDashboard(state);
    renderWaitingCars(state.waitingCars || {});
    renderPassingCars(state.passEvents || []);
    removeMissingWaitingCars(state.waitingCars || {});
}

function updateDashboard(state) {
    const serverSpeed = Number(state.speedMultiplier || 1);
    if (pendingSpeed !== null && Math.abs(serverSpeed - pendingSpeed) < 0.01) {
        pendingSpeed = null;
    }

    isRunning = Boolean(state.running);
    currentSpeed = pendingSpeed ?? serverSpeed;

    ui.runningState.textContent = isRunning ? "Yes" : "No";
    ui.playBtn.textContent = isRunning ? "Pause" : "Play";
    ui.greenLight.textContent = state.greenDirection;
    ui.carsPassed.textContent = state.totalCarsPassed;
    ui.threadCount.textContent = state.activeThreads;
    ui.speedRange.value = currentSpeed;
    ui.speedLabel.textContent = `${currentSpeed.toFixed(1)}x`;

    ui.qN.textContent = state.queueLengths?.NORTH ?? 0;
    ui.qS.textContent = state.queueLengths?.SOUTH ?? 0;
    ui.qE.textContent = state.queueLengths?.EAST ?? 0;
    ui.qW.textContent = state.queueLengths?.WEST ?? 0;

    for (const direction of DIRECTIONS) {
        lightEls[direction].classList.toggle("green", direction === state.greenDirection);
    }
}

function renderWaitingCars(waitingCars) {
    for (const direction of DIRECTIONS) {
        const cars = waitingCars[direction] || [];
        const visibleCars = cars.slice(0, QUEUE_LIMIT_PER_LANE);

        visibleCars.forEach((car, index) => {
            const existing = carsById.get(car.id);
            if (existing?.status === "passing") {
                return;
            }

            const position = queuePosition(direction, index);
            if (existing) {
                moveWaitingCar(existing, position);
            } else {
                createWaitingCar(car, direction, position);
            }
        });
    }
}

function createWaitingCar(car, direction, position) {
    const el = takeCarElement();
    setCarStyle(el, car.color, direction, "waiting");

    const edge = entryEdge(direction);
    placeCar(el, edge);
    intersectionEl.appendChild(el);

    void el.offsetHeight;
    el.style.transition = "left 350ms ease-out, top 350ms ease-out";
    placeCar(el, position);

    carsById.set(car.id, {
        el,
        direction,
        status: "waiting",
        x: position.x,
        y: position.y
    });
}

function moveWaitingCar(entry, position) {
    if (entry.x === position.x && entry.y === position.y) {
        return;
    }

    entry.el.style.transition = "left 350ms ease-out, top 350ms ease-out";
    placeCar(entry.el, position);
    entry.x = position.x;
    entry.y = position.y;
}

function renderPassingCars(passEvents) {
    const duration = Math.max(300, 1200 / currentSpeed);

    for (const event of passEvents) {
        const existing = carsById.get(event.carId);
        if (existing?.status === "passing") {
            continue;
        }

        const el = existing?.el || takeCarElement();
        const to = exitEdge(event.direction);
        setCarStyle(el, event.color, event.direction, "passing");

        if (!existing) {
            placeCar(el, entryEdge(event.direction));
            intersectionEl.appendChild(el);
        }

        carsById.set(event.carId, {
            el,
            direction: event.direction,
            status: "passing",
            x: to.x,
            y: to.y
        });

        void el.offsetHeight;
        el.style.transition = `left ${duration}ms linear, top ${duration}ms linear`;
        placeCar(el, to);

        window.setTimeout(() => {
            const current = carsById.get(event.carId);
            if (current?.el === el) {
                carsById.delete(event.carId);
            }
            releaseCarElement(el);
        }, duration + 80);
    }
}

function removeMissingWaitingCars(waitingCars) {
    const visibleIds = new Set();

    for (const direction of DIRECTIONS) {
        const cars = waitingCars[direction] || [];
        cars.slice(0, QUEUE_LIMIT_PER_LANE).forEach((car) => visibleIds.add(car.id));
    }

    for (const [carId, entry] of carsById) {
        if (entry.status === "waiting" && !visibleIds.has(carId)) {
            carsById.delete(carId);
            releaseCarElement(entry.el);
        }
    }
}

function setCarStyle(el, color, direction, status) {
    el.className = status === "passing" ? "car moving" : "car";
    if (direction === "NORTH" || direction === "SOUTH") {
        el.classList.add("vertical");
    }
    el.style.background = color;
}

function takeCarElement() {
    return carPool.pop() || document.createElement("div");
}

function releaseCarElement(el) {
    el.remove();
    el.className = "car";
    el.style.cssText = "";

    if (carPool.length < CAR_POOL_LIMIT && !carPool.includes(el)) {
        carPool.push(el);
    }
}

function clearCars() {
    for (const entry of carsById.values()) {
        releaseCarElement(entry.el);
    }
    carsById.clear();
}

function placeCar(el, position) {
    el.style.left = `${position.x}%`;
    el.style.top = `${position.y}%`;
}

function queuePosition(direction, index) {
    const gap = 3.6;

    if (direction === "NORTH") {
        return { x: LANE.NORTH, y: STOP_START - 3 - index * gap };
    }
    if (direction === "SOUTH") {
        return { x: LANE.SOUTH, y: STOP_END + 3 + index * gap };
    }
    if (direction === "EAST") {
        return { x: STOP_END + 3 + index * gap, y: LANE.EAST };
    }
    return { x: STOP_START - 3 - index * gap, y: LANE.WEST };
}

function entryEdge(direction) {
    const margin = 2.5;

    if (direction === "NORTH") {
        return { x: LANE.NORTH, y: -margin };
    }
    if (direction === "SOUTH") {
        return { x: LANE.SOUTH, y: 100 + margin };
    }
    if (direction === "EAST") {
        return { x: 100 + margin, y: LANE.EAST };
    }
    return { x: -margin, y: LANE.WEST };
}

function exitEdge(direction) {
    const margin = 2.5;

    if (direction === "NORTH") {
        return { x: LANE.NORTH, y: 100 + margin };
    }
    if (direction === "SOUTH") {
        return { x: LANE.SOUTH, y: -margin };
    }
    if (direction === "EAST") {
        return { x: -margin, y: LANE.EAST };
    }
    return { x: 100 + margin, y: LANE.WEST };
}

async function post(url, body) {
    try {
        const response = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: body ? JSON.stringify(body) : undefined
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const state = await response.json();
        renderState(state);
        return state;
    } catch (error) {
        console.error("Simulation request failed", error);
        return null;
    }
}

async function loadState() {
    try {
        const response = await fetch("/api/simulation/state");
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        renderState(await response.json());
    } catch (error) {
        console.error("Could not load simulation state", error);
    }
}
