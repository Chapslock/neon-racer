# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew build              # Build both modules
./gradlew :app:run           # Run the game (requires display/GPU)
./gradlew :app:shadowJar     # Create fat JAR at app/build/libs/neon-racer-<version>.jar
./gradlew :core:test         # Run core unit tests (JUnit 5)
./gradlew :app:test          # Run app unit tests
./gradlew test               # Run all tests
```

JVM flag `--enable-native-access=ALL-UNNAMED` is required at runtime (configured in build.gradle.kts).

## Project Structure

Two Gradle modules (Kotlin DSL):
- **core** — Reusable game engine library (`org.chapzlock.core`)
- **app** — The neon-racer game built on top of core (`org.chapzlock.app`)

### Core Packages

- `ecs/` — GameSystem, SystemPhase, SystemScheduler, Component, Tag, Entity, ComponentRegistry, ComponentStore, ComponentRule, EntityFactory, TransformSystem
- `application/` — Application, Engine, ApplicationSpecification, Disposable, ApplicationUtil
- `scene/` — Scene, SceneManager, SceneInstance, SceneBuilder, EntityRecipe, SceneLoadedEvent, SceneUnloadedEvent
- `render/` — RenderSystem, RenderPass, RenderContext, OpaquePass, SpritePass, PbrMaterialRenderer, SpriteRenderer, CameraProjection, MeshRegistry, TextureRegistry, ShaderRegistry, RefCounted, ShaderProps, GL utilities
- `component/` — All data components (Camera, Transform, Mesh, PbrMaterial, SpriteMaterial, PointLight, DirectionalLight, PhysicsBody, Skeleton, etc.)
- `physics/` — PhysicsSystem and all physics utilities
- `event/` — EventBus, Event, lifecycle events
- `input/` — InputMap, Keyboard/mouse event classes
- `window/` — Window, WindowSpecification
- `asset/` — FileUtils, RawMeshData, RawMeshDataFactory, gltf/ (GltfLoader, GltfAssetManager, GltfUtil)
- `logging/` — Log
- `tween/` — TweenRegistry, TweenSystem, Ease

## Architecture

### ECS (Entity-Component-System)

Entities are plain integer IDs (`Entity.create(registry)`). Components are marker-interface data holders (`implements Component`). Tags are empty components used for querying (`implements Tag`). Systems implement `GameSystem` and receive dependencies via `onInit(Engine engine)`. They operate on entities via typed `forEach` overloads: `registry.forEach(A.class, B.class, (entityId, a, b) -> { ... })`.

`ComponentRegistry` stores components in typed `ComponentStore` maps, indexed by entity ID. It uses bitset masks for fast archetype queries. Query results are cached internally and invalidated on component mutations — queries are O(1) on stable frames. Other query methods: `getSingleton(T.class)` for singleton components, `getEntityByName("name")` for named entity lookup, `count(T.class)` for entity counting, `entityExists(id)` for existence checks.

### Engine

`Engine` is a typed service container for all engine subsystems. Created via `Engine.create(ApplicationSpecification)` which builds all subsystems bottom-up. No singletons — all dependencies flow through Engine.

**Registries:** ComponentRegistry, MeshRegistry, TextureRegistry, ShaderRegistry, GltfAssetManager.

**Systems (tick every frame):** TweenSystem, AnimationSystem, TransformSystem, PhysicsSystem, RenderSystem — registered as permanent systems in the SystemScheduler.

**Utils (no tick):** CameraProjection, EntityFactory.

**Infrastructure:** EventBus, Window, Time, TweenRegistry, Physics, SceneManager, SystemScheduler.

**Lifecycle:** `Engine.create(spec)` constructs everything, registers permanent systems into the scheduler, and initializes them. `Engine.dispose()` unloads scenes, destroys systems in reverse order, frees GPU resources, and closes the window.

**Testing:** `Engine.testEngine()` creates a minimal engine with fresh ComponentRegistry + EventBus + Time + TweenRegistry + EntityFactory + SceneManager + SystemScheduler (GPU systems null) for unit tests.

### Scene System

A `Scene` stores entity recipes (via `EntityRecipe` — a name + `Consumer<EntityBuilder>` lambda) and game-logic systems. Scenes are built via a fluent builder where everything is an entity with direct component attachment:

```java
Scene.builder("name")
    .entity("terrain", e -> e.component(new GltfModel("models/track.glb")))
    .entity("sun", e -> e.directionalLight(light))
    .entity("player", e -> e.component(Transform.at(0, 0, 0)).component(new GltfModel("glb/car.glb")).tag(new PlayerTag()))
    .entity(e -> e.camera(cam))  // nameless entity
    .systems(new PlayerSystem())
    .inputMap(controls)
    .build();
