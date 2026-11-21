# E-ventory (Forge 1.20.1)

Replaces the **E** inventory with: Curios slots | big player | armor column | Backpacked backpack above your inventory.

## Build
Needs JDK 17. Put these in `libs/` (already there: curios, backpacked):
  - curios-5.14.1.jar
  - backpacked-3.0.9.jar
  - framework-0.8.0.jar   (MrCrayfish's Framework, now included)

Then, using the Gradle wrapper from the official Forge 1.20.1 (47.3.0) MDK:
  ./gradlew build
Jar appears in build/libs/. Drop it in your mods folder together with Forge, Curios, Backpacked and Framework.
(Easiest: copy this project's `src/`, `libs/` and `build.gradle` into a fresh Forge 1.20.1 MDK.)

## Notes
- Only survival/adventure E screen is replaced; creative inventory is untouched.
- No crafting grid or offhand slot (not in your design). F still swaps offhand.
- Backpack shows the currently selected backpack. Locked slots show a padlock; unlock them in Backpacked's own backpack screen (its keybind still works).
- Curios slots only exist if a datapack/mod defines them for the player.

## HUD (merged from ui-unified)
- Ring hotbar replaces the vanilla hotbar (bottom-left); selected slot is centred and enlarged, offhand shown beside it.
- Health/armor/food/air/XP and Legendary Survival Overhaul bars (health_overhaul, thirst, temperature, wetness, body_damage, cold_hunger) are moved to the bottom-right. LSO is NOT a compile dependency; it is matched by overlay id only.
- Options: config/gridinventory-client.toml (hudEnabled, hudMargin, ringRadius, centerScale, scrollSmoothing, showOffhand, moveSurvivalRight, survivalYOffset).
- Not merged: the 9-slot inventory limit, mixins, blur, and the "E opens Backpacked screen" redirect (they conflict with the grid inventory screen).

## GitHub Actions
`.github/workflows/main.yml` builds the jar on every push (Java 17, Gradle 8.8). Push the whole folder to a repo (including `libs/`), then download `gridinventory-jar` from the run's Artifacts.

## UI notes
- Dark theme throughout; the screen darkens the world with a translucent tint instead of black.
- 2x2 crafting grid sits under the player (full vanilla crafting logic; items return to you on close).
- Armor items render at 32px (2x). The whole 44px armor cell is clickable.

## HUD positions (current)
- Ring hotbar: unchanged.
- Hearts, armor, hunger, thirst, temperature, etc.: NOT moved (default vanilla / Legendary Survival Overhaul positions). Set `moveSurvivalRight = true` in config/gridinventory-client.toml to move them to the right again.
- ParCool stamina: kept where ParCool draws it, pulled down by `staminaYOffset` pixels (default 20). Raise it to push lower, lower/negative to push up.

## Update: bottom-right survival stack
Bars stack in the bottom-right corner (bottom to top): hunger, thirst, hearts, armor, air. Temperature, wetness and body-damage icons sit to the left of the stack. The experience bar and vanilla hotbar are hidden. `taczReserveBottom` (default 44) keeps the bottom-right corner free for the TACZ gun HUD; `barRowSpacing` sets row spacing.

## Containers + Petite Inventory
- Opening a chest, barrel, trapped chest or ender chest now opens the grid inventory with that container's GUI in the middle column (where the player model normally is). Curios, armor, backpack, crafting and inventory stay as they are. The window widens to fit the 9-wide container. Shulker boxes, furnaces, crafting tables and modded menus are untouched and keep their normal screens.
- Petite Inventory (optional): when installed, the grid inventory screen is switched into Petite's grid layout once per session (config `petiteAutoEnable`), so multi-slot items work in the inventory and the Backpacked backpack. Locked backpack slots are skipped by Petite through Backpacked's own slot class.

## Effects hidden
Active effects are not shown anywhere: the HUD effect icons (vanilla and any other mod's potion/effect overlay) and the effect list in inventory screens (including creative) are cancelled. Toggle with `hideEffects` in config/gridinventory-client.toml. The grid inventory screen never showed effects.

## Rummage compatibility
Rummage works on any menu through the generic slot/menu/screen classes, so the container slots in the middle column are rummaged exactly like a normal chest (masks, hover search, sounds). The chest swap runs at the lowest event priority so Rummage sees the original menu close and our menu open in a clean order.

## E-ventory update notes
- Vanilla hotbar removed unconditionally (`removeVanillaHotbar`, independent of `hudEnabled`); the held-item name popup is removed (`hideHeldItemName`).
- Tooltips fixed: the screen's render() was skipping the background and tooltip calls that concrete container screens must make themselves.
- Creative mode: E now opens the vanilla creative inventory; survival uses E-ventory. Spectators keep the vanilla behaviour.
- Item-on-item clicks (battery -> flashlight, bundles) are handled inside the E-ventory menu itself, left or right click, client and server, so they no longer depend on Petite/Flashlight Fix click hooks.
- Spatial GUI: the screen is a normal container screen, so Spatial hooks it (enable "containers"/"inventory" in Spatial's config); our dark wash is skipped while Spatial draws the plane, and the ring hotbar hides when Spatial hides the HUD.
- TLOU2-style UI: charcoal wash + vignette, hairline warm-white frames with corner brackets, amber accent, line-art equipment icons, small-caps headers, restyled ring hotbar. Hearts/hunger/thirst icons are vanilla/Legendary Survival Overhaul textures and are unchanged (they would need a resource pack).
- Libs: Backpacked 3.0.9.1 (identical API to 3.0.9).

## Hotbar returning while holding a TACZ gun (fixed)
Petite Inventory merges hotbar slots for multi-slot items like guns and then draws its own hotbar copy from inside its overlay handler (it only skips this if a mod with id `unifiedui` is loaded). E-ventory now cancels the hotbar overlay at HIGHEST priority so Petite's handler never runs.

## Battery / flashlight diagnostic
Clicking a battery onto a flashlight in the E-ventory screen prints a chat line from the client and from the server, e.g.
`[E-ventory] server: flashierflashlights:battery on flashierflashlights:flashlight (SECONDARY) -> carried item handled=false, slot item handled=false`
It reports whether the battery item or the flashlight item accepted the click. Disable with `debugItemClicks = false` in config/gridinventory-client.toml once batteries work.
