package org.aksw.deer.helper.prefixFinder;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

public class PrefixFinderTest {
    private PrefixFinder myPF;
    @Before
    public void setUp() throws Exception {
        this.myPF = new PrefixFinder();
    }


    @Test
    public void testGetUnknownPrefixes() throws Exception {
        Model testModel = ModelFactory.createDefaultModel();
        testModel.setNsPrefix("dings", "http://dings.com/");
        testModel.add(
                testModel.createResource("http://dangs.com/Angela_Merkel"),
                testModel.createProperty("http://dangs.com/", "ist"),
                ResourceFactory.createResource("dings:bloed")
        );
        System.out.println(testModel.getNsPrefixMap());
        System.out.println(testModel.getNsPrefixURI("dings"));
        System.out.println(testModel.createList());
        System.out.println(testModel);
        String outputFile = "testfile.ttl";
        FileWriter outFile = null;
        try {
            outFile = new FileWriter(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        testModel.write(outFile,"TURTLE");
    }

//    @Test
    public void testGetPrefixResolution() throws Exception {
        assertTrue("URL is not right", myPF.getPrefixResolution("dbpedia").equals("http://dbpedia.org/resource/"));
        assertTrue("has to return null when prefix is null", myPF.getPrefixResolution(null) == null);
        assertTrue("has to return null when prefix is an empty String", myPF.getPrefixResolution("") == null);
        assertTrue("has to return null when prefix is n because n is not resolutionable (may change over time)",
                myPF.getPrefixResolution("n") == null);

    }
}