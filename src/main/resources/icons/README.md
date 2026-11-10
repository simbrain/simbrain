# Simbrain SVG icons

Scalable, theme-aware icons resolved by [`Icons`](../../../../main/kotlin/org/simbrain/util/Icons.kt).

## How resolution works

Code still asks for the legacy raster path (e.g. `ResourceManager.getSmallIcon("menu_icons/Save.png")`).
`Icons` kebab-cases the file's base name and looks for a matching SVG **before** falling back to the raster:

| Raster request | SVG looked up |
|---|---|
| `menu_icons/Save.png` | `icons/save.svg` |
| `menu_icons/ZoomIn.png` | `icons/zoom-in.svg` |
| `menu_icons/brokenChainIcon.png` | `icons/broken-chain-icon.svg` |
| `menu_icons/AddTableColumn.png` | `icons/add-table-column.svg` |

So **dropping a correctly-named SVG into this folder auto-upgrades that icon — no code change.**

## Single-color vs multicolor

- **Single-color** icons live here in `icons/`. Author them in **black** (`currentColor` or `#000000`).
  The global `FlatSVGIcon.ColorFilter` (installed by `Theme.installSimbrainSvgIconColors`) recolors
  near-black to the Look-and-Feel foreground, so they track light/dark themes.
- **Multicolor** icons (status badges like a green check / red x) live in `icons/multicolor/` and must
  **avoid pure black** so the recolor filter passes them through unchanged.

## Source set

Icons are sourced from [Tabler Icons](https://tabler.io/icons) (MIT), outline style only: the filled set
is partial, so mixing styles produces mismatched pairs. When Tabler has nothing that fits, a bespoke glyph is
drawn on Tabler's grid (24×24 viewBox, `stroke-width="2"`, round caps and joins, `stroke="currentColor"`) and
starts with a `bespoke Simbrain glyph: ...` comment instead of Tabler's tag header. Examples: the `op-*`
transformer glyphs, `mouse-icon`, `network-icon-black`, `pixel-plot`, `coupling` and `coupling-list`.

Check icons at their real 16 px size, in light and dark, with
`./gradlew processResources uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.IconGallerySnapshot -Ptheme=dark`.
