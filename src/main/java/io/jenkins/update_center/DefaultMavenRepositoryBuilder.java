package io.jenkins.update_center;

import io.jenkins.update_center.util.Environment;

public class DefaultMavenRepositoryBuilder {

    private static String API_USERNAME = Environment.getString("NXRM_USERNAME");
    private static String API_PASSWORD = Environment.getString("NXRM_PASSWORD");

    private DefaultMavenRepositoryBuilder () {
        
    }

    private static BaseMavenRepository instance;
    
    public static synchronized BaseMavenRepository getInstance() {
        if (instance == null) {
            if (API_PASSWORD != null && API_USERNAME != null) {
                instance = new NxrmRepositoryImpl(API_USERNAME, API_PASSWORD);
            } else {
                throw new IllegalStateException("ARTIFACTORY_USERNAME and ARTIFACTORY_PASSWORD need to be set");
            }
        }
        return instance;
    }
}
