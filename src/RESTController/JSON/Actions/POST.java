/**
 * Abstract Class POST implementing the JSON interface
 */
package RESTController.JSON.Actions;

//Only import what you need, exactly when you need it! 
//Improved modularity, security and speed.
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import RESTController.Interfaces.JSON;

/**
 * @author Mike Dietz - 2022
 *
 */
public abstract class POST implements JSON {
	
	private static String title ="";
	private static String body ="";
	private static String output = "";
	private static String response = "";
	private static int responsecode = 0;
	private static int userId = 0;
	

	/**
	 * @param title2 - set the data using a temporary variable
	 */
	public static void setTitle(String title2) {
		title = title2;
	}

	/**
	 * @param body2 - set the data using a temporary variable
	 */
	public static void setBody(String body2) {
		body = body2;
	}
	
	/**
	 * @param userid2 - set the data using a temporary variable
	 */
	public static void setUserId(int userid2) {
		userId = userid2;
	}
	

	/**
	 * @param uri - the endpoint to extract data
	 * @throws java.io.IOException - throw an exception if not able to connect
	 * 
	 */
	public static void setPayloadURI(String uri) throws IOException{
		
		HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();

		connection.setRequestMethod("POST");
		
		String postData = "title=" + URLEncoder.encode(title, "UTF-8");
		postData += "&body=" + URLEncoder.encode(body, "UTF-8");
		postData += "&userId=" + userId;
		
		connection.setDoOutput(true);
	    OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
	    wr.write(postData);
	    wr.flush();
	   
	    responsecode = connection.getResponseCode();
		response = connection.getResponseMessage();
		
		if(responsecode == 201){
			output = "POST > "+postData+" < was successful, Entry Created." + response +"\n\n";
		}
		else {
			output = "Please check configuration." + response +"\n: ResponseCode: "+responsecode+"\n\n";
		}
	}
	
	/**
	 * @return output
	 */
	public static String outputMessage() {
		return output;
	}	
}