import { useState } from "react";

const phases = [
  {
    id: 1,
    name: "Foundation",
    subtitle: "Engine & Core Systems",
    color: "#e8612c",
    estimateWeeks: "8–12 weeks",
    features: [
      {
        id: "F01",
        title: "Project Setup & Tech Stack",
        scope: "XS",
        weeks: "1w",
        description: "Choose engine (Unity/Unreal/Godot), version control, CI/CD pipeline, folder structure conventions, coding standards.",
        tags: ["infra"],
      },
      {
        id: "F02",
        title: "Third-Person Camera System",
        scope: "M",
        weeks: "1–2w",
        description: "Follow camera with adjustable distance, collision avoidance, lock-on targeting for combat. Smooth interpolation and zoom.",
        tags: ["camera", "core"],
      },
      {
        id: "F03",
        title: "Player Controller & Movement",
        scope: "M",
        weeks: "2w",
        description: "Run, walk, dodge/roll, jump, crouch. Physics-based with responsive feel. Input abstraction layer for keyboard/gamepad.",
        tags: ["player", "core"],
      },
      {
        id: "F04",
        title: "Basic Combat Framework",
        scope: "L",
        weeks: "2–3w",
        description: "Attack state machine, hitbox/hurtbox system, combo buffer, hit detection, damage numbers, knockback. Foundation all weapons build on.",
        tags: ["combat", "core"],
      },
      {
        id: "F05",
        title: "Animation Controller",
        scope: "M",
        weeks: "1–2w",
        description: "Blend tree for locomotion, attack animation triggers, transition weights, root motion handling, IK for ground alignment.",
        tags: ["animation", "core"],
      },
    ],
  },
  {
    id: 2,
    name: "Combat Depth",
    subtitle: "Weapons, Combos & Enemies",
    color: "#c0392b",
    estimateWeeks: "8–14 weeks",
    features: [
      {
        id: "C01",
        title: "Combo System",
        scope: "L",
        weeks: "2–3w",
        description: "Chained light/heavy attacks, timing windows, finishers, cancel windows into dodge. Visual and audio feedback per hit. Combo counter UI.",
        tags: ["combat"],
      },
      {
        id: "C02",
        title: "Weapon Archetypes (3–4 types)",
        scope: "XL",
        weeks: "3–4w",
        description: "e.g. Greatsword (slow, AoE), Dual Blades (fast, flurry), Staff (ranged + melee), Hammer (stagger-focused). Unique movesets and feel per archetype.",
        tags: ["combat", "content"],
      },
      {
        id: "C03",
        title: "Enemy AI — Basic",
        scope: "L",
        weeks: "2–3w",
        description: "NavMesh pathing, aggro/patrol states, melee attack behaviors, telegraphing animations, basic difficulty scaling. 3–5 base enemy types.",
        tags: ["ai", "combat"],
      },
      {
        id: "C04",
        title: "Enemy AI — Advanced (Bosses)",
        scope: "XL",
        weeks: "3–4w",
        description: "Multi-phase boss fights, unique attack patterns, enrage thresholds, cinematic moments. 2–3 bosses with full attack libraries.",
        tags: ["ai", "combat", "content"],
      },
      {
        id: "C05",
        title: "Status Effects System",
        scope: "M",
        weeks: "1–2w",
        description: "Burn, freeze, stun, poison, bleed etc. Stackable, timed, visual indicators. Affects both enemies and player.",
        tags: ["combat"],
      },
    ],
  },
  {
    id: 3,
    name: "World Systems",
    subtitle: "Procedural Maps & Portals",
    color: "#27ae60",
    estimateWeeks: "8–12 weeks",
    features: [
      {
        id: "W01",
        title: "Procedural Map Generator",
        scope: "XL",
        weeks: "3–5w",
        description: "Tile/chunk-based generation with biome rules (forest, dungeon, ruins, volcanic). Guaranteed traversable layout, enemy spawn zones, loot placements. Seed-based for reproducibility.",
        tags: ["world", "proc-gen"],
      },
      {
        id: "W02",
        title: "Environmental Art Pass",
        scope: "L",
        weeks: "2–3w",
        description: "Modular props per biome, lighting moods, particle effects (fog, embers, dust). Ensure proc-gen feels handcrafted.",
        tags: ["world", "art"],
      },
      {
        id: "W03",
        title: "Portal System",
        scope: "M",
        weeks: "1–2w",
        description: "Portal objects in hub and maps that load missions or other zones. Visual portal FX, loading transition, zone metadata (difficulty, type, rewards).",
        tags: ["world", "systems"],
      },
      {
        id: "W04",
        title: "Destructible Environment Elements",
        scope: "M",
        weeks: "1–2w",
        description: "Breakable crates, barrels, pillars. Physics debris, loot drops on break. Adds combat dynamism.",
        tags: ["world"],
      },
    ],
  },
  {
    id: 4,
    name: "Game Loop",
    subtitle: "Waves, Missions & Hub",
    color: "#2980b9",
    estimateWeeks: "8–12 weeks",
    features: [
      {
        id: "G01",
        title: "Wave System",
        scope: "L",
        weeks: "2–3w",
        description: "Wave director: spawn budgets, escalating difficulty, wave composition rules (elites, ranged, swarmers). Between-wave rest periods. Wave counter UI.",
        tags: ["gameplay", "systems"],
      },
      {
        id: "G02",
        title: "Mission Framework",
        scope: "L",
        weeks: "2–3w",
        description: "Mission types: survive N waves, kill boss, escort, timed clear. Objectives UI, success/fail conditions, end-of-mission summary with loot reveal.",
        tags: ["gameplay", "systems"],
      },
      {
        id: "G03",
        title: "Main Hub Area",
        scope: "M",
        weeks: "1–2w",
        description: "Central social/preparation space: blacksmith NPC, quest board, storage, portal plaza. Non-combat zone with ambient life.",
        tags: ["world", "gameplay"],
      },
      {
        id: "G04",
        title: "Quest Board & Mission Selection",
        scope: "M",
        weeks: "1–2w",
        description: "Browse available missions by difficulty/type, preview rewards, lock-in and launch. Rotates over time or unlocks via progression.",
        tags: ["gameplay", "ui"],
      },
      {
        id: "G05",
        title: "Loot & Drop System",
        scope: "L",
        weeks: "2w",
        description: "Enemy loot tables, chest spawns, rarity tiers (Common→Legendary), item identification, equip-on-pickup option. Satisfying drop VFX.",
        tags: ["gameplay", "systems"],
      },
    ],
  },
  {
    id: 5,
    name: "RPG Progression",
    subtitle: "Stats, Skills & Gear",
    color: "#8e44ad",
    estimateWeeks: "6–10 weeks",
    features: [
      {
        id: "R01",
        title: "Character Stats & Leveling",
        scope: "M",
        weeks: "1–2w",
        description: "XP from kills/missions, level-up with stat point allocation (STR, DEX, VIT, INT, etc.). Stat influence on damage, speed, health, mana.",
        tags: ["rpg", "systems"],
      },
      {
        id: "R02",
        title: "Skill Tree",
        scope: "XL",
        weeks: "3–4w",
        description: "Passive and active abilities per weapon archetype + global tree. Node unlock with skill points. Visual tree UI with preview tooltips.",
        tags: ["rpg", "ui"],
      },
      {
        id: "R03",
        title: "Equipment & Inventory System",
        scope: "L",
        weeks: "2–3w",
        description: "Gear slots (weapon, armor, accessories), item comparison, stat tooltips, inventory grid UI, drag-and-drop equip.",
        tags: ["rpg", "ui"],
      },
      {
        id: "R04",
        title: "Crafting & Upgrade System",
        scope: "L",
        weeks: "2w",
        description: "Weapon/armor upgrades at blacksmith using harvested materials. Recipes, upgrade trees, visual changes on upgraded gear.",
        tags: ["rpg", "systems"],
      },
      {
        id: "R05",
        title: "Progression Persistence (Save System)",
        scope: "M",
        weeks: "1–2w",
        description: "Save/load character state, inventory, unlocks, mission progress. Cloud save support. Multiple save slots.",
        tags: ["rpg", "systems"],
      },
    ],
  },
  {
    id: 6,
    name: "Polish & UX",
    subtitle: "UI, Audio & Game Feel",
    color: "#d4a017",
    estimateWeeks: "6–10 weeks",
    features: [
      {
        id: "P01",
        title: "HUD Design",
        scope: "M",
        weeks: "1–2w",
        description: "Health/stamina/mana bars, combo counter, wave indicator, minimap, buff/debuff icons. Clean, readable in action.",
        tags: ["ui"],
      },
      {
        id: "P02",
        title: "Main Menu & Settings",
        scope: "S",
        weeks: "1w",
        description: "Start, load, settings, credits screens. Graphics, audio, control remapping options.",
        tags: ["ui"],
      },
      {
        id: "P03",
        title: "Sound Design & Music",
        scope: "L",
        weeks: "2–3w",
        description: "Combat SFX (hits, blocks, special moves), ambient biome audio, hub music, dynamic combat music layering, boss themes.",
        tags: ["audio"],
      },
      {
        id: "P04",
        title: "VFX & Juice Pass",
        scope: "L",
        weeks: "2–3w",
        description: "Screen shake, hit freeze, particle bursts, weapon trails, spell FX, death animations. The layer that makes combat feel impactful.",
        tags: ["vfx"],
      },
      {
        id: "P05",
        title: "Accessibility & QoL",
        scope: "M",
        weeks: "1–2w",
        description: "Colorblind modes, subtitle options, controller haptics, rebindable controls, difficulty presets.",
        tags: ["ux"],
      },
    ],
  },
  {
    id: 7,
    name: "Release Prep",
    subtitle: "Testing, Optimization & Launch",
    color: "#7f8c8d",
    estimateWeeks: "4–8 weeks",
    features: [
      {
        id: "X01",
        title: "Performance Optimization",
        scope: "L",
        weeks: "2–3w",
        description: "LOD systems, occlusion culling, draw call batching, memory profiling, target 60fps on mid-range hardware.",
        tags: ["tech"],
      },
      {
        id: "X02",
        title: "QA & Bug Fix Sprint",
        scope: "L",
        weeks: "2–3w",
        description: "Systematic playtesting across all missions/biomes, balance pass on enemy difficulty and loot rates, bug triage and fixes.",
        tags: ["qa"],
      },
      {
        id: "X03",
        title: "Platform Build & Certification",
        scope: "M",
        weeks: "1–2w",
        description: "Platform-specific builds (PC/console), store page assets, certification checklist compliance.",
        tags: ["release"],
      },
    ],
  },
];

