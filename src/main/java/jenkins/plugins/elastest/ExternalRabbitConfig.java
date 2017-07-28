package jenkins.plugins.elastest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExternalRabbitConfig {
	
	@JsonProperty("host")	
	private String host;
	
	@JsonProperty("port")	
	private String port;
	
	@JsonProperty("username")	
	private String username;
	
	@JsonProperty("password")	
	private String password;
	
	@JsonProperty("virtualHost")	
	private String virtualHost;
	
	public ExternalRabbitConfig(){}
	
	public ExternalRabbitConfig(String host, String port, String username, String password, String virtualHost){
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.virtualHost = virtualHost;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getVirtualHost() {
		return virtualHost;
	}

	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}
	
	

}
