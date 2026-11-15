**StynaST Client**

#####

Features:
  * Zoom
  * use any font from google fonts
  * Skins (allows 128x128 & 256x256 skins) [Anyone using this mod will be able to see your skin]
  * Capes from Mojang, OptiFine, MinecraftCapes, Cosmetica and LabyMod, plus custom capes [custom capes are shared like skins, switch your Mojang cape in-game]
  * Freecam
  * Sound muffler (mute any sound in the game without muting the whole game)
  * Longer chat history (keeps 16384 lines instead of 100)
  * Armor HUD beside the hotbar
  * Durability in item tooltips
  * Enchantment descriptions and gear icons in item tooltips
  * Shulker box and container previews in tooltips (Shift for compact, Alt+Shift for the full grid)
  * Chat Heads: player heads next to chat messages
  * Resource pack hot reload: add "StynaSTMC.hotreload": true to a folder pack's pack.mcmeta and texture/lang edits apply live
  * Totem of undying drawn with your current skin
  * Key bind search with conflict, unbound and changed filters
  * Better statistics screen (searchable stats, item grid with sorting, mob kills)
  * Better advancements screen (bigger tree, side panel listing missing and obtained criteria)
  * No fog
  * No view bobbing (toggle in Tweaks)
  * Hide armor on your own player (toggle in Tweaks, Player section)
  * First person body: see your own body, arms and items when looking down (F6 toggles, port of FirstPersonModel)
  * Darkness effect (wardens, sculk shriekers) no longer darkens the screen
  * Librarian auto cycle with Trade Cycling: pick wanted books, press F7 in the trade screen and it rerolls until one shows up
  * Pickup notifications: animated feed of picked up items and experience with the carried total, stacking from a screen corner
  * Light level overlay: block light numbers and spawnable markers (F7 toggles, port of MiniHUD's overlay; replaces More Overlays' one when installed)
  * Photo mode: fly a block-colliding camera and take 1920x1080 screenshots with custom time, weather and fog (F10 toggles, left click opens the shot menu, client-side only)

Run `./gradlew publishRelease` in each Minecraft version branch to build and publish its Fabric and NeoForge JARs. Local publishing reads `RELEASE_TOKEN` from the environment or `~/.env` and requires a clean commit pushed to `origin`.

All branches using the same `mod_version` upload to one `v<mod_version>` release. JAR filenames retain the Minecraft version and loader. Publishing again replaces only matching files, keeps other builds and release notes, and leaves the shared tag in place. The first publisher creates the tag at its commit. The manual Forgejo release workflow uses this same task.
