/**
 * A RESTController package. The main execution interface.
 * 
 */
package RESTController;

//Only import what you need, exactly when you need it! 
//Improved modularity, security and speed.
import java.io.IOException;
import RESTController.Entities.Person;
import RESTController.JSON.Actions.GET;
import RESTController.JSON.Actions.POST;

/**
 * @author Mike Dietz - 2022. 
 * THIS IS A HANDCODED EXAMPLE. The objective is to demonstrate differences between abstract classes and abstract interfaces. 
 * The entity example demonstrates the principle of tightly coupled contract.
 * The REST interface example demonstrates the loosely-coupled flexibility of multiple interfaces.   
 */
public class RESTController {

	/**
	 * RESTController Constructor(){}
	 */
	public RESTController() {}
	
	/**
	 * Abstraction of the method interfaces, Encapsulation of the data
	 * 
	 */
	public abstract class getURL extends GET{}
	
	/**
	 * postURL extends POST interface
	 *
	 */
	public abstract class postURL extends POST{}
	
	/**
	 * A sample of tightly-coupled OOP contract
	 *
	 */
	public class PersonEntity extends Person{
		
        // Inheritance, Overriding, and Hiding
		// Example of a contract between class and interface
		@Override
		public Object DataConnection() {
			// TODO Example Create Data Connection here
			return null;
		}

		@Override
		public Object DataInterface() {
			// TODO Example Interface to data here
			//String PostData = "post data";
			return null;
			
		}

		@Override
		public Object DataId() {
			// TODO Connect to the super interface here
			return null;
		}
		
	}
	
	/**
	 * @param args - Input String array
	 * @throws IOException - Throw an IO Exception if program has trouble with execution
	 */
	public static void main(String[] args) throws IOException {
		
		//Test PersonEntity Setters
		PersonEntity.setFirstName("Mike");
		PersonEntity.setLastName("Dietz");
		
		//Test POST Setters
		postURL.setTitle("Object Oriented Programming in Java and C++!");
		postURL.setBody("The article content goes here!");
		postURL.setUserId(1);
		
		//Test POST interface
		postURL.setPayloadURI("https://jsonplaceholder.typicode.com/posts");
		System.out.println(postURL.outputMessage());
		
		//Test the Get interface
		getURL.setPayloadURI("https://jsonplaceholder.typicode.com/users/3");
		System.out.print(getURL.getPayload()+"\n");
		
		//Display stored Entity information.
		System.out.println(Person.getFirstName()+" - "+Person.getLastName()+"\n");
		System.out.println(PersonEntity.getFirstName()+" "+PersonEntity.getLastName());

	}
}
