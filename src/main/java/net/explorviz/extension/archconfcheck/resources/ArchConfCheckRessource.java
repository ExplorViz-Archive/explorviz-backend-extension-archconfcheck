package net.explorviz.extension.archconfcheck.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import net.explorviz.api.ExtensionAPI;
import net.explorviz.api.ExtensionAPIImpl;
import net.explorviz.extension.archconfcheck.model.Status;
import net.explorviz.model.application.Application;
import net.explorviz.model.application.ApplicationCommunication;
import net.explorviz.model.application.Clazz;
import net.explorviz.model.application.Component;
import net.explorviz.model.helper.EProgrammingLanguage;
import net.explorviz.model.landscape.Landscape;
import net.explorviz.model.landscape.Node;
import net.explorviz.model.landscape.NodeGroup;
import net.explorviz.model.landscape.System;
import net.explorviz.repository.helper.DummyLandscapeHelper;
import net.explorviz.server.helper.FileSystemHelper;
import net.explorviz.server.security.Secured;

@Secured
@Path("/landscape")
public class ArchConfCheckRessource {

	private final ExtensionAPIImpl api = ExtensionAPI.get();
	private final String MODEL_REPOSITORY = "modellRepository";
	private static final String saveAs = "status";

	@GET
	// the timestamps parameter is conventionalized to be first: monitored timestamp
	// then second modeltimestamp
	@Path("/comparedModel/{timestamps}")
	@Produces("application/vnd.api+json")
	public Landscape getArchConfCheckLandscape(@PathParam("timestamps") final String timestamps) {
		// this split can be done because it is convented like that!
		// //(monitoredTimestamp-monitoredActions+modelTimestamp-modelActions)
		final String monitoredString = timestamps.split("_")[0];
		final String modelString = timestamps.split("_")[1];

		Landscape monitoredLandscape = null;
		Landscape modelLandscape = null;
		Landscape confCheckedLandscape = null;

		// open the two files, and serialize them into landscapes

		// open the file of the monitored Landscape

		// open file of the model

		final File modelDirectory = new File(
				FileSystemHelper.getExplorVizDirectory() + File.separator + MODEL_REPOSITORY);
		final File[] modelFileList = modelDirectory.listFiles();

		if (modelFileList != null) {
			for (final File f : modelFileList) {
				final String filename = f.getName();
				if (filename.endsWith(".expl") && filename.equals(modelString + ".expl")) {
					// first validation check -> filename
					modelLandscape = api.getLandscape(Long.parseLong(modelString.split("-")[0]), MODEL_REPOSITORY);
					break;
				}
			}
		} else {
			// error modelReplayRepository is empty
		}

		// TODO change from latest Landscape to an actual landscape from the repository
		monitoredLandscape = api.getLatestLandscape();

		// this way I can alter any one Landscape without interfering with the other
		// one!
		modelLandscape = copyLandscape(monitoredLandscape);
		monitoredLandscape = copyLandscape(modelLandscape);

		alterLandscape1(modelLandscape);
		alterLandscape2(monitoredLandscape);

		confCheckedLandscape = calculateArchConfCheckLandscape(monitoredLandscape, modelLandscape);
		checkCommunications(confCheckedLandscape, monitoredLandscape, modelLandscape);
		return confCheckedLandscape;
		// return modelLandscape;

	}

