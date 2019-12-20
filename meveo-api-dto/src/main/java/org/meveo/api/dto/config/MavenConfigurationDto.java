package org.meveo.api.dto.config;

import java.io.Serializable;
import java.util.List;

/**
 * @author Edward P. Legaspi | <czetsuya@gmail.com>
 * @lastModifiedVersion 6.5.0
 */
public class MavenConfigurationDto implements Serializable {

	private static final long serialVersionUID = -7629326572073747356L;

	private String mavenExecutablePath;
	private List<String> mavenRepositories;

	public String getMavenExecutablePath() {
		return mavenExecutablePath;
	}

	public void setMavenExecutablePath(String mavenExecutablePath) {
		this.mavenExecutablePath = mavenExecutablePath;
	}

	public List<String> getMavenRepositories() {
		return mavenRepositories;
	}

	public void setMavenRepositories(List<String> mavenRepositories) {
		this.mavenRepositories = mavenRepositories;
	}
}
