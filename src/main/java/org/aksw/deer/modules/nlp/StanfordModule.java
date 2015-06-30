package org.aksw.deer.modules.nlp;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.impl.SelectorImpl;
import edu.stanford.nlp.ie.machinereading.structure.ExtractionObject;
import org.aksw.deer.helper.vacabularies.SPECS;
import org.aksw.deer.io.Reader;
import org.aksw.deer.json.ParameterType;
import org.aksw.deer.modules.DeerModule;
import org.apache.log4j.Logger;



import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.*;

//Staford imports
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations.RelationMentionsAnnotation;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.RelationExtractorAnnotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.jgap.gp.terminal.True;


/**
 * Integrates Staford Cor Named Entity Recognition and Relation Extraction to DEER
 * @author wo
 * @mail otto@studserv.uni-leipzig.de
 */
public class StanfordModule implements DeerModule{
	private static final Logger logger = Logger.getLogger(StanfordModule.class.getName());

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

	// From Fox Module
	public static final String        CFG_KEY_LIVEIN             = StanfordModule.class.getName().concat(".liveIn");
	public static final String        CFG_KEY_LOCATEDIN          = StanfordModule.class.getName().concat(".locatedIn");
	public static final String        CFG_KEY_ORGBASEDIN         = StanfordModule.class.getName().concat(".orgbasedIn");
	public static final String        CFG_KEY_WORKFOR            = StanfordModule.class.getName().concat(".workFor");






	private Model model = null;

	private HashMap<String, String> usedParam;


//	EvaluationThings
	private Boolean evaluationMetaData = Boolean.TRUE;
	private Model extractedTriples = ModelFactory.createDefaultModel();
	private Integer doubleExtraction = 0;
	public Integer getNumberOfDoubleExtraction() {
		return doubleExtraction;
	}
	private void addExtractedTriple(Statement triple) {
		if (extractedTriples.contains(triple)) {
			this.doubleExtraction += 1;
		} else {
			extractedTriples.add(triple);
		}
	}
	private Model mappedExtractedTriples = null;

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
		answerMap.put("onlydbPediaObject", 0);
		answerMap.put("onlydbPediaSubject", 0);
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




	private StanfordCoreNLP stanfordNLP = null;

	private RelationExtractorAnnotator relationExtractorAnnotator = null;

	private void initDefaultParams() {
		usedParam = new HashMap<String, String>();
		usedParam.put(LITERAL_PROPERTY, LITERAL_PROPERTY_DEFAULT);
		usedParam.put(ADDED_PROPERTY, ADDED_PROPERTY_DEFAULT);
		usedParam.put(PREFERED_ANNOTATION, PREFERED_ANNOTATION_DEFAULT);
		usedParam.put(FILTER_ANNOTATIONS, FILTER_ANNOTATIONS_DEFAULT);
	}

	public void addParams(HashMap<String, String> newParams){
		for (String param:newParams.keySet()) {
			if (!(usedParam.containsKey(param))){
				logger.error("Parameter " + param + " is not settable. ");
			} else {
				usedParam.put(param, newParams.get(param));
			}
		}
	}
	// CONSTRUCTOR
	// ___________

	/**
	 *
	 * @param fileNameOrUri
	 * @param literalPropertyURI
	 */
	public StanfordModule(String fileNameOrUri, String literalPropertyURI) {
		this(Reader.readModel(fileNameOrUri), ResourceFactory.createProperty(literalPropertyURI));
	}
	/**
	 * @param model
	 * @param literalProperty
	 *@author sherif
	 */
	public StanfordModule(Model model, Property literalProperty) {
		this(model);
		//super();
		this.usedParam.put(LITERAL_PROPERTY, literalProperty.getURI());
	}


	/**
	 *
	 *@author sherif
	 */
	public StanfordModule(Model model) {
		this();
		//super();
		this.model = model;

	}
	/**
	 *
	 */
	public StanfordModule() {
		//super();
		initDefaultParams();
		logger.info("Start of stanford initialization.");
		this.stanfordNLP = new StanfordCoreNLP();
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,lemma,pos,parse,ner");
		this.relationExtractorAnnotator = new RelationExtractorAnnotator(props);
		logger.info("End of stanford initialization.");
	}

