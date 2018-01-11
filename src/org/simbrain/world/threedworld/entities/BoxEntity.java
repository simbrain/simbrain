package org.simbrain.world.threedworld.entities;

import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.util.TangentBinormalGenerator;

public class BoxEntity extends PhysicalEntity {
    private Box box;
	private Geometry geometry;
	
	public BoxEntity(ThreeDEngine engine, String name) {
		this(engine, name, Vector3f.UNIT_XYZ, 1, "Materials/BlueTile.j3m");
	}
	
	public BoxEntity(ThreeDEngine engine, String name, Vector3f size, float mass, String material) {
		super(engine, new Node(name));
		box = new Box(size.x, size.y, size.z);
        TangentBinormalGenerator.generate(box);
        geometry = new Geometry(name + "Geometry", box);
        geometry.setMaterial(engine.getAssetManager().loadMaterial(material));
        getNode().attachChild(geometry);
        CollisionShape shape = CollisionShapeFactory.createBoxShape(geometry);
        RigidBodyControl body = new RigidBodyControl(shape, mass);
        setBody(body);
	}
	
	public Vector3f getSize() {
		return new Vector3f(box.xExtent, box.yExtent, box.zExtent);
	}
	
	public void setSize(Vector3f value) {
		box = new Box(value.x, value.y, value.z);
		TangentBinormalGenerator.generate(box);
		geometry.setMesh(box);
		CollisionShape shape = CollisionShapeFactory.createBoxShape(geometry);
		getBody().setCollisionShape(shape);
	}
	
	public float getMass() {
		return getBody().getMass();
	}
	
	public void setMass(float value) {
		getBody().setMass(value);
	}
	
	public String getMaterial() {
		return geometry.getMaterial().getAssetName();
	}
	
	public void setMaterial(String value) {
		geometry.setMaterial(getEngine().getAssetManager().loadMaterial(value));
	}
	
	@Override
	public BoundingVolume getBounds() {
	    return geometry.getWorldBound();
	}
	
	@Override
	public Editor getEditor() {
	    return new BoxEditor(this);
	}
}
