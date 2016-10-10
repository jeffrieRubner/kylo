package com.thinkbiganalytics.nifi.rest;

import com.thinkbiganalytics.feedmgr.nifi.CreateFeedBuilder;
import com.thinkbiganalytics.feedmgr.nifi.PropertyExpressionResolver;
import com.thinkbiganalytics.feedmgr.rest.model.FeedCategory;
import com.thinkbiganalytics.feedmgr.rest.model.FeedMetadata;
import com.thinkbiganalytics.nifi.feedmgr.InputOutputPort;
import com.thinkbiganalytics.nifi.rest.client.LegacyNifiRestClient;
import com.thinkbiganalytics.nifi.rest.client.NifiRestClientConfig;
import com.thinkbiganalytics.nifi.rest.model.NifiProcessorSchedule;
import com.thinkbiganalytics.nifi.rest.model.NifiProperty;
import com.thinkbiganalytics.nifi.rest.support.NifiPropertyUtil;
import com.thinkbiganalytics.rest.JerseyClientException;

import org.apache.commons.io.IOUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.apache.nifi.web.api.dto.TemplateDTO;
import org.apache.nifi.web.api.entity.ConnectionEntity;
import org.apache.nifi.web.api.entity.ListingRequestEntity;
import org.apache.nifi.web.api.entity.ProvenanceEventEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

/**
 * Created by sr186054 on 4/28/16.
 */
public class NifiRestTest {


    private LegacyNifiRestClient restClient;

    //@Before
    public void setupRestClient() {
        NifiRestClientConfig config = new NifiRestClientConfig();
        config.setHost("localhost");
        config.setPort(8079);
        restClient = new LegacyNifiRestClient(config);
    }




    //@Test
    public void testReplay() {

        ProvenanceEventEntity e = restClient.replayProvenanceEvent(3382L);
    }

    // @Test
    public void testEvent() {
        try {
            ProvenanceEventEntity provenanceEventEntity = restClient.getProvenanceEvent("123456");
        } catch (Exception e) {

            if (e instanceof NotFoundException) {
                int i = 0;
            } else if (e instanceof ProcessingException) {
                if (e.getCause() instanceof NoHttpResponseException) {
                    //connection error

                } else if (e.getCause() instanceof HttpHostConnectException) {
                    //connection error
                }
            }
        }

    }


    //@Test
    public void testCreateFeed1() throws JerseyClientException {
        TemplateDTO templateDTO = restClient.getTemplateByName("New Data Ingest");
        String inputType = "org.apache.nifi.processors.standard.GetFile";

        NifiProcessorSchedule schedule = new NifiProcessorSchedule();
        schedule.setSchedulingStrategy("TIMER_DRIVEN");
        schedule.setSchedulingPeriod("10 sec");
        String inputPortName = "From Data Ingest Feed";

        String feedOutputPortName = "To Data Ingest";

        FeedMetadata feedMetadata = new FeedMetadata();
        feedMetadata.setCategory(new FeedCategory());
        feedMetadata.getCategory().setSystemName("online");
        feedMetadata.setSystemFeedName("Scotts Feed");

        CreateFeedBuilder.newFeed(restClient, feedMetadata, templateDTO.getId(), new PropertyExpressionResolver()).inputProcessorType(inputType)
            .feedSchedule(schedule).addInputOutputPort(new InputOutputPort(inputPortName, feedOutputPortName)).build();
    }



