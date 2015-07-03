package org.aksw.deer.modules.nlp;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.*;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.RelationExtractorAnnotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by wolfo on 02.07.15.
 */
public class StanfordREExtractor implements NLPExtractor {

    private static final Logger logger = Logger.getLogger(MultipleExtractorNLPModule.class.getName());


    private HashMap<String, String> usedParam;

    //GENERAL CONFIGURATION
    public static final String LITERAL_PROPERTY 	= "literalProperty";
    public static final String LITERAL_PROPERTY_DESC =
            "Literal property used by Spotlight for NER. " +
                    "If not set, the top ranked literal property will be pecked";
    public static final String LITERAL_PROPERTY_DEFAULT = null;

    public static final String ADDED_PROPERTY = "addedProperty";
    public static final String ADDED_PROPERTY_DESC =
            "Property added to the input model by dbpedia" +
                    "knowledge through Spotlight. By default, " +
                    "this parameter is set to 'gn:relatedTo'. " +
                    "Need to be valid URI.";
    public static final String ADDED_PROPERTY_DEFAULT =
            "http://geoknow.org/ontology/relatedTo";




    //Configuration
    public HashMap<String, String> getDefaultParam () {
        HashMap<String, String> defaultParam = new HashMap<String, String>();
        defaultParam.put(LITERAL_PROPERTY, LITERAL_PROPERTY_DEFAULT);
        defaultParam.put(ADDED_PROPERTY, ADDED_PROPERTY_DEFAULT);

        return defaultParam;
    }

    @Override
    public void addParams(HashMap<String, String> newParams){
        for (String param:newParams.keySet()) {
            if (!(usedParam.containsKey(param))){
//                logger.error("Parameter " + param + " is not settable. ");
            } else {
                usedParam.put(param, newParams.get(param));
            }
        }
    }

    public StanfordREExtractor (){
        usedParam = getDefaultParam();
    }



    //Stanford model and Function to load trained Relation Extraction model
    private StanfordCoreNLP stanfordNLP = null;
    private RelationExtractorAnnotator relationExtractorAnnotator = null;

    private void loadStanfordModels() {
        logger.info("Start of stanford initialization. (This will happens only ones.)");
        this.stanfordNLP = new StanfordCoreNLP();
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,lemma,pos,parse,ner");

        this.relationExtractorAnnotator = new RelationExtractorAnnotator(props);
        logger.info("End of stanford initialization.");
    }










    @Override
    public Model extractFromText(Literal textLiteral) {
        Model modFromTextExtraction = extractFromText(null, textLiteral);

        return modFromTextExtraction;
    }

    /**
     * Get new to subject related Properties and Relation in given text.
     * @autor wotto
     * based on fox Class REStanford from @author rspeck
     * paper of stanford relation extraction:
     * https://github.com/stanfordnlp/CoreNLP/blob/672d43a9677272fdef79ef15f3caa9ed7bc26164/src/edu/stanford/nlp/ie/machinereading/domains/roth/RothCONLL04Reader.java
     *
     * @param recConnectedToText The Subject to whom the found Entities are related
     * @param textLiteral Text as Literal in which related Resources should be found
     * @return Model with all new Entities and Relations
     */
    @Override
    public Model extractFromText(Resource recConnectedToText, Literal textLiteral) {
        Model namedEntitymodel = ModelFactory.createDefaultModel();
        String text = textLiteral.getString();

        if (!textLiteral.getLanguage().equals("en")) {
            logger.info("Tried to extract from not english Text From Stanford RE");

            return namedEntitymodel;
        }

        //add relation extractor when needed (Takes half a minute)
        if (this.relationExtractorAnnotator == null) {
            loadStanfordModels();
        }


        // ----------------------------------------------------------------------------
        // find relations
        // ----------------------------------------------------------------------------
        try {
            logger.debug("Start...");
            Annotation doc = new Annotation(text);
            logger.debug("Annotate the doc...");
            stanfordNLP.annotate(doc);
            logger.debug("RelationExtractorAnnotator the doc...");
            relationExtractorAnnotator.annotate(doc);
            logger.debug("For all relation ...");
            for (CoreMap sentenceAnnotation : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
                List<RelationMention> relationMentions = (sentenceAnnotation.get(MachineReadingAnnotations.RelationMentionsAnnotation.class));
                logger.debug("relationMentions.size():" + relationMentions.size());
                for (RelationMention relationMention : relationMentions) {
//					if (checkrules(relationMention)) {
                    if (!relationMention.getType().equals("_NR") ) { //No Relation is best matching relation
                        namedEntitymodel = getModelFromRelationMention(relationMention, text);
                        if (logger.isDebugEnabled()) {
                            logger.debug(relationMention);
                        }
                    }
                }
            }
            logger.debug("Relations done.");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }

        return namedEntitymodel;
    }



