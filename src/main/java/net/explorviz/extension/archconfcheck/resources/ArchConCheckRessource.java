package net.explorviz.extension.archconfcheck.resources;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import net.explorviz.api.ExtensionAPI;
import net.explorviz.api.ExtensionAPIImpl;
import net.explorviz.extension.modeleditor.resources.ModelLandscapeResource;
import net.explorviz.model.landscape.Landscape;
import net.explorviz.server.helper.FileSystemHelper;

// @Secured
// Add the "Secured" annotation to enable authentication

@Path("/test")
public class ArchConCheckRessource extends ModelLandscapeResource {

	private final ExtensionAPIImpl api = ExtensionAPI.get();
	// TODO build new fatjar!
	// private static final String MODEL_REPOSITORY = super.getModelRepository()
	private static final String MODEL_REPOSITORY = "modellRepository";

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
		final Landscape calculatedLandscape = null;

		return calculatedLandscape;

	}

}