    //@Test
    public void testLoad() {
        //setup constants for the test
        String templateName = "Data Ingest";
        int num = 10;
        String processGroupName = "LoadTest";
        String feedPrefix = "LT_";
        String inputType = "org.apache.nifi.processors.standard.GetFile";
        List<NifiProperty> templateProperties = new ArrayList<>();

        String schedulePeriod = "10 sec";

        String GET_FILE_PROCESSOR_NAME = "Poll filesystem";
        String UPDATE_PARAMETERS_PROCESSOR_NAME = "Update flow parameters";

        String INPUT_DIRECTORY_PROPERTY = "Input Directory";
        String SOURCE_PROPERTY = "source";
        String ENTITY_PROPERTY = "entity";

        try {
            TemplateDTO template = restClient.getTemplateByName(templateName);

            List<NifiProperty> propertyList = restClient.getPropertiesForTemplate(template.getId());
            NifiProperty inputDirectory = NifiPropertyUtil
                .getProperty(GET_FILE_PROCESSOR_NAME, INPUT_DIRECTORY_PROPERTY, propertyList);
            NifiProperty entity = NifiPropertyUtil.getProperty(UPDATE_PARAMETERS_PROCESSOR_NAME, SOURCE_PROPERTY, propertyList);
            NifiProperty source = NifiPropertyUtil.getProperty(UPDATE_PARAMETERS_PROCESSOR_NAME, ENTITY_PROPERTY, propertyList);
            templateProperties.add(inputDirectory);
            templateProperties.add(entity);
            templateProperties.add(source);

            NifiProcessorSchedule schedule = new NifiProcessorSchedule();
            schedule.setSchedulingStrategy("TIMER_DRIVEN");
            schedule.setSchedulingPeriod(schedulePeriod);
            for (int i = 0; i < num; i++) {
                String feedName = feedPrefix + i;

                List<NifiProperty> instanceProperties = NifiPropertyUtil.copyProperties(templateProperties);
                //update the properties
                NifiPropertyUtil.getProperty(GET_FILE_PROCESSOR_NAME, INPUT_DIRECTORY_PROPERTY, instanceProperties).setValue("/tmp/" + feedName);
                NifiPropertyUtil.getProperty(UPDATE_PARAMETERS_PROCESSOR_NAME, SOURCE_PROPERTY, instanceProperties).setValue(processGroupName);
                NifiPropertyUtil.getProperty(UPDATE_PARAMETERS_PROCESSOR_NAME, ENTITY_PROPERTY, instanceProperties).setValue(feedName);

                FeedMetadata feedMetadata = new FeedMetadata();
                feedMetadata.setCategory(new FeedCategory());
                feedMetadata.getCategory().setSystemName(processGroupName);
                feedMetadata.setSystemFeedName("feedPrefix + i");

                 CreateFeedBuilder.newFeed(restClient,feedMetadata, template.getId(),new PropertyExpressionResolver()).inputProcessorType(inputType)
                    .feedSchedule(schedule).properties(instanceProperties).build();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void testCreateFeed() throws Exception {
        TemplateDTO templateDTO = restClient.getTemplateByName("New Data Ingest");
        String inputType = "org.apache.nifi.processors.standard.GetFile";

        NifiProcessorSchedule schedule = new NifiProcessorSchedule();
        schedule.setSchedulingStrategy("TIMER_DRIVEN");
        schedule.setSchedulingPeriod("10 sec");
        String inputPortName = "From Data Ingest Feed";

        String feedOutputPortName = "To Data Ingest";

        FeedMetadata feedMetadata = new FeedMetadata();
        feedMetadata.setCategory(new FeedCategory());
        feedMetadata.getCategory().setSystemName("online");
        feedMetadata.setSystemFeedName("Scotts Feed");

        CreateFeedBuilder.newFeed(restClient,feedMetadata,templateDTO.getId(), new PropertyExpressionResolver()).inputProcessorType(inputType)
            .feedSchedule(schedule).addInputOutputPort(new InputOutputPort(inputPortName, feedOutputPortName)).build();
    }




    // @Test
    public void testUpdateProcessor() throws JerseyClientException {
        NifiProperty p = new NifiProperty();
        p.setProcessGroupId("0b013850-d6bb-44e4-87c2-1784858e60ab");
        p.setProcessorId("795509d5-1433-4e64-b7bd-d05c6adfb95a");
        p.setKey("Source Database Connection");
        p.setValue("4688ee71-262c-46bc-af35-9e9825507160");
        restClient.updateProcessorProperty(p.getProcessGroupId(), p.getProcessorId(), p);
        int i = 0;
    }

    // @Test
    public void testFile() throws IOException, JerseyClientException {
        InputStream in = NifiRestTest.class
            .getResourceAsStream("/template.xml");
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer, "UTF-8");
        String theString = writer.toString();

        restClient.importTemplate("test", theString);
    }

    //@Test
    public void testFailures() throws JerseyClientException {
        String id = "7653afbc-7cdd-49c6-9ffc-c5a0c31137f4";
        Set<ProcessorDTO> failureProcessors = restClient.getFailureProcessors(id);
        int i = 0;
    }

    // @Test
    public void testConnection() throws JerseyClientException {
        ConnectionEntity entity = restClient.getConnection("72f23ad8-2b7f-47e6-a6db-f8e92b7d1f59", "9ed6ea0f-8401-4e56-826e-f5b7556976b9");

        ListingRequestEntity queue = restClient.getConnectionQueue("72f23ad8-2b7f-47e6-a6db-f8e92b7d1f59", "9ed6ea0f-8401-4e56-826e-f5b7556976b9");

        int i = 0;

    }

    // @Test
    public void testStopInputs() throws JerseyClientException {
        String id = "b1d5e073-f3cd-4339-86ea-7a28434bca43";
        restClient.disableAllInputProcessors(id);
        //attempt to stop all processors
        try {
            restClient.stopInputs(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String connectionId = "61e16428-9368-4f40-a0bb-7acaae92b286";
        String groupId = "01fd0596-0bb7-47b3-a5b7-eaccbe10d645";
        ConnectionEntity connectionEntity = restClient.getConnection(groupId, connectionId);
        restClient.deleteConnection(connectionEntity.getConnection(), false);

    }


}
