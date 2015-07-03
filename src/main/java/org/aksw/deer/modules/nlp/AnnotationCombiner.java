package org.aksw.deer.modules.nlp;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wolfo on 02.07.15.
 */
public class AnnotationCombiner {

    private static final Logger logger = Logger.getLogger(MultipleExtractorNLPModule.class.getName());

    private Boolean evaluate = Boolean.TRUE;

    public static final String PREFERED_ANNOTATION = "preferedAnnotation";
    public static final String PREFERED_ANNOTATION_DEFAULT =
            "http://ns.aksw.org/scms/tools/Spotlight";
    public static final String PREFERED_ANNOTATION_DESC =
            "URI of prefered Annotation Tool. If possible (If there are any) " +
                    "the Annotation of specified tool is used.";
    public static final String FILTER_ANNOTATIONS = "filterAnnotations";
    public static final String FILTER_ANNOTATIONS_DEFAULT = "True";
    public static final String FILTER_ANNOTATIONS_DESC =
            "If Filter Annotation is 'TRUE' all Annotation Blank Nodes will be filtered out.";


    private HashMap<String, String> usedParam = null;
    //Configuration
    public HashMap<String, String> getDefaultParam () {
        HashMap<String, String> defaultParam = new HashMap<String, String>();
        defaultParam.put(PREFERED_ANNOTATION, PREFERED_ANNOTATION_DEFAULT);
        defaultParam.put(FILTER_ANNOTATIONS, FILTER_ANNOTATIONS_DEFAULT);

        return defaultParam;
    }

    public void addParams(HashMap<String, String> newParams){
        for (String param:newParams.keySet()) {
            if (!(usedParam.containsKey(param))){
//                logger.error("Parameter " + param + " is not settable. ");
            } else {
                usedParam.put(param, newParams.get(param));
            }
        }
    }

    public AnnotationCombiner(){
        this.usedParam = getDefaultParam();
    }


    public Model combineAnnotation(Model annotatedModel, HashMap<String, String> props){
        addParams(props);

        Property preferedAnnotation = ResourceFactory.createProperty(usedParam.get(PREFERED_ANNOTATION));

        Boolean deleteAnnotations = usedParam.get(FILTER_ANNOTATIONS).toLowerCase().equals("true");

//			[ a                ann:Annotation ;
//			scms:beginIndex  "160"^^xsd:int ;
//			scms:endIndex    "173"^^xsd:int ;
//			scms:means       <http://ns.aksw.org/scms/annotations/stanford/Angela Merkel> ;
//			scms:source      <http://ns.aksw.org/scms/tools/StanfordRE> ;
//			ann:annotate     "The philosopher and mathematician Leibniz was born in Leipzig in 1646 and attended the University of Leipzig from 1661-1666. The current chancellor of Germany, Angela Merkel, also attended this university. This are to english sentences."^^xsd:string
//			] .


        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
                        "PREFIX scms: <http://ns.aksw.org/scms/> " +
                        "PREFIX ann: <http://www.w3.org/2000/10/annotation-ns#> " +
                        "SELECT  ?entityToDelete ?typeToDelete ?annToFilter ?entityToPrefere "  +
                        "WHERE { ?annPref rdf:type ann:Annotation . " +
                        "	?annPref ann:annotates ?textBase . " +
                        "	?annPref scms:beginIndex ?beginning ." +
                        "	?annPref scms:endIndex ?ending ." +
                        "	?annPref scms:means ?entityToPrefere ." +
                        "	?annPref scms:source <" + preferedAnnotation.getURI() + "> ." +
                        "	?annToFilter rdf:type ann:Annotation . " +
                        "	?annToFilter ann:annotates ?textBase . " +
                        "	?annToFilter scms:beginIndex ?beginning ." +
                        "	?annToFilter scms:endIndex ?ending ." +
                        "	?annToFilter scms:means ?entityToDelete ." +
                        "	?entityToDelete rdf:type ?typeToDelete ." +
                        "   FILTER (?entityToPrefere != ?entityToDelete)" +
                        "}\n ";
        Query query = QueryFactory.create(queryString);
//			logger.info(queryString);
//			extractedTriples = ModelFactory.createUnion(extractedTriples, annotatedModel);
        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, annotatedModel);
        ResultSet results =  qe.execSelect();
        //Delete all Annotations with entityToDelete as means
        List<QuerySolution> rows = ResultSetFormatter.toList(results);
        qe.close();
//        logger.info("Number of Entities to Replace because of better Annotation: "
//                + Integer.valueOf(rows.size()).toString());
        for (QuerySolution row:rows) {
            //Delete the type of the entityToDelete
            annotatedModel.remove(
                    row.getResource("entityToDelete"),
                    ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                    row.getResource("typeToDelete")
            );
            //Replace all Statements with entityToDelete as Object
            StmtIterator statementIterator = annotatedModel.listStatements(null, null, row.getResource("entityToDelete"));
            List<Statement> stmtList = new ArrayList<Statement>();
            while (statementIterator.hasNext()) {stmtList.add(statementIterator.nextStatement());}
            for (Statement stmtToDel: stmtList) {
                annotatedModel.add(ResourceFactory.createStatement(
                        stmtToDel.getSubject(),
                        stmtToDel.getPredicate(),
                        row.getResource("entityToPrefere")
                ));
                annotatedModel.remove(stmtToDel);
            }
            //Replace all Statements with entityToDelete as Object
            statementIterator = annotatedModel.listStatements(row.getResource("entityToDelete"), null, (RDFNode) null);
            stmtList = new ArrayList<Statement>();
            while (statementIterator.hasNext()) {stmtList.add(statementIterator.nextStatement());}
            for (Statement stmtToDel: stmtList) {
                annotatedModel.add(ResourceFactory.createStatement(
                        row.getResource("entityToPrefere"),
                        stmtToDel.getPredicate(),
                        stmtToDel.getObject()
                ));
                annotatedModel.remove(stmtToDel);
            }
            //Replace all Annotation, which are now not used any more
            row.getResource("annToFilter").removeProperties();

        }


