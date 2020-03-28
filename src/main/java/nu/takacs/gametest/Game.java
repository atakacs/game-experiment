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
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;

public class Game extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;

    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;

    private TerrainQuad terrain;
    private Material terrainMaterial;
    private RigidBodyControl terrainBodyControl;


    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();

    public static void main(String[] args) {

        Game app = new Game();

        //AppSettings settings = new AppSettings(true);
        //settings.setTitle("The Little Bunny Game");
        //app.setSettings(settings);

        app.start();
    }

    private final void initTerrain() {
        /** 1. Create terrain material and load four textures into it. */
        terrainMaterial = new Material(assetManager,
                "Common/MatDefs/Terrain/Terrain.j3md");

        /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
        terrainMaterial.setTexture("Alpha", assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap.png"));

        /** 1.2) Add GRASS texture into the red layer (Tex1). */
        Texture grass = assetManager.loadTexture(
                "Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("Tex1", grass);
        terrainMaterial.setFloat("Tex1Scale", 64f);

        /** 1.3) Add DIRT texture into the green layer (Tex2) */
        Texture dirt = assetManager.loadTexture(
                "Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("Tex2", dirt);
        terrainMaterial.setFloat("Tex2Scale", 32f);

        /** 1.4) Add ROAD texture into the blue layer (Tex3) */
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/road.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("Tex3", rock);
        terrainMaterial.setFloat("Tex3Scale", 128f);

        /** 2. Create the height map */
        AbstractHeightMap heightmap = null;
        Texture heightMapImage = assetManager.loadTexture(
                "Textures/Terrain/splat/mountains512.png");
        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();

        /** 3. We have prepared material and heightmap.
         * Now we create the actual terrain:
         * 3.1) Create a TerrainQuad and name it "my terrain".
         * 3.2) A good value for terrain tiles is 64x64 -- so we supply 64+1=65.
         * 3.3) We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
         * 3.4) As LOD step scale we supply Vector3f(1,1,1).
         * 3.5) We supply the prepared heightmap itself.
         */
        int patchSize = 65;
        terrain = new TerrainQuad("my terrain", patchSize, 513, heightmap.getHeightMap());

        /** 4. We give the terrain its material, position & scale it, and attach it. */
        terrain.setMaterial(terrainMaterial);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 1f, 2f);

        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);
    }

    private Geometry createGrenade() {
        final Sphere sphere = new Sphere(20, 20, 0.5f);
        final Geometry grenade = new Geometry("Grenade", sphere);

        final Material grenadeMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        grenadeMaterial.setColor("Color", ColorRGBA.Red);
        grenade.setMaterial(grenadeMaterial);

        grenade.setLocalTranslation(cam.getLocation()
                .add(cam.getDirection().normalizeLocal().mult(2.0f)));

        rootNode.attachChild(grenade);

        final RigidBodyControl bulletBodyControl =
                new RigidBodyControl(10.0f);

        grenade.addControl(bulletBodyControl);
        grenade.addControl(new DestructionControl(spatial -> {
            System.out.println("BOOM!");
            createExplosion(spatial.getLocalTranslation());
        }, 2000L));

        bulletAppState.getPhysicsSpace().add(bulletBodyControl);

        bulletBodyControl.setRestitution(0f);
        bulletBodyControl.setLinearVelocity(
                cam.getDirection().mult(50.0f));

        return grenade;
    }

    private void createExplosion(final Vector3f location) {
        final Sphere sphere = new Sphere(20, 20, 0.5f);
        final Geometry grenade = new Geometry("Grenade", sphere);

        final Material grenadeMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        grenadeMaterial.setColor("Color", ColorRGBA.Blue);
        grenade.setMaterial(grenadeMaterial);

        grenade.setLocalTranslation(location);

        rootNode.attachChild(grenade);

    }

    @Override
    public void simpleInitApp() {
        /** Set up Physics */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        //bulletAppState.setDebugEnabled(true);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);
        setUpKeys();

        initTerrain();

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape(terrain);
        terrainBodyControl = new RigidBodyControl(sceneShape, 0);

        terrain.addControl(terrainBodyControl);

        CapsuleCollisionShape playerCapsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(playerCapsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);

        rootNode.attachChild(terrain);

        bulletAppState.getPhysicsSpace().add(terrainBodyControl);
        bulletAppState.getPhysicsSpace().add(player);

        terrainBodyControl.setRestitution(0);

        player.setGravity(new Vector3f(0, -50f, 0));
        player.setPhysicsLocation(new Vector3f(0, 40, 0));
    }

    @Override
    public void simpleUpdate(float tpf) {
        final Vector3f walkingPlane
                = new Vector3f(1.0f, 0.0f, 1.0f);

        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
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
                System.out.println("SHOOT!");
                createGrenade();
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

        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
        inputManager.addListener(this, "Shoot");
    }
}