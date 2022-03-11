/**
 * Abstract Class GET implementing the JSON interface
 */
package RESTController.JSON.Actions;

//Only import what you need, exactly when you need it! 
//Improved modularity, security and speed.
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import RESTController.Interfaces.JSON;


/**
 * @author Mike Dietz - 2022
 *
 */
public abstract class GET implements JSON {
	private static String payload = "";
	private static int recordcount = 0;
	
	
	/**
	 * @param uri - pass the uri enpoint to retrieve the payload
	 * @throws IOException - throw an IO Exception if there is an issue connecting.
	 */
	public static void setPayloadURI(String uri) throws IOException{
			HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
			connection.setRequestMethod("GET");
			recordcount = connection.getResponseCode();
			if(recordcount == 200){
				//String response = "";
				Scanner sc = new Scanner(connection.getInputStream());
				while(sc.hasNextLine()){
					payload += sc.nextLine();
					payload += "\n";
				}
				sc.close();
				
			}else {
				// an error happened
				payload = null;
			}
	}
	
	/**
	 * @return payload - return the payload from the GET reponse.
	 */
	public static String getPayload() {
		return payload;
	
	}
}
