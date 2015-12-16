package stefvanschuylenburg.functionalecore;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
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
import org.eclipse.m2m.qvt.oml.ExecutionDiagnostic;
import org.eclipse.m2m.qvt.oml.ModelExtent;
import org.eclipse.m2m.qvt.oml.TransformationExecutor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handles the command to generate a functional metamodel based on an ecore metamodel.
 */
public class GenerateMetaModelHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    Shell shell = HandlerUtil.getActiveShell(event);
	    
		ISelection iSelection = HandlerUtil.getActiveMenuSelection(event);
		IStructuredSelection selection = (IStructuredSelection) iSelection;
		
		// getting the file
		IFile selectedFile = (IFile) selection.getFirstElement();
		URI source = URI.createURI(selectedFile.getLocationURI().toString());
		
		// save to
		URI target = targetURI(source);
		
		// start the generation
		try {
			generateMetaModel(source, target);
		} catch (IOException e) {
			MessageDialog.openInformation(shell, "Info",
			          e.getMessage());
			MessageDialog.openError(shell, "Error Generating Metamodel", e.getMessage());
		}
		
		// return value is not really used...
		return null;
	}
	
	/**
	 * Creates the URI for where to save the generated metamodel.
	 * This URI is based on the URI of the source metamodel.
	 */
	static URI targetURI(URI source) {
		URI directory = source.trimSegments(1);
		return directory.appendSegment("functional").appendFileExtension("ecore");
	}
	
	/**
	 * Generates the Function metamodel for the given Metamodel
	 * @throws IOException 
	 */
	private void generateMetaModel(URI sourceURI, URI targetURI) throws IOException {
		URI transformation = URI.createPlatformPluginURI("org.example.functionalecore.builder/transforms/addFunctions.qvto", true);
		TransformationExecutor executor = new TransformationExecutor(transformation);
		
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource source = resourceSet.getResource(sourceURI, true);
		Resource ecore = resourceSet.getResource(URI.createURI("http://www.eclipse.org/emf/2002/Ecore"), true);
		
		// create the input extents with its initial contents
		ModelExtent input = new BasicModelExtent(source.getContents());
		ModelExtent ecoreModel = new BasicModelExtent(ecore.getContents());
		
		// create an empty extent to catch the output
		ModelExtent output = new BasicModelExtent();

		// setup the execution environment details -> 
		// configuration properties, logger, monitor object etc.
		ExecutionContextImpl context = new ExecutionContextImpl();
		context.setConfigProperty("keepModeling", true);

		// run the transformation assigned to the executor with the given 
		// input and output and execution context -> ChangeTheWorld(in, out)
		// Remark: variable arguments count is supported
		ExecutionDiagnostic result = executor.execute(context, input, ecoreModel, output);

		// on sucess save the file
		if(result.getSeverity() == Diagnostic.OK) {
			List<EObject> outObjects = output.getContents();
			// let's persist them using a resource 
		    ResourceSet resourceSet2 = new ResourceSetImpl();
			Resource outResource = resourceSet2.createResource(targetURI);
			outResource.getContents().addAll(outObjects);
			outResource.save(Collections.emptyMap());
		} else {
			// turn the result diagnostic into status and send it to error log			
			IStatus status = BasicDiagnostic.toIStatus(result);
			Activator.getDefault().getLog().log(status);
		}
	}
	

}
