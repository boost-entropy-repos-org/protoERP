/*
 * copyright 2013-2020
 * codebb.gr
 * ProtoERP - Open source invocing program
 * info@codebb.gr
 */
/*
 * Changelog
 * =========
 * 04/10/2020 (georgemoralis) - Initial commit
 */
package gr.codebb.protoerp;

import gr.codebb.util.git.GitInfo;
import java.io.IOException;
import java.util.Locale;

public class MainSettings {

  private static MainSettings instance = null;

  private int major;
  private int minor;
  private int revision;
  private String buildJavaVersion;
  private String buildDateTime;
  public Locale applocale = new Locale("el", "GR");
  public String appName = "protoERP";

  protected MainSettings() {
    /** Intialize global variables */
    major = 0;
    minor = 0;
    revision = 0; // autogenerated from gitinfo above
    buildJavaVersion = System.getProperty(("java.version"));

    GitInfo gitinfo = new GitInfo();
    try {
      revision = Integer.parseInt(gitinfo.getGitRepositoryState().getClosestTagCommitCount());
      buildDateTime = gitinfo.getGitRepositoryState().getBuildTime();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public static MainSettings getInstance() {
    if (instance == null) {
      instance = new MainSettings();
    }
    return instance;
  }

  public String getVersion() {
    return "" + major + "." + minor + "." + revision;
  }

  public Locale getApplocale() {
    return applocale;
  }

  public String getAppName() {
    return appName;
  }

  public String getAppNameWithVersion() { // used for application title
    return "ProtoERP v" + getVersion();
  }

  public String getBuildJavaVersion() {
    return buildJavaVersion;
  }

  public String getBuildDateTime() {
    return buildDateTime;
  }
}
