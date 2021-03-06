// This file was generated by Mendix Business Modeler 4.0.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package unittesting.actions;

import unittesting.TestManager;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.UserAction;

/**
 * 
 */
public class RunAllUnitTests extends UserAction<Boolean>
{
	private IMendixObject __testRun;
	private unittesting.proxies.TestSuite testRun;

	public RunAllUnitTests(IMendixObject testRun)
	{
		super();
		this.__testRun = testRun;
	}

	@Override
	public Boolean executeAction() throws Exception
	{
		this.testRun = __testRun == null ? null : unittesting.proxies.TestSuite.initialize(getContext(), __testRun);

		// BEGIN USER CODE
		return TestManager.instance().runAllTests(testRun, getContext(), null); //this action is not picked up by the client so do not sent it along
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@Override
	public String toString()
	{
		return "RunAllUnitTests";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}
