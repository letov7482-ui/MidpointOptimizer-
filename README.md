# Midpoint

Лёгкий FPS HUD и умный оптимизатор производительности для Fabric (Minecraft 1.21.11).

## Сборка

Требуется JDK 21.

```bash
git clone https://github.com/midpoint/midpoint-mod.git
cd midpoint-mod
./gradlew build
```

Готовый `.jar` появится в `build/libs/midpoint-1.0.0.jar`.

## Тестовый запуск в среде разработки

```bash
./gradlew runClient
```

## Конфиг

После первого запуска создаётся файл `config/midpoint.json` со всеми настройками HUD и оптимизатора.

## Структура проекта

```
src/main/java/net/midpoint/
├── MidpointMod.java                 # точка входа (ModInitializer)
├── config/
│   └── MidpointConfig.java          # JSON конфиг
├── hud/
│   └── MidpointFpsHud.java          # HUD: FPS / frame time / RAM
└── mixin/
    └── MidpointOptimizerMixin.java  # умный оптимизатор + low-end режим

src/main/resources/
├── fabric.mod.json
└── midpoint.mixins.json
```

## Примечание по версиям зависимостей

Версии `yarn_mappings`, `loader_version` и `fabric_version` в `gradle.properties`
нужно свериться с актуальными билдами под 1.21.11 на https://fabricmc.net/develop
перед первой сборкой — Fabric регулярно выпускает новые билды маппингов и API
даже для уже вышедших версий игры.
