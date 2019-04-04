package step.client.credentials;

/**
 * This class is a factory for controller credentials
 * It reads the controller host and credentials from the following system properties:
 * <ul>
 * 	<li>rcHostname: the hostname of the controller</li>
 * 	<li>rcPort: the port of the controller</li>
 * 	<li>rcUsername: the username to be used for login</li>
 * 	<li>rcPassword: the password to be used for login</li>
 * </ul>
 *
 */
public class SyspropCredendialsBuilder {

	public static ControllerCredentials build(){
		if(System.getProperty("rcHostname")!=null) {
			return new ControllerCredentials(
					System.getProperty("rcHostname"),
					Integer.parseInt(System.getProperty("rcPort")),
					System.getProperty("rcUsername"),
					System.getProperty("rcPassword")
					);
		} else if(System.getProperty("rcServerUrl")!=null) {
			return new ControllerCredentials(
					System.getProperty("rcServerUrl"),
					System.getProperty("rcUsername"),
					System.getProperty("rcPassword")
					);
		} else {
			return new DefaultLocalCredentials();
		}
		
	}
	
	public static void setDefaultNightlyProperties(){
		setDefaultProperties(new DefaultNightlyCredentials());
	}
	
	public static void setDefaultEENightlyProperties(){
		setDefaultProperties(new DefaultEENightlyCredentials());
	}
	
	public static void setDefaultLocalProperties(){
		setDefaultProperties(new DefaultLocalCredentials());
	}
	
	public static void setDefaultProperties(ControllerCredentials credentials){
		System.setProperty("rcServerUrl", credentials.getServerUrl());
		System.setProperty("rcUsername", credentials.getUsername());
		System.setProperty("rcPassword", credentials.getPassword());
	}
	
	public static String getDefaultUrlProperty(){
		return System.getProperty("rcServerUrl");
	}
	
	public static String getDefaultUsernameProperty(){
		return System.getProperty("rcUsername");
	}
	
	public static String getDefaultPasswordProperty(){
		return System.getProperty("rcPassword");
	}
}