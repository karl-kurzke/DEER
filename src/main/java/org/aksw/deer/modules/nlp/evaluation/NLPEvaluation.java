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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
        Boolean writeResultModel = Boolean.TRUE;
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
                modelForEvaluation.write(outFileForModel, "TURTLE");
                outFileForModel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (writeExtractedTripleModel) {
            FileWriter outFileForExtractionModel = null;
            try {
                outFileForExtractionModel = new FileWriter(outputExtractionModelfilename);
                extractor.getCombiner().getMappedExtractedTriples().write(outFileForExtractionModel, "TURTLE");
                outFileForExtractionModel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }
    public void runMultipleEvaluations (String fileName){

        File dir = new File(fileName);

        if (dir.isDirectory()) {
            for (File currentFile : dir.listFiles()) {
                if (currentFile.isFile() && !(currentFile.getName().startsWith("."))){
                    System.out.println(currentFile.getName());
                    runEvaluation(currentFile.getAbsolutePath());
                }
            }
        } else {
            runEvaluation(fileName);
        }

    };

    public static void main (String[] args) {

        NLPEvaluation myEval = new NLPEvaluation();
        if (args.length == 1) {
            myEval.runMultipleEvaluations(args[0]);
        } else {
            System.out.println("You have to specify a test set file as argument. ");
        }


    }
}