package nu.takacs.gametest;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import nu.takacs.gametest.factory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Game extends SimpleApplication implements ActionListener {

    private static final Logger LOG = LoggerFactory.getLogger(Game.class);

    private static final float GRENADE_EXPLOSION_CUTOFF = 40.0f;
    private static final float GRENADE_FORCE_SIZE = 2500.0f;

    private BulletAppState bulletAppState;

    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;

    private TerrainQuad terrain;
    private Material terrainMaterial;
    private RigidBodyControl terrainBodyControl;

    private ExplosionFactory explosionFactory;
    private FireFactory fireFactory;
    private GrenadeFactory grenadeFactory;
    private TerrainFactory terrainFactory;
    private BoxFactory boxFactory;

    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();

    public static void main(String[] args) {
        try {
            Game app = new Game();

            //AppSettings settings = new AppSettings(true);
            //settings.setTitle("The Little Bunny Game");
            //app.setSettings(settings);

            app.start();
        } catch (final Exception e) {
            LOG.error("Unhandled exception", e);
        }
    }

    @Override
    public void simpleInitApp() {
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

        final CapsuleCollisionShape playerCapsuleShape =
                new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(playerCapsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);

        bulletAppState.getPhysicsSpace().add(player);

        player.setGravity(new Vector3f(0, -50f, 0));
        player.setPhysicsLocation(new Vector3f(-200, -10, 0));
    }

    private void initTerrain() {
        terrain = terrainFactory.createTerrain();

        /** 5. The LOD (level of detail) depends on were the camera is: */
        final var control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        final CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape(terrain);
        terrainBodyControl = new RigidBodyControl(sceneShape, 0);

        terrain.addControl(terrainBodyControl);

        rootNode.attachChild(terrain);

        bulletAppState.getPhysicsSpace().add(terrainBodyControl);
    }

    private void createGrenade() {
        final var grenade = grenadeFactory.createGrenade();
        LOG.debug("Camera rotation: {}", cam.getRotation());

        //var x = new Quaternion().fromAngleAxis(-3.1415f/2, new Vector3f(1.0f, 0, 0));
        grenade.rotate(cam.getRotation());
        grenade.rotate(-3.1415f / 2, 0, 0);

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
        bulletAppState.getPhysicsSpace()
                .getRigidBodyList()
                .forEach(body -> {
                    LOG.debug("Body translation={}, kinematic={}, userobject={}",
                            body.getPhysicsLocation(), body.isKinematic(), body.getUserObject());
                    final var spatial = (Spatial)body.getUserObject();

                    if (!(spatial instanceof TerrainQuad)
                            && spatial.getUserData("object_type") != "grenade") {

                        final Vector3f diff = body
                                .getPhysicsLocation()
                                .subtract(translation);

                        final float distance = diff.length();

                        if (distance < GRENADE_EXPLOSION_CUTOFF) {
                            final float normalizedDistance = (distance / GRENADE_EXPLOSION_CUTOFF);
                            final float impulseLength = GRENADE_FORCE_SIZE
                                    * (1 - normalizedDistance * normalizedDistance);

                            final var impulse = diff.normalize()
                                    .mult(impulseLength);

                            body.applyImpulse(
                                    impulse, new Vector3f(0, 0, 0));

                            final var currentHealth = (Integer)spatial.getUserData("health");
                            if(currentHealth != null) {
                                spatial.setUserData("health",
                                        currentHealth - (int)(40.0f*(1.0f - normalizedDistance * normalizedDistance)));
                            }
                        }
                    }
                });
    }

    private void createNpc() {
        final Spatial npcSpatial = assetManager.loadModel("Models/Oto/Oto.mesh.xml");

        final Material npcMaterial = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");

        final Texture npcTexture = assetManager.loadTexture("Models/Oto/Oto.jpg");
        //npcTexture.setWrap(Texture.WrapMode.EdgeClamp);
        npcMaterial.setTexture("ColorMap", npcTexture);

        npcSpatial.setMaterial(npcMaterial);

//        final CapsuleCollisionShape collisionShape =
//                 CollisionShapeFactory.createBoxShape()

        final RigidBodyControl bodyControl =
                new RigidBodyControl(100.0f);

        npcSpatial.addControl(bodyControl);

        bodyControl.setPhysicsLocation(cam.getLocation()
                .add(cam.getDirection().normalize().mult(20.0f)));

        bulletAppState.getPhysicsSpace().add(bodyControl);

        rootNode.attachChild(npcSpatial);
    }

    private void createBox() {
        final Spatial box = boxFactory.createBox();

        final var healthDestructionControl = new HealthDestructionControl(spatial -> {
            bulletAppState.getPhysicsSpace().removeAll(spatial);

            LOG.debug("BOOM!");
            rootNode.attachChild(
                    explosionFactory.createExplosion(spatial.getLocalTranslation()));

            applyExplosionForce(spatial.getLocalTranslation());
        });

        box.addControl(healthDestructionControl);

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
        cam.setLocation(player.getPhysicsLocation());
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
            }
        } else if (binding.equals("Interact")) {
            if (isPressed) {
                LOG.debug("Interacting!");
                createNpc();
            }
        } else if (binding.equals("Interact2")) {
            if (isPressed) {
                LOG.debug("Interacting2!");
                createBox();
            }
        } else if (binding.equals("Fire")) {
            if (isPressed) {
                LOG.debug("Fire!");
                createFire();
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

        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
        inputManager.addListener(this, "Shoot");
        inputManager.addListener(this, "Interact");
        inputManager.addListener(this, "Interact2");
        inputManager.addListener(this, "Fire");

    }
}