	private void alterLandscape2(final Landscape landscape) {
		final System system1 = new System();
		system1.setName("Webshop2.0");
		system1.setParent(landscape);
		landscape.getSystems().add(system1);

		final NodeGroup nodegroup1 = new NodeGroup();
		nodegroup1.setName("100.0.1.200 - 100.0.1.250");
		nodegroup1.setParent(system1);
		system1.getNodeGroups().add(nodegroup1);

		final Node node1 = new Node();
		node1.setIpAddress("100.0.1.201");
		node1.setName("webshop.server1");
		node1.setCpuUtilization(20.5);
		node1.setFreeRAM(500);
		node1.setUsedRAM(1500);
		node1.setParent(nodegroup1);
		nodegroup1.getNodes().add(node1);

		final Node node2 = new Node();
		node2.setIpAddress("100.0.1.215");
		node2.setName("webshop.server2");
		node2.setCpuUtilization(20.5);
		node2.setFreeRAM(500);
		node2.setUsedRAM(2500);
		node2.setParent(nodegroup1);
		nodegroup1.getNodes().add(node2);

		final Node node3 = new Node();
		node3.setIpAddress("100.0.1.226");
		node3.setName("webshop.server3");
		node3.setCpuUtilization(30.5);
		node3.setFreeRAM(500);
		node3.setUsedRAM(3500);
		node3.setParent(nodegroup1);
		nodegroup1.getNodes().add(node3);

		final Node node4 = new Node();
		node4.setIpAddress("100.0.1.255");
		node4.setName("mysterous server");
		node4.setCpuUtilization(40.5);
		node4.setFreeRAM(500);
		node4.setUsedRAM(4500);
		node4.setParent(nodegroup1);
		nodegroup1.getNodes().add(node4);

		final Application app1 = new Application();
		app1.setName("unsuspicousApp");
		app1.setLastUsage(123);
		app1.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		app1.setParent(node1);
		node1.getApplications().add(app1);

		final Application app2 = new Application();
		app2.setName("unsuspicousApp");
		app2.setLastUsage(223);
		app2.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		app2.setParent(node2);
		node2.getApplications().add(app2);

		final Application app3 = new Application();
		app3.setName("unsuspicousApp");
		app3.setLastUsage(323);
		app3.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		app3.setParent(node3);
		node3.getApplications().add(app3);

		final Application app4 = new Application();
		app4.setName("suspicousApp");
		app4.setLastUsage(423);
		app4.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		app4.setParent(node4);
		node4.getApplications().add(app4);

		final Application app42 = createApplication("totallyunsuspicousApp", EProgrammingLanguage.JAVA, node1);

		createApplicationCommunication(app42, app4, landscape, 250);

		final Component comp1 = createComponent("nothing2C", null, app42);
		app42.getComponents().add(comp1);
		final Component comp2 = createComponent("MainComponent", null, app42);
		app42.getComponents().add(comp2);
		final Component comp3 = createComponent("MeanComponent", null, app42);
		app42.getComponents().add(comp3);
		final Component comp4 = createComponent("Shop4RealMoney", null, app42);
		app42.getComponents().add(comp4);
		final Component comp5 = createComponent("MikroTransaktions", null, app42);
		app42.getComponents().add(comp5);
		final Component comp6 = createComponent("MikroTransaktions", comp5, app42);
		final Component comp7 = createComponent("SmallTransaktions", comp5, app42);
		final Component comp8 = createComponent("HugeFisch", comp5, app42);
		final Component comp9 = createComponent("TrueStory", comp5, app42);
		// final Component comp10 = createComponent("financialComponent", comp6, null);
		// final Component comp11 = createComponent("financialComponent", comp7, null);
		// final Component comp12 = createComponent("financialComponent", comp8, null);
		// final Component comp13 = createComponent("financialComponent", comp9, null);
		// createClazz("financialDecoy", comp10, 10);
		// createClazz("financialDecoy", comp11, 10);
		// createClazz("financialDecoy", comp12, 10);
		// createClazz("financialDecoy", comp13, 10);

		final ApplicationCommunication appcom1 = new ApplicationCommunication();
		appcom1.setRequests(100);
		appcom1.setSourceApplication(app1);
		appcom1.setTargetApplication(app2);
		app1.getOutgoingApplicationCommunications().add(appcom1);

		final ApplicationCommunication appcom2 = new ApplicationCommunication();
		appcom2.setRequests(100);
		appcom2.setSourceApplication(app2);
		appcom2.setTargetApplication(app3);
		app2.getOutgoingApplicationCommunications().add(appcom2);

		final ApplicationCommunication appcom3 = new ApplicationCommunication();
		appcom3.setRequests(100);
		appcom3.setSourceApplication(app3);
		appcom3.setTargetApplication(app4);
		app3.getOutgoingApplicationCommunications().add(appcom3);

		final ApplicationCommunication appcom4 = new ApplicationCommunication();
		appcom4.setRequests(100);
		appcom4.setSourceApplication(app1);
		appcom4.setTargetApplication(app4);
		app1.getOutgoingApplicationCommunications().add(appcom4);

		final ApplicationCommunication appcom5 = new ApplicationCommunication();
		appcom5.setRequests(100);
		appcom5.setSourceApplication(app1);
		appcom5.setTargetApplication(app3);
		app1.getOutgoingApplicationCommunications().add(appcom5);

		final ApplicationCommunication appcom6 = new ApplicationCommunication();
		appcom6.setRequests(100);
		appcom6.setSourceApplication(app2);
		appcom6.setTargetApplication(app4);
		app2.getOutgoingApplicationCommunications().add(appcom6);

		final ApplicationCommunication appcom7 = new ApplicationCommunication();
		appcom7.setRequests(888);
		appcom7.setSourceApplication(app4);
		appcom7.setTargetApplication(
				landscape.getSystems().get(3).getNodeGroups().get(0).getNodes().get(0).getApplications().get(0));
		app4.getOutgoingApplicationCommunications().add(appcom7);

		final System system2 = new System();
		system2.setName("HanaDB");
		system2.setParent(landscape);
		landscape.getSystems().add(system2);

		final NodeGroup nodegroup2 = new NodeGroup();
		nodegroup2.setName("10.0.5.1");
		nodegroup2.setParent(system2);
		system2.getNodeGroups().add(nodegroup2);

		final Node node5 = new Node();
		node5.setName("10.0.5.1");
		node5.setIpAddress("10.0.5.1");
		node5.setParent(nodegroup2);
		nodegroup2.getNodes().add(node5);

		final Application app5 = new Application();
		app5.setName("Interface");
		app5.setParent(node5);
		app5.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		node5.getApplications().add(app5);

		final NodeGroup nodegroup3 = new NodeGroup();
		nodegroup3.setName("10.0.5.1");
		nodegroup3.setParent(system2);
		system2.getNodeGroups().add(nodegroup3);

		final Node node6 = new Node();
		node6.setName("10.0.6.1");
		node6.setIpAddress("10.0.6.1");
		node6.setParent(nodegroup2);
		nodegroup3.getNodes().add(node6);

		final Application app6 = new Application();
		app6.setName("Database");
		app6.setParent(node5);
		app6.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		node6.getApplications().add(app6);

		final Node node7 = new Node();
		node7.setName("10.0.6.2");
		node7.setIpAddress("10.0.6.2");
		node7.setParent(nodegroup3);
		nodegroup3.getNodes().add(node7);

		final Application app7 = new Application();
		app7.setName("Database Webconnector");
		app7.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		app7.setParent(node7);
		node7.getApplications().add(app7);

		final ApplicationCommunication appcom8 = new ApplicationCommunication();
		appcom8.setRequests(555);
		appcom8.setSourceApplication(app5);
		appcom8.setTargetApplication(app6);
		app5.getOutgoingApplicationCommunications().add(appcom8);

		final ApplicationCommunication appcom9 = new ApplicationCommunication();
		appcom9.setRequests(555);
		appcom9.setSourceApplication(app5);
		appcom9.setTargetApplication(app7);
		app5.getOutgoingApplicationCommunications().add(appcom9);

		final ApplicationCommunication appcom10 = new ApplicationCommunication();
		appcom10.setRequests(555);
		appcom10.setSourceApplication(app7);
		appcom10.setTargetApplication(app4);
		app7.getOutgoingApplicationCommunications().add(appcom10);
	}