```

`SceneManager` manages scene lifecycle: one primary scene plus zero or more additive scenes. It handles instantiation directly (entity creation via EntityFactory, GPU asset tracking, system initialization) and returns a `SceneInstance`.

**Transitions:** `loadImmediate(scene)` for initial load (synchronous). `load(scene)` queues a deferred transition (old scene unloaded, new scene instantiated at end-of-frame via `processPendingTransitions()`). `loadAdditive(scene)` adds a persistent overlay scene. `unload(scene)` removes an additive scene.

**Asset ownership:** GPU registries (MeshRegistry, TextureRegistry, ShaderRegistry) use `RefCounted` for reference counting (`retain()`/`release()`). SceneManager retains assets when instantiating a scene and releases them on teardown. Shared assets (same path across scenes) survive until all referencing scenes are unloaded.

**Component validation:** `EntityFactory` checks `ComponentRule` dependencies (e.g., Mesh requires Transform, PointLight requires Transform). Invalid entities are skipped with a clear error message.

### Application Lifecycle

`Application.run(spec)` is the entry point. `ApplicationSpecification` declares `windowSpec`, optional `physicsConfig`, and `initialScene`. Internally it calls `Engine.create(spec)` which builds all subsystems and engine systems in one shot, then loads the initial scene and enters the main loop.

**Main loop order per frame:** `onUpdate(delta)` → `onRender(delta)` → `onDebugRender(delta)` on all active systems (sorted by SystemPhase), then `sceneManager.processPendingTransitions()`.

### SystemPhase

Systems declare their execution phase via `default SystemPhase phase()`. The enum values are: `INPUT`, `LOGIC` (default), `PHYSICS`, `RENDER`. Core systems (PhysicsSystem, RenderSystem) declare their own phase; game-specific systems default to LOGIC.

### Rendering Pipeline

Two independent render passes, each owning its renderer directly:

- **OpaquePass** (order 200) — renders 3D world geometry. Queries `OpaqueLayer + PbrMaterial + Mesh + Transform`, batches by material, uses `PbrMaterialRenderer` (Cook-Torrance BRDF with metallic-roughness workflow).
- **SpritePass** (order 800) — renders 2D sprites/HUD. Queries `SpriteLayer + SpriteMaterial + Mesh + Transform`, batches by material, uses `SpriteRenderer` (orthographic projection, alpha blending).

Each renderer owns its shader lifecycle internally. `PbrMaterial` is a data component with texture maps and factors. `SpriteMaterial` is a record holding texture + tint. No shared `MaterialRenderer` interface — each pass has its own concrete renderer.

`RenderContext` is populated once per frame with camera matrices, lights (position read from Transform), fog, and ambient settings. Passes read from it rather than querying the registry directly.

### Physics

JBullet (Bullet physics Java port). `PhysicsSystem` owns a `DiscreteDynamicsWorld`. Entities with `PhysicsBody` + `Transform` components are registered into the physics world via lifecycle events. Transform synchronization is handled via `EntityMotionState`.

### Event System

`EventBus` with typed publish/subscribe. Listeners return `true` to stop propagation. Input events (key/mouse) flow through this bus. Subscribe returns a `Subscription` handle for cleanup. Each Engine owns its own EventBus instance.

### Resource Cleanup

GPU registries (MeshRegistry, TextureRegistry, ShaderRegistry) implement `Disposable`. They track all allocated OpenGL resources and free them in `dispose()`. `Engine.dispose()` cascades to all disposable subsystems on shutdown. Each registry uses a `RefCounted` instance for reference counting (`retain()`/`release()`) with a type-specific cleanup callback. `dispose()` frees everything unconditionally at shutdown regardless of ref counts.

## Naming Convention

| Suffix | Definition | Ticks? | Example |
|--------|-----------|--------|---------|
| `System` | Runs logic every frame. Implements `GameSystem`, registered in scheduler. | Yes | `PhysicsSystem`, `RenderSystem` |
| `Registry` | Owns and indexes a collection of resources by key/ID. | No | `ComponentRegistry`, `MeshRegistry` |
| `Manager` | Orchestrates lifecycle of complex objects (create, transition, destroy). | No | `SceneManager` |
| `Util` | Stateless logic. All methods could be `static` or class holds only config. | No | `TransformUtil`, `ShaderUtil` |
| No suffix | Pure data, value objects, components. | No | `Transform`, `Camera`, `Fog` |

Rule: if naming something `FooSystem`, it must implement `GameSystem` with meaningful `onUpdate`/`onRender` logic. If not, pick a different suffix.

## Tech Stack

- Java 21+, Lombok for boilerplate reduction
- LWJGL 3 (OpenGL, GLFW, STB)
- JOML for math (vectors, matrices, quaternions)
- JBullet for physics
- fastutil for primitive collections
