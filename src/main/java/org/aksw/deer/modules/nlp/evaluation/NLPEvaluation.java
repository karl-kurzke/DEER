package org.aksw.deer.modules.nlp.evaluation;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import org.aksw.deer.io.Reader;
import org.aksw.deer.modules.nlp.MultipleExtractorNLPModule;
import org.aksw.deer.modules.nlp.NLPExtractor;
import org.apache.commons.io.FilenameUtils;
import org.apache.xpath.operations.Bool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wolfo on 29.06.15.
 */
public class NLPEvaluation {

    private HashMap<String, HashMap<String,String>> eval = new HashMap< String,HashMap<String,String> >();

    private void runEvaluation(String testFilename) {
        Boolean writeResultModel = Boolean.FALSE;
        Boolean writeExtractedTripleModel = Boolean.TRUE;
        //Create ResultFile
        File resultFile = new File(FilenameUtils.removeExtension(testFilename) + "_Results");
        resultFile.mkdirs();
//        String modelFile = "datasets/6/input.ttl";
//        modelFile = "datasets/6/testModelProcess.ttl";
//        modelFile = "datasets/nlpTestData/drugbank_dump.ttl";
////        modelFile = "datasets/nlpTestData/jamendo-rdf/jamendo.rdf";
//                                     "datasets/nlpTestData/dbpedia_AdministrativeRegion4.ttl";
//        modelFile = testFilename;
////        String outputFileSpotlight =            "datasets/nlpTestData/spotlightReturnData.ttl";
////        String outputFileExtractedTriples =     "datasets/nlpTestData/extractedTriples.ttl";
//        String ergebnisFilename =               "datasets/nlpTestData/Ergebnisse.txt";
//        String outputFileExtractedTriples =     resultDir + "extractedRelations.ttl";
        String outputModelfilename =           resultFile.getAbsolutePath() + "/outputmodel.ttl";
        String outputExtractionModelfilename =           resultFile.getAbsolutePath() + "/extractedTriples.ttl";
        String ergebnisFilename =              resultFile.getAbsolutePath() + "/resultAnalyses.txt";
//
        HashMap<String, String> settings = new HashMap<String, String>();
////        settings.put("literalProperty", "http://purl.org/ontology/mo/biography");
        settings.put("literalProperty", "http://dbpedia.org/ontology/abstract");
        Model modelForEvaluation = Reader.readModel(testFilename);
        HashMap<String, String> thisEvalInfo = new HashMap<String,String>();
        thisEvalInfo.put("triplesBefore", new Long(modelForEvaluation.size()).toString());


//
        //Extraction
        MultipleExtractorNLPModule extractor = new MultipleExtractorNLPModule();
        modelForEvaluation = extractor.process(modelForEvaluation, settings);

        //Evaluation
        thisEvalInfo.put("triplesAfter", new Long(modelForEvaluation.size()).toString());
        eval.put(this.getClass().getSimpleName(), thisEvalInfo);
        eval.putAll(extractor.getEvaluation());

        //store evaluation
        try {
            Files.write(Paths.get(ergebnisFilename), eval.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }


        //save result model
        if (writeResultModel) {
            FileWriter outFileForModel = null;
            try {
                outFileForModel = new FileWriter(outputModelfilename);
            } catch (IOException e) {
                e.printStackTrace();
            }
            modelForEvaluation.write(outFileForModel, "TURTLE");
        }
        if (writeExtractedTripleModel) {
            FileWriter outFileForExtractionModel = null;
            try {
                outFileForExtractionModel = new FileWriter(outputExtractionModelfilename);
            } catch (IOException e) {
                e.printStackTrace();
            }
            extractor.getCombiner().getMappedExtractedTriples().write(outFileForExtractionModel, "TURTLE");
        }

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
//


    }

    public static void main (String[] args) {
        NLPEvaluation myEval = new NLPEvaluation();
        if (args.length == 1) {
            myEval.runEvaluation(args[0] );
        } else {
            System.out.println("You have to specify a test set file as argument. ");
        }


    }
}