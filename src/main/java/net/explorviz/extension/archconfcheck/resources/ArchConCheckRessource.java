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

		// TODO Do magic code that compares the two Landscapes and returns a new and
		// better with attributes Landscape and such
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
					calculatedLandscape.getSystems().add(comparedSystem);
					compareCheck = true;
					// now the following subcategories can be anything! oO
					checkNodegroups(calculatedLandscape, child1, child2);
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

	private void checkNodegroups(final Landscape landscape, final System child1, final System child2) {

	}
}