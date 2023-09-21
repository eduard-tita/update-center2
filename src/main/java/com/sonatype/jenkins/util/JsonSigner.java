package com.sonatype.jenkins.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

import io.jenkins.update_center.LatestPluginVersions;
import io.jenkins.update_center.Main;
import io.jenkins.update_center.NxrmRepositoryImpl;
import io.jenkins.update_center.Signer;
import io.jenkins.update_center.util.Environment;
import org.kohsuke.args4j.ClassParser;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class JsonSigner
{
  private static final Logger LOGGER = Logger.getLogger(JsonSigner.class.getName());

  private static final String UPDATE_CENTER_JSON_FILENAME = "update-center.json";
  private static final String UPDATE_CENTER_ACTUAL_JSON_FILENAME = "update-center.actual.json";
  private static final String EOL = System.getProperty("line.separator");

  @Option(name = "--www-dir", usage = "Generate simple output files, JSON(ish) and others, into this directory")
  @CheckForNull
  public File www;

  public static void main(String[] args) throws Exception {
    System.exit(new JsonSigner().run(args));
  }

  public int run(String[] args) throws Exception {
    String API_USERNAME = "etita";
    String API_PASSWORD = "nzYNQSf6._puMSf";
    NxrmRepositoryImpl repository = new NxrmRepositoryImpl(API_USERNAME, API_PASSWORD);

    LatestPluginVersions.initialize(repository);

    // this object is serialized into JSON
    UpdateCenterSimple updateCenterRoot =
        new UpdateCenterSimple("sonatype-update-center", "https://www.google.com/", repository);

    Signer signer = new Signer();
    CmdLineParser p = new CmdLineParser(this);
    new ClassParser().parse(signer, p);
    p.parseArgument(args);

    if (!signer.isConfigured()) {
      LOGGER.log(Level.SEVERE, "signer must be configured");
      return 1;
    }

    final String signedUpdateCenterJson = updateCenterRoot.encodeWithSignature(signer, true);
    LOGGER.log(Level.INFO, "output: " + signedUpdateCenterJson);

    writeToFile(updateCenterPostCallJson(signedUpdateCenterJson), new File(www, UPDATE_CENTER_JSON_FILENAME));
    writeToFile(signedUpdateCenterJson, new File(www, UPDATE_CENTER_ACTUAL_JSON_FILENAME));

    return 0;
  }

  private String updateCenterPostCallJson(String updateCenterJson) {
    return "updateCenter.post(" + EOL + updateCenterJson + EOL + ");";
  }

  private static void writeToFile(String string, final File file) throws IOException {
    File parentFile = file.getParentFile();
    if (parentFile != null && !parentFile.isDirectory() && !parentFile.mkdirs()) {
      throw new IOException("Failed to create parent directory " + parentFile);
    }
    PrintWriter rhpw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
    rhpw.print(string);
    rhpw.close();
  }
}
