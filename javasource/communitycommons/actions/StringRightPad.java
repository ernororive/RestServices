// This file was generated by Mendix Business Modeler 4.0.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package communitycommons.actions;

import com.mendix.systemwideinterfaces.core.UserAction;

/**
 * Pads a string on the right to a certain length. 
 * value : the original value
 * amount: the desired length of the resulting string.
 * fillCharacter: the character to pad with. (or space if empty)
 * 
 * For example
 * StringRightPad("hello", 8, "-")  returns "hello---"
 * StringLeftpad("hello", 2, "-")  returns "hello"
 */
public class StringRightPad extends UserAction<String>
{
	private String value;
	private Long amount;
	private String fillCharacter;

	public StringRightPad(String value, Long amount, String fillCharacter)
	{
		super();
		this.value = value;
		this.amount = amount;
		this.fillCharacter = fillCharacter;
	}

	@Override
	public String executeAction() throws Exception
	{
		// BEGIN USER CODE
		return communitycommons.StringUtils.rightPad(value, amount, fillCharacter);
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@Override
	public String toString()
	{
		return "StringRightPad";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}