    // if any Annotation have to be filtered do this.
        if (deleteAnnotations) {
            ResIterator annResIter = annotatedModel.listSubjectsWithProperty(
                    ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                    ResourceFactory.createResource("http://www.w3.org/2000/10/annotation-ns#Annotation")
            );
            List<Resource> annotationBlanknodes = new ArrayList<Resource>();
            while (annResIter.hasNext()) {annotationBlanknodes.add(annResIter.nextResource());}
            for (Resource resToDel: annotationBlanknodes) {
                resToDel.removeProperties();
            }
        }

        if (evaluate) {
            addMappedExtractesTriplesFromModel(annotatedModel);
        }




        return annotatedModel;
    }
    // Evaluation things

    private Model mappedExtractedTriples;
    /**
     * get all StanfordRessources from extractionModel and add them to mappedExtractedTriples
     */
    private void addMappedExtractesTriplesFromModel (Model extractionModel) {
        mappedExtractedTriples = ModelFactory.createDefaultModel();
        StmtIterator statementIterator = extractionModel.listStatements(null, null, (RDFNode) null);
        while (statementIterator.hasNext()) {
            Statement currentStatement = statementIterator.nextStatement();
            Property currentProperty = currentStatement.getPredicate();
            if(currentProperty.getURI().startsWith("http://ns.aksw.org/scms/annotations/stanford/")) {
                mappedExtractedTriples.add(currentStatement);
            }
        }
    }

    public Model getMappedExtractedTriples() {
        return this.mappedExtractedTriples;
    }

    public HashMap<String,Integer> getStatementAnalyse() {
        HashMap<String,Integer> answerMap = new HashMap<String,Integer>();
        answerMap.put("onlyDBpedia", 0);
        answerMap.put("onlyDBpediaObject", 0);
        answerMap.put("onlyDBpediaSubject", 0);
        answerMap.put("onlyStanford", 0);
        answerMap.put("literalAsObject", 0);
        if(mappedExtractedTriples == null) {
            return answerMap;
        }
        StmtIterator statementIterator = mappedExtractedTriples.listStatements(null, null, (RDFNode) null);
        while (statementIterator.hasNext()) {
            Statement currentStatement = statementIterator.nextStatement();
            RDFNode object = currentStatement.getObject();
            if (object.isLiteral()){
                answerMap.put("literalAsObject",(answerMap.get("literalAsObject")+1));
            } else {
                if (currentStatement.getSubject().getURI().startsWith("http://dbpedia")) {
                    if (object.asResource().getURI().startsWith("http://dbpedia")) {
                        answerMap.put("onlyDBpedia",(answerMap.get("onlyDBpedia")+1));
                    } else {
                        answerMap.put("onlyDBpediaSubject", (answerMap.get("onlyDBpediaSubject") + 1));
                    }
                } else {
                    if (currentStatement.getSubject().getURI().startsWith("http://dbpedia")) {
                        answerMap.put("onlyDBpediaObject", (answerMap.get("onlyDBpediaObject") + 1));
                    } else {
                        answerMap.put("onlyStanford", (answerMap.get("onlyStanford") + 1));
                    }
                }
            }
        }
        return answerMap;
    }
}
