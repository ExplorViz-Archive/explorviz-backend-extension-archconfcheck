package net.explorviz.extension.archconfcheck.main;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.explorviz.server.helper.PropertyService;

@WebListener
public class SetupListener implements ServletContextListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupListener.class);

	@Override
	public void contextInitialized(final ServletContextEvent servletContextEvent) {

		LOGGER.info("* * * * * * * * * * * * * * * * * * *\n");
		LOGGER.info("Dummy Extension Servlet initialized.\n");
		LOGGER.info("* * * * * * * * * * * * * * * * * * *");

		// Comment out or remove the following lines to use live monitoring data
		try {
			PropertyService.setBooleanProperty("useDummyMode", true);
		} catch (final IOException e) {
			LOGGER.error("Could not set dummy mode to true", e);
		}

	}

	@Override
	public void contextDestroyed(final ServletContextEvent servletContextEvent) {
		// Nothing to dispose
	}

}
