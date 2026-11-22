# Публикация на Modrinth и CurseForge

Готовые материалы в этой папке:

| Файл | Зачем |
|---|---|
| `icon-512.png` | иконка проекта (Modrinth и CurseForge) |
| `description.md` | полное описание страницы мода (Markdown, подходит обеим площадкам) |
| `../CHANGELOG.md` | текст changelog для версии |
| `../build/libs/axiom-utilities-1.0.0+mc26.3.jar` | файл версии (после `./build.sh`) |

## Перед публикацией

1. Запустить игру с этим jar и прогнать все функции — после переименования в Axiom Utilities сборку в игре ещё не проверяли.
2. Снять скриншоты для галереи (лучше 1920×1080, интерфейс крупно):
   - раздел Placement с галочками «Offset target point» и «Paint in air next to blocks»;
   - «до/после»: кисть рисует слой воздуха у стены;
   - Global presets в режиме Blocks (плитки), наведение с подсказкой;
   - окно сохранения пресета с галочками;
   - окно Select Block с кнопкой пипетки и подсвеченный блок в мире.
3. Посмотреть правила Axiom/Moulberry про аддоны. В описании мод прямо назван неофициальным, кода Axiom в jar нет.

## Modrinth

- **Name:** Axiom Utilities · **Slug:** `axiom-utilities` (на 22.11.2026 свободен)
- **Summary:** `Quality-of-life addon for Axiom: offset target point for every tool, paint in air next to blocks, global presets and a block pipette.`
- **Categories:** Utility (можно добавить Management, как у самого Axiom)
- **Client side:** Required · **Server side:** Unsupported
- **License:** MIT · **Source:** https://github.com/Vivegass/axiom-utilities · **Issues:** https://github.com/Vivegass/axiom-utilities/issues
- **Версия:** номер `1.0.0+mc26.3`, канал Release, загрузчик Fabric, игра 26.3
- **Зависимость:** Axiom (`axiom`, id `N6n5dqoA`) — Required

## CurseForge

- **Project name:** Axiom Utilities · **Class:** Mods · **Main category:** Map and Information / Utility & QoL
- **Summary:** то же, что на Modrinth
- **Description:** вставить `description.md` (редактор Markdown)
- **License:** MIT
- **Source URL / Issues URL:** ссылки на GitHub выше
- **Файл:** Release, Fabric, Minecraft 26.3, Java 25; Related Projects: Axiom → Required Dependency (если он есть на CurseForge)
- **Логотип:** `icon-512.png` (CurseForge просит квадрат не меньше 400×400)
