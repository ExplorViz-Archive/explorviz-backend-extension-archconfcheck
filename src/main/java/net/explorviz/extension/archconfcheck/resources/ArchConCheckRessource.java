package net.explorviz.extension.archconfcheck.resources;

import java.io.File;
import java.util.Random;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import net.explorviz.api.ExtensionAPI;
import net.explorviz.api.ExtensionAPIImpl;
import net.explorviz.extension.archconfcheck.model.Status;
import net.explorviz.extension.modeleditor.resources.ModelLandscapeResource;
import net.explorviz.model.application.Application;
import net.explorviz.model.application.Clazz;
import net.explorviz.model.application.Component;
import net.explorviz.model.landscape.Landscape;
import net.explorviz.model.landscape.Node;
import net.explorviz.model.landscape.NodeGroup;
import net.explorviz.model.landscape.System;
import net.explorviz.server.helper.FileSystemHelper;

// @Secured
// Add the "Secured" annotation to enable authentication

@Path("/test")
public class ArchConCheckRessource extends ModelLandscapeResource {

	private final ExtensionAPIImpl api = ExtensionAPI.get();
	// TODO build new fatjar!
	private final String MODEL_REPOSITORY = super.getModelRepository();
	private static final String saveAs = "status";

	@GET
	// the timestamps parameter is conventionalized to be first: monitored timestamp
	// then second modeltimestamp
	@Path("/comparedModel{timestamps}")
	@Produces("application/vnd.api+json")
	public Landscape getArchConfCheckLandscape(@PathParam("timestamps") final String timestamps) {
		// this split can be done because it is convented like that!
		// //(monitoredTimestamp-monitoredActions+modelTimestamp-modelActions)
		final String monitoredString = timestamps.split("+")[0];
		final String modelString = timestamps.split("+")[1];

		Landscape monitoredLandscape = null;
		Landscape modelLandscape = null;
		Landscape confCheckedLandscape = null;

		// open the two files, and serialize them into landscapes

		// open the file of the monitored Landscape
		// TODO change from latest Landscape to an actual landscape from the repository
		monitoredLandscape = api.getLatestLandscape();

		// open file of the model

		final File modelDirectory = new File(
				FileSystemHelper.getExplorVizDirectory() + File.separator + MODEL_REPOSITORY);
		final File[] modelFileList = modelDirectory.listFiles();

		if (modelFileList != null) {
			for (final File f : modelFileList) {
				final String filename = f.getName();
				if (filename.endsWith(".expl") && filename.equals(modelString)) {
					// first validation check -> filename
					modelLandscape = api.getLandscape(Long.parseLong(modelString.split("-")[0]), MODEL_REPOSITORY);
				}
			}
		} else {
			// error modelReplayRepository is empty
		}

		// write a function that compares the entries of the landscape with each other
		// and gives them attributes accordingly

		confCheckedLandscape = calculateArchConfCheckLandscape(monitoredLandscape, modelLandscape);

		return confCheckedLandscape;
	}

