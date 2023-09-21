package com.sonatype.jenkins.model;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

import com.alibaba.fastjson.annotation.JSONField;
import hudson.util.VersionNumber;
import io.jenkins.update_center.HPI;
import io.jenkins.update_center.IssueTrackerSource;
import io.jenkins.update_center.LatestPluginVersions;
import io.jenkins.update_center.MaintainersSource;
import io.jenkins.update_center.Plugin;
import io.jenkins.update_center.Popularities;

/**
 * An entry of a plugin in the update center metadata.
 *
 */
public class PluginEntry
{
    @JSONField
    public String name;

    @JSONField
    public String buildDate;

    @JSONField
    public String defaultBranch = "main";

    @JSONField
    public List<PluginDependency> dependencies;

    @JSONField
    public List<PluginDeveloper> developers;

    @JSONField
    public String excerpt;

    @JSONField
    public String gav;

    @JSONField
    public List<String> labels;

    @JSONField
    public long popularity;

    @JSONField
    public Date previousTimestamp;

    @JSONField
    public String previousVersion;

    @JSONField
    public Date releaseTimestamp;

    @JSONField
    public String requiredCore;

    @JSONField
    public String scm;

    @JSONField
    public String sha1;

    @JSONField
    public String sha256;

    @JSONField
    public long size;

    @JSONField
    public String title;

    @JSONField
    public String url;

    @JSONField
    public String version;

    @JSONField
    public String wiki;

    public PluginEntry() {}
}
