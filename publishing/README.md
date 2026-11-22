# Публикация на Modrinth и CurseForge

Готовые материалы в этой папке:

| Файл | Зачем |
|---|---|
| `icon-512.png` | иконка проекта (Modrinth и CurseForge) |
| `media/demo.gif` | анимация в начале описания (подключена ссылкой на GitHub) |
| `media/*.webp` | скриншоты для галереи: offset вкл/выкл, рисование в воздухе, пипетка |
| `description.md` | полное описание страницы мода (Markdown, подходит обеим площадкам) |
| `../CHANGELOG.md` | текст changelog для версии |
| `../build/libs/axiom-utilities-1.0.0+mc26.3.jar` | файл версии (после `./build.sh`) |

## Статус

- Мод проверен в игре, GitHub-релиз `v1.0.0+mc26.3` опубликован.
- Modrinth: проект создан через API, версия загружена, отправлен на модерацию; описание и галерея загружены.
- CurseForge: проект нужно создать на сайте вручную (API этого не умеет), потом файл можно залить через API.
- Перед публикацией стоит проверить правила Axiom/Moulberry про аддоны. В описании мод прямо назван неофициальным, кода Axiom в jar нет.

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
- **Images:** CurseForge не принимает картинки через API — загрузить файлы из `media/` вручную во вкладке Images проекта.
- Видео без звука для YouTube: `axiom-utilities-demo.mp4` (лежит рядом с исходным клипом Medal). YouTube-ссылку можно вставить в описание обеих площадок.
