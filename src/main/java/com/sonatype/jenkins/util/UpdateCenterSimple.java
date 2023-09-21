package com.sonatype.jenkins.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Functions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.update_center.BaseMavenRepository;
import io.jenkins.update_center.Deprecations;
import io.jenkins.update_center.MavenRepository;
import io.jenkins.update_center.Plugin;
import io.jenkins.update_center.PluginUpdateCenterEntry;
import io.jenkins.update_center.json.UpdateCenterCore;
import io.jenkins.update_center.json.UpdateCenterDeprecation;
import io.jenkins.update_center.json.UpdateCenterWarning;
import io.jenkins.update_center.json.WithSignature;
import org.apache.commons.lang3.StringUtils;

public class UpdateCenterSimple
    extends WithSignature
{
    private static final Logger LOGGER = Logger.getLogger(UpdateCenterSimple.class.getName());

    @JSONField
    @SuppressFBWarnings(value = "SS_SHOULD_BE_STATIC", justification = "Accessed by JSON serializer")
    public final String updateCenterVersion = "1";

    @JSONField
    public String connectionCheckUrl;

    @JSONField
    public String id;

    @JSONField
    public UpdateCenterCore core;

    @JSONField
    public Map<String, PluginUpdateCenterEntry> plugins = new TreeMap<>();

    @JSONField
    public List<UpdateCenterWarning> warnings = Collections.emptyList();

    @JSONField
    public Map<String, UpdateCenterDeprecation> deprecations = Collections.emptyMap();

    // for JSON deser.
    public UpdateCenterSimple() { }

    public UpdateCenterSimple(String id, String connectionCheckUrl, MavenRepository repo) throws IOException {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("'id' is required");
        }
        this.id = id;
        this.connectionCheckUrl = connectionCheckUrl;

        for (Plugin plugin : repo.listJenkinsPlugins()) {
            try {
                PluginUpdateCenterEntry entry = new PluginUpdateCenterEntry(plugin);
                plugins.put(plugin.getArtifactId(), entry);
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "Failed to add update center entry for: " + plugin, ex);
            }
        }

        core = new UpdateCenterCore();

        core.buildDate = "Sep 12, 2023";
        core.name = "core";
        core.sha1 = "CunRUI7kSbEn68QeCMkMgV9/Pus=";
        core.sha256 = "12jW99d7KRbrhoOIhyePYDtgpszog4nGBusxAyHCaJ4=";
        core.size= 89567725;
        core.url = "https://updates.jenkins.io/download/war/2.423/jenkins.war";
        core.version = "2.423";
    }
}
