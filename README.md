# Grid Inventory (Forge 1.20.1)

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
