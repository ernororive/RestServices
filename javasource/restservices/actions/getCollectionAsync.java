// This file was generated by Mendix Business Modeler 4.0.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package restservices.actions;

import restservices.consume.RestConsumer;
import com.mendix.systemwideinterfaces.core.UserAction;

/**
 * 
 */
public class getCollectionAsync extends UserAction<Boolean>
{
	private String collectionUrl;
	private String callbackMicroflow;

	public getCollectionAsync(String collectionUrl, String callbackMicroflow)
	{
		super();
		this.collectionUrl = collectionUrl;
		this.callbackMicroflow = callbackMicroflow;
	}

	@Override
	public Boolean executeAction() throws Exception
	{
		// BEGIN USER CODE
		RestConsumer.getCollectionAsync(collectionUrl, callbackMicroflow);
		return true;
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@Override
	public String toString()
	{
		return "getCollectionAsync";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}
