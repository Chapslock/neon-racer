## Engine TODO

* particle effects
* font rendering
* save/load feature
* UI/HUD enhancements

## Engine Architecture

### Core Systems

```mermaid
graph TD
    App[Application] --> EC[EngineContext]
    EC --> CR[ComponentRegistry]
    EC --> EB[EventBus]
    EC --> MR[MeshRegistry]
    EC --> TR[TextureRegistry]
    EC --> SR[ShaderRegistry]
    EC --> CS[CameraSystem]
    EC --> SM[SceneManager]
    EC --> Win[Window]
    EC --> GAM[GltfAssetManager]

    MR --> RC[RefCounted]
    TR --> RC
    SR --> RC

    SM --> SI[SceneInstantiator]
    SI --> GMF[GltfModelFactory]
    SI --> CV[ComponentRule]
```

### Rendering Pipeline

```mermaid
graph LR
    RS[RenderSystem] --> RCtx[RenderContext]
    RCtx --> OP[OpaquePass]
    RCtx --> SP[SpritePass]
    OP --> PBR[PbrMaterialRenderer]
    SP --> SRen[SpriteRenderer]
    OP -.-> MR[MeshRegistry]
    OP -.-> TR[TextureRegistry]
    OP -.-> SR[ShaderRegistry]
```

Per frame:

```mermaid
sequenceDiagram
    participant RS as RenderSystem
    participant RC as RenderContext
    participant OP as OpaquePass
    participant SP as SpritePass

    RS->>RC: beginFrame
    Note over RC: Collect camera, lights, fog
    RS->>OP: collectVisibles
    Note over OP: Query OpaqueLayer + PbrMaterial + Mesh + Transform
    RS->>OP: execute
    Note over OP: Batch by PbrMaterial, draw
    RS->>SP: collectVisibles
    Note over SP: Query SpriteLayer + SpriteMaterial + Mesh + Transform
    RS->>SP: execute
    Note over SP: Batch by SpriteMaterial, draw
```

### Scene System

```mermaid
graph TD
    SB[SceneBuilder] --> BP[EntityBlueprint]
    SB --> SC[Scene]
    SC --> SM[SceneManager]
    SM --> SI[SceneInstantiator]
    SI --> GMF[GltfModelFactory]
    SI --> VAL[ComponentRule Validation]
    SI --> INST[SceneInstance]
```

### Engine Lifecycle

```mermaid
sequenceDiagram
    participant App as Application
    participant EC as EngineContext
    participant SM as SceneManager
    participant SI as SceneInstantiator
    participant ES as EngineSystems

    Note over App: STARTUP
    App->>EC: createSubsystems
    App->>App: createEngineSystems
    Note over App: EntitySystem, TweenSystem, AnimationSystem,<br/>TransformSystem, PhysicsSystem, RenderSystem
    App->>SM: new SceneManager
    App->>ES: onInit
    App->>SM: loadImmediate(initialScene)
    SM->>SI: instantiate
    SI-->>SM: SceneInstance

    Note over App: MAIN LOOP
    loop Every Frame
        App->>ES: onUpdate (all systems sorted by phase)
        App->>ES: onRender
        App->>ES: onDebugRender
        App->>SM: processPendingTransitions
    end

    Note over App: SHUTDOWN
    App->>SM: unloadAll
    App->>ES: onDestroy
    App->>EC: dispose
```

### System Execution Order

```mermaid
graph LR
    I[INPUT] --> L[LOGIC]
    L --> P[PHYSICS]
    P --> R[RENDER]
```

| Phase | Systems | Managed By |
|-------|---------|-----------|
| INPUT | EntitySystem | Engine |
| LOGIC | TweenSystem, AnimationSystem, TransformSystem, *scene systems* | Engine + Scene |
| PHYSICS | PhysicsSystem | Engine |
| RENDER | RenderSystem | Engine |

Engine systems are created internally by `Application.createEngineSystems()`. Scene-provided systems (user-defined) run in LOGIC phase alongside engine systems, sorted by `SystemOrdering`.

### System Ownership

```mermaid
graph TD
    subgraph Engine-Managed
        ES[EntitySystem<br/>INPUT]
        TS[TweenSystem<br/>LOGIC]
        AS[AnimationSystem<br/>LOGIC]
        TrS[TransformSystem<br/>LOGIC]
        PS[PhysicsSystem<br/>PHYSICS]
        RS[RenderSystem<br/>RENDER]
    end

    subgraph Scene-Provided
        GS1[Game System 1<br/>LOGIC]
        GS2[Game System 2<br/>LOGIC]
        GSN[...<br/>LOGIC]
    end

    App[Application] -->|createEngineSystems| ES
    App -->|createEngineSystems| TS
    App -->|createEngineSystems| AS
    App -->|createEngineSystems| TrS
    App -->|createEngineSystems| PS
    App -->|createEngineSystems| RS

    SM[SceneManager] -->|scene.systems| GS1
    SM -->|scene.systems| GS2
    SM -->|scene.systems| GSN
```

### Configuration API

```mermaid
graph TD
    AS[ApplicationSpecification] --> WS[windowSpec]
    AS --> PC[physicsConfig]
    AS --> IS[initialScene]

    WS --> Win[Window]
    WS --> CS[CameraSystem]
    PC --> PS[PhysicsSystem]
    IS --> SM[SceneManager.loadImmediate]

    Note1[All engine systems<br/>auto-created internally]
```

Usage:

```java
Application.create(ApplicationSpecification.builder()
    .windowSpec(new WindowSpecification("Neon Racer", 1920, 1080))
    .physicsConfig(PhysicsConfig.defaults())  // optional
    .initialScene(myScene)
    .build()
).run();
```

### Package Organization

```mermaid
graph TD
    subgraph ecs
        Component
        Tag
        Entity
        ComponentRegistry
        GameSystem
        SystemPhase
        SystemOrdering
        EntitySystem
        TransformSystem
    end

    subgraph render
        RenderSystem
        CameraSystem
        MeshRegistry
        TextureRegistry
        ShaderRegistry
        OpaquePass
        SpritePass
        RenderContext
        PbrMaterialRenderer
        SpriteRenderer
    end

    subgraph physics
        PhysicsSystem
        PhysicsWorld
        Physics
        CollisionShapeFactory
    end

    subgraph scene
        Scene
        SceneManager
        SceneInstantiator
        GltfModelFactory
        SceneInstance
    end

    subgraph animation
        AnimationSystem
        Animation
        Channel
    end

    subgraph application
        Application
        ApplicationSpecification
        EngineContext
        Time
    end
```

### ECS

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

| Component | Requires |
|-----------|----------|
| Mesh | Transform |
| PhysicsBody | Transform |
| PointLight | Transform |
| SpriteMaterial | Mesh |
| PbrMaterial | Mesh |
| Skeleton | Mesh |

Entities violating these rules are skipped with a clear error message.

## Tech Stack

- Java 21+, Lombok
- LWJGL 3 (OpenGL, GLFW, STB)
- JOML (vectors, matrices, quaternions)
- JBullet (physics)
- fastutil (primitive collections)