	// SETTER, GETTER
	// ______________
	/**
	 * @return the relatedToProperty
	 */
	public String getRelatedToProperty() {
		return usedParam.get(ADDED_PROPERTY);
	}

	/**
	 * @param relatedToProperty the relatedToProperty to set
	 */
	public void setRelatedToProperty(Property relatedToProperty) {
		this.usedParam.put(ADDED_PROPERTY, relatedToProperty.getURI());
	}

	/**
	 * @return the model
	 */
	public Model getModel() {
		return model;
	}


	/**
	 * @return the literalProperty
	 */
	public String getliteralProperty() {
		return usedParam.get(LITERAL_PROPERTY);
	}

	public HashMap<String, String> getUsedParameters(){
		return this.usedParam;
	}

	/**
	 * @param model the model to setModel
	 */
	public void setModel(Model model) {
		this.model = model;
	}


	/**
	 * @param literalProperty the literalProperty to set
	 */
	public void setliteralProperty(String literalProperty) {
		this.usedParam.put(LITERAL_PROPERTY, literalProperty);
	}

	// MAIN METHOD FOR MODULE
	// ______________________



	/* (non-Javadoc)
	 * @see org.aksw.geolift.modules.GeoLiftModule#process(com.hp.hpl.jena.rdf.model.Model, java.util.Map)
	 */
	public Model process(Model inputModel, Map<String, String> parameters){
		logger.info("--------------- STANFORD NLP Module ---------------");
		model = inputModel;
		if( parameters.containsKey("input")){
			model = Reader.readModel(parameters.get("input"));
		}
		//import parameters
		this.addParams((HashMap) parameters);

		// Model From Spotlight
		Model newModel = getNewTripleAsModel();
		model = model.union(newModel);

		if( parameters.containsKey("output")){
			String outputFile = parameters.get("output");
			FileWriter outFile = null;
			try {
				outFile = new FileWriter(outputFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			model.write(outFile,"TURTLE");
		}
		return model;
	}



	// GET NEW TRIPLE FOR MODEL
	// ________________________

	/**
	 * Get all Literal Strings defined by LITERAL_PROPERTY.
	 * Build a Model with entities found in Literal Strings
	 * @return Model with all new found Relations
	 * @author wolf otto
	 */
	public Model getNewTripleAsModel(){

		// build empty model
		Model resultModel = ModelFactory.createDefaultModel();
		//get literal to examine from usedParams
		//if no LITERAL_PROPERTY get ranked Properties From ranker (is null)
		if (this.usedParam.get(LITERAL_PROPERTY) == null) {
			LiteralPropertyRanker lpr = new LiteralPropertyRanker(model);
			Property topRankedProp = lpr.getTopRankedLiteralProperty();
			if(topRankedProp == null){
				logger.info("No Literal Properties!, return input model.");
				return resultModel;
			}
			//mostly the ann:annotate Property is selected as first, when other NLP Module are Used
			// but we do not want to use text used for previous Annotations, so select second best one.
			if(topRankedProp.getURI().equals("http://www.w3.org/2000/10/annotation-ns#annotates")) {
				Property secondBest = lpr.getTopNRankedLiteralProperty(2).lastEntry().getValue();
				if(secondBest == null){
					logger.info("No Literal Properties except ann:annotates !, return input model.");
					return resultModel;
				}
				topRankedProp = secondBest;
			}
			logger.info("Top ranked Literal Property: " + topRankedProp);
			usedParam.put(LITERAL_PROPERTY, topRankedProp.getURI());
		}


		Property literalProperty = ResourceFactory.createProperty(usedParam.get(LITERAL_PROPERTY));
		//find new Relations in this related Literals
		StmtIterator stItr = model.listStatements(null, literalProperty, (RDFNode) null);
		logger.info("--------------- Added triples through STANFORD NLP ---------------");
		while (stItr.hasNext()) {

			Statement st = stItr.nextStatement();
			RDFNode object = st.getObject();
			RDFNode subject = st.getSubject();
			try{
				if(object.isLiteral()){
					Literal text = object.asLiteral();
					if (text.getLanguage().equals("en")) {
						Model namedEntityModel = getModelFromStanfordRE((Resource) subject, text.getString());
						resultModel = resultModel.union(namedEntityModel);
						resultModel.setNsPrefixes(namedEntityModel.getNsPrefixMap());
					}
				}
			}catch (Exception e) {
				logger.error(e);
				logger.error(object.toString());
			}
		}
		//replace Entities with prefered annotation from preferences PREFERED_ANNOTATION
		//delete all annotation if said so in Preferences FILTER_ANNOTATIONS

		logger.info("Replace annotated Entities and delete all annotations if FILTER_ANNOTATIONS is set in Prefs. ");
		resultModel = mapResourcesOnPreferedAnnotationModel(resultModel);

		return resultModel;
	}


	// Stanford Search
	// ________________

	public enum StanfordRelations {

		Live_In("Live_In"),
		Located_In("Located_In"),
		OrgBased_In("OrgBased_In"),
		Work_For("Work_For"),
		NoRelation("_NR");

		private String label;

		private StanfordRelations(String text) {
			this.label = text;
		}

		@Override
		public String toString() {
			return this.label;
		}

		public static StanfordRelations fromString(String label) {
			if (label != null)
				for (StanfordRelations b : StanfordRelations.values())
					if (label.equalsIgnoreCase(b.label))
						return b;
			return null;
		}
	}

	/*
    Stanford relations (http://www.cnts.ua.ac.be/conll2004/pdf/00108rot.pdf)

    located in      loc loc (New York, US)
    work for        per org (Bill Gates, Microsoft)
    orgBased in     org loc (HP, Palo Alto)
    live in         per loc (Bush, US)
    */
//	public boolean checkrules(RelationMention relationMention) {
//		boolean valid = false;
//		if (relationMention.getType() != null && relationMention.getType() != StanfordRelations.NoRelation.toString()) {
//			List<EntityMention> entities = relationMention.getEntityMentionArgs();
//			if (entities.size() != 2) {
//				logger.warn("EntityMention for relation is not 2!");
//				logger.warn(entities);
//			} else {
//				EntityMention emOne = entities.get(0);
//				EntityMention emTwo = entities.get(1);
//
//				StanfordRelations stanfordRelation = StanfordRelations.fromString(relationMention.getType());
//
//				if (logger.isTraceEnabled())
//					logger.trace(stanfordRelation + "(" + emOne.getType() + " " + emTwo.getType() + ")" + " (" + emOne.getValue() + " " + emTwo.getValue() + ")");
//
//				switch (stanfordRelation) {
//					case Live_In:
//						if (EntityClassMap.P.equals(EntityClassMap.stanford(emOne.getType())) &&
//								EntityClassMap.L.equals(EntityClassMap.stanford(emTwo.getType())))
//							valid = true;
//						break;
//
//					case Work_For:
//						if (EntityClassMap.P.equals(EntityClassMap.stanford(emOne.getType())) &&
//								EntityClassMap.O.equals(EntityClassMap.stanford(emTwo.getType())))
//							valid = true;
//						break;
//					case OrgBased_In:
//						if (EntityClassMap.O.equals(EntityClassMap.stanford(emOne.getType())) &&
//								EntityClassMap.L.equals(EntityClassMap.stanford(emTwo.getType())))
//							valid = true;
//						break;
//					case Located_In:
//						if (EntityClassMap.L.equals(EntityClassMap.stanford(emOne.getType())) &&
//								EntityClassMap.L.equals(EntityClassMap.stanford(emTwo.getType())))
//							valid = true;
//						break;
//					default: {
//					}
//				}
//			}
//		}
//		return valid;
//
//	}

	/**
	 * Get new to subject related Properties and Relation in given text.
	 * @autor wotto
	 * based on fox Class REStanford from @author rspeck
	 * paper of stanford relation extraction:
	 * https://github.com/stanfordnlp/CoreNLP/blob/672d43a9677272fdef79ef15f3caa9ed7bc26164/src/edu/stanford/nlp/ie/machinereading/domains/roth/RothCONLL04Reader.java
	 *
	 * @param subject The Subject to whom the found Entities are related
	 * @param text Text in which related Resources should be found
	 * @return Model with all new Entities and Relations
	 */
	public Model getModelFromStanfordRE(Resource subject, String text) {
		Model namedEntitymodel = ModelFactory.createDefaultModel();

		//Map<StanfordRelations, List<URI>> relationURIs               = new HashMap<>();


		// ----------------------------------------------------------------------------
		// find relations
		// ----------------------------------------------------------------------------
		try {
			logger.info("Start...");
			Annotation doc = new Annotation(text);
			logger.debug("Annotate the doc...");
			stanfordNLP.annotate(doc);
			logger.debug("RelationExtractorAnnotator the doc...");
			relationExtractorAnnotator.annotate(doc);
			logger.debug("For all relation ...");
			for (CoreMap sentenceAnnotation : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
				List<RelationMention> relationMentions = (sentenceAnnotation.get(RelationMentionsAnnotation.class));
				logger.debug("relationMentions.size():" + relationMentions.size());
				for (RelationMention relationMention : relationMentions) {
//					if (checkrules(relationMention)) {
					if (!relationMention.getType().equals("_NR") ) { //No Relation is best matching relation
						Model newRelationModel = getModelFromRelationMention(relationMention, text);
						namedEntitymodel = ModelFactory.createUnion(namedEntitymodel,newRelationModel);
//						namedEntitymodel.setNsPrefixes(newRelationModel.getNsPrefixMap());
						if (logger.isDebugEnabled()) {
							logger.debug(relationMention);
						}
					}
				}
			}
			logger.info("Relations done.");
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}

		return namedEntitymodel;
	}


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
		Property annotates = ResourceFactory.createProperty(prefMap.get("ann") + "annotate");
		//RELATION
		Property relation = ResourceFactory.createProperty(prefMap.get("stanford"), relationMention.getType());

		//AGENS
		EntityMention agensMention = relationMention.getEntityMentionArgs().get(0);
		Resource agens = ResourceFactory.createResource(prefMap.get("stanford") + agensMention.getExtentString());
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
		Resource patiens = ResourceFactory.createResource(prefMap.get("stanford") + patiensMention.getValue());
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
		logger.info(annotationModel);
        relationModel = ModelFactory.createUnion(relationModel, annotationModel);
		relationModel.setNsPrefixes(annotationModel.getNsPrefixMap());


		return relationModel;
	}


	public Model mapResourcesOnPreferedAnnotationModel(Model annotatedModel) {
		if (usedParam.get(PREFERED_ANNOTATION) != null) {
			Property preferedAnnotation = ResourceFactory.createProperty("http://ns.aksw.org/scms/tools/Spotlight");
			String queryString =
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
							"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  "+
							"PREFIX scms: <http://ns.aksw.org/scms/>" +
							"PREFIX ann: <http://www.w3.org/2000/10/annotation-ns#>" +
							"select  ?entityToDelete ?typeToDelete ?annToFilter ?entityToPrefere "  +
							"{ ?annPref rdf:type ann:Annotation . " +
							"	?annPref ann:annotates ?textBase . " +
							"	?annPref scms:beginIndex ?beginning ." +
							"	?annPref scms:endIndex ?ending ." +
							"	?annPref scms:means ?entityToPrefere ." +
							"	?annPref scms:source <" + preferedAnnotation.getNameSpace() + preferedAnnotation.getLocalName() + "> ." +
							"	?annToFilter rdf:type ann:Annotation . " +
							"	?annToFilter ann:annotates ?textBase . " +
							"	?annToFilter scms:beginIndex ?beginning ." +
							"	?annToFilter scms:endIndex ?ending ." +
							"	?annToFilter scms:means ?entityToDelete ." +
							"	?entityToDelete rdf:type ?typeToDelete ." +
							"   FILTER (?entityToPrefere != ?entityToDelete)" +
							"}\n ";
			Query query = QueryFactory.create(queryString);
//			"http://ns.aksw.org/scms/tools/Spotlight";

			// Execute the query and obtain results
			QueryExecution qe = QueryExecutionFactory.create(query, annotatedModel);
			ResultSet results =  qe.execSelect();
			//Delete all Annotations with entityToDelete as means
			List<QuerySolution> rows = ResultSetFormatter.toList(results);
			qe.close();
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
		}
		// if any Annotation have to be filtered do this.
		if (usedParam.get(FILTER_ANNOTATIONS) != null && usedParam.get(FILTER_ANNOTATIONS).toLowerCase().equals("true")) {
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
		if(evaluationMetaData) {
			logger.info("Evaluation: store all extracted Triples seperate. start...");
			addMappedExtractesTriplesFromModel(annotatedModel);
			logger.info("Evaluation: store all extracted Triples seperate. end");
		}

		return annotatedModel;
	}
	/* (non-Javadoc)
	 * Get the possible Parameters as List
	 * @see org.aksw.geolift.modules.GeoLiftModule#getParameters()
	 */
	public List<String> getParameters() {
		List<String> parameters = new ArrayList<String>();
		for (String param: usedParam.keySet()) {
			parameters.add(param);
		}
		return parameters;
	}

	/* (non-Javadoc)
	 * No Properties necessary
	 * @see org.aksw.geolift.modules.GeoLiftModule#getNecessaryParameters()
	 */
	@Override
	public List<String> getNecessaryParameters() {
		List<String> parameters = new ArrayList<String>();
		return parameters;
	}


	/**
	 * Self configuration
	 * Set all parameters to default values, also extract all NEs
	 * @param source
	 * @param target
	 * @return Map of (key, value) pairs of self configured parameters
	 * @author sherif
	 */
	public Map<String, String> selfConfig(Model source, Model target) {

		//		Set<Resource> uriObjects = getDiffUriObjects(source, target);

		Map<String, String> p = new HashMap<String, String>();
//		p.put(NER_TYPE, ALL);
		return p;
	}

	//@todo update main
	public static void main(String args[]) throws IOException {
		StanfordModule stanfordEnricher= new StanfordModule();

		Map<String, String> parameters = new HashMap<String, String>();

//		// set parameters from command line
		for(int i=0; i<args.length; i+=2){
			HashMap<String, String> wantedParams = new HashMap<String, String>();
			List<String> params = stanfordEnricher.getParameters();
			for (String param: params) {
				if (("--"+param.toLowerCase()).equals(args[i].toLowerCase())){
					wantedParams.put(param, args[i+1]);
				}
			}

//			if(args[i].equals("-?") || args[i].toLowerCase().equals("--help")){
//				logger.info(
//						"Basic parameters:\n" +
//								"\t-i --input: input file/URI" + "\n" +
//								"\t-o --output: output file/URI" + "\n" +
//								"\t-p --literalProperty: literal property used for NER" + "\n" +
			//								"Spotlight parameters (current version use always default values, which is the first one):\n"+
//								"\t--foxType: { text | url }" + "\n" +
//								"\t--foxTask: { NER }" + "\n" +
//								"\t--foxInput: text or an url" + "\n" +
//								"\t--foxOutput: { JSON-LD | N-Triples | RDF/{ JSON | XML } | Turtle | TriG | N-Quads}" + "\n" +
//								"\t--foxUseNif: { false | true }" + "\n" +
//						"\t--foxReturnHtml: { false | true }" );
//				System.exit(0);
//			}
		}
		if(!parameters.containsKey("input")){
			logger.error("No input file/URI, Exit with error!!");
			System.exit(1);
		}

		Model enrichedModel = stanfordEnricher.process(null, parameters);

		if(!parameters.containsKey("output")){
			logger.info("Enriched MODEL:");
			logger.info("---------------");
			enrichedModel.write(System.out,"TTL");
		}
	}

	//@todo update this method
	@Override
	public List<ParameterType> getParameterWithTypes() {
		List<ParameterType> parameters = new ArrayList<ParameterType>();
//		parameters.add(new ParameterType(ParameterType.STRING, LITERAL_PROPERTY, LITERAL_PROPERTY_DESC, false));
//		parameters.add(new ParameterType(ParameterType.STRING, ADDED_PROPERTY, USE_FOX_LIGHT_VALUES, ADDED_PROPERTY_DESC, false));
//		parameters.add(new ParameterType(ParameterType.STRING, USE_FOX_LIGHT, USE_FOX_LIGHT_DESC, false));
//		parameters.add(new ParameterType(ParameterType.BOOLEAN, ASK_END_POINT, ASK_END_POINT_DESC, false));
//		parameters.add(new ParameterType(ParameterType.STRING, NER_TYPE, NER_TYPE_VALUES, NER_TYPE_DESC, false));
		return parameters;
	}

	@Override
	public Resource getType(){
		//TODO wrong add this module to specs
		return SPECS.NLPModule;
	}

}