    /**
     * transforms stanfords RelationMention into RDF Model
     * Adds Annotation Information
     * @param relationMention
     * @param originalText
     * @return
     */
    private Model getModelFromRelationMention(RelationMention relationMention, String originalText) {
        Boolean metaInfo = true;
        //Prefixes
        HashMap<String, String> prefMap = new HashMap<String, String>();
        prefMap.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefMap.put("ann", "http://www.w3.org/2000/10/annotation-ns#");
        prefMap.put("dbpedia-res", "http://dbpedia.org/resource/");
        prefMap.put("dbpedia-ont", "http://dbpedia.org/ontology/");
        prefMap.put("geo", "http://geoknow.org/ontology/");
        prefMap.put("scms", "http://ns.aksw.org/scms/");
        prefMap.put("stanford", "http://ns.aksw.org/scms/annotations/stanford/");
        prefMap.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        //model to fill
        Model relationModel = ModelFactory.createDefaultModel();
        relationModel.setNsPrefixes(prefMap);
        //model for meta information: Annotational information
        Model annotationModel = ModelFactory.createDefaultModel();
        annotationModel.setNsPrefixes(prefMap);
        //vocabulary needed more then one time
        Property means = ResourceFactory.createProperty(prefMap.get("scms"), "means");
        Property rdfType = ResourceFactory.createProperty(prefMap.get("rdf"), "type");

        Property surfaceForm = ResourceFactory.createProperty(prefMap.get("scms"), "body");
        Property beginIndex = ResourceFactory.createProperty(prefMap.get("scms"), "beginIndex");
        Property endIndex = ResourceFactory.createProperty(prefMap.get("scms"), "endIndex");
        Property extractedBy = ResourceFactory.createProperty(prefMap.get("scms"), "source");
        Resource extractor = ResourceFactory.createResource(prefMap.get("scms") + "tools/StanfordRE");
        Resource annotationClass = ResourceFactory.createResource(prefMap.get("ann") + "Annotation");
        Property annotates = ResourceFactory.createProperty(prefMap.get("ann") + "annotates");
        //RELATION
        Property relation = ResourceFactory.createProperty(prefMap.get("stanford"), relationMention.getType());

        //AGENS
        EntityMention agensMention = relationMention.getEntityMentionArgs().get(0);
        String agensName = agensMention.getExtentString().replace(" ", "_");
        Resource agens = ResourceFactory.createResource(prefMap.get("stanford") + agensName);
        Resource agensType = ResourceFactory.createResource(prefMap.get("stanford") + agensMention.getType());
        relationModel.add(agens, rdfType, agensType);

        //AGENS annotational Information
        Resource agensAnnotation = ResourceFactory.createResource();
        annotationModel.add(agensAnnotation, rdfType, annotationClass);
        annotationModel.add(agensAnnotation, means, agens);
        annotationModel.add(
                agensAnnotation,
                beginIndex,
                Integer.toString(agensMention.getSyntacticHeadToken().endPosition() -
                        agensMention.getExtentString().length()),
                XSDDatatype.XSDint);

        annotationModel.add(
                agensAnnotation,
                endIndex,
                Integer.toString(agensMention.getSyntacticHeadToken().endPosition()),
                XSDDatatype.XSDint);
        annotationModel.add(agensAnnotation, extractedBy, extractor);
        annotationModel.add(agensAnnotation, annotates, originalText, XSDDatatype.XSDstring);
//		One original Sentence. Can not be used for Matching. It have to be the whole text string.
//		relationMention.getSentence().toString()


        //PATIENS
        EntityMention patiensMention = relationMention.getEntityMentionArgs().get(1);
        String patiensName = patiensMention.getValue().replace(" ","_");
        Resource patiens = ResourceFactory.createResource(prefMap.get("stanford") + patiensName);
        Resource patiensType = ResourceFactory.createResource(prefMap.get("stanford") + patiensMention.getType());
        relationModel.add(patiens, rdfType, patiensType);

        //Patiens annotational Information
        Resource patiensAnnotation = ResourceFactory.createResource();
        annotationModel.add(patiensAnnotation, rdfType, annotationClass);
        annotationModel.add(patiensAnnotation, means, patiens);
        annotationModel.add(
                patiensAnnotation,
                beginIndex,
                Integer.toString(patiensMention.getSyntacticHeadToken().endPosition() -
                        patiensMention.getExtentString().length()),
                XSDDatatype.XSDint);
        annotationModel.add(
                patiensAnnotation,
                endIndex,
                Integer.toString(patiensMention.getSyntacticHeadToken().endPosition()),
                XSDDatatype.XSDint);
        annotationModel.add(patiensAnnotation, extractedBy, extractor);
        annotationModel.add(patiensAnnotation, annotates, originalText, XSDDatatype.XSDstring);

        //add relation
        relationModel.add(agens, relation, patiens);
        if (evaluationMetaData) {
            addExtractedTriple(ResourceFactory.createStatement(agens, relation, patiens));

        }


        //add Annotation Info to result model
        relationModel = ModelFactory.createUnion(relationModel, annotationModel);
        relationModel.setNsPrefixes(annotationModel.getNsPrefixMap());


        return relationModel;
    }



    //	EvaluationThings

    @Override
    public HashMap<String, String> getEvaluation() {
        HashMap<String, String> eval = new HashMap<String, String>();
//        eval.put("numberOfExtraction", numberOfExtraction.toString());
        eval.put("doubleExtraction", doubleExtraction.toString());

        return eval;
    }
    private Boolean evaluationMetaData = Boolean.TRUE;
    private Model extractedTriples = ModelFactory.createDefaultModel();
    public Model getExtractedTriple() { return extractedTriples;}
    private Integer numberOfExtraction = 0;
    private Integer doubleExtraction = 0;
    public Integer getNumberOfDoubleExtraction() {
        return doubleExtraction;
    }
    private void addExtractedTriple(Statement triple) {
//        logger.info(triple.toString());
        numberOfExtraction += 1;
        if (extractedTriples.contains(triple)) {
            this.doubleExtraction += 1;
        } else {
            extractedTriples.add(triple);
        }
    }



}
