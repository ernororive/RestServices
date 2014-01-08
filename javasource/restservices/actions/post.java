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
import com.mendix.systemwideinterfaces.core.IMendixObject;

/**
 * 
 */
public class post extends UserAction<String>
{
	private String collectionUrl;
	private IMendixObject dataObject;

	public post(String collectionUrl, IMendixObject dataObject)
	{
		super();
		this.collectionUrl = collectionUrl;
		this.dataObject = dataObject;
	}

	@Override
	public String executeAction() throws Exception
	{
		// BEGIN USER CODE
		return RestConsumer.postObject(getContext(), collectionUrl, dataObject, false);
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@Override
	public String toString()
	{
		return "post";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}
