package org.aksw.deer.modules.nlp;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import org.aksw.deer.io.Reader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class StanfordModuleTest {
    private String base = ("src/test/java/org/aksw/deer/modules/nlp/testModelFiles/");
    private StanfordModule sm = null;
    @Before
    public void setUp() throws Exception {
        sm = new StanfordModule();
    }

    @After
    public void tearDown() throws Exception {

    }

//    //    @Test
//    public void testProcess() throws Exception {
//        HashMap<String, String> props = new HashMap<String, String>();
//        Model testModel = Reader.readModel(base + "testModelProcess.ttl");
//        testModel = sm.process(testModel, props);
//        String outputFile = "testModelProcessReturn.ttl";
//        FileWriter outFile = null;
//        try {
//            outFile = new FileWriter(base + outputFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        testModel.write(outFile,"TURTLE");
//    }
//    //    @Test
//    public void testGetModelFromStanfordRE() throws Exception {
//        Resource testRes = ResourceFactory.createResource("testRes");
//        String testString = "The philosopher and mathematician Leibniz " +
//                "was born in Leipzig in 1646 and attended the University " +
//                "of Leipzig from 1661-1666. The current chancellor of " +
//                "Germany, Angela Merkel, also attended this university.";
//
//        Model resultMod = sm.getModelFromStanfordRE(testRes, testString);
//        System.out.println(resultMod.toString());
//        String outputFile = "testGetModelFromStanfordRE.ttl";
//        FileWriter outFile = null;
//        try {
//            outFile = new FileWriter(base + outputFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        resultMod.write(outFile,"TURTLE");
//    }
//    //    @Test
//    public void testMapResourcesOnPreferedAnnotationModel () {
//        Model testModel = Reader.readModel(base + "testfileFilterAnnotations.ttl");
////        Property preferedAnnotation = ResourceFactory.createProperty("http://ns.aksw.org/scms/tools/Spotlight");
//        testModel = sm.mapResourcesOnPreferedAnnotationModel(testModel);
//
//        String outputFile = base + "testfileFilterAnnotationsResult.ttl";
//        try {
//            testModel.write(new FileWriter(outputFile), "TURTLE");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
////        mapResourcesOnPreferedAnnotationModel
//    }
}
