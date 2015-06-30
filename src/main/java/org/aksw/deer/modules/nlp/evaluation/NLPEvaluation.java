package org.aksw.deer.modules.nlp.evaluation;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import org.aksw.deer.io.Reader;
import org.aksw.deer.modules.nlp.SpotlightModule;
import org.aksw.deer.modules.nlp.StanfordModule;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wolfo on 29.06.15.
 */
public class NLPEvaluation {

    private Integer getNumberOfTriples(Model modelToExamine){
        String queryString =
                        "PREFIX ann: <http://www.w3.org/2000/10/annotation-ns#>\n" +
                        "SELECT  ( COUNT (?subject ) AS ?tripleNumber ) \n" +
                        "WHERE { ?subject ?predicate ?object .\n" +
                               "FILTER (NOT EXISTS {?subject a ann:Annotation .})\n" +
                        "}\n ";
        Query query = QueryFactory.create(queryString);
//			"http://ns.aksw.org/scms/tools/Spotlight";

        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, modelToExamine);
        ResultSet results =  qe.execSelect();
        //Delete all Annotations with entityToDelete as means
        List<QuerySolution> rows = ResultSetFormatter.toList(results);
        qe.close();

        return rows.get(0).get("tripleNumber").asLiteral().getInt();

    }


    public static void main (String[] Args) {
        String modelFile = "datasets/6/input.ttl";
        modelFile = "datasets/6/testModelProcess.ttl";
//        modelFile = "datasets/nlpTestData/drugbank_dump.ttl";
//        modelFile = "datasets/nlpTestData/jamendo-rdf/jamendo.rdf";
//        modelFile =                             "datasets/nlpTestData/dbpedia_AdministrativeRegion4.ttl";
        String outputFileSpotlight =            "datasets/nlpTestData/spotlightReturnData.ttl";
        String outputFileExtractedTriples =     "datasets/nlpTestData/extractedTriples.ttl";
        String ergebnisFilename =               "datasets/nlpTestData/Ergebnisse.txt";
        NLPEvaluation thisEval = new NLPEvaluation();

        HashMap<String, String> settings = new HashMap<String, String>();
//        settings.put("literalProperty", "http://purl.org/ontology/mo/biography");
        settings.put("literalProperty", "http://dbpedia.org/ontology/abstract");
        Model modelForEvaluation = Reader.readModel(modelFile);
        Integer triplesBefore = thisEval.getNumberOfTriples(modelForEvaluation);

        //Spotlight
        SpotlightModule spotMod = new SpotlightModule();
        Model spotLightModel = spotMod.process(modelForEvaluation, settings);
        Integer triplesAfterSpotlight = thisEval.getNumberOfTriples(spotLightModel);
        Integer newTriplesFromSpotlight = (triplesAfterSpotlight - triplesBefore);
        FileWriter outFile = null;
        try {
            outFile = new FileWriter(outputFileSpotlight);
        } catch (IOException e) {
            e.printStackTrace();
        }
        spotLightModel.write(outFile, "TURTLE");

        String report = "Number Of Triples before Extraction: " +
                triplesBefore.toString() +
                "\nNew Triples after SpotlightExtraction: " +
                newTriplesFromSpotlight.toString() +
                "\nNumber of extracted Entities with spotlight nominal: "  +
                spotMod.getNumberOfExtractedEntities().toString() +
                "\nNumber of extracted Entities with spotlight distinct: " +
                spotMod.getNumberOfDistinctExtractedEntities();
        try {
            Files.write(Paths.get(ergebnisFilename), report.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }



        //Stanford Module
        StanfordModule stanMod = new StanfordModule();
        Model stanfordModel = stanMod.process(spotLightModel, settings);
        Integer triplesAfterStanford = thisEval.getNumberOfTriples(stanfordModel);
        Integer newStanford = (triplesAfterStanford - triplesAfterSpotlight);

        //Export extracted Triples for further analyses
        stanMod.getMappedExtractedTriples();
        FileWriter outFileExtractedTriples = null;
        try {
            outFileExtractedTriples = new FileWriter(outputFileExtractedTriples);
        } catch (IOException e) {
            e.printStackTrace();
        }
        stanMod.getMappedExtractedTriples().write(outFileExtractedTriples, "TURTLE");

        HashMap<String, Integer> statementAnalyse = stanMod.getStatementAnalyse();
        report += "\nStatement Analyse: ";
        report += statementAnalyse.toString();

        report += "\nNew Triples through StanfordExtraction: " +
                newStanford.toString();


        report += "\nDouble Triples through StanfordExtraction: " +
                stanMod.getNumberOfDoubleExtraction();
        try {
            Files.write(Paths.get(ergebnisFilename), report.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}