	private Landscape calculateArchConfCheckLandscape(final Landscape monitoredLandscape,
			final Landscape modelLandscape) {

		// TODO Do magic code that looks over the Communications
		final Landscape calculatedLandscape = new Landscape();
		calculatedLandscape.initializeID();
		calculatedLandscape.setOverallCalls(new Random().nextInt(300000));

		for (final net.explorviz.model.landscape.System child1 : monitoredLandscape.getSystems()) {
			boolean compareCheck = false;
			for (final net.explorviz.model.landscape.System child2 : modelLandscape.getSystems()) {
				if (child1.getName().equals(child2.getName())) {
					// there is a system in each landscape with the same name
					final System comparedSystem = new System();
					comparedSystem.initializeID();
					comparedSystem.setName(child1.getName());
					comparedSystem.setParent(calculatedLandscape);
					comparedSystem.getExtensionAttributes().put(saveAs, Status.ASMODELLED);
					// now the following subcategories can be anything! oO
					checkNodegroups(comparedSystem, child1, child2);
					calculatedLandscape.getSystems().add(comparedSystem);
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// there is a system in the monitoredLandscape, that was not modeled!
				final System warnSystem = new System();
				warnSystem.initializeID();
				warnSystem.setName(child1.getName());
				warnSystem.setParent(calculatedLandscape);
				warnSystem.getExtensionAttributes().put(saveAs, Status.WARNING);
				// now every subcategory of this system is also a WARNING!
				setStatusOfNodegroups(warnSystem, child1, Status.WARNING);
				calculatedLandscape.getSystems().add(warnSystem);

			}
		}
		// now the backwards search begins!
		for (final net.explorviz.model.landscape.System child2 : modelLandscape.getSystems()) {
			boolean compareCheck = false;
			for (final net.explorviz.model.landscape.System child1 : monitoredLandscape.getSystems()) {
				if (child1.getName().equals(child2.getName())) {
					// just a reminder this was already covered in the forward search!
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// this system was modeled but didn't show up in the monitoredLandscape
				final System ghostSystem = new System();
				ghostSystem.initializeID();
				ghostSystem.setName(child2.getName());
				ghostSystem.setParent(calculatedLandscape);
				ghostSystem.getExtensionAttributes().put(saveAs, Status.GHOST);
				// now every subcategory of this system is also a GHOST
				setStatusOfNodegroups(ghostSystem, child2, Status.GHOST);
				calculatedLandscape.getSystems().add(ghostSystem);

			}
		}
		return calculatedLandscape;
	}

	private void setStatusOfNodegroups(final System comparedSystem, final System system, final Status status) {
		for (final NodeGroup nodeGroup : system.getNodeGroups()) {
			final NodeGroup comparedNG = nodeGroup;
			comparedNG.getExtensionAttributes().put(saveAs, status);
			setStatusOfNodes(comparedNG, nodeGroup, status);
			comparedSystem.getNodeGroups().add(comparedNG);
		}
	}

	private void setStatusOfNodes(final NodeGroup comparedNodeGroup, final NodeGroup nodeGroup, final Status status) {
		for (final Node node : nodeGroup.getNodes()) {
			final Node comparedNode = node;
			comparedNode.getExtensionAttributes().put(saveAs, status);
			setStatusOfApplications(comparedNode, node, status);
			comparedNodeGroup.getNodes().add(comparedNode);
		}
	}

	private void setStatusOfApplications(final Node comparedNode, final Node node, final Status status) {
		for (final Application app : node.getApplications()) {
			final Application comparedApp = app;
			comparedApp.getExtensionAttributes().put(saveAs, status);
			setStatusOfChildComponents(comparedApp, app, status);
			comparedNode.getApplications().add(comparedApp);
		}
	}

	private void setStatusOfChildComponents(final Application comparedApp, final Application app, final Status status) {
		for (final Component component : app.getComponents()) {
			final Component comparedComponent = component;
			comparedComponent.getExtensionAttributes().put(saveAs, status);
			setStatusOfComponents(comparedComponent, component, status);
			app.getComponents().add(comparedComponent);
		}
	}

	private void setStatusOfComponents(final Component comparedComponent, final Component component,
			final Status status) {
		for (final Component child : component.getChildren()) {
			final Component comparedChild = child;
			comparedChild.getExtensionAttributes().put(saveAs, status);
			setStatusOfComponents(comparedChild, child, status);
			comparedComponent.getChildren().add(comparedChild);
		}
		for (final Clazz clazz : component.getClazzes()) {
			final Clazz comparedClazz = clazz;
			comparedClazz.getExtensionAttributes().put(saveAs, status);
			comparedComponent.getClazzes().add(comparedClazz);
		}
	}

	private void checkNodegroups(final System comparedSystem, final System monitoredSystem,
			final System modeledSystem) {
		for (final NodeGroup monitoredNG : monitoredSystem.getNodeGroups()) {
			boolean compareCheck = false;
			for (final NodeGroup modeledNG : modeledSystem.getNodeGroups()) {
				if (monitoredNG.getName().equals(modeledNG.getName())) {
					// this NG was ASMODELED
					final NodeGroup comparedNG = monitoredNG;
					comparedNG.getExtensionAttributes().put(saveAs, Status.ASMODELLED);
					checkNodes(comparedNG, monitoredNG, modeledNG);
					comparedSystem.getNodeGroups().add(comparedNG);
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// now we know it is a NG that was not in the model but was in the monitored
				// Data => WARNING
				final NodeGroup comparedNG = monitoredNG;
				comparedNG.getExtensionAttributes().put(saveAs, Status.WARNING);
				setStatusOfNodes(comparedNG, monitoredNG, Status.WARNING);
				comparedSystem.getNodeGroups().add(comparedNG);
			}
		}
		// now the backwards search
		for (final NodeGroup modeledNG : modeledSystem.getNodeGroups()) {
			boolean compareCheck = false;
			for (final NodeGroup monitoredNG : monitoredSystem.getNodeGroups()) {
				if (monitoredNG.getName().equals(modeledNG.getName())) {
					// was handled in the "forward" search!
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// we now know there is a NodeGroup that is a GHOST
				final NodeGroup comparedNG = modeledNG;
				comparedNG.getExtensionAttributes().put(saveAs, Status.GHOST);
				setStatusOfNodes(comparedNG, modeledNG, Status.GHOST);
				comparedSystem.getNodeGroups().add(comparedNG);
			}
		}

	}

	private void checkNodes(final NodeGroup comparedNG, final NodeGroup monitoredNG, final NodeGroup modeledNG) {
		for (final Node monitoredNode : monitoredNG.getNodes()) {
			boolean compareCheck = false;
			for (final Node modeledNode : modeledNG.getNodes()) {
				if (monitoredNode.getName().equals(modeledNode.getName())) {
					// this NG was ASMODELED
					final Node comparedNode = monitoredNode;
					comparedNode.getExtensionAttributes().put(saveAs, Status.ASMODELLED);
					checkApplications(comparedNode, monitoredNode, modeledNode);
					comparedNG.getNodes().add(comparedNode);
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// now we know it is a NG that was not in the model but was in the monitored
				// Data => WARNING
				final Node comparedNode = monitoredNode;
				comparedNode.getExtensionAttributes().put(saveAs, Status.WARNING);
				setStatusOfApplications(comparedNode, monitoredNode, Status.WARNING);
				comparedNG.getNodes().add(comparedNode);
			}
		}
		// now the backwards search
		for (final Node modeledNode : modeledNG.getNodes()) {
			boolean compareCheck = false;
			for (final Node monitoredNode : monitoredNG.getNodes()) {
				if (monitoredNode.getName().equals(modeledNode.getName())) {
					// was handled in the "forward" search!
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// we now know there is a Node that is a GHOST
				final Node comparedNode = modeledNode;
				comparedNode.getExtensionAttributes().put(saveAs, Status.GHOST);
				setStatusOfApplications(comparedNode, modeledNode, Status.GHOST);
				comparedNG.getNodes().add(comparedNode);
			}
		}
	}

	private void checkApplications(final Node comparedNode, final Node monitoredNode, final Node modeledNode) {
		for (final Application monitoredApplication : monitoredNode.getApplications()) {
			boolean compareCheck = false;
			for (final Application modeledApplication : modeledNode.getApplications()) {
				if (monitoredApplication.getName().equals(modeledApplication.getName())) {
					// this Node was ASMODELED
					final Application comparedApplication = monitoredApplication;
					comparedApplication.getExtensionAttributes().put(saveAs, Status.ASMODELLED);
					checkChildComponents(comparedApplication, monitoredApplication, modeledApplication);
					comparedNode.getApplications().add(comparedApplication);
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// now we know it is a Node that was not in the model but was in the monitored
				// Data => WARNINode
				final Application comparedApplication = monitoredApplication;
				comparedApplication.getExtensionAttributes().put(saveAs, Status.WARNING);
				setStatusOfChildComponents(comparedApplication, monitoredApplication, Status.WARNING);
				comparedNode.getApplications().add(comparedApplication);
			}
		}
		// now the backwards search
		for (final Application modeledApplication : modeledNode.getApplications()) {
			boolean compareCheck = false;
			for (final Application monitoredApplication : monitoredNode.getApplications()) {
				if (monitoredApplication.getName().equals(modeledApplication.getName())) {
					// was handled in the "forward" search!
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// we now know there is a Application that is a GHOST
				final Application comparedApplication = modeledApplication;
				comparedApplication.getExtensionAttributes().put(saveAs, Status.GHOST);
				setStatusOfChildComponents(comparedApplication, modeledApplication, Status.GHOST);
				comparedNode.getApplications().add(comparedApplication);
			}
		}

	}

	private void checkChildComponents(final Application comparedApplication, final Application monitoredApplication,
			final Application modeledApplication) {
		for (final Component monitoredComponent : monitoredApplication.getComponents()) {
			boolean compareCheck = false;
			for (final Component modeledComponent : modeledApplication.getComponents()) {
				if (monitoredComponent.getFullQualifiedName().equals(modeledComponent.getFullQualifiedName())) {
					// this Application was ASMODELED
					final Component comparedComponent = monitoredComponent;
					comparedComponent.getExtensionAttributes().put(saveAs, Status.ASMODELLED);
					checkComponents(comparedComponent, monitoredComponent, modeledComponent);
					comparedApplication.getComponents().add(comparedComponent);
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// now we know it is a Application that was not in the model but was in the
				// monitored
				// Data => WARNIApplication
				final Component comparedComponent = monitoredComponent;
				comparedComponent.getExtensionAttributes().put(saveAs, Status.WARNING);
				setStatusOfComponents(comparedComponent, monitoredComponent, Status.WARNING);
				comparedApplication.getComponents().add(comparedComponent);
			}
		}
		// now the backwards search
		for (final Component modeledComponent : modeledApplication.getComponents()) {
			boolean compareCheck = false;
			for (final Component monitoredComponent : monitoredApplication.getComponents()) {
				if (monitoredComponent.getFullQualifiedName().equals(modeledComponent.getFullQualifiedName())) {
					// was handled in the "forward" search!
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// we now know there is a Component that is a GHOST
				final Component comparedComponent = modeledComponent;
				comparedComponent.getExtensionAttributes().put(saveAs, Status.GHOST);
				setStatusOfComponents(comparedComponent, modeledComponent, Status.GHOST);
				comparedApplication.getComponents().add(comparedComponent);
			}
		}

	}

	private void checkComponents(final Component comparedComponent, final Component monitoredComponent,
			final Component modeledComponent) {
		for (final Component monitoredChildComponent : monitoredComponent.getChildren()) {
			boolean compareCheck = false;
			for (final Component modeledChildComponent : modeledComponent.getChildren()) {
				if (monitoredChildComponent.getFullQualifiedName()
						.equals(modeledChildComponent.getFullQualifiedName())) {
					// this Component was ASMODELED
					final Component comparedChildComponent = monitoredChildComponent;
					comparedChildComponent.getExtensionAttributes().put(saveAs, Status.ASMODELLED);
					checkComponents(comparedChildComponent, monitoredChildComponent, modeledChildComponent);
					comparedComponent.getChildren().add(comparedChildComponent);
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// now we know it is a Component that was not in the model but was in the
				// monitored
				// Data => WARNIComponent
				final Component comparedChildComponent = monitoredChildComponent;
				comparedChildComponent.getExtensionAttributes().put(saveAs, Status.WARNING);
				setStatusOfComponents(comparedChildComponent, monitoredChildComponent, Status.WARNING);
				comparedComponent.getChildren().add(comparedChildComponent);
			}
		}
		// now the backwards search
		for (final Component modeledChildComponent : modeledComponent.getChildren()) {
			boolean compareCheck = false;
			for (final Component monitoredChildComponent : monitoredComponent.getChildren()) {
				if (monitoredChildComponent.getFullQualifiedName()
						.equals(modeledChildComponent.getFullQualifiedName())) {
					// was handled in the "forward" search!
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// we now know there is a Component that is a GHOST
				final Component comparedChildComponent = modeledChildComponent;
				comparedChildComponent.getExtensionAttributes().put(saveAs, Status.GHOST);
				setStatusOfComponents(comparedChildComponent, modeledChildComponent, Status.GHOST);
				comparedComponent.getChildren().add(comparedChildComponent);
			}
		}
		for (final Clazz monitoredClazz : monitoredComponent.getClazzes()) {
			boolean compareCheck = false;
			for (final Clazz modeledClazz : modeledComponent.getClazzes()) {
				if (monitoredClazz.getFullQualifiedName().equals(modeledClazz.getFullQualifiedName())) {
					// this Clazz is ASMODELED
					final Clazz comparedClazz = monitoredClazz;
					comparedClazz.getExtensionAttributes().put(saveAs, Status.ASMODELLED);
					// does not have any submodules (clazzes are ALWAYS leaves of the landscape
					// tree)
					comparedComponent.getClazzes().add(comparedClazz);
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// now we know it is a Component that was not in the model but was in the
				// monitored
				// Data => WARNIComponent
				final Clazz comparedClazz = monitoredClazz;
				comparedClazz.getExtensionAttributes().put(saveAs, Status.WARNING);
				// does not have any submodules (clazzes are ALWAYS leaves of the landscape
				// tree)
				comparedComponent.getClazzes().add(comparedClazz);
			}
		}
		// now the backwards search
		for (final Clazz modeledClazz : modeledComponent.getClazzes()) {
			boolean compareCheck = false;
			for (final Clazz monitoredClazz : monitoredComponent.getClazzes()) {
				if (monitoredClazz.getFullQualifiedName().equals(modeledClazz.getFullQualifiedName())) {
					// was handled in the "forward" search!
					compareCheck = true;
					break;
				}
			}
			if (compareCheck == false) {
				// we now know there is a Component that is a GHOST
				final Clazz comparedClazz = modeledClazz;
				comparedClazz.getExtensionAttributes().put(saveAs, Status.GHOST);
				// does not have any submodules (clazzes are ALWAYS leaves of the landscape
				// tree)
				comparedComponent.getClazzes().add(comparedClazz);
			}
		}

	}
}