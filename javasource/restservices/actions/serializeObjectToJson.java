// This file was generated by Mendix Business Modeler 4.0.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package restservices.actions;

import restservices.util.JsonSerializer;
import com.mendix.systemwideinterfaces.core.UserAction;
import com.mendix.systemwideinterfaces.core.IMendixObject;

/**
 * 
 */
public class serializeObjectToJson extends UserAction<String>
{
	private IMendixObject sourceObject;

	public serializeObjectToJson(IMendixObject sourceObject)
	{
		super();
		this.sourceObject = sourceObject;
	}

	@Override
	public String executeAction() throws Exception
	{
		// BEGIN USER CODE
		return JsonSerializer.writeMendixObjectToJson(getContext(), sourceObject).toString(4);
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@Override
	public String toString()
	{
		return "serializeObjectToJson";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}
