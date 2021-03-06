/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.streams.twitter.processor;

import org.apache.streams.components.http.HttpProcessorConfiguration;
import org.apache.streams.components.http.processor.SimpleHTTPGetProcessor;
import org.apache.streams.core.StreamsDatum;
import org.apache.streams.core.StreamsProcessor;
import org.apache.streams.pojo.json.Activity;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class gets a global share count from Twitter API for links on Activity datums.
 */
public class TwitterUrlApiProcessor extends SimpleHTTPGetProcessor implements StreamsProcessor {

  private static final String STREAMS_ID = "TwitterUrlApiProcessor";

  /**
   * TwitterUrlApiProcessor constructor.
   */
  public TwitterUrlApiProcessor() {
    super();
    this.configuration.setHostname("urls.api.twitter.com");
    this.configuration.setResourcePath("/1/urls/count.json");
    this.configuration.setEntity(HttpProcessorConfiguration.Entity.ACTIVITY);
    this.configuration.setExtension("twitter_url_count");
  }

  /**
   * TwitterUrlApiProcessor constructor.
   */
  public TwitterUrlApiProcessor(HttpProcessorConfiguration processorConfiguration) {
    super(processorConfiguration);
    this.configuration.setHostname("urls.api.twitter.com");
    this.configuration.setResourcePath("/1/urls/count.json");
    this.configuration.setEntity(HttpProcessorConfiguration.Entity.ACTIVITY);
    this.configuration.setExtension("twitter_url_count");
  }

  @Override
  public String getId() {
    return STREAMS_ID;
  }

  @Override
  public List<StreamsDatum> process(StreamsDatum entry) {
    Preconditions.checkArgument(entry.getDocument() instanceof Activity);
    Activity activity = mapper.convertValue(entry.getDocument(), Activity.class);
    if ( activity.getLinks() != null && activity.getLinks().size() > 0) {
      return super.process(entry);
    } else {
      return Stream.of(entry).collect(Collectors.toList());
    }
  }

  @Override
  protected Map<String, String> prepareParams(StreamsDatum entry) {

    Map<String, String> params = new HashMap<>();

    params.put("url", mapper.convertValue(entry.getDocument(), Activity.class).getLinks().get(0));

    return params;
  }
}
