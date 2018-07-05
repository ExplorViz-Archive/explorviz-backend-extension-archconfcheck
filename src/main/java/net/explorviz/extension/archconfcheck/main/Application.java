package net.explorviz.extension.archconfcheck.main;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import net.explorviz.api.ExtensionAPIImpl;
import net.explorviz.extension.archconfcheck.model.DummyModel;
import net.explorviz.extension.archconfcheck.model.SubDummyModel;
import net.explorviz.extension.archconfcheck.providers.DummyModelProvider;

@ApplicationPath("/extension/dummy")
public class Application extends ResourceConfig {

	public Application() {

		// register the models that you wan't to parse to JSONAPI-conform JSON,
		// i.e. exchange with frontend
		final ExtensionAPIImpl coreAPI = ExtensionAPIImpl.getInstance();

		coreAPI.registerSpecificModel("DummyModel", DummyModel.class);
		coreAPI.registerSpecificModel("SubDummyModel", SubDummyModel.class);

		// register DI
		register(new ExtensionDependencyInjectionBinder());

		// Enable CORS
		register(CORSResponseFilter.class);

		// register all providers in the given package
		register(DummyModelProvider.class);

		// register all resources in the given package
		packages("net.explorviz.extension.archconfcheck.resources");
	}
}