	private void alterLandscape1(final Landscape landscape) {
		final System system1 = new System();
		system1.setName("Webshop2.0");
		system1.setParent(landscape);
		landscape.getSystems().add(system1);

		final NodeGroup nodegroup1 = new NodeGroup();
		nodegroup1.setName("100.0.1.200 - 100.0.1.250");
		nodegroup1.setParent(system1);
		system1.getNodeGroups().add(nodegroup1);

		final Node node1 = new Node();
		node1.setIpAddress("100.0.1.201");
		node1.setName("webshop.server1");
		node1.setCpuUtilization(20.5);
		node1.setFreeRAM(500);
		node1.setUsedRAM(1500);
		node1.setParent(nodegroup1);
		nodegroup1.getNodes().add(node1);

		final Node node2 = new Node();
		node2.setIpAddress("100.0.1.215");
		node2.setName("webshop.server2");
		node2.setCpuUtilization(20.5);
		node2.setFreeRAM(500);
		node2.setUsedRAM(2500);
		node2.setParent(nodegroup1);
		nodegroup1.getNodes().add(node2);

		final Node node3 = new Node();
		node3.setIpAddress("100.0.1.226");
		node3.setName("webshop.server3");
		node3.setCpuUtilization(30.5);
		node3.setFreeRAM(500);
		node3.setUsedRAM(3500);
		node3.setParent(nodegroup1);
		nodegroup1.getNodes().add(node3);

		final Node node4 = createNode("unsuspicousCommunicator", nodegroup1);
		final Application app0 = createApplication("communicator2.0", EProgrammingLanguage.PERL, node4);

		final Application app1 = new Application();
		app1.setName("unsuspicousApp");
		app1.setLastUsage(123);
		app1.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		app1.setParent(node1);
		node1.getApplications().add(app1);

		final Application app2 = new Application();
		app2.setName("unsuspicousApp");
		app2.setLastUsage(223);
		app2.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		app2.setParent(node2);
		node2.getApplications().add(app2);

		final Application app3 = new Application();
		app3.setName("unsuspicousApp");
		app3.setLastUsage(323);
		app3.setProgrammingLanguage(EProgrammingLanguage.JAVA);
		app3.setParent(node3);
		node3.getApplications().add(app3);

		final Application app4 = createApplication("extension", EProgrammingLanguage.JAVA, node3);

		createApplicationCommunication(app0, app4, landscape, 200);

		final ApplicationCommunication appcom1 = new ApplicationCommunication();
		appcom1.setRequests(100);
		appcom1.setSourceApplication(app1);
		appcom1.setTargetApplication(app2);
		app1.getOutgoingApplicationCommunications().add(appcom1);

		final ApplicationCommunication appcom2 = new ApplicationCommunication();
		appcom2.setRequests(100);
		appcom2.setSourceApplication(app2);
		appcom2.setTargetApplication(app3);
		app2.getOutgoingApplicationCommunications().add(appcom2);

		final ApplicationCommunication appcom3 = new ApplicationCommunication();
		appcom3.setRequests(100);
		appcom3.setSourceApplication(app0);
		appcom3.setTargetApplication(app1);
		app0.getOutgoingApplicationCommunications().add(appcom3);

		final ApplicationCommunication appcom4 = new ApplicationCommunication();
		appcom4.setRequests(100);
		appcom4.setSourceApplication(app0);
		appcom4.setTargetApplication(app2);
		app0.getOutgoingApplicationCommunications().add(appcom4);

		final ApplicationCommunication appcom5 = new ApplicationCommunication();
		appcom5.setRequests(100);
		appcom5.setSourceApplication(app1);
		appcom5.setTargetApplication(app3);
		app1.getOutgoingApplicationCommunications().add(appcom5);

		final ApplicationCommunication appcom6 = new ApplicationCommunication();
		appcom6.setRequests(100);
		appcom6.setSourceApplication(app0);
		appcom6.setTargetApplication(app3);
		app0.getOutgoingApplicationCommunications().add(appcom6);

		final ApplicationCommunication appcom7 = new ApplicationCommunication();
		appcom7.setRequests(100);
		appcom7.setSourceApplication(app0);
		appcom7.setTargetApplication(
				landscape.getSystems().get(3).getNodeGroups().get(0).getNodes().get(0).getApplications().get(0));
		app0.getOutgoingApplicationCommunications().add(appcom7);
	}

	private Landscape copyLandscape(final Landscape landscape) {
		final Landscape copyLandscape = new Landscape();
		for (final System system : landscape.getSystems()) {
			final System copySystem = new System();
			copySystem.setName(system.getName());
			copySystem.setParent(copyLandscape);
			copyLandscape.getSystems().add(copySystem);
			for (final NodeGroup nodegroup : system.getNodeGroups()) {
				final NodeGroup copyNG = new NodeGroup();
				copyNG.setName(nodegroup.getName());
				copyNG.setParent(copySystem);
				copySystem.getNodeGroups().add(copyNG);
				for (final Node node : nodegroup.getNodes()) {
					final Node copyNode = new Node();
					copyNode.setName(node.getName());
					copyNode.setIpAddress(node.getIpAddress());
					copyNode.setCpuUtilization(node.getCpuUtilization());
					copyNode.setFreeRAM(node.getFreeRAM());
					copyNode.setUsedRAM(node.getUsedRAM());
					copyNode.setParent(copyNG);
					copyNG.getNodes().add(copyNode);
					for (final Application app : node.getApplications()) {
						final Application copyApp = new Application();
						copyApp.setLastUsage(app.getLastUsage());
						copyApp.setName(app.getName());
						// schwieriger als es hier aussieht!
						// copyApp.setOutgoingApplicationCommunications();
						copyApp.setProgrammingLanguage(app.getProgrammingLanguage());
						copyApp.setParent(copyNode);
						copyNode.getApplications().add(copyApp);
						for (final Component comp : app.getComponents()) {
							final Component newChildComp = copyComponent(comp);
							newChildComp.setBelongingApplication(copyApp);
							copyApp.getComponents().add(newChildComp);
						}
					}
				}
			}
		}

		// now we can add all the communications!
		for (final System system : landscape.getSystems()) {
			for (final NodeGroup nodegroup : system.getNodeGroups()) {
				for (final Node node : nodegroup.getNodes()) {
					for (final Application app : node.getApplications()) {
						for (final ApplicationCommunication appCom : app.getOutgoingApplicationCommunications()) {
							final ApplicationCommunication copyAppCom = new ApplicationCommunication();
							copyAppCom.setAverageResponseTime(appCom.getAverageResponseTime());
							copyAppCom.setRequests(appCom.getRequests());
							copyAppCom.setTechnology(appCom.getTechnology());
							for (final System suchSystem : copyLandscape.getSystems()) {
								for (final NodeGroup suchNodeGroup : suchSystem.getNodeGroups()) {
									for (final Node suchNode : suchNodeGroup.getNodes()) {
										for (final Application suchApp : suchNode.getApplications()) {
											if (suchApp.getName().equals(appCom.getTargetApplication().getName())
													&& suchNode.getDisplayName().equals(
															appCom.getTargetApplication().getParent().getDisplayName())
													&& suchNodeGroup.getName()
															.equals(appCom.getTargetApplication().getParent()
																	.getParent().getName())
													&& suchSystem.getName().equals(appCom.getTargetApplication()
															.getParent().getParent().getParent().getName())) {
												// durch diese leicht
												// durchschaubare If-Abfrage
												// versichern wir, dass die
												// suchApp der TargetApp in
												// allen parentalen Ebenen die
												// gleichen Namen aufweisen und
												// das heißt wir können die neue
												// suchApp jetzt als TargetApp
												// eintragen #EZ
												copyAppCom.setTargetApplication(suchApp);
											}
											if (suchApp.getName().equals(appCom.getSourceApplication().getName())
													&& suchNode.getDisplayName().equals(
															appCom.getSourceApplication().getParent().getDisplayName())
													&& suchNodeGroup.getName()
															.equals(appCom.getSourceApplication().getParent()
																	.getParent().getName())
													&& suchSystem.getName().equals(appCom.getSourceApplication()
															.getParent().getParent().getParent().getName())) {
												// hier kann man die Source app eintragen! #EZierThanEZ
												copyAppCom.setSourceApplication(suchApp);
												// #EZ #nomercy #computationalTimeWHAT?
												suchApp.getOutgoingApplicationCommunications().add(copyAppCom);
											}
										}
									}
								}
							}

							copyLandscape.getOutgoingApplicationCommunications().add(copyAppCom);
						}
					}
				}
			}
		}

		return copyLandscape;
	}

