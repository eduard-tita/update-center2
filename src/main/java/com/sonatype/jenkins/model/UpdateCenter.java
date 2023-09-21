package com.sonatype.jenkins.model;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.alibaba.fastjson.annotation.JSONField;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.update_center.MavenRepository;
import io.jenkins.update_center.Plugin;
import io.jenkins.update_center.PluginUpdateCenterEntry;
import io.jenkins.update_center.json.UpdateCenterCore;
import io.jenkins.update_center.json.UpdateCenterDeprecation;
import io.jenkins.update_center.json.UpdateCenterWarning;
import io.jenkins.update_center.json.WithSignature;
import org.apache.commons.lang3.StringUtils;

public class UpdateCenter
    extends WithSignature
{
    @JSONField
    @SuppressFBWarnings(value = "SS_SHOULD_BE_STATIC", justification = "Accessed by JSON serializer")
    public final String updateCenterVersion = "1";

    @JSONField
    public String connectionCheckUrl = "https://www.google.com/";

    @JSONField
    public String id;

    @JSONField
    public UpdateCenterCore core;

    @JSONField
    public Map<String, PluginEntry> plugins = new TreeMap<>();

    @JSONField
    public List<UpdateCenterWarning> warnings = Collections.emptyList();

    @JSONField
    public Map<String, UpdateCenterDeprecation> deprecations = Collections.emptyMap();

    public UpdateCenter() {}
}
