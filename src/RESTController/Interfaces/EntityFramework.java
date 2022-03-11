/**
 * An Abstract Entity Framework Interface extended from Data Interface.
 */
package RESTController.Interfaces;

/**
 * @author Mike Dietz 2022  
 * Create an Entity framework extended from data interface
 * that demonstrates tightly coupled contract.
 *
 */
public abstract interface EntityFramework extends Data {
	
	/**
	 * @return - return Object Connection
	 */
	abstract Object DataConnection();
	
	/**
	 * @return - return Object Data Interface
	 */
	abstract Object DataInterface();
}
