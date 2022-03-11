/**
 * Abstract Class Person implementing the Entity Framework interface
 */
package RESTController.Entities;

//Only import what you need, exactly when you need it! 
//Improved modularity, security and speed.
import RESTController.Interfaces.EntityFramework;


/**
 * @author Mike Dietz - 2022
 *
 */
public abstract class Person implements EntityFramework {
	
	//encapsulate the data
	private static String FirstName = "";
	private static String LastName = "";
	
	/**
	 * @return FirstName - return the firstname string
	 */
	public static String getFirstName() {
		return FirstName;
	}
	
	/**
	 * @param FirstName2 - set the data using a temporary variable
	 */
	public static void setFirstName(String FirstName2) {
		Person.FirstName = FirstName2;
	}
	
	/**
	 * @return LastName - set the data using a temporary variable
	 */
	public static String getLastName() {
		return LastName;
	}

	/**
	 * @param LastName2 - set the data using a temporary variable
	 */
	public static void setLastName(String LastName2) {
		Person.LastName = LastName2;
	}
	/**
	 * @param id - Pass the user id to get the firstname data
	 * @return firstname string
	 */
	public static String getFirstNameById(int id) {
		//figure out how where to post the id
		//and return the username
		return "firstname";
	} 
	/**
	 * @param id - Pass the user id to get the lastname data
	 * @return lastname string
	 */
	public static String getLastNameById(int id) {
		//figure out how where to post the id
		//and return the username
		return "lastname";
	}	
}
