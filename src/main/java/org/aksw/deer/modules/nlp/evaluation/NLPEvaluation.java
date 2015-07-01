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

    static Integer getNumberOfTriples(Model modelToExamine){

        String queryString =
                        "PREFIX ann: <http://www.w3.org/2000/10/annotation-ns#>\n" +
                        "SELECT  ( COUNT (* ) AS ?tripleNumber ) \n" +
                        "WHERE { ?subject ?predicate ?object .\n" +
//                               "FILTER (NOT EXISTS {?subject a ann:Annotation .})\n" +
                        "}\n ";
        Query query = QueryFactory.create(queryString);
//			"http://ns.aksw.org/scms/tools/Spotlight";

        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, modelToExamine);

        ResultSet results =  qe.execSelect();

        //Delete all Annotations with entityToDelete as means
        Integer tripleNumber = 999999999 ;
        tripleNumber = results.next().get("tripleNumber").asLiteral().getInt();

//        List<QuerySolution> rows = ResultSetFormatter.toList(results);

        qe.close();
        System.out.println("Number of Triples after Spotlight");

        System.out.println(tripleNumber);
//        return rows.get(0).get("tripleNumber").asLiteral().getInt();
        return tripleNumber;
    }
    static void runEvaluation(String testFilename) {
        String modelFile = "datasets/6/input.ttl";
        modelFile = "datasets/6/testModelProcess.ttl";
//        modelFile = "datasets/nlpTestData/drugbank_dump.ttl";
//        modelFile = "datasets/nlpTestData/jamendo-rdf/jamendo.rdf";
        modelFile =                             "datasets/nlpTestData/dbpedia_AdministrativeRegion25.ttl";
        modelFile = testFilename;
//        String outputFileSpotlight =            "datasets/nlpTestData/spotlightReturnData.ttl";
//        String outputFileExtractedTriples =     "datasets/nlpTestData/extractedTriples.ttl";
//        String ergebnisFilename =               "datasets/nlpTestData/Ergebnisse.txt";
        String outputFileExtractedTriples =     "extractedTriples.ttl";
        String ergebnisFilename =               "Ergebnisse.txt";

        HashMap<String, String> settings = new HashMap<String, String>();
//        settings.put("literalProperty", "http://purl.org/ontology/mo/biography");
        settings.put("literalProperty", "http://dbpedia.org/ontology/abstract");
        Model modelForEvaluation = Reader.readModel(modelFile);
        Integer triplesBefore = NLPEvaluation.getNumberOfTriples(modelForEvaluation);

        //Spotlight
        SpotlightModule spotMod = new SpotlightModule();
        Model spotLightModel = spotMod.process(modelForEvaluation, settings);
        System.out.println("XXX Evaluation got model from spotlight");
        Integer triplesAfterSpotlight = NLPEvaluation.getNumberOfTriples(spotLightModel);
        System.out.println("XXX Evaluation got number of triples");
        Integer newTriplesFromSpotlight = (triplesAfterSpotlight - triplesBefore);

        System.out.println("Spotlight finished");
//        FileWriter outFile = null;
//        try {
//            outFile = new FileWriter(outputFileSpotlight);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        spotLightModel.write(outFile, "TURTLE");
        System.out.println("Spotlight model not written in file");
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
        Integer triplesAfterStanford = NLPEvaluation.getNumberOfTriples(stanfordModel);
        Integer newStanford = (triplesAfterStanford - triplesAfterSpotlight);

        //Export extracted Triples for further analyses
        FileWriter outFileExtractedTriples = null;
        try {
            outFileExtractedTriples = new FileWriter(outputFileExtractedTriples);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        stanMod.getExtractedTriple().write(outFileExtractedTriples, "TURTLE");
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

    public static void main (String[] args) {
        if (args.length == 1) {
            NLPEvaluation.runEvaluation(args[0]);
        } else {
            System.out.println("You have to specify a test set file as Argument. Nothing else allowed.");
        }


    }
}