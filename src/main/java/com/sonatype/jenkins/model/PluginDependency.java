package com.sonatype.jenkins.model;

import com.alibaba.fastjson.annotation.JSONField;

public class PluginDependency
{
  @JSONField
  public String name;

  @JSONField
  public boolean optional = false;

  @JSONField
  public String version;

  public PluginDependency() {}
}
