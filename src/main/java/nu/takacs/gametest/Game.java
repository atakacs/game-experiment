package nu.takacs.gametest;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.LinearBlendSpace;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.util.SkyFactory;
import nu.takacs.gametest.control.AiControl;
import nu.takacs.gametest.control.HealthDestructionControl;
import nu.takacs.gametest.control.TimedDestructionControl;
import nu.takacs.gametest.factory.*;
import nu.takacs.gametest.hud.Hud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


public class Game extends SimpleApplication implements ActionListener {

    private static final Logger LOG = LoggerFactory.getLogger(Game.class);

    private static final float GRENADE_EXPLOSION_CUTOFF = 40.0f;
    private static final float GRENADE_FORCE_SIZE = 2500.0f;

    private BulletAppState bulletAppState;

    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;

    private Hud hud;

    private ExplosionFactory explosionFactory;
    private FireFactory fireFactory;
    private GrenadeFactory grenadeFactory;
    private TerrainFactory terrainFactory;
    private BoxFactory boxFactory;

    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();

    public static void main(String[] args) {
        try {
            Game app = new Game();

            app.setShowSettings(false);

            final var settings = new AppSettings(true);
            settings.setTitle("Alexander's Playground");
            settings.setResolution(1280, 768);

            app.setSettings(settings);

            app.start();
        } catch (final Exception e) {
            LOG.error("Unhandled exception", e);
        }
    }

    private void saveState() {
        LOG.info("Saving game state...");

        final BinaryExporter exporter = BinaryExporter.getInstance();
        final File file = new File("saved-state.j3o");
        try {
            exporter.save(rootNode, file);
        } catch (IOException e) {
            LOG.error("Failed to save game state", e);
        }

        LOG.info("Game state saved");
    }

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false);
        setDisplayFps(false);

        hud = new Hud(this, guiFont, settings.getWidth(), settings.getHeight());

        terrainFactory = new TerrainFactory(this);
        explosionFactory = new ExplosionFactory(this);
        fireFactory = new FireFactory(this);
        grenadeFactory = new GrenadeFactory(this);
        boxFactory = new BoxFactory(this, fireFactory);

        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        //bulletAppState.setDebugEnabled(true);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);

        setUpKeys();

        getRootNode().attachChild(SkyFactory.createSky(
                getAssetManager(), "Textures/Sky/Bright/BrightSky.dds",
                SkyFactory.EnvMapType.CubeMap));

        initTerrain();

        final var playerCapsuleShape =
                new CapsuleCollisionShape(0.5f, 2f, 1);
        player = new CharacterControl(playerCapsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);

        bulletAppState.getPhysicsSpace().add(player);

        player.setGravity(new Vector3f(0, -50f, 0));
        player.setPhysicsLocation(new Vector3f(0, 20, -100));
    }

    private void initTerrain() {

        var terrain = terrainFactory.createTerrain();

        /** 5. The LOD (level of detail) depends on were the camera is: */
        final var control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);

        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
        rootNode.addLight(sun);

        final CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape(terrain);
        final var terrainBodyControl = new RigidBodyControl(sceneShape, 0);

        terrain.addControl(terrainBodyControl);

        rootNode.attachChild(terrain);

        bulletAppState.getPhysicsSpace().add(terrainBodyControl);
    }

    private void createGrenade() {
        final var grenade = grenadeFactory.createGrenade();

        grenade.rotate(cam.getRotation());
        //grenade.rotate(-3.1415f / 2, 0, 0);

        grenade.setLocalTranslation(cam.getLocation()
                .add(cam.getDirection().normalizeLocal().mult(4.0f)));

        rootNode.attachChild(grenade);

        final RigidBodyControl grenadeBodyControl =
                new RigidBodyControl(5.0f);

        grenade.addControl(grenadeBodyControl);
        grenade.addControl(new TimedDestructionControl(spatial -> {
            bulletAppState.getPhysicsSpace().removeAll(spatial);

            LOG.debug("BOOM!");
            rootNode.attachChild(
                    explosionFactory.createExplosion(spatial.getLocalTranslation()));

            applyExplosionForce(spatial.getLocalTranslation());
        }, 2000L));

        bulletAppState.getPhysicsSpace().add(grenadeBodyControl);

        grenadeBodyControl.setLinearVelocity(
                cam.getDirection().mult(40.0f));
    }

    private void applyExplosionForce(final Vector3f translation) {

        //TODO: use visitor https://wiki.jmonkeyengine.org/docs/3.4/core/scene/traverse_scenegraph.html

        bulletAppState.getPhysicsSpace()
                .getRigidBodyList()
                .forEach(body -> {
                    final var spatial = (Spatial) body.getUserObject();

                    if (!(spatial instanceof TerrainQuad)
                            && spatial.getUserData("object_type") != "grenade") {

                        final Vector3f diff = body
                                .getPhysicsLocation()
                                .subtractLocal(translation);

                        final float distance = diff.length();

                        if (distance < GRENADE_EXPLOSION_CUTOFF) {
                            final float normalizedDistance = (distance / GRENADE_EXPLOSION_CUTOFF);
                            final float impulseLength = GRENADE_FORCE_SIZE
                                    * (1 - normalizedDistance * normalizedDistance);

                            final var impulse = diff.normalize()
                                    .mult(impulseLength);

                            body.applyImpulse(
                                    impulse, new Vector3f(0, 0, 0));


                            final var healthControl = spatial.getControl(HealthDestructionControl.class);
                            if (healthControl != null) {
                                final var currentHealth = healthControl.getHealth();
                                healthControl.setHealth(
                                        currentHealth - (int) (40.0f * (1.0f - normalizedDistance * normalizedDistance)));
                            }
                        }
                    }
                });
    }

    private void createNpc() {
//        final var sphere = new Sphere(50, 50, 1.0f);
//        final var npcSpatial = new Geometry("npcSphere", sphere);
//
//        final var npcMaterial = new Material(getAssetManager(),
//                "Common/MatDefs/Light/Lighting.j3md");
//
//        npcMaterial.setBoolean("UseMaterialColors",true);
//        npcMaterial.setColor("Ambient", ColorRGBA.Green);
//        npcMaterial.setColor("Diffuse", ColorRGBA.Green);
//
//        npcSpatial.setMaterial(npcMaterial);
//
        final var npcModel = assetManager.loadModel("Models/Oto/Oto.mesh.xml");

        npcModel.setName("npcModel");
        npcModel.setLocalScale(0.25f);
        npcModel.setLocalTranslation(0.0f, 1.1f, 0.0f);

        final var npcSpatial = new Node("npcNode");
        npcSpatial.attachChild(npcModel);

        final var animComposer = npcModel.getControl(AnimComposer.class);
        animComposer.actionBlended("Attack", new LinearBlendSpace(0f, 0.5f), "Dodge");
        for (AnimClip animClip : animComposer.getAnimClips()) {
            LOG.debug("animClip: {}", animClip.getName());

            Action action = animComposer.action(animClip.getName());
//            if(!"stand".equals(animClip.getName())) {
//                action = new BaseAction(Tweens.sequence(action, Tweens.callMethod(this, "backToStand", animComposer)));
//            }
            animComposer.addAction(animClip.getName(), action);
        }

        final var npcControl = new BetterCharacterControl(1.0f, 2f, 50f);
        npcSpatial.addControl(npcControl);
        npcControl.setPhysicsDamping(0.0f);
        bulletAppState.getPhysicsSpace().add(npcControl);

        npcSpatial.addControl(new AiControl());

        final var spawnLocation = cam.getLocation()
                .addLocal(cam.getDirection().normalizeLocal().multLocal(5.0f));
        LOG.info("Spawn location = {}", spawnLocation);
        npcControl.warp(spawnLocation);

        final var skinningControl = npcModel.getControl(SkinningControl.class);

        rootNode.attachChild(npcSpatial);
    }

    public Tween backToStand(AnimComposer animComposer) {
        return animComposer.setCurrentAction("stand");
    }

    private void createBox() {
        final Spatial box = boxFactory.createBox(spatial -> {
            bulletAppState.getPhysicsSpace().removeAll(spatial);

            LOG.debug("BOOM!");
            rootNode.attachChild(
                    explosionFactory.createExplosion(spatial.getLocalTranslation()));

            applyExplosionForce(spatial.getLocalTranslation());
        });


        final RigidBodyControl boxControl =
                new RigidBodyControl(100.0f);

        box.addControl(boxControl);

        boxControl.setPhysicsLocation(cam.getLocation()
                .add(cam.getDirection().normalize().mult(20.0f)));

        bulletAppState.getPhysicsSpace().add(boxControl);

        rootNode.attachChild(box);
    }


    private void createFire() {
        final Spatial fire = fireFactory.createFire(cam.getLocation()
                .add(cam.getDirection().normalize().mult(20.0f)));

        rootNode.attachChild(fire);
    }

    @Override
    public void simpleUpdate(float tpf) {
        final Vector3f walkingPlane
                = new Vector3f(1.0f, 0.0f, 1.0f);

        camDir.set(cam.getDirection()).multLocal(0.3f);
        camLeft.set(cam.getLeft()).multLocal(0.2f);
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft.mult(walkingPlane));
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate().mult(walkingPlane));
        }
        if (up) {
            walkDirection.addLocal(camDir.mult(walkingPlane));
        }
        if (down) {
            walkDirection.addLocal(camDir.negate().mult(walkingPlane));
        }
        player.setWalkDirection(walkDirection);

        final var playerPosition = player.getPhysicsLocation();
        cam.setLocation(playerPosition);

