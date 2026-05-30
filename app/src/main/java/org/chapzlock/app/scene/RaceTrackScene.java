package org.chapzlock.app.scene;

import java.util.List;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.systems.PlayerCameraFollowSystem;
import org.chapzlock.app.systems.PlayerInputSystem;
import org.chapzlock.app.systems.PlayerMovementSystem;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.asset.FileUtils;
import org.chapzlock.core.asset.RawMeshData;
import org.chapzlock.core.asset.RawMeshDataFactory;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Reflection;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Terrain;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.physics.CollisionShapeFactory;
import org.chapzlock.core.physics.PhysicsSpecs;
import org.chapzlock.core.render.EntityShaderProps;
import org.chapzlock.core.render.TerrainShaderProps;
import org.chapzlock.core.render.UiShaderProps;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.Key;
import org.chapzlock.core.scene.MaterialType;
import org.chapzlock.core.scene.Scene;
import org.joml.Vector3f;

public class RaceTrackScene {

    public static Scene create() {

        return Scene.builder("race-track")
            .inputMap(InputMap.builder()
                .action("accelerate", Key.UP, Key.W)
                .action("brake", Key.DOWN, Key.S)
                .action("steer_left", Key.LEFT, Key.A)
                .action("steer_right", Key.RIGHT, Key.D)
                .action("quit", Key.ESCAPE)
                .build())
            .entity("player", e -> e
                .transform(0, 10, -5, 0, 0, 180)
                .component(new PlayerInputComponent())
                .tag(new PlayerTag())
                .component(ctx -> {
                    var meshSystem = ctx.getMeshSystem();
                    var textureSystem = ctx.getTextureSystem();
                    var materialSystem = ctx.getMaterialSystem();

                    Mesh mesh = meshSystem.load("obj/funcar.obj");
                    Texture texture = textureSystem.load("textures/funcar.png");
                    Material material = new Material(
                        new Shader(EntityShaderProps.VERTEX_FILE, EntityShaderProps.FRAGMENT_FILE),
                        texture,
                        Reflection.builder().reflectivity(.5f).shineDamper(1f).build());
                    materialSystem.register(material, MaterialType.ENTITY);

                    PhysicsBody physicsBody = new PhysicsBody(
                        CollisionShapeFactory.createConvexHullShape(meshSystem.getRawMeshById(mesh.getId())),
                        PhysicsSpecs.builder().mass(1).build());

                    return new Component[]{ mesh, material, physicsBody };
                }))
            .entity("terrain", e -> e
                .transform(-400, 0, -400, 10, 0, 0)
                .component(ctx -> {
                    var meshSystem = ctx.getMeshSystem();
                    var textureSystem = ctx.getTextureSystem();
                    var materialSystem = ctx.getMaterialSystem();

                    Terrain terrainProps = Terrain.builder().maxHeight(40).build();
                    Material terrainMaterial = Material.builder()
                        .textures(List.of(
                            textureSystem.load("textures/terrain/grass.png"),
                            textureSystem.load("textures/terrain/dirt.png"),
                            textureSystem.load("textures/terrain/concrete.png")))
                        .blendMap(textureSystem.load("textures/terrain/blendMap.png"))
                        .shader(new Shader(TerrainShaderProps.VERTEX_FILE, TerrainShaderProps.FRAGMENT_FILE))
                        .build();
                    materialSystem.register(terrainMaterial, MaterialType.TERRAIN);

                    RawMeshData rawMeshData = RawMeshDataFactory.generateTerrainFromHeightMap(
                        FileUtils.loadBufferedImage("textures/terrain/heightmap.png"),
                        terrainProps.getSize(), terrainProps.getMaxHeight());
                    Mesh mesh = meshSystem.load(rawMeshData);

                    PhysicsBody physicsBody = new PhysicsBody(
                        CollisionShapeFactory.createBvhTriangleMeshShapeFromRaw(rawMeshData, true),
                        PhysicsSpecs.builder().restitution(0).build());

                    return new Component[]{ terrainProps, terrainMaterial, mesh, physicsBody };
                }))
            .entity("ui", e -> e
                .transform(0, 0, 0, 0, 0, 0, 200)
                .component(ctx -> {
                    var meshSystem = ctx.getMeshSystem();
                    var textureSystem = ctx.getTextureSystem();
                    var materialSystem = ctx.getMaterialSystem();

                    Texture texture = textureSystem.load("textures/speedometer.png");
                    Material material = Material.builder()
                        .shader(new Shader(UiShaderProps.VERTEX_FILE, UiShaderProps.FRAGMENT_FILE))
                        .textures(List.of(texture))
                        .build();
                    materialSystem.register(material, MaterialType.UI);

                    Mesh mesh = meshSystem.load(RawMeshDataFactory.createUi());
                    return new Component[]{ material, mesh };
                }))
            .pointLight(-5, 300, -10)
            .camera(0, 5, 10)
            .sky()
            .systems(
                new PlayerInputSystem(),
                new PlayerMovementSystem(),
                new PlayerCameraFollowSystem())
            .build();
    }
}
