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

package org.apache.streams.gplus.test.processors;

import org.apache.streams.core.StreamsDatum;
import org.apache.streams.exceptions.ActivitySerializerException;
import org.apache.streams.jackson.StreamsJacksonMapper;
import org.apache.streams.pojo.json.Activity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.api.services.plus.model.Person;
import com.google.gplus.processor.GooglePlusTypeConverter;
import com.google.gplus.serializer.util.GPlusActivityDeserializer;
import com.google.gplus.serializer.util.GPlusPersonDeserializer;
import com.google.gplus.serializer.util.GooglePlusActivityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests conversion of gplus inputs to Activity
 */
public class GooglePlusTypeConverterIT {

  private final static Logger LOGGER = LoggerFactory.getLogger(GooglePlusTypeConverterIT.class);
  private GooglePlusTypeConverter googlePlusTypeConverter;
  private ObjectMapper objectMapper;

  @BeforeClass
  public void setup() {
    objectMapper = StreamsJacksonMapper.getInstance();
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addDeserializer(Person.class, new GPlusPersonDeserializer());
    simpleModule.addDeserializer(com.google.api.services.plus.model.Activity.class, new GPlusActivityDeserializer());
    objectMapper.registerModule(simpleModule);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    googlePlusTypeConverter = new GooglePlusTypeConverter();
    googlePlusTypeConverter.prepare(null);
  }

  @Test(dependsOnGroups={"testGPlusUserDataProvider"})
  public void testProcessPerson() throws IOException, ActivitySerializerException {

    File file = new File("target/test-classes/GPlusUserDataProviderIT.stdout.txt");
    InputStream is = new FileInputStream(file);
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);

    while (br.ready()) {
      String line = br.readLine();
      if (!StringUtils.isEmpty(line)) {
        LOGGER.info("raw: {}", line);
        Activity activity = new Activity();

        Person person = objectMapper.readValue(line, Person.class);
        StreamsDatum streamsDatum = new StreamsDatum(person);

        assertNotNull(streamsDatum.getDocument());

        List<StreamsDatum> retList = googlePlusTypeConverter.process(streamsDatum);
        GooglePlusActivityUtil.updateActivity(person, activity);

        assertEquals(retList.size(), 1);
        assert(retList.get(0).getDocument() instanceof Activity);
        assertEquals(activity, retList.get(0).getDocument());
      }
    }
  }

  @Test(dependsOnGroups={"testGPlusUserActivityProvider"})
  public void testProcessActivity() throws IOException, ActivitySerializerException{

    File file = new File("target/test-classes/GPlusUserActivityProviderIT.stdout.txt");
    InputStream is = new FileInputStream(file);
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);

    while (br.ready()) {
      String line = br.readLine();
      if (!StringUtils.isEmpty(line)) {
        LOGGER.info("raw: {}", line);
        Activity activity = new Activity();

        com.google.api.services.plus.model.Activity gPlusActivity = objectMapper.readValue(line, com.google.api.services.plus.model.Activity.class);
        StreamsDatum streamsDatum = new StreamsDatum(gPlusActivity);

        assertNotNull(streamsDatum.getDocument());

        List<StreamsDatum> retList = googlePlusTypeConverter.process(streamsDatum);
        GooglePlusActivityUtil.updateActivity(gPlusActivity, activity);

        assertEquals(retList.size(), 1);
        assertTrue(retList.get(0).getDocument() instanceof Activity);
        assertEquals(activity, retList.get(0).getDocument());
      }
    }
  }

}