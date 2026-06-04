# Fantasy Combat RPG — MVP Development Roadmap

> Third-person combo fighter · Procedural maps · Wave combat · RPG progression  
> **Estimated timeline: 29–48 weeks (solo dev)**  
> **17 work items across 6 phases**

---

## Scope Legend

| Label | Size | Estimate |
|-------|------|----------|
| `XS` | Trivial | < 3 days |
| `S` | Small | 3–5 days |
| `M` | Medium | 1–2 weeks |
| `L` | Large | 2–3 weeks |
| `XL` | Epic | 3–5 weeks |

---

## Phase 1 — Core Foundation
**Est. 6–8 weeks**

This phase produces nothing visually exciting, but everything else depends on getting it right. The goal is a player character that moves and hits things in a way that already feels *good* — before any content exists.

---

### F01 · Project Setup `XS` · 2–3 days

Stand up the full project skeleton before writing a single line of gameplay code.

- **Engine selection:** Choose and lock in your engine (Unity, Unreal, or Godot). For a solo dev targeting this scope, Unity or Godot are pragmatic choices. Document the rationale.
- **Version control:** Git repo with `.gitignore` tuned for the engine. Commit conventions (e.g. conventional commits). Branch strategy (main + feature branches).
- **Folder structure:** Establish canonical directories for scripts, assets, scenes, audio, shaders, prefabs. A clean structure now saves weeks of confusion later.
- **Input abstraction:** Set up an input manager that maps logical actions (`Attack`, `Dodge`, `Interact`) to physical inputs. This decouples gameplay code from hardware and makes gamepad/keyboard swapping trivial.
- **Build pipeline:** Configure at least one automated build target (PC) so you can produce a testable binary at any point.

---

### F02 · Player Controller & Movement `M` · 1–2 weeks

The movement controller is the lens through which the player experiences everything. It must feel responsive and physically convincing before combat is layered on top.

- **Locomotion states:** Idle, walk, run, sprint. Acceleration and deceleration curves — avoid instant start/stop; use a small ramp (0.1–0.15s) for weight.
- **Dodge/roll:** Directional roll with invincibility frames (i-frames) during the active dodge window (typically frames 3–12). Cooldown of ~0.8s. This is a core defensive mechanic and must feel crisp.
- **Jump:** Optional for this genre but useful for terrain navigation. Keep it simple — single jump, no air control complexity.
- **Slope & step handling:** CharacterController or physics capsule should smoothly traverse ramps and small ledges without bouncing or catching.
- **Stamina gating:** Dodge and sprint consume stamina. Stamina regenerates when idle or walking. This creates meaningful resource pressure in combat without needing complex systems.
- **Gamepad & keyboard parity:** Both input schemes must be fully functional from day one. Test regularly on both.

---

### F03 · Third-Person Camera `M` · 1 week

The camera is effectively a second player controller — it mediates every combat interaction and must never fight the player.