	private Component copyComponent(final Component comp) {
		final Component newComp = new Component();
		newComp.setFullQualifiedName(comp.getFullQualifiedName());
		newComp.setName(comp.getName());
		for (final Component childComp : comp.getChildren()) {
			final Component newChildComp = copyComponent(childComp);
			newComp.getChildren().add(newChildComp);
			childComp.setParentComponent(newComp);
		}
		for (final Clazz childClazz : comp.getClazzes()) {
			final Clazz newChildClazz = new Clazz();
			newChildClazz.setFullQualifiedName(childClazz.getFullQualifiedName());
			newChildClazz.setName(childClazz.getName());
			newChildClazz.setInstanceCount(childClazz.getInstanceCount());
			newChildClazz.setParent(newComp);
			newComp.getClazzes().add(newChildClazz);
		}
		return newComp;
	}

	// taken from LandscapeDummyCreator:
	static int applicationId = 0;
	static int formatFactor = 1024 * 1024 * 1024;

	private static System createSystem(final String name, final Landscape parent) {
		final System system = new System();
		system.setName(name);
		system.setParent(parent);
		parent.getSystems().add(system);
		return system;
	}

	private static NodeGroup createNodeGroup(final String name, final System parent) {
		final NodeGroup nodeGroup = new NodeGroup();
		nodeGroup.setName(name);
		nodeGroup.setParent(parent);
		parent.getNodeGroups().add(nodeGroup);
		return nodeGroup;
	}

	private static Node createNode(final String ipAddress, final NodeGroup parent) {
		final Node node = new Node();
		node.setIpAddress(ipAddress);
		node.setName(ipAddress);
		node.setParent(parent);

		// set random usage
		node.setCpuUtilization(((double) DummyLandscapeHelper.getRandomNum(10, 100)) / 100);
		node.setFreeRAM(((long) DummyLandscapeHelper.getRandomNum(1, 4)) * formatFactor);
		node.setUsedRAM(((long) DummyLandscapeHelper.getRandomNum(1, 4)) * formatFactor);

		// add it to the nodeGroup
		parent.getNodes().add(node);

		return node;
	}

	private static Application createApplication(final String name, final EProgrammingLanguage language,
			final Node parent) {
		final Application application = new Application();

		applicationId = applicationId + 1;
		application.setParent(parent);

		application.setLastUsage(java.lang.System.currentTimeMillis());

		application.setProgrammingLanguage(language);

		application.setName(name);
		parent.getApplications().add(application);

		return application;
	}

	private static ApplicationCommunication createApplicationCommunication(final Application source,
			final Application target, final Landscape landscape, final int requests) {
		final ApplicationCommunication communication = new ApplicationCommunication();
		communication.setSourceApplication(source);
		communication.setTargetApplication(target);
		communication.setRequests(requests);
		source.getOutgoingApplicationCommunications().add(communication);
		landscape.getOutgoingApplicationCommunications().add(communication);

		return communication;
	}

	private static Component createComponent(final String name, final Component parent, final Application app) {
		final Component component = new Component();
		component.initializeID();
		component.setName(name);
		component.setParentComponent(parent);
		component.setBelongingApplication(app);
		// one of the parent or app are always null!!!
		if (parent != null) {
			component.setFullQualifiedName(parent.getFullQualifiedName() + "." + name);
			parent.getChildren().add(component);
		} else {
			component.setFullQualifiedName(name);
		}
		return component;
	}

	private static Clazz createClazz(final String name, final Component component, final int instanceCount) {
		final Clazz clazz = new Clazz();
		clazz.initializeID();
		clazz.setName(name);
		clazz.setFullQualifiedName(component.getFullQualifiedName() + "." + name);
		clazz.setInstanceCount(instanceCount);
		clazz.setParent(component);
		component.getClazzes().add(clazz);

		return clazz;
	}

