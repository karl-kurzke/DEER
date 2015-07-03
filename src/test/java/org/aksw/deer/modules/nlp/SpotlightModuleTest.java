package org.aksw.deer.modules.nlp;

import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.query.*;

import java.util.HashMap;
import java.util.List;

import com.hp.hpl.jena.rdf.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;

public class SpotlightModuleTest {
//    private final String DBPEDIA_END_POINT = "dbpediaendpoint";
//    private String testTextShort = "Queen Elizabeth is the Queen of Great Britain";
//    private SpotlightModule slm = new SpotlightModule();
//
////    @Before
//    public void setUp() throws Exception {
//
//
//    }
//
////    @After
//    public void tearDown() throws Exception {
//
//    }
//
//
//
//
////    @Test
//    public void testRefineString() throws Exception {
//
//    }
//
////    @Test
//    public void testGetEntityHashMapFromSpotlightRest() throws Exception {
//        HashMap<String, String> newConfig = new HashMap<String,String>();
//        newConfig.put(SpotlightModule.SPOTLIGHT_CONFIDENCE, "0.2");
//        newConfig.put(SpotlightModule.SPOTLIGHT_SUPPORT, "20");
//
//        slm.addParams(newConfig);
//
//        testTextShort = "Queen Elizabeth is the Queen of Great Britain";
//        HashMap<String, Object> result = slm.getEntityHashMapFromSpotlightRest(testTextShort);
//        assertTrue( "Resources have to be a key in result HashMap. ", result.containsKey("Resources"));
//        ArrayList<HashMap<String, String>> resources = (ArrayList<HashMap<String, String>>) result.get("Resources");
//        assertTrue("Two Resources have to be found", resources.size() == 3);
//        System.out.println("Return from Rest:");
//        System.out.println(result.toString());
//    }
//
////    @Test
//    public void testHashMapAnswerToModel() throws Exception {
//        //TODO load testHashMap from json
//        HashMap<String, String> newConfig = new HashMap<String,String>();
//        newConfig.put(SpotlightModule.SPOTLIGHT_CONFIDENCE, "0.2");
//        newConfig.put(SpotlightModule.SPOTLIGHT_SUPPORT, "20");
//        slm.addParams(newConfig);
//        testTextShort = "Queen Elizabeth is the Queen of Great Britain";
//        HashMap<String, Object> testHashMap = slm.getEntityHashMapFromSpotlightRest(testTextShort);
//
//        Resource subject = ResourceFactory.createResource("queenHashMapTest");
//
//
//
//        Model testModel = slm.HashMapAnswerToModel(subject, testHashMap);
//        Property addedProp = ResourceFactory.createProperty(
//                slm.getUsedParameters().get(SpotlightModule.ADDED_PROPERTY)); //get added Property Value
//        Resource expectedResource = ResourceFactory.createResource(
//                "http://dbpedia.org/resource/RMS_Queen_Elizabeth"
//        );
//        assertTrue(testModel.contains(subject, addedProp, expectedResource));
//        expectedResource = ResourceFactory.createResource(
//                "http://dbpedia.org/resource/SS_Great_Britain"
//        );
//        assertTrue(testModel.contains(subject,  addedProp, expectedResource));
//
//
////        System.out.println(testModel);
//    }
//
//
////    @Test
//    public void testGetModelFromSpotlightRest() throws Exception {
//        HashMap<String, String> newConfig = new HashMap<String,String>();
//        newConfig.put(SpotlightModule.SPOTLIGHT_CONFIDENCE, "0.2");
//        newConfig.put(SpotlightModule.SPOTLIGHT_SUPPORT, "20");
//
//        slm.addParams(newConfig);
//        Resource subject = ResourceFactory.createResource("queenModelFromRestTest");
//        testTextShort = "Queen Elizabeth is the Queen of Great Britain";
//        Model result = slm.getModelFromSpotlightRest(subject, testTextShort);
////        System.out.println("testGetModelFromSpotlightRest");
////        System.out.println(result);
////        System.out.println("testGetModelFromSpotlightRest");
//
//    }
//
//
////    @Test
//    public void testGetNewTripleAsModel() throws Exception {
//        // Build up model to test
//        Resource subject = ResourceFactory.createResource("queenTripleTest");
//        Property descriptionProp = ResourceFactory.createProperty("description");
//
//        Literal description = ResourceFactory.createPlainLiteral(testTextShort);
//
//        Model testModel = ModelFactory.createDefaultModel();
//        testModel.add(subject, descriptionProp, description);
//        slm.setModel(testModel);
//        HashMap<String, String> newConfig = new HashMap<String,String>();
////        newConfig.put(SpotlightModule.LITERAL_PROPERTY, descriptionProp.getURI() );
//        slm.addParams(newConfig);
//        System.out.println("newTripleTest");
//        System.out.println(slm.getModel());
//
//        Model newTripleModel = slm.getNewTripleAsModel();
//
//        System.out.println(newTripleModel);
//    }
//
//
//
////    @Test
//    public void testProcess() throws Exception {
//
//        Resource subject = ResourceFactory.createResource("queenProcessTest");
//        Property descriptionProp = ResourceFactory.createProperty("description");
//        Literal description = ResourceFactory.createPlainLiteral(testTextShort);
//        Model testModel = ModelFactory.createDefaultModel();
//        testModel.add(subject, descriptionProp, description);
//        SpotlightModule slmProcess = new SpotlightModule( );
//        HashMap<String, String> props = new HashMap<String, String>();
//        props.put("output", "testfile2.ttl");
//        Model resultModel = slmProcess.process(testModel, props);
//        System.out.println("Ergebnisse:");
//        System.out.println(resultModel.toString());
//
//    }
//
//
//
//    public List<String> getDBpediaAbstaracts(Integer limit){
//        List<String> result = new ArrayList<String>();
//
//        String queryString = "SELECT distinct ?o WHERE {" +
//                "?s a <http://dbpedia.org/ontology/Place>." +
//                "?s <http://dbpedia.org/ontology/abstract> ?o } LIMIT " + limit.toString();
//        Query query = QueryFactory.create(queryString);
//        QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIA_END_POINT, query);
//        ResultSet queryResults = qexec.execSelect();
//        while(queryResults.hasNext()){
//            QuerySolution qs=queryResults.nextSolution();
//            result.add( qs.getLiteral("o").toString());
//        }
//        qexec.close() ;
//        return result;
//    }
//
//


}