- **Follow camera:** Smooth follow with configurable lag (spring-arm or manual lerp). Camera should trail the player slightly to give a sense of momentum.
- **Collision avoidance:** Raycast from player to camera. When obstructed, pull the camera in toward the player along the arm. Prevents walls eating the view.
- **Horizontal orbit:** Right stick / mouse moves the camera around the player. Vertical pitch with clamped limits (don't let the player look straight down or into the ground).
- **Lock-on system:** Press a button to lock onto the nearest enemy within a cone in front of the player. Camera pivots to keep the target framed. Lock-on dot/indicator on target. Release on button press or if target moves out of range. This is essential for combo-based combat — the player shouldn't be managing the camera mid-fight.
- **Camera shake hooks:** Expose a `ShakeCamera(intensity, duration)` function now. You'll call it from combat later.

---

### F04 · Combat Framework `L` · 2–3 weeks

This is the most foundational technical system in the game. Every weapon, enemy, and ability plugs into it. Build it carefully — a shaky framework here will cause cascading pain.

- **Attack state machine:** Player has explicit states: `Idle`, `Attacking`, `InComboWindow`, `Recovering`, `Dodging`, `Staggered`. Transitions between states are explicit and testable. No spaghetti `if` chains.
- **Hitbox/hurtbox system:** Separate collision volumes for dealing damage (hitboxes, enabled only during active frames) and receiving damage (hurtboxes, always active). Hitboxes are child objects activated/deactivated by animation events.
- **Combo buffer:** When the player presses attack during a combo window, queue the input for execution on the next available frame. Buffer window of ~200ms. This makes combos feel deliberate rather than accidental.
- **Damage calculation:** `damage = baseDamage * attackMultiplier * (1 - defenseReduction)`. Keep it readable. Log every damage event in debug mode.
- **Knockback:** Apply directional force to the target on hit. Light hits: small stagger. Heavy hits: larger displacement. Enemies should react visibly to being struck.
- **Hit stun:** Briefly pause attacker's animation on a successful hit (2–4 frames of "freeze"). This is a core component of satisfying hit feedback.
- **Invincibility frames:** Already covered in dodge, but the framework should support i-frames on any state generically.

---

### F05 · Animation Controller `M` · 1 week

The animation layer translates state machine transitions into visible character behavior. Sloppy animations destroy the feel of good code.

- **Locomotion blend tree:** 2D blend tree mixing idle, walk, run by velocity magnitude and direction. Player should lean into turns.
- **Attack animation triggers:** Each attack state triggers a specific animation clip. Use animation events to fire hitbox activation, sound cues, and VFX spawns at precise frames.
- **Transition tuning:** Keep transitions short (0.05–0.1s) for combat animations, slightly longer (0.1–0.2s) for locomotion. Snappy combat, smooth movement.
- **Root motion:** Use root motion for attack animations to get authentic lunge/advance. Locomotion can use script-driven movement.
- **IK ground alignment:** Basic foot IK so feet plant correctly on uneven terrain. Prevents the floating-above-slopes issue.
- **Animation rigging (placeholder OK):** If custom art isn't ready, use Mixamo rigs or engine primitives. The system should be art-agnostic.

---

## Phase 2 — Combat & Enemies
**Est. 5–8 weeks**

With the framework in place, this phase is about making combat *feel* good and giving the player something to fight. The output of this phase should be a demo-able combat sandbox.

---

### C01 · Combo System `L` · 2 weeks

The combo system is the primary mechanic the player will engage with for the entire game. It needs to reward timing and mastery.

- **Light/heavy attack chains:** Light attacks are fast with short recovery. Heavy attacks deal more damage, have more startup, but break through enemy guards. Chain: L-L-L, L-L-H, L-H, H-H are your base routes.
- **Timing windows:** Each attack has three windows: `Startup` (vulnerable, can't cancel), `Active` (hitbox live), `Recovery` (can combo into next attack if input is buffered). Make windows visible in debug mode as frame annotations.
- **Finishers:** Completing a full combo chain triggers a finisher — a longer, higher-damage animation with a more dramatic visual. Gives players a goal to chase.
- **Dodge cancel:** Player can dodge out of recovery frames of any non-finisher attack. This is the skill-expression escape valve — advanced players will cancel into dodges fluidly.
- **Combo counter UI:** Display current combo hit count on screen. Resets on taking damage or a gap in attacks. Simple but satisfying.
- **Hit feedback:** Each successful hit should produce: a sound cue (pitch-varied per hit in chain), a hit spark particle, a brief enemy flash (white or red), and a camera micro-shake. These four together are what make hitting things satisfying.

---

### C02 · Two Weapon Archetypes `L` · 2–3 weeks

Two archetypes is enough to provide meaningful player choice and replayability without overwhelming scope. Make them feel genuinely different — not just stat swaps.

**Greatsword (Slow, High Damage, AoE)**
- Wide horizontal swings that hit multiple enemies in an arc
- Heavy startup (20–30 frames) but devastating recovery knockback
- Signature move: a charged slam that deals AoE damage in a radius and briefly stuns enemies
- Playstyle: deliberate, positional, punishing mistakes

**Dual Blades (Fast, Low Damage, High Mobility)**
- Fast multi-hit combos (5–7 hits per chain) with short individual damage
- Built-in evasive steps between combo hits (auto-dash on each swing)
- Signature move: a burst flurry that fires off all remaining combo hits rapidly
- Playstyle: aggressive, mobile, sustained DPS

Each weapon needs: its own combo routes, its own finisher, distinct SFX, and a unique idle/movement animation overlay.

---

### C03 · Enemy AI — 3 Types `L` · 2–3 weeks

Three enemy types give the wave system meaningful composition variety. Design each type to create a distinct threat that encourages different player responses.

**Melee Grunt**
- Walks toward player, attacks when in melee range
- Two attacks: quick jab (low damage, low telegraph) and overhead slam (high damage, 1-second telegraph with a windup animation)
- Staggers easily from combos — intended to be satisfying cannon fodder
- Spawns in groups of 3–6

**Ranged Archer**
- Maintains distance from player, retreats if player closes in
- Fires projectiles with a visible aim-indicator before each shot
- Low health — punishes players who ignore them
- Creates positioning pressure that breaks up pure melee focus

**Elite Brute**
- High health, guard mechanic (blocks attacks until guard is broken by enough hits)
- Three attacks: charge, shockwave slam, grab attempt
- Does not stagger from light hits — requires heavy attacks or combo finishers to interrupt
- Spawns alone or as the lead of a mixed group; effectively a mini-boss

All enemies: NavMesh pathing, aggro radius, idle patrol, death ragdoll, loot drop on death.

---

## Phase 3 — World & Maps
**Est. 5–8 weeks**

---

### W01 · Procedural Map Generator `XL` · 3–5 weeks

⚠️ **Highest risk item in the project.** Timebox aggressively. If quality isn't meeting the bar by week 4, fall back to a pool of handcrafted maps and ship that instead — do not let this block the rest of development.

- **Chunk system:** Divide the map into a grid of chunks (e.g. 20×20 units each). Each chunk is a prefab room tile with connection points on N/S/E/W edges.
- **Layout algorithm:** Use a BSP (Binary Space Partitioning) or random walk algorithm to connect chunks into a traversable layout. Guarantee at least one path between spawn and all objective zones.
- **Tile variety (single biome):** Create 15–20 chunk variants: open arena, narrow corridor, elevated platform, pillared hall, collapsed ruin. Enough variety to feel non-repetitive across runs.
- **Spawn zone placement:** Generator must mark: player spawn, enemy spawn zones (per wave), optional chest/loot nodes. Spawn zones must be spaced — enemies shouldn't materialize on top of the player.
- **Seed-based generation:** Every map is reproducible from an integer seed. Store the seed in the mission summary for debugging.
- **Boundary walls:** Auto-generate invisible walls or cliff-edge colliders at map boundaries. Players shouldn't be able to walk off the map.
- **Fallback:** Maintain a pool of 5 handcrafted maps. If the generator fails validation (no valid path, overlapping geometry), pick randomly from the pool.

---

### W02 · Portal System `S` · 3–5 days

Portals are the primary UX surface for mission selection. Keep it simple and reliable.

- **Hub portal object:** A portal in the hub that the player interacts with to launch a mission. Interacting opens a simple mission select screen (difficulty, seed preview).
- **Visual FX:** Particle-based portal effect (vortex or shimmer). Nothing elaborate — just enough to make it feel like a magical gateway.
- **Load transition:** Fade-to-black on entry, load the procedural map with the selected seed and difficulty parameters, fade in on spawn.
- **Return portal:** A fixed portal in the mission map (appears after all waves are cleared, or always visible) that returns the player to the hub. No quitting to menu required.
- **Data passing:** Portal passes `{ seed, difficulty, missionType }` to the map generator and wave director.

---

### W03 · Main Hub Area `M` · 1 week

The hub is a non-combat space for preparation, gear management, and narrative grounding. Handcrafted.

- **Layout:** Small plaza with clear landmarks: portal arch (centre), vendor stall (left), storage chest (right), exit/rest area. Player should be able to parse the space in under 5 seconds.
- **Vendor NPC:** Sells gear at fixed prices using gold earned from missions. No crafting — just buy/sell. Inventory of 6–10 items that refreshes after each mission.
- **Storage chest:** Access to full item stash. Manage gear between missions here.
- **Ambient life:** A few idle NPCs, ambient sound, basic lighting. Enough to feel inhabited without investing in complex world-building.
- **No enemies:** Hub is a guaranteed safe zone. Make this clear with a visual/narrative distinction (warm light, calm music).

---

## Phase 4 — Game Loop
**Est. 5–7 weeks**

---

### G01 · Wave System `L` · 2 weeks

The wave director is the game's pacing engine. It controls tension, difficulty escalation, and the rhythm of play.

- **Wave composition:** Each wave has a spawn budget. Budget is spent on enemy types with different costs (grunt = 1 point, archer = 2, brute = 4). Early waves: cheap enemies only. Later waves: mixed compositions including elites.
- **Difficulty scaling:** Budget increases each wave. Formula: `budget = baseBudget + (waveNumber * scalingFactor)`. Difficulty setting multiplies scaling factor (Easy: ×0.7, Normal: ×1.0, Hard: ×1.4).
- **Spawn pacing:** Don't dump all enemies at once. Drip-feed them from spawn zones at a rate that keeps pressure consistent. Use a short delay between individual enemy spawns within a wave.
- **Rest period:** After all enemies in a wave are killed, a 10-second rest window before the next wave begins. Display a countdown. Player can use this window to heal (if consumables exist) or reposition.
- **Wave counter UI:** Large, clear wave indicator: "Wave 3 / 7". Displays incoming enemy types as icons before the wave spawns — gives players a moment to prepare.
- **Final wave:** Last wave of a mission always includes an Elite Brute. Signals a climax. Spawn it with a brief dramatic pause.

---

### G02 · Mission Framework `M` · 1–2 weeks

- **Single mission type (v1):** Survive all waves. Clear = all enemies dead across all waves. Fail = player health reaches zero.
- **Mission parameters:** Each mission has: wave count (5–10), difficulty tier (1–3), map seed, reward table. These are set by the portal on launch.
- **Win screen:** On clear — display: time taken, damage dealt, damage taken, enemies killed, gold earned, loot dropped. Present loot as a reveal (one item at a time with rarity flash).
- **Fail screen:** On death — display: wave reached, option to retry (same seed) or return to hub.
- **Mid-mission checkpoint:** Auto-save progress after each wave cleared. On restart after crash/quit, resume from last completed wave rather than wave 1.

---

### G03 · Loot & Drop System `M` · 1–2 weeks

Loot is the primary reward loop. It needs to feel meaningful without requiring a deep crafting system.

- **Enemy drop tables:** Each enemy type has a loot table with weighted entries. Grunts drop gold and occasionally common gear. Brutes drop uncommon/rare gear more frequently. All tables have a small chance of rare drops.
- **Rarity tiers (3):** Common (grey), Uncommon (green), Rare (blue). Stats scale with rarity. No Legendary tier yet — save that for v1.1.
- **Item types:** Weapons (greatsword, dual blades variants), Chest armor, Boots, Ring (accessory). Keep the item pool small (~20 items total) — better to have fewer items with clear identity than a flood of indistinct gear.
- **Drop presentation:** Items drop as physical pickups in the world with a coloured glow matching rarity. Player walks over to pick up, or interacts. Show item name and stat delta vs currently equipped item on pickup prompt.
- **Gold drops:** Enemies drop gold directly. No secondary currency. Gold is spent at the hub vendor.

---

## Phase 5 — Progression
**Est. 4–6 weeks**

---

### R01 · XP & Leveling `M` · 1 week

Progression should give players a sense of growing power without requiring a complex skill tree.

- **XP sources:** Kills (scaled by enemy type) and mission clear bonus. Display XP gain as floating text on kill.
- **Level cap:** 20 for v1. Enough to feel meaningful progress, not so many levels that early ones feel trivial.
- **Stat boosts on level-up:** Each level grants flat increases to: max health (+10), stamina regen (+0.5/s), base damage (+2%). No player choice required — this is pure progression reward, not a decision point.
- **Level-up feedback:** Full-screen flash, sound cue, brief stat summary popup. Make it feel celebratory.
- **Level gates:** Some vendor items require a minimum level to purchase. Creates soft pacing — players can't rush to best-in-slot gear.

---

### R02 · Inventory & Equipment `M` · 1–2 weeks

- **Gear slots:** Weapon (1), Armor (1), Accessory (1). Simple enough to understand instantly, enough slots to feel like meaningful gear decisions.
- **Inventory grid:** A flat list of owned items. No grid puzzles. Sort by: type, rarity, level requirement.
- **Stat comparison:** When hovering/selecting an item, display current equipped stats vs candidate stats side-by-side. Green = improvement, red = downgrade.
- **Equip on pickup:** Prompt when picking up gear: "Equip now?" with comparison shown. Player can also manage gear freely from the hub chest or inventory screen.
- **Item limit:** Cap inventory at 30 items. Forces regular sell/discard decisions at the vendor. Prevents hoarding paralysis.
- **Sell from inventory:** Player can sell unwanted gear directly from the inventory screen (not just at vendor). Convenience feature that reduces friction.

---

### R03 · Save System `S` · 3–5 days

- **What is saved:** Player level, XP, gold, equipped gear, inventory contents, which missions have been completed (seed + difficulty).
- **Save triggers:** Auto-save on mission complete (win or loss), on return to hub via portal, on vendor transaction. Never mid-combat.
- **Single save slot:** One save file per player. No multiple characters yet.
- **Save format:** JSON to a local file. Human-readable for debugging. Add a version field for future migration support.
- **Corruption handling:** Validate save on load. If malformed, prompt player to start a new save rather than crashing.

---

## Phase 6 — Ship It
**Est. 4–6 weeks**

---

### P01 · HUD `S` · 3–5 days

Keep the HUD minimal. Every element earns its screen space.

- **Health bar:** Large, always visible. Red fill, dark background. Segments at 25% intervals so partial health loss is readable at a glance.
- **Stamina bar:** Smaller, below health. Depletes on dodge/sprint, regens passively. Flash when empty (can't dodge).
- **Wave indicator:** Top-centre. "Wave 4 / 7" with enemy type icons for incoming wave during rest period.
- **Combo counter:** Bottom-centre or right. Shows current hit count. Fades out 2 seconds after last hit.
- **Boss health bar:** Large bar at the bottom of the screen, visible only when an Elite Brute is alive. Name label above it.
- **No minimap for v1:** Not worth the complexity. Maps are small enough to navigate without one.

---

### P02 · Main Menu & Settings `S` · 3–4 days

- **Main menu screens:** Start New Game, Continue (if save exists), Settings, Quit.
- **Settings:** Master volume, Music volume, SFX volume, Display resolution, Fullscreen toggle, Controller/keyboard toggle, Invert Y-axis.
- **Pause menu (in-game):** Resume, Settings, Return to Hub, Quit to Desktop. Accessible at any time outside of active combat waves.
- **Loading screen:** Simple branded screen with a tip or lore blurb while map generates. Progress indicator.

---

### P03 · Core SFX `M` · 1 week

Audio does 50% of the work of making combat feel impactful. Even placeholder SFX is better than silence.

- **Combat SFX:** Hit impacts (3–4 variants, pitch-randomised per hit), sword swings (whoosh per weapon type), enemy grunt/react on hit, enemy death sound, player dodge swish.
- **UI SFX:** Button hover, button click, level-up chime, item pickup, menu open/close.
- **Ambient SFX:** Footstep sounds (surface-aware if feasible — stone vs grass), wind ambience in maps, hub ambient sounds (crowd murmur, distant forge).
- **Music:** One combat track (energetic, looping, ~3 min). One hub track (calm, atmospheric). Crossfade between them on zone transitions. No dynamic layering for v1 — that's a v1.1 feature.
- **Audio mixer:** Separate buses for Music, SFX, UI. Tied to settings sliders.

---

### P04 · Basic Hit Feedback (Juice) `M` · 1 week

Juice is not polish — it's the core sensation of play. These effects make the combat framework feel alive.

- **Screen shake:** Triggered by `ShakeCamera()` on heavy attacks and on taking damage. Heavy hit: 0.3s, moderate intensity. Taking damage: 0.4s, sharper. Light hits: micro-shake only.
- **Hit stop (freeze frames):** On a successful hit, freeze both the attacker and target for 2–4 frames. This is a subtle but critical component of impactful-feeling hits.
- **Enemy hit flash:** On receiving damage, briefly (1–2 frames) set enemy material to solid white or red. Confirms the hit registered.
- **Weapon trail:** Additive trail renderer on weapon, active during attack active frames. Greatsword: wide arc. Dual Blades: tight streaks.
- **Death effects:** Enemy ragdoll on death, brief flash, small particle burst at death position. Corpses fade out after 10 seconds (prevent clutter in late waves).
- **Level-up particle:** Full-screen golden ring burst on level up. One moment of excess is earned.

---

### P05 · Performance & QA Pass `L` · 2–3 weeks

- **Performance target:** Stable 60fps on a mid-range machine (e.g. GTX 1060 / RX 580, 8GB RAM). Profile on target hardware, not development hardware.
- **Key optimisations:** Static batching for map geometry, LOD groups on enemy meshes, object pooling for projectiles and particles, occlusion culling enabled, shadow distance tuned.
- **Memory:** No leaks between scene loads. Profile with engine memory tools after 5+ mission runs.
- **QA checklist:**
  - Complete all 3 difficulty tiers on at least 5 different generated maps
  - All enemy types in all wave compositions
  - Equip/unequip all item types
  - Save, quit, reload — verify state is correct
  - Die on wave 1, die on final wave, complete a full run
  - All menu screens and transitions
- **Bug triage:** Fix all crash-level and progression-blocking bugs. Cosmetic bugs are acceptable for launch if logged.
- **Build verification:** Final release build tested end-to-end on a clean machine with no dev tools installed.

---

## Post-MVP Backlog (v1.1+)

These features were deliberately cut to keep MVP scope manageable. Build them once the core game is proven fun.

| Feature | Notes |
|---------|-------|
| Multi-phase boss fights | Requires solid combat foundation first |
| Skill tree | Add once flat leveling has proven insufficient for depth |
| Crafting & upgrade system | Extend loot loop after players have experienced drop economy |
| Status effects | Burns, freeze, poison — add to combat depth |
| Additional biomes | Forest → Volcanic → Dungeon progression |
| 3rd+ weapon archetype | Spear, hammer, staff |
| Dynamic music layering | Stacking intensity tracks per combat phase |
| Colorblind & accessibility modes | High priority for v1.1 |
| Quest board rotation | Multiple concurrent mission types |
| Cloud saves | Cross-device progression |
| Destructible environment | Breakable pillars, crates for cover |
| Multiplayer co-op | Significant architectural addition — plan for it, don't build it yet |

---

*Roadmap version 1.0 — MVP scope*