const scopeColors = {
  XS: { bg: "#1a2a1a", text: "#4ade80", border: "#4ade80" },
  S: { bg: "#1a2520", text: "#34d399", border: "#34d399" },
  M: { bg: "#1a2030", text: "#60a5fa", border: "#60a5fa" },
  L: { bg: "#251a20", text: "#f97316", border: "#f97316" },
  XL: { bg: "#2a1a1a", text: "#f87171", border: "#f87171" },
};

const tagColors = {
  core: "#e8612c",
  combat: "#c0392b",
  rpg: "#8e44ad",
  "proc-gen": "#27ae60",
  world: "#16a085",
  gameplay: "#2980b9",
  systems: "#2471a3",
  ui: "#d4ac0d",
  audio: "#1abc9c",
  vfx: "#e74c3c",
  ai: "#e67e22",
  art: "#27ae60",
  player: "#3498db",
  camera: "#9b59b6",
  infra: "#95a5a6",
  animation: "#1abc9c",
  content: "#f39c12",
  tech: "#7f8c8d",
  qa: "#bdc3c7",
  release: "#ecf0f1",
  ux: "#48c9b0",
};

export default function Roadmap() {
  const [expanded, setExpanded] = useState({});
  const [filter, setFilter] = useState("all");

  const allTags = [...new Set(phases.flatMap(p => p.features.flatMap(f => f.tags)))].sort();

  const toggle = (id) => setExpanded(e => ({ ...e, [id]: !e[id] }));

  const totalMin = phases.reduce((a, p) => a + parseInt(p.estimateWeeks), 0);
  const totalMax = phases.reduce((a, p) => a + parseInt(p.estimateWeeks.split("–")[1] || p.estimateWeeks), 0);

  return (
    <div style={{
      fontFamily: "'Georgia', 'Times New Roman', serif",
      background: "#0d0d0d",
      minHeight: "100vh",
      color: "#e8e0d0",
      padding: "0",
    }}>
      {/* Header */}
      <div style={{
        background: "linear-gradient(180deg, #1a0a00 0%, #0d0d0d 100%)",
        borderBottom: "1px solid #3a2a1a",
        padding: "40px 32px 32px",
        position: "relative",
        overflow: "hidden",
      }}>
        <div style={{
          position: "absolute", inset: 0,
          backgroundImage: "radial-gradient(ellipse at 20% 50%, rgba(232,97,44,0.08) 0%, transparent 60%), radial-gradient(ellipse at 80% 20%, rgba(142,68,173,0.06) 0%, transparent 50%)",
          pointerEvents: "none",
        }} />
        <div style={{ maxWidth: 900, margin: "0 auto", position: "relative" }}>
          <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginBottom: 6 }}>
            <span style={{ color: "#e8612c", fontSize: 11, letterSpacing: 4, textTransform: "uppercase", fontFamily: "monospace" }}>Game Dev Roadmap</span>
          </div>
          <h1 style={{
            margin: "0 0 12px",
            fontSize: "clamp(28px, 5vw, 48px)",
            fontWeight: 700,
            letterSpacing: "-0.02em",
            lineHeight: 1.1,
            color: "#f0e8d8",
          }}>
            Fantasy Combat RPG
          </h1>
          <p style={{ margin: "0 0 24px", color: "#8a7a6a", fontSize: 15, maxWidth: 500 }}>
            Third-person combo fighter · Procedural maps · Wave combat · RPG progression
          </p>
          <div style={{ display: "flex", gap: 20, flexWrap: "wrap" }}>
            {[
              { label: "Phases", value: phases.length },
              { label: "Work Items", value: phases.reduce((a, p) => a + p.features.length, 0) },
              { label: "Est. Timeline", value: `${totalMin}–${totalMax} weeks` },
            ].map(s => (
              <div key={s.label} style={{
                background: "#1a1510",
                border: "1px solid #2a1e10",
                borderRadius: 6,
                padding: "10px 18px",
              }}>
                <div style={{ fontSize: 20, fontWeight: 700, color: "#e8612c" }}>{s.value}</div>
                <div style={{ fontSize: 11, color: "#6a5a4a", letterSpacing: 1, textTransform: "uppercase" }}>{s.label}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Filter bar */}
      <div style={{
        background: "#111",
        borderBottom: "1px solid #222",
        padding: "12px 32px",
        display: "flex",
        gap: 8,
        flexWrap: "wrap",
        alignItems: "center",
      }}>
        <span style={{ fontSize: 11, color: "#5a4a3a", textTransform: "uppercase", letterSpacing: 2, marginRight: 4 }}>Filter:</span>
        {["all", ...allTags].map(tag => (
          <button key={tag} onClick={() => setFilter(tag)} style={{
            padding: "3px 10px",
            borderRadius: 12,
            border: `1px solid ${filter === tag ? (tagColors[tag] || "#e8612c") : "#2a2a2a"}`,
            background: filter === tag ? (tagColors[tag] ? tagColors[tag] + "22" : "#e8612c22") : "transparent",
            color: filter === tag ? (tagColors[tag] || "#e8612c") : "#5a4a3a",
            fontSize: 11,
            cursor: "pointer",
            letterSpacing: 0.5,
            textTransform: "uppercase",
            transition: "all 0.15s",
          }}>{tag}</button>
        ))}
      </div>

      {/* Phases */}
      <div style={{ maxWidth: 900, margin: "0 auto", padding: "32px 16px" }}>
        {phases.map((phase, pi) => {
          const filteredFeatures = filter === "all"
            ? phase.features
            : phase.features.filter(f => f.tags.includes(filter));
          if (filteredFeatures.length === 0) return null;
          return (
            <div key={phase.id} style={{ marginBottom: 40 }}>
              {/* Phase header */}
              <div style={{
                display: "flex",
                alignItems: "flex-end",
                gap: 16,
                marginBottom: 16,
                paddingBottom: 12,
                borderBottom: `2px solid ${phase.color}44`,
              }}>
                <div style={{
                  width: 36, height: 36,
                  borderRadius: "50%",
                  background: phase.color + "22",
                  border: `2px solid ${phase.color}`,
                  display: "flex", alignItems: "center", justifyContent: "center",
                  fontSize: 14, fontWeight: 700, color: phase.color, flexShrink: 0,
                }}>{phase.id}</div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 11, color: phase.color, letterSpacing: 3, textTransform: "uppercase", marginBottom: 2 }}>
                    Phase {phase.id} · {phase.estimateWeeks}
                  </div>
                  <h2 style={{ margin: 0, fontSize: 22, fontWeight: 700, color: "#f0e8d8", letterSpacing: "-0.01em" }}>
                    {phase.name} <span style={{ color: "#4a3a2a", fontWeight: 400 }}>— {phase.subtitle}</span>
                  </h2>
                </div>
              </div>

              {/* Feature cards */}
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {filteredFeatures.map(f => {
                  const sc = scopeColors[f.scope];
                  const isOpen = expanded[f.id];
                  return (
                    <div key={f.id} style={{
                      background: isOpen ? "#1a1510" : "#141414",
                      border: `1px solid ${isOpen ? phase.color + "44" : "#222"}`,
                      borderRadius: 8,
                      overflow: "hidden",
                      transition: "all 0.15s",
                    }}>
                      <button
                        onClick={() => toggle(f.id)}
                        style={{
                          width: "100%",
                          padding: "12px 16px",
                          background: "transparent",
                          border: "none",
                          cursor: "pointer",
                          display: "flex",
                          alignItems: "center",
                          gap: 12,
                          textAlign: "left",
                        }}
                      >
                        <span style={{
                          fontFamily: "monospace",
                          fontSize: 11,
                          color: "#4a3a2a",
                          minWidth: 32,
                          flexShrink: 0,
                        }}>{f.id}</span>

                        <span style={{ flex: 1, fontSize: 15, fontWeight: 600, color: "#d8cfc0" }}>
                          {f.title}
                        </span>

                        <span style={{
                          fontSize: 11,
                          color: "#5a4a3a",
                          flexShrink: 0,
                          marginRight: 8,
                        }}>{f.weeks}</span>

                        <span style={{
                          padding: "2px 8px",
                          borderRadius: 4,
                          border: `1px solid ${sc.border}66`,
                          background: sc.bg,
                          color: sc.text,
                          fontSize: 11,
                          fontFamily: "monospace",
                          letterSpacing: 0.5,
                          flexShrink: 0,
                        }}>{f.scope}</span>

                        <span style={{
                          color: isOpen ? phase.color : "#4a3a2a",
                          fontSize: 16,
                          transition: "transform 0.15s",
                          transform: isOpen ? "rotate(90deg)" : "rotate(0deg)",
                          display: "inline-block",
                          flexShrink: 0,
                        }}>›</span>
                      </button>

                      {isOpen && (
                        <div style={{ padding: "0 16px 14px 60px" }}>
                          <p style={{ margin: "0 0 10px", color: "#8a7a6a", fontSize: 14, lineHeight: 1.6 }}>
                            {f.description}
                          </p>
                          <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                            {f.tags.map(tag => (
                              <span key={tag} style={{
                                padding: "2px 8px",
                                borderRadius: 10,
                                background: (tagColors[tag] || "#666") + "18",
                                color: tagColors[tag] || "#888",
                                fontSize: 11,
                                border: `1px solid ${(tagColors[tag] || "#666")}44`,
                                letterSpacing: 0.5,
                                cursor: "pointer",
                              }} onClick={(e) => { e.stopPropagation(); setFilter(tag); }}>{tag}</span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}

        {/* Scope legend */}
        <div style={{
          background: "#111",
          border: "1px solid #222",
          borderRadius: 8,
          padding: "16px 20px",
          marginTop: 8,
        }}>
          <div style={{ fontSize: 11, color: "#4a3a2a", letterSpacing: 2, textTransform: "uppercase", marginBottom: 12 }}>Scope Legend</div>
          <div style={{ display: "flex", gap: 16, flexWrap: "wrap" }}>
            {[
              { scope: "XS", label: "Trivial", desc: "< 3 days" },
              { scope: "S", label: "Small", desc: "3–5 days" },
              { scope: "M", label: "Medium", desc: "1–2 weeks" },
              { scope: "L", label: "Large", desc: "2–3 weeks" },
              { scope: "XL", label: "Epic", desc: "3–5 weeks" },
            ].map(({ scope, label, desc }) => {
              const sc = scopeColors[scope];
              return (
                <div key={scope} style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{
                    padding: "2px 8px",
                    borderRadius: 4,
                    border: `1px solid ${sc.border}66`,
                    background: sc.bg,
                    color: sc.text,
                    fontSize: 11,
                    fontFamily: "monospace",
                  }}>{scope}</span>
                  <span style={{ fontSize: 13, color: "#6a5a4a" }}>{label} <span style={{ color: "#3a2a1a" }}>· {desc}</span></span>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
