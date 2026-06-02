# Neon Racer

A 3D game engine and racing game built from scratch in Java with LWJGL 3/OpenGL.

## Build & Run

```bash
./gradlew build              # Build both modules
./gradlew :app:run           # Run the game (requires display/GPU)
./gradlew :app:shadowJar     # Create fat JAR
./gradlew :core:test         # Run core unit tests
./gradlew test               # Run all tests
```

## Project Structure

```
neon-racer/
  core/   — Reusable game engine library (org.chapzlock.core)
  app/    — The neon-racer game (org.chapzlock.app)
```

## Engine Architecture

### Core Systems Overview

```
+-------------------+       +------------------+
|   Application     |------>|  EngineContext   |
| (creates & runs)  |       | (holds all subs) |
+-------------------+       +--------+---------+
                                     |
         +-------+-------+-------+--+--+-------+-------+-------+
         |       |       |       |     |       |       |       |
         v       v       v       v     v       v       v       v
     +------+ +-----+ +------+ +---+ +----+ +-----+ +-----+ +----+
     |Comp  | |Event| |Mesh  | |Tex| |Shdr| |Cam  | |Scene| |Win |
     |Regis | |Bus  | |Sys   | |Sys| |Sys | |Sys  | |Mgr  | |dow |
     +------+ +-----+ +------+ +---+ +----+ +-----+ +-----+ +----+
                        |        |     |
                        +---+----+-----+
                            |
                       +----+-----+
                       |RefCounted|  (shared ref-counting)
                       +----------+

Scene System:
  SceneManager ──delegates──> SceneInstantiator ──uses──> DescriptorResolver
       |                            |                         GltfSceneInstantiator
       |                            |
       v                            v
  SceneInstance              ComponentRule (validation)
  (entityIds, assets, systems)

Rendering Pipeline:
  RenderSystem
       |
       +──> OpaquePass ──> PbrMaterialRenderer  (3D world geometry)
       |
       +──> SpritePass ──> SpriteRenderer       (2D sprites/HUD)
```

### Rendering Pipeline

```
Each frame:
  RenderContext.beginFrame()     Collect camera, lights, fog from registry
       |
       v
  OpaquePass.collectVisibles()  Query: OpaqueLayer + PbrMaterial + Mesh + Transform
  OpaquePass.execute()          Batch by PbrMaterial, render each entity
       |
       v
  SpritePass.collectVisibles()  Query: SpriteLayer + SpriteMaterial + Mesh + Transform
  SpritePass.execute()          Batch by SpriteMaterial, render each sprite
```

### Scene System

```
Scene Definition (compile-time):
  Scene.builder("name")
    .entity("terrain", e -> e.gltfModel("track.glb"))
    .entity("sun", e -> e.directionalLight(...))
    .entity("player", e -> e.transform(...).gltfModel("car.glb"))
    .entity(e -> e.camera(...))
    .systems(new PlayerSystem())
    .inputMap(controls)
    .build()

Scene Instantiation (runtime):
  SceneBuilder ──build()──> Scene ──load()──> SceneManager
                                                    |
                                              SceneInstantiator
                                                    |
                                    +---------------+---------------+
                                    |               |               |
                              Resolve          Load glTF        Validate
                             descriptors        models        ComponentRules
                                    |               |               |
                                    v               v               v
                              Components      Child entities   Skip invalid
                              added to         parented to     entities with
                              registry         owner entity    error message
                                    |               |
                                    +-------+-------+
                                            |
                                            v
                                      SceneInstance
                                   (entityIds, assets, systems)
```

### Engine Lifecycle

```
STARTUP
  1. ApplicationUtil.init()         Initialize GLFW
  2. createSubsystems()             Create all engine systems
  3. SceneManager created           Holds SceneInstantiator
  4. Engine systems onInit()        PhysicsSystem, RenderSystem, TransformSystem, etc.
  5. loadImmediate(initialScene)    First scene instantiated

MAIN LOOP (every frame)
  1. Time.update(delta)
  2. InputMap.frameReset()
  3. For all active systems (sorted by SystemPhase):
       onUpdate(delta)              [INPUT -> LOGIC -> PHYSICS -> RENDER]
  4. For all active systems:
       onRender(delta)              RenderSystem executes passes here
  5. For all active systems:
       onDebugRender(delta)         Physics debug visualization
  6. processPendingTransitions()    Deferred scene load/unload
  7. swapBuffers + pollEvents       Present frame, process OS events

SHUTDOWN
  1. SceneManager.unloadAll()       Destroy scene entities, release assets
  2. Engine systems onDestroy()
  3. EngineContext.dispose()        Free all GPU resources
  4. Window.destroy()
  5. ApplicationUtil.terminate()    Cleanup GLFW
```

### System Execution Order

```
Per frame, systems execute in SystemPhase order:

  INPUT          InputMap.frameReset()
     |
     v
  LOGIC          TransformSystem, AnimationSystem, game systems (default phase)
     |
     v
  PHYSICS        PhysicsSystem (fixed 60Hz timestep)
     |
     v
  RENDER         RenderSystem -> OpaquePass -> SpritePass
```

### ECS (Entity-Component-System)

- **Entities** are plain integer IDs (`Entity.create(registry)`)
- **Components** implement `Component` — data holders (Transform, Mesh, PbrMaterial, PointLight, etc.)
- **Tags** implement `Tag` — empty markers for query filtering (OpaqueLayer, SpriteLayer, PlayerTag)
- **Systems** implement `GameSystem` — logic that operates on entities via queries

Query API:
```java
// Zero-alloc iteration with typed components
registry.forEach(Transform.class, Velocity.class, (entityId, transform, vel) -> {
    transform.getPosition().add(vel.dx() * delta, vel.dy() * delta, 0);
});

// Singleton access
Camera camera = registry.getSingleton(Camera.class).orElse(null);

// Name-based lookup
int player = registry.getEntityByName("player");
```

### Component Validation

Scene instantiation validates component dependencies:
- `Mesh` requires `Transform`
- `PhysicsBody` requires `Transform`
- `PointLight` requires `Transform`
- `SpriteMaterial` requires `Mesh`
- `PbrMaterial` requires `Mesh`
- `Skeleton` requires `Mesh`

Entities violating these rules are skipped with a clear error message.

## Tech Stack

- Java 21+, Lombok
- LWJGL 3 (OpenGL, GLFW, STB)
- JOML (vectors, matrices, quaternions)
- JBullet (physics)
- fastutil (primitive collections)
