package model.simulation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import com.sun.prism.paint.Color;

import gui.Controller;
import gui.element.Direction;
import gui.element.shape.ConveyorShape;
import gui.element.shape.GateDoorShape;
import gui.element.shape.SensorShape;
import gui.element.shape.SwitchShape;
import gui.element.shape.TurntableShape;
import javafx.scene.Group;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import model.UaClientUtil;
import model.elements.ActuatorDefinition;
import model.elements.BinaryActuator;
import model.elements.BinarySensor;
import model.elements.BinarySwitch;
import model.elements.Conveyor;
import model.elements.Gate;
import model.elements.GateDoor;
import model.elements.SensorDefinition;
import model.elements.SimulationElementName;
import model.elements.StorageModule;
import model.elements.Turntable;
import static java.util.EnumSet.complementOf;

import java.util.EnumSet;

/**
 * Builder class to help setting up a simulation
 */
public class SimulationBuilder {

	private final static CompletableFuture<OpcUaClient> future = new CompletableFuture<>();

	private FtPlantSimulation simulation;
	private Controller guiController;
	private Pane pane;

	public SimulationBuilder(Controller guiController) {
		this.guiController = guiController;
		this.pane = guiController.getPane();
	}

	public FtPlantSimulation build(String opcUaServerEndpointUrl, int updateInterval) throws Exception {
		OpcUaClient client = UaClientUtil.createClient(opcUaServerEndpointUrl);
		client.connect().get();
		this.simulation = new FtPlantSimulation(client, updateInterval);
		this.simulation.setController(this.guiController);
		addSimulationElements();
		return simulation;
	}

	private void addSimulationElements() {
		// Setup first switch
		SensorDefinition switchDef = SensorDefinition.B1_S06;
		BinarySensor switchSensor = new BinarySwitch(switchDef, new SwitchShape(pane, switchDef.getX(), switchDef.getY(), switchDef.getTagName()),
				simulation, guiController);
		this.simulation.addSensor(switchDef, switchSensor);

		// Setup all sensors (excluding switch)
		for (SensorDefinition sensorName : SensorDefinition.getConveyorSensors()) {
			this.simulation.addSensor(sensorName, new BinarySensor(sensorName,
					new SensorShape(pane, sensorName.getX(), sensorName.getY(), sensorName.getDirection(), sensorName.getTagName()), simulation));
		}

		// Set storage module
		this.simulation.setStorageModule(new StorageModule());

		// Setup conveyors, gate, turntable
		Conveyor conveyor1 = new Conveyor(SimulationElementName.Conveyor1, pane, 920, 235, true, ActuatorDefinition.B1_A01, ActuatorDefinition.NULL,
				simulation, 150);
		this.simulation.addUpdateable(conveyor1);

		Conveyor conveyor2 = new Conveyor(SimulationElementName.Conveyor2, pane, 760, 235, true, ActuatorDefinition.B1_A02, ActuatorDefinition.NULL,
				simulation, 150);
		this.simulation.addUpdateable(conveyor2);

		Conveyor conveyor3 = new Conveyor(SimulationElementName.Conveyor3, pane, 600, 235, true, ActuatorDefinition.B1_A07, ActuatorDefinition.NULL,
				simulation, 150);
		this.simulation.addUpdateable(conveyor3);

		Conveyor conveyor4 = new Conveyor(SimulationElementName.Conveyor4, pane, 340, 235, true, ActuatorDefinition.B1_A08, ActuatorDefinition.NULL,
				simulation, 250);
		this.simulation.addUpdateable(conveyor4);

		
		// setup Gate
		BinarySensor leftDoorOpenSensor = new BinarySensor(SensorDefinition.B1_S12, new SensorShape(pane, 440, 400, Direction.None, "B1_S12"),
				simulation);
		BinarySensor leftDoorClosedSensor = new BinarySensor(SensorDefinition.B1_S13, new SensorShape(pane, 440, 310, Direction.None, "B1_S13"),
				simulation);
		GateDoor leftDoor = new GateDoor(simulation, new GateDoorShape(pane, 440, 320, "Left Gate Door"), ActuatorDefinition.B1_A16,
				ActuatorDefinition.B1_A15, leftDoorOpenSensor, leftDoorClosedSensor, 100);
		leftDoorClosedSensor.activate();

		BinarySensor rightDoorOpenSensor = new BinarySensor(SensorDefinition.B1_S10, new SensorShape(pane, 440, 130, Direction.None, "B1_S10"),
				simulation);
		BinarySensor rightDoorClosedSensor = new BinarySensor(SensorDefinition.B1_S11, new SensorShape(pane, 440, 220, Direction.None, "B1_S11"),
				simulation);
		GateDoor rightDoor = new GateDoor(simulation, new GateDoorShape(pane, 440, 140, "Right Gate Door"), ActuatorDefinition.B1_A14,
				ActuatorDefinition.B1_A13, rightDoorOpenSensor, rightDoorClosedSensor, 100);
		rightDoorClosedSensor.activate();

		Gate gate = new Gate(SimulationElementName.Gate, leftDoor, rightDoor);
		this.simulation.addUpdateable(gate);
		

		// setup Turntable
		BinarySensor horizontalSensor = new BinarySensor(SensorDefinition.B1_S22, new SensorShape(pane, 180, 320, Direction.North, "B1_S22"),
				simulation);
		BinarySensor verticalSensor = new BinarySensor(SensorDefinition.B1_S21, new SensorShape(pane, 300, 190, Direction.East, "B1_S21"),
				simulation);

		Pane turntablePane = new Pane();
		turntablePane.setLayoutX(170);
		turntablePane.setLayoutY(190);
		Turntable turntable = new Turntable(SimulationElementName.Turntable, simulation, new TurntableShape(turntablePane, 0, 0, "B1_S20"),
				ActuatorDefinition.B1_A22, ActuatorDefinition.B1_A21, horizontalSensor, verticalSensor, 75, ActuatorDefinition.B1_A20,
				ActuatorDefinition.NULL);
		pane.getChildren().add(turntablePane);
		this.simulation.addUpdateable(turntable);
		
		BinarySensor turntableConveyorSensor = new BinarySensor(SensorDefinition.B1_S20,
				new SensorShape(turntablePane, 80, 100, Direction.North, "B1_S20"), simulation);
		this.simulation.addSensor(SensorDefinition.B1_S20, turntableConveyorSensor);
		

		Conveyor conveyor5 = new Conveyor(SimulationElementName.ConveyorLeft, pane, 10, 235, true, ActuatorDefinition.B1_A23, ActuatorDefinition.NULL,
				simulation, 150);
		this.simulation.addUpdateable(conveyor5);

		Conveyor conveyor6 = new Conveyor(SimulationElementName.ConveyorTop, pane, 240, 20, false, ActuatorDefinition.B1_A24, ActuatorDefinition.NULL,
				simulation, 150);
		this.simulation.addUpdateable(conveyor6);

		

	}
}
