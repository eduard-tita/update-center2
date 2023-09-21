package com.sonatype.jenkins.model;

import com.alibaba.fastjson.annotation.JSONField;

public class PluginDeveloper
{
  @JSONField
  public String developerId;

  @JSONField
  public String name;

  public PluginDeveloper() {}
}
