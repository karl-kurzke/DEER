package org.aksw.deer.modules.nlp.evaluation;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import org.aksw.deer.io.Reader;
import org.aksw.deer.modules.nlp.MultipleExtractorNLPModule;
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
        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, modelToExamine);

        ResultSet results =  qe.execSelect();

        //Delete all Annotations with entityToDelete as means
        Integer tripleNumber = results.next().get("tripleNumber").asLiteral().getInt();
        qe.close();
        return tripleNumber;
    }
    static void runEvaluation(String testFilename, String resultDir) {
        String modelFile = "datasets/6/input.ttl";
//        modelFile = "datasets/6/testModelProcess.ttl";
//        modelFile = "datasets/nlpTestData/drugbank_dump.ttl";
////        modelFile = "datasets/nlpTestData/jamendo-rdf/jamendo.rdf";
        modelFile =                             "datasets/nlpTestData/dbpedia_AdministrativeRegion4.ttl";
//        modelFile = testFilename;
////        String outputFileSpotlight =            "datasets/nlpTestData/spotlightReturnData.ttl";
////        String outputFileExtractedTriples =     "datasets/nlpTestData/extractedTriples.ttl";
////        String ergebnisFilename =               "datasets/nlpTestData/Ergebnisse.txt";
//        String outputFileExtractedTriples =     resultDir + "extractedRelations.ttl";
        String outputModelfilename =           "outputmodel.ttl";
//        String ergebnisFilename =               resultDir + "resultAnalyses.txt";
//
        HashMap<String, String> settings = new HashMap<String, String>();
////        settings.put("literalProperty", "http://purl.org/ontology/mo/biography");
        settings.put("literalProperty", "http://dbpedia.org/ontology/abstract");
        Model modelForEvaluation = Reader.readModel(modelFile);
//        Integer triplesBefore = NLPEvaluation.getNumberOfTriples(modelForEvaluation);
//
        //Extraction
        MultipleExtractorNLPModule extractor = new MultipleExtractorNLPModule();
        Model enrichedModel = extractor.process(modelForEvaluation, settings);
        //save result model
        FileWriter outFileForModel = null;
        try {
            outFileForModel = new FileWriter(outputModelfilename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        enrichedModel.write(outFileForModel, "TURTLE");
////        stanMod.getExtractedTriple().write(outFileExtractedTriples, "TURTLE");
//        stanfordModel.write(outFileExtractedTriples, "TURTLE");
        ;
//        System.out.println("XXX Evaluation got model from spotlight");
//        Integer triplesAfterSpotlight = NLPEvaluation.getNumberOfTriples(spotLightModel);
//        System.out.println("XXX Evaluation got number of triples");
//        Integer newTriplesFromSpotlight = (triplesAfterSpotlight - triplesBefore);
//
//        System.out.println("Spotlight finished");
////        FileWriter outFile = null;
////        try {
////            outFile = new FileWriter(outputFileSpotlight);
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////        spotLightModel.write(outFile, "TURTLE");
//        System.out.println("Spotlight model not written in file");
//        String report = "Number Of Triples before Extraction: " +
//                triplesBefore.toString() +
//                "\nNew Triples after SpotlightExtraction with annotation blank nodes: " +
//                newTriplesFromSpotlight.toString() +
//                "\nNumber of extracted Entities with spotlight nominal: "  +
//                spotMod.getNumberOfExtractedEntities().toString() +
//                "\nNumber of extracted Entities with spotlight distinct: " +
//                spotMod.getNumberOfDistinctExtractedEntities();
//        try {
//            Files.write(Paths.get(ergebnisFilename), report.getBytes());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//
//        //Stanford Module
//        StanfordModule stanMod = new StanfordModule();
//        Model stanfordModel = stanMod.process(spotLightModel, settings);
//        Integer triplesAfterStanford = NLPEvaluation.getNumberOfTriples(stanfordModel);
//
//
//        //Export extracted Triples for further analyses
//        FileWriter outFileExtractedTriples = null;
//        try {
//            outFileExtractedTriples = new FileWriter(outputFileExtractedTriples);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
////        stanMod.getExtractedTriple().write(outFileExtractedTriples, "TURTLE");
//        stanMod.getMappedExtractedTriples().write(outFileExtractedTriples, "TURTLE");
//
//        HashMap<String, Integer> statementAnalyse = stanMod.getStatementAnalyse();
//        report += "\nStatement Analyse: ";
//        report += statementAnalyse.toString();
//
//        report += "\n Triples after StanfordExtraction: " +
//                triplesAfterStanford.toString();
//
//
//        report += "\nDouble Triples through StanfordExtraction: " +
//                stanMod.getNumberOfDoubleExtraction();
//        try {
//            Files.write(Paths.get(ergebnisFilename), report.getBytes());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//


    }

    public static void main (String[] args) {
        if (args.length == 2) {
            NLPEvaluation.runEvaluation(args[0], args[1]);
        } else {
            System.out.println("You have to specify a test set file as first " +
                    "Argument and an existing directiory to save the results " +
                    "as second argument. ");
        }


    }
}