package stefvanschuylenburg.functionalecore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2m.qvt.oml.BasicModelExtent;
import org.eclipse.m2m.qvt.oml.ExecutionContextImpl;
import org.eclipse.m2m.qvt.oml.ModelExtent;
import org.eclipse.m2m.qvt.oml.TransformationExecutor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import stefvanschuylenburg.functionalecore.transformation.Generate;

/**
 * Generates the Transformation to transform model with functions to models without functions
 */
public class GenerateTransformationHandler extends AbstractHandler {
	
	/**
	 * The resource manager.
	 * Used to retrieve and create the resources.
	 */
	private ResourceSet resourceSet = new ResourceSetImpl();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    Shell shell = HandlerUtil.getActiveShell(event);
		
		ISelection iSelection = HandlerUtil.getActiveMenuSelection(event);
		IStructuredSelection selection = (IStructuredSelection) iSelection;
		
		// getting the file
		IFile selectedFile = (IFile) selection.getFirstElement();
		URI original = URI.createURI(selectedFile.getLocationURI().toString());
		
		// get the extended metamodel
		URI extension = GenerateMetaModelHandler.targetURI(original);
		ResourceSet resources = new ResourceSetImpl();
		if (resources.getResource(extension, false) == null) { // there is no extended metamodel
			// creating the extension
			// TODO: ask user before doing this
			new GenerateMetaModelHandler().execute(event);
		}
		
		// create the EPackageContainer
		ModelExtent originalModel = loadModel(original);
		ModelExtent extensionModel = loadModel(extension);
		
		ModelExtent packageContainer = generateEPackageContainer(originalModel, extensionModel);
		
		// Generating the transformation file
		try {
			// now ePackageContainer should contain one root object holding the two models
			EObject container = packageContainer.getContents().get(0);
			File targetFolder = new File(selectedFile.getParent().getLocationURI());
			Generate generator = new Generate(container, targetFolder, new ArrayList<Object>());
			
			generator.doGenerate(null);
		} catch (IOException e) {
			MessageDialog.openInformation(shell, "Info",
			          e.getMessage());
			MessageDialog.openError(shell, "Error Generating Metamodel", e.getMessage());
		}
		
		
		
		return null;
	}
	
	/**
	 * Generates a EPackageContainer holding the packages of the given original and extension metamodels.
	 */
	private ModelExtent generateEPackageContainer(ModelExtent original, ModelExtent extension) {
		URI transformation = URI.createPlatformPluginURI("org.example.functionalecore.builder/transforms/packageContainer.qvto", true);
		TransformationExecutor executor = new TransformationExecutor(transformation);
		
		// The output Model
		ModelExtent container = new BasicModelExtent();

		// setup the execution environment
		ExecutionContextImpl context = new ExecutionContextImpl();
		context.setConfigProperty("keepModeling", true);

		// run the transformation assigned to the executor
		executor.execute(context, original, extension, container);

		return container;
	}
	
	/**
	 * Loads a model file from an URI and creates the ModelExtent.
	 */
	private ModelExtent loadModel(URI uri) {
		Resource resource = resourceSet.getResource(uri, true);
		return new BasicModelExtent(resource.getContents());
	}

}