	private Landscape calculateArchConfCheckLandscape(final Landscape monitoredLandscape,
			final Landscape modelLandscape) {

		final Landscape calculatedLandscape = new Landscape();
		calculatedLandscape.initializeID();
		calculatedLandscape.setOverallCalls(new Random().nextInt(300000));

		// all of this can be put into a wider range of methods?!!?
		// just get some methods that work on BaseEntity and have switches!?
		if (monitoredLandscape != null) {
			for (final net.explorviz.model.landscape.System child1 : monitoredLandscape.getSystems()) {
				boolean compareCheck = false;
				if (modelLandscape != null) {
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
		} else {
			// monitoredLandscape == null
		}
		// now the backwards search begins!
		if (modelLandscape != null) {
			for (final net.explorviz.model.landscape.System child2 : modelLandscape.getSystems()) {
				boolean compareCheck = false;
				if (monitoredLandscape != null) {
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
			}
		}
		return calculatedLandscape;
	}

	private void setStatusOfNodegroups(final System comparedSystem, final System system, final Status status) {
		if (system != null) {
			for (final NodeGroup nodeGroup : system.getNodeGroups()) {
				final NodeGroup comparedNG = new NodeGroup();
				comparedNG.setName(nodeGroup.getName());
				comparedNG.setParent(comparedSystem);
				comparedNG.getExtensionAttributes().put(saveAs, status);
				setStatusOfNodes(comparedNG, nodeGroup, status);
				comparedSystem.getNodeGroups().add(comparedNG);
			}
		}
	}

	private void setStatusOfNodes(final NodeGroup comparedNodeGroup, final NodeGroup nodeGroup, final Status status) {
		if (nodeGroup != null) {
			for (final Node node : nodeGroup.getNodes()) {
				final Node comparedNode = new Node();
				comparedNode.setName(node.getName());
				comparedNode.setParent(comparedNodeGroup);
				comparedNode.getExtensionAttributes().put(saveAs, status);
				setStatusOfApplications(comparedNode, node, status);
				comparedNodeGroup.getNodes().add(comparedNode);
			}
		}
	}

	private void setStatusOfApplications(final Node comparedNode, final Node node, final Status status) {
		if (node != null) {
			for (final Application app : node.getApplications()) {
				final Application comparedApp = new Application();
				comparedApp.setName(app.getName());
				comparedApp.setParent(comparedNode);
				comparedApp.setProgrammingLanguage(app.getProgrammingLanguage());
				comparedApp.getExtensionAttributes().put(saveAs, status);
				setStatusOfChildComponents(comparedApp, app, status);
				comparedNode.getApplications().add(comparedApp);
			}
		}
	}

	private void setStatusOfChildComponents(final Application comparedApp, final Application app, final Status status) {
		if (app != null) {
			for (final Component component : app.getComponents()) {
				final Component comparedComponent = new Component();
				comparedComponent.setName(component.getName());
				comparedComponent.setFullQualifiedName(component.getFullQualifiedName());
				comparedComponent.setBelongingApplication(comparedApp);
				comparedComponent.getExtensionAttributes().put(saveAs, status);
				setStatusOfComponents(comparedComponent, component, status);
				comparedApp.getComponents().add(comparedComponent);
			}
		}
	}

	private void setStatusOfComponents(final Component comparedComponent, final Component component,
			final Status status) {
		if (component != null) {
			for (final Component child : component.getChildren()) {
				final Component comparedChild = child;
				comparedChild.setName(child.getName());
				comparedChild.setParentComponent(comparedComponent);
				comparedChild.setFullQualifiedName(child.getFullQualifiedName());
				comparedChild.getExtensionAttributes().put(saveAs, status);
				setStatusOfComponents(comparedChild, child, status);
				comparedComponent.getChildren().add(comparedChild);
			}
			for (final Clazz clazz : component.getClazzes()) {
				final Clazz comparedClazz = new Clazz();
				comparedClazz.setFullQualifiedName(clazz.getFullQualifiedName());
				comparedClazz.setName(clazz.getName());
				// not needed for models but maybe in future and just for completion mentioned
				// here:
				// comparedClazz.setInstanceCount(clazz.getInstanceCount());
				comparedClazz.setParent(comparedComponent);
				comparedClazz.getExtensionAttributes().put(saveAs, status);
				comparedComponent.getClazzes().add(comparedClazz);
			}
		}
	}

	private void checkNodegroups(final System comparedSystem, final System monitoredSystem,
			final System modeledSystem) {
		if (monitoredSystem != null) {
			for (final NodeGroup monitoredNG : monitoredSystem.getNodeGroups()) {
				boolean compareCheck = false;
				if (modeledSystem != null) {
					for (final NodeGroup modeledNG : modeledSystem.getNodeGroups()) {
						if (monitoredNG.getName().equals(modeledNG.getName())) {
							// this NG was ASMODELED
							final NodeGroup comparedNG = new NodeGroup();
							java.lang.System.out.println(
									"modeledNG: " + modeledNG.getName() + " monitoredNG: " + monitoredNG.getName());
							comparedNG.setName(monitoredNG.getName());
							comparedNG.setParent(comparedSystem);
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
						final NodeGroup comparedNG = new NodeGroup();
						comparedNG.setName(monitoredNG.getName());
						comparedNG.setParent(comparedSystem);
						comparedNG.getExtensionAttributes().put(saveAs, Status.WARNING);
						setStatusOfNodes(comparedNG, monitoredNG, Status.WARNING);
						comparedSystem.getNodeGroups().add(comparedNG);
					}
				}
			}
		}
		// now the backwards search
		if (modeledSystem != null) {
			for (final NodeGroup modeledNG : modeledSystem.getNodeGroups()) {
				boolean compareCheck = false;
				if (monitoredSystem != null) {
					for (final NodeGroup monitoredNG : monitoredSystem.getNodeGroups()) {
						if (monitoredNG.getName().equals(modeledNG.getName())) {
							// was handled in the "forward" search!
							compareCheck = true;
							break;
						}
					}
					if (compareCheck == false) {
						// we now know there is a NodeGroup that is a GHOST
						final NodeGroup comparedNG = new NodeGroup();
						comparedNG.setName(modeledNG.getName());
						comparedNG.setParent(comparedSystem);
						comparedNG.getExtensionAttributes().put(saveAs, Status.GHOST);
						setStatusOfNodes(comparedNG, modeledNG, Status.GHOST);
						comparedSystem.getNodeGroups().add(comparedNG);
					}
				}
			}
		}

	}

	private void checkNodes(final NodeGroup comparedNG, final NodeGroup monitoredNG, final NodeGroup modeledNG) {
		if (monitoredNG != null) {
			for (final Node monitoredNode : monitoredNG.getNodes()) {
				boolean compareCheck = false;
				if (modeledNG != null) {
					for (final Node modeledNode : modeledNG.getNodes()) {
						if (monitoredNode.getDisplayName().equals(modeledNode.getDisplayName())) {
							final Node comparedNode = new Node();
							comparedNode.setName(monitoredNode.getName());
							comparedNode.setParent(comparedNG);
							comparedNode.setIpAddress(monitoredNode.getIpAddress());
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
						final Node comparedNode = new Node();
						comparedNode.setName(monitoredNode.getName());
						comparedNode.setParent(comparedNG);
						comparedNode.setIpAddress(monitoredNode.getIpAddress());
						java.lang.System.out.println("kommt er hier rein fragezeichen!");
						comparedNode.getExtensionAttributes().put(saveAs, Status.WARNING);
						setStatusOfApplications(comparedNode, monitoredNode, Status.WARNING);
						comparedNG.getNodes().add(comparedNode);
					}
				}
			}
		}
		// now the backwards search
		if (modeledNG != null) {
			for (final Node modeledNode : modeledNG.getNodes()) {
				boolean compareCheck = false;
				if (monitoredNG != null) {
					for (final Node monitoredNode : monitoredNG.getNodes()) {
						if (monitoredNode.getDisplayName().equals(modeledNode.getDisplayName())) {
							// was handled in the "forward" search!
							compareCheck = true;
							break;
						}
					}
					if (compareCheck == false) {
						// we now know there is a Node that is a GHOST
						final Node comparedNode = new Node();
						comparedNode.setName(modeledNode.getName());
						comparedNode.setParent(comparedNG);
						comparedNode.setIpAddress(modeledNode.getIpAddress());
						comparedNode.getExtensionAttributes().put(saveAs, Status.GHOST);
						setStatusOfApplications(comparedNode, modeledNode, Status.GHOST);
						comparedNG.getNodes().add(comparedNode);
					}
				}
			}
		}
	}

	private void checkApplications(final Node comparedNode, final Node monitoredNode, final Node modeledNode) {
		if (monitoredNode != null) {
			for (final Application monitoredApplication : monitoredNode.getApplications()) {
				boolean compareCheck = false;
				if (modeledNode != null) {
					for (final Application modeledApplication : modeledNode.getApplications()) {
						if (monitoredApplication.getName().equals(modeledApplication.getName())) {
							// this Node was ASMODELED
							final Application comparedApplication = new Application();
							comparedApplication.setName(monitoredApplication.getName());
							comparedApplication.setParent(comparedNode);
							comparedApplication.setProgrammingLanguage(monitoredApplication.getProgrammingLanguage());
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
						final Application comparedApplication = new Application();
						comparedApplication.setName(monitoredApplication.getName());
						comparedApplication.setParent(comparedNode);
						comparedApplication.setProgrammingLanguage(monitoredApplication.getProgrammingLanguage());
						comparedApplication.getExtensionAttributes().put(saveAs, Status.WARNING);
						setStatusOfChildComponents(comparedApplication, monitoredApplication, Status.WARNING);
						comparedNode.getApplications().add(comparedApplication);
					}
				}
			}
		}
		// now the backwards search
		if (modeledNode != null) {
			for (final Application modeledApplication : modeledNode.getApplications()) {
				boolean compareCheck = false;
				if (monitoredNode != null) {
					for (final Application monitoredApplication : monitoredNode.getApplications()) {
						if (monitoredApplication.getName().equals(modeledApplication.getName())) {
							// was handled in the "forward" search!
							compareCheck = true;
							break;
						}
					}
					if (compareCheck == false) {
						// we now know there is a Application that is a GHOST
						final Application comparedApplication = new Application();
						comparedApplication.setName(modeledApplication.getName());
						comparedApplication.setParent(comparedNode);
						comparedApplication.setProgrammingLanguage(modeledApplication.getProgrammingLanguage());
						comparedApplication.getExtensionAttributes().put(saveAs, Status.GHOST);
						setStatusOfChildComponents(comparedApplication, modeledApplication, Status.GHOST);
						comparedNode.getApplications().add(comparedApplication);
					}
				}
			}
		}
	}

	private void checkChildComponents(final Application comparedApplication, final Application monitoredApplication,
			final Application modeledApplication) {
		if (monitoredApplication != null) {
			for (final Component monitoredComponent : monitoredApplication.getComponents()) {
				boolean compareCheck = false;
				if (modeledApplication != null) {
					for (final Component modeledComponent : modeledApplication.getComponents()) {
						if (monitoredComponent.getFullQualifiedName().equals(modeledComponent.getFullQualifiedName())) {
							// this Application was ASMODELED
							final Component comparedComponent = new Component();
							comparedComponent.setName(monitoredComponent.getName());
							comparedComponent.setBelongingApplication(comparedApplication);
							comparedComponent.setFullQualifiedName(monitoredComponent.getFullQualifiedName());
							comparedComponent.getExtensionAttributes().put(saveAs, Status.ASMODELLED);
							checkComponents(comparedComponent, monitoredComponent, modeledComponent);
							comparedApplication.getComponents().add(comparedComponent);
							compareCheck = true;
							break;
						}
					}
					if (compareCheck == false) {
						// now we know it is a Application that was not in the model but was in the
						// monitored data
						// Data => WARNIApplication
						final Component comparedComponent = new Component();
						comparedComponent.setName(monitoredComponent.getName());
						comparedComponent.setBelongingApplication(comparedApplication);
						comparedComponent.setFullQualifiedName(monitoredComponent.getFullQualifiedName());
						comparedComponent.getExtensionAttributes().put(saveAs, Status.WARNING);
						setStatusOfComponents(comparedComponent, monitoredComponent, Status.WARNING);
						comparedApplication.getComponents().add(comparedComponent);
					}
				}
			}
		}
		// now the backwards search
		if (modeledApplication != null) {
			for (final Component modeledComponent : modeledApplication.getComponents()) {
				boolean compareCheck = false;
				if (monitoredApplication != null) {
					for (final Component monitoredComponent : monitoredApplication.getComponents()) {
						if (monitoredComponent.getFullQualifiedName().equals(modeledComponent.getFullQualifiedName())) {
							// was handled in the "forward" search!
							compareCheck = true;
							break;
						}
					}
					if (compareCheck == false) {
						// we now know there is a Component that is a GHOST
						final Component comparedComponent = new Component();
						comparedComponent.setName(modeledComponent.getName());
						comparedComponent.setBelongingApplication(comparedApplication);
						comparedComponent.setFullQualifiedName(modeledComponent.getFullQualifiedName());
						comparedComponent.getExtensionAttributes().put(saveAs, Status.GHOST);
						setStatusOfComponents(comparedComponent, modeledComponent, Status.GHOST);
						comparedApplication.getComponents().add(comparedComponent);
					}
				}
			}
		}
	}

	private void checkComponents(final Component comparedComponent, final Component monitoredComponent,
			final Component modeledComponent) {
		if (monitoredComponent != null) {
			for (final Component monitoredChildComponent : monitoredComponent.getChildren()) {
				boolean compareCheck = false;
				if (modeledComponent != null) {
					for (final Component modeledChildComponent : modeledComponent.getChildren()) {
						if (monitoredChildComponent.getFullQualifiedName()
								.equals(modeledChildComponent.getFullQualifiedName())) {
							// this Component was ASMODELED
							final Component comparedChildComponent = new Component();
							comparedChildComponent.setName(monitoredChildComponent.getName());
							comparedChildComponent.setFullQualifiedName(monitoredChildComponent.getFullQualifiedName());
							comparedChildComponent.setParentComponent(comparedComponent);
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
						final Component comparedChildComponent = new Component();
						comparedChildComponent.setName(monitoredChildComponent.getName());
						comparedChildComponent.setFullQualifiedName(monitoredChildComponent.getFullQualifiedName());
						comparedChildComponent.setParentComponent(comparedComponent);
						comparedChildComponent.getExtensionAttributes().put(saveAs, Status.WARNING);
						setStatusOfComponents(comparedChildComponent, monitoredChildComponent, Status.WARNING);
						comparedComponent.getChildren().add(comparedChildComponent);
					}
				}
			}
		}
		// now the backwards search
		if (modeledComponent != null) {
			for (final Component modeledChildComponent : modeledComponent.getChildren()) {
				boolean compareCheck = false;
				if (monitoredComponent != null) {
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
						final Component comparedChildComponent = new Component();
						comparedChildComponent.setName(modeledChildComponent.getName());
						comparedChildComponent.setFullQualifiedName(modeledChildComponent.getFullQualifiedName());
						comparedChildComponent.setParentComponent(comparedComponent);
						comparedChildComponent.getExtensionAttributes().put(saveAs, Status.GHOST);
						setStatusOfComponents(comparedChildComponent, modeledChildComponent, Status.GHOST);
						comparedComponent.getChildren().add(comparedChildComponent);
					}
				}
			}
		}

		// now for the clazzes
		if (monitoredComponent != null) {
			for (final Clazz monitoredClazz : monitoredComponent.getClazzes()) {
				boolean compareCheck = false;
				if (modeledComponent != null) {
					for (final Clazz modeledClazz : modeledComponent.getClazzes()) {
						if (monitoredClazz.getFullQualifiedName().equals(modeledClazz.getFullQualifiedName())) {
							// this Clazz is ASMODELED
							final Clazz comparedClazz = new Clazz();
							comparedClazz.setName(monitoredClazz.getName());
							comparedClazz.setFullQualifiedName(monitoredClazz.getFullQualifiedName());
							comparedClazz.setParent(comparedComponent);
							// just here for completness reasons
							// comparedClazz.setInstanceCount(monitoredClazz.getInstanceCount());
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
						final Clazz comparedClazz = new Clazz();
						comparedClazz.setName(monitoredClazz.getName());
						comparedClazz.setFullQualifiedName(monitoredClazz.getFullQualifiedName());
						comparedClazz.setParent(comparedComponent);
						// just here for completness reasons
						// comparedClazz.setInstanceCount(monitoredClazz.getInstanceCount());
						comparedClazz.getExtensionAttributes().put(saveAs, Status.WARNING);
						// does not have any submodules (clazzes are ALWAYS leaves of the landscape
						// tree)
						comparedComponent.getClazzes().add(comparedClazz);
					}
				}
			}
		}
		// now the backwards search
		if (modeledComponent != null) {
			for (final Clazz modeledClazz : modeledComponent.getClazzes()) {
				boolean compareCheck = false;
				if (monitoredComponent != null) {
					for (final Clazz monitoredClazz : monitoredComponent.getClazzes()) {
						if (monitoredClazz.getFullQualifiedName().equals(modeledClazz.getFullQualifiedName())) {
							// was handled in the "forward" search!
							compareCheck = true;
							break;
						}
					}
					if (compareCheck == false) {
						// we now know there is a Component that is a GHOST
						final Clazz comparedClazz = new Clazz();
						comparedClazz.setName(modeledClazz.getName());
						comparedClazz.setFullQualifiedName(modeledClazz.getFullQualifiedName());
						comparedClazz.setParent(comparedComponent);
						// just here for completness reasons
						// comparedClazz.setInstanceCount(modeledClazz.getInstanceCount());
						comparedClazz.getExtensionAttributes().put(saveAs, Status.GHOST);
						// does not have any submodules (clazzes are ALWAYS leaves of the landscape
						// tree)
						comparedComponent.getClazzes().add(comparedClazz);
					}
				}
			}
		}
	}

	private void checkCommunications(final Landscape comparedLandscape, final Landscape monitoredLandscape,
			final Landscape modelledLandscape) {
		int counter = 0;
		int reversecounter = 0;
		// do magic code that somehow compares ALL the communications:
		// go through all the communications in monitored and compare them to modelled,
		// but wait, we need to check if they are ASMODELLED
		// there is a couple of different methods used to go through the communications
		// there are cummulated and aggregated where the first one is all communications
		// between A and B and B and A; and the second one is All Communications between
		// A and B but not the communications between B and A
		if (comparedLandscape == null)
			return;
		// first we add all Communications from monitoredLandscape to comparedLandscape
		final boolean doubleCommunications = false;
		if (comparedLandscape != null && monitoredLandscape != null) {
			for (final System comparedSystem : comparedLandscape.getSystems()) {
				for (final System monitoredSystem : monitoredLandscape.getSystems()) {
					if (monitoredSystem.getName().equals(comparedSystem.getName())) {
						java.lang.System.out.println("systems heißen gleich:" + comparedSystem.getName());
						for (final NodeGroup comparedNodeGroup : comparedSystem.getNodeGroups()) {
							for (final NodeGroup monitoredNodeGroup : monitoredSystem.getNodeGroups()) {
								if (comparedNodeGroup.getName().equals(monitoredNodeGroup.getName())) {
									java.lang.System.out
											.println("nodegroups heißen gleich:" + comparedNodeGroup.getName());
									for (final Node comparedNode : comparedNodeGroup.getNodes()) {
										for (final Node monitoredNode : monitoredNodeGroup.getNodes()) {
											if (comparedNode.getDisplayName().equals(monitoredNode.getDisplayName())) {
												java.lang.System.out.println(
														"nodes heißen gleich:" + comparedNode.getDisplayName());
												for (final Application comparedApp : comparedNode.getApplications()) {
													for (final Application monitoredApp : monitoredNode
															.getApplications()) {
														if (comparedApp.getName().equals(monitoredApp.getName())) {
															java.lang.System.out.println("Applications heißen gleich:"
																	+ comparedApp.getName());
															final List<ApplicationCommunication> listOfCom = new ArrayList();
															for (final ApplicationCommunication monitoredAppCommunication : monitoredApp
																	.getOutgoingApplicationCommunications()) {
																// schleife zum verhindern doppelter Einträge (wer würde
																// denn sowas machen ?)
																boolean doppelteCom = false;
																for (final ApplicationCommunication doubleAppCom : listOfCom) {
																	if (doubleAppCom == monitoredAppCommunication)
																		doppelteCom = true;
																}
																if (!doppelteCom) {
																	listOfCom.add(monitoredAppCommunication);

																	java.lang.System.out
																			.println("# of coms:" + monitoredApp
																					.getOutgoingApplicationCommunications()
																					.size());
																	final ApplicationCommunication comparedAppCommunication = new ApplicationCommunication();
																	counter += 1;

																	comparedAppCommunication.initializeID();
																	comparedAppCommunication.setAverageResponseTime(
																			monitoredAppCommunication
																					.getAverageResponseTime());
																	comparedAppCommunication.setRequests(
																			monitoredAppCommunication.getRequests());
																	// comparedAppCommunication.setSourceApplication(
																	// monitoredAppCommunication
																	// .getSourceApplication());
																	// statt die sourceApplication setzen zu können
																	// müssen wir sie jetzt finden und dann neu setzen
																	// ist doch toll!
																	// okay bei source geht das noch ganz easy aber
																	// target könnte da ein ticken schwieriger werden ;)

																	comparedAppCommunication
																			.setSourceApplication(comparedApp);

																	// java.lang.System.out
																	// .println("neue Communication von:"
																	// + monitoredAppCommunication
																	// .getSourceApplication()
																	// + " nach "
																	// + monitoredAppCommunication
																	// .getTargetApplication());

																	// nun zum spaßigen Teil mit den tollen Suchen nach
																	// dem Target xD juchuuu

																	for (final System suchSystem : comparedLandscape
																			.getSystems()) {
																		for (final NodeGroup suchNodeGroup : suchSystem
																				.getNodeGroups()) {
																			for (final Node suchNode : suchNodeGroup
																					.getNodes()) {
																				for (final Application suchApp : suchNode
																						.getApplications()) {
																					if (suchApp.getName().equals(
																							monitoredAppCommunication
																									.getTargetApplication()
																									.getName())
																							&& suchNode.getDisplayName()
																									.equals(monitoredAppCommunication
																											.getTargetApplication()
																											.getParent()
																											.getDisplayName())
																							&& suchNodeGroup.getName()
																									.equals(monitoredAppCommunication
																											.getTargetApplication()
																											.getParent()
																											.getParent()
																											.getName())
																							&& suchSystem.getName()
																									.equals(monitoredAppCommunication
																											.getTargetApplication()
																											.getParent()
																											.getParent()
																											.getParent()
																											.getName())) {
																						// durch diese leicht
																						// durchschaubare If-Abfrage
																						// versichern wir, dass die
																						// suchApp der TargetApp in
																						// allen parentalen Ebenen die
																						// gleichen Namen aufweisen und
																						// das heißt wir können die neue
																						// suchApp jetzt als TargetApp
																						// eintragen #EZ

																						comparedAppCommunication
																								.setTargetApplication(
																										suchApp);
																						break;
																					}
																				}
																			}
																		}
																	}

																	// das war der alte und langweilige Weg, welcher
																	// leider auch nicht funktionierte xD

																	// comparedAppCommunication.setTargetApplication(
																	// monitoredAppCommunication
																	// .getTargetApplication());
																	comparedAppCommunication.setTechnology(
																			monitoredAppCommunication.getTechnology());
																	// java.lang.System.out
																	// .println("neue comparedCommunication von:"
																	// + comparedAppCommunication
																	// .getSourceApplication()
																	// + " nach "
																	// + comparedAppCommunication
																	// .getTargetApplication());
																	comparedAppCommunication.getExtensionAttributes()
																			.put(saveAs, Status.WARNING);
																	comparedApp.getOutgoingApplicationCommunications()
																			.add(comparedAppCommunication);
																	comparedLandscape
																			.getOutgoingApplicationCommunications()
																			.add(comparedAppCommunication);
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		// // now we do the reverse, now all added are GHOSTs if they are not yet added,
		// // otherwise they are ASMODELLED
		boolean asModelled = false;
		if (comparedLandscape != null && modelledLandscape != null) {
			for (final System comparedSystem : comparedLandscape.getSystems()) {
				for (final System modelledSystem : modelledLandscape.getSystems()) {
					if (modelledSystem.getName().equals(comparedSystem.getName())) {
						for (final NodeGroup comparedNodeGroup : comparedSystem.getNodeGroups()) {
							for (final NodeGroup modelledNodeGroup : modelledSystem.getNodeGroups()) {
								if (comparedNodeGroup.getName().equals(modelledNodeGroup.getName())) {
									for (final Node comparedNode : comparedNodeGroup.getNodes()) {
										for (final Node modelledNode : modelledNodeGroup.getNodes()) {
											if (comparedNode.getDisplayName().equals(modelledNode.getDisplayName())) {
												for (final Application comparedApp : comparedNode.getApplications()) {
													for (final Application modelledApp : modelledNode
															.getApplications()) {
														if (comparedApp.getName().equals(modelledApp.getName())) {
															for (final ApplicationCommunication modelledAppCommunication : modelledApp
																	.getOutgoingApplicationCommunications()) {
																// here we need to differentiate, if it already exists
																// in the comparedApp, we need to change the Status and
																// if it doesn't exist yet, we are safe to assume that
																// it is in fact a GHOST
																asModelled = false;

																final List<ApplicationCommunication> listOfCom = new ArrayList();

																boolean doppelteCom = false;
																for (final ApplicationCommunication doubleAppCom : listOfCom) {
																	if (doubleAppCom == modelledAppCommunication) {
																		doppelteCom = true;
																	}
																}
																if (!doppelteCom) {
																	listOfCom.add(modelledAppCommunication);

																	for (final ApplicationCommunication comparedAppCommunication : comparedApp
																			.getOutgoingApplicationCommunications()) {
																		if (modelledAppCommunication
																				.getTargetApplication().getName()
																				.equals(comparedAppCommunication
																						.getTargetApplication()
																						.getName())
																				&& modelledAppCommunication
																						.getTargetApplication()
																						.getParent().getDisplayName()
																						.equals(comparedAppCommunication
																								.getTargetApplication()
																								.getParent()
																								.getDisplayName())
																				&& modelledAppCommunication
																						.getTargetApplication()
																						.getParent().getParent()
																						.getName()
																						.equals(comparedAppCommunication
																								.getTargetApplication()
																								.getParent().getParent()
																								.getName())
																				&& modelledAppCommunication
																						.getTargetApplication()
																						.getParent().getParent()
																						.getParent().getName()
																						.equals(comparedAppCommunication
																								.getTargetApplication()
																								.getParent().getParent()
																								.getParent()
																								.getName())) {
																			// java.lang.System.out
																			// .println(comparedAppCommunication
																			// .getExtensionAttributes()
																			// .get(saveAs));
																			comparedAppCommunication
																					.getExtensionAttributes()
																					.remove(saveAs);
																			// java.lang.System.out
																			// .println(comparedAppCommunication
																			// .getExtensionAttributes()
																			// .get(saveAs));
																			comparedAppCommunication
																					.getExtensionAttributes()
																					.put(saveAs, Status.ASMODELLED);
																			// java.lang.System.out
																			// .println(comparedAppCommunication
																			// .getExtensionAttributes()
																			// .get(saveAs));
																			// I sure hope it works in both locations,
																			// but
																			// it should work as well in the
																			// landscapes
																			// OutgoingApplicationCommunications
																			asModelled = true;
																			reversecounter += 1;
																			break;
																		}
																	}
																}
																if (!asModelled) {
																	final ApplicationCommunication comparedAppCommunication = new ApplicationCommunication();
																	reversecounter += 1;
																	comparedAppCommunication.setAverageResponseTime(
																			modelledAppCommunication
																					.getAverageResponseTime());
																	comparedAppCommunication.setRequests(
																			modelledAppCommunication.getRequests());

																	comparedAppCommunication
																			.setSourceApplication(comparedApp);

																	// here we look for a targetsystem
																	for (final System suchSystem : comparedLandscape
																			.getSystems()) {
																		for (final NodeGroup suchNodeGroup : suchSystem
																				.getNodeGroups()) {
																			for (final Node suchNode : suchNodeGroup
																					.getNodes()) {
																				for (final Application suchApp : suchNode
																						.getApplications()) {
																					if (suchApp.getName().equals(
																							modelledAppCommunication
																									.getTargetApplication()
																									.getName())
																							&& suchNode.getDisplayName()
																									.equals(modelledAppCommunication
																											.getTargetApplication()
																											.getParent()
																											.getDisplayName())
																							&& suchNodeGroup.getName()
																									.equals(modelledAppCommunication
																											.getTargetApplication()
																											.getParent()
																											.getParent()
																											.getName())
																							&& suchSystem.getName()
																									.equals(modelledAppCommunication
																											.getTargetApplication()
																											.getParent()
																											.getParent()
																											.getParent()
																											.getName())) {
																						// durch diese leicht
																						// durchschaubare If-Abfrage
																						// versichern wir, dass die
																						// suchApp der TargetApp in
																						// allen parentalen Ebenen die
																						// gleichen Namen aufweisen und
																						// das heißt wir können die neue
																						// suchApp jetzt als TargetApp
																						// eintragen #EZ
																						comparedAppCommunication
																								.setTargetApplication(
																										suchApp);
																						break;
																					}
																				}
																			}
																		}
																	}

																	comparedAppCommunication.setTechnology(
																			modelledAppCommunication.getTechnology());
																	comparedAppCommunication.getExtensionAttributes()
																			.put(saveAs, Status.GHOST);
																	comparedApp.getOutgoingApplicationCommunications()
																			.add(comparedAppCommunication);
																	comparedLandscape
																			.getOutgoingApplicationCommunications()
																			.add(comparedAppCommunication);
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		java.lang.System.out.println("counter: " + counter + " reverse: " + reversecounter);
	}
}