//        hud.append(String.format("playerPosition = (%.2f, %.2f, %.2f)",
//                playerPosition.x, playerPosition.y, playerPosition.z));
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        if (binding.equals("Left")) {
            left = isPressed;
        } else if (binding.equals("Right")) {
            right = isPressed;
        } else if (binding.equals("Up")) {
            up = isPressed;
        } else if (binding.equals("Down")) {
            down = isPressed;
        } else if (binding.equals("Jump")) {
            if (isPressed) {
                player.jump(new Vector3f(0, 20f, 0));
            }
        } else if (binding.equals("Shoot")) {
            if (isPressed) {
                createGrenade();
                hud.consoleAppend("Grenade!");
            }
        } else if (binding.equals("Interact")) {
            if (isPressed) {
                createNpc();
                hud.consoleAppend("Creating NPC!");
            }
        } else if (binding.equals("Interact2")) {
            if (isPressed) {
                createBox();
            }
        } else if (binding.equals("Fire")) {
            if (isPressed) {
                createFire();
            }
        } else if (binding.equals("Save")) {
            if (isPressed) {
                saveState();
            }
        } else if (binding.equals("Console")) {
            if (isPressed) {
                hud.toggleVisibility();
                hud.consoleAppend("toggle");
            }
        }

    }

    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));

        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Interact", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Interact2", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("Fire", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("Save", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("Console", new KeyTrigger(KeyInput.KEY_1));

        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
        inputManager.addListener(this, "Shoot");
        inputManager.addListener(this, "Interact");
        inputManager.addListener(this, "Interact2");
        inputManager.addListener(this, "Fire");
        inputManager.addListener(this, "Save");
        inputManager.addListener(this, "Console");
    }
}