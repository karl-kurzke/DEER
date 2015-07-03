package org.aksw.deer.modules.nlp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.*;
import org.aksw.deer.helper.vacabularies.SPECS;
import org.aksw.deer.io.Reader;
import org.aksw.deer.json.ParameterType;
import org.aksw.deer.modules.DeerModule;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by wolfo on 02.07.15.
 */
public class MultipleExtractorNLPModule implements DeerModule{


    private static final Logger logger = Logger.getLogger(MultipleExtractorNLPModule.class.getName());

    //GENERAL CONFIGURATION
    public static final String INPUT 	= "input";
    public static final String INPUT_DESC =
            "If you want to load data from file define the filename in INPUT";
    public static final String INPUT_DEFAULT = null;
    public static final String OUTPUT 	= "output";
    public static final String OUTPUT_DESC =
            "If you want to save all data after extraction into file define the filename in OUTPUT";
    public static final String OUTPUT_DEFAULT = null;

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

    public static final String USED_EXTRACTOR = "usedExtractor";
    public static final String USE_EXTRACTOR_DESCRIPTION =
            "Comma separated List of Classnames of Extractor. Implemented: " +
                    "StanfordREExtractor, SpotlightNEExtractor";
    public static final String USED_EXTRACTOR_DEFAULT = "StanfordREExtractor, SpotlightNEExtractor";




    private Model model = null;

    private ArrayList<NLPExtractor> extractors = null;

    private AnnotationCombiner annoComb = null;
    //For Evaluation
    private Integer numberOfExtractedEntities = 0;
    private Set<String> extractedElements = new HashSet<String>();

    private HashMap<String, String> usedParam;

    private void initDefaultParams() {
        usedParam = new HashMap<String, String>();
        usedParam.put(LITERAL_PROPERTY, LITERAL_PROPERTY_DEFAULT);
        usedParam.put(ADDED_PROPERTY, ADDED_PROPERTY_DEFAULT);
        usedParam.put(INPUT, INPUT_DEFAULT);
        usedParam.put(OUTPUT, OUTPUT_DEFAULT);
        usedParam.put(USED_EXTRACTOR, USED_EXTRACTOR_DEFAULT);
        for (NLPExtractor extr: extractors) {
            usedParam.putAll(extr.getDefaultParam());
        }
        usedParam.putAll(this.annoComb.getDefaultParam());
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

    // CONSTRUCTOR
    // ___________
    /**
     *
     */
    public MultipleExtractorNLPModule() {
        this.extractors = new ArrayList<NLPExtractor>();
        this.extractors.add(new SpotlightNEExtractor());
        this.extractors.add(new StanfordREExtractor());
        this.annoComb = new AnnotationCombiner();
        initDefaultParams();
    }
    /**
     *
     */
    public MultipleExtractorNLPModule(Model model) {
        this();
        this.model = model;
    }
    /**
     *
     */
    public MultipleExtractorNLPModule(Model model, Property literalProperty) {
        this();
        this.model = model;
        initDefaultParams();
        this.usedParam.put(LITERAL_PROPERTY, literalProperty.getURI());
    }
    /**
     *
     */
    public MultipleExtractorNLPModule(String fileNameOrUri, String literalPropertyURI) {
        this();
        this.model = Reader.readModel(fileNameOrUri);
        this.usedParam.put(LITERAL_PROPERTY, literalPropertyURI);
    }



    // SETTER, GETTER
    // ______________

    /**
     * For Evaluation Purpose
     * @return the relatedToProperty
     */
    public Integer getNumberOfExtractedEntities() {
        return numberOfExtractedEntities;
    }
    public Integer getNumberOfDistinctExtractedEntities() {
        return extractedElements.size();
    }
    public Set<String> getExtractedElements() {return extractedElements;}

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
        logger.info("--------------- MULTIPLE EXTRACTOR NLP Module ---------------");
        model = inputModel;
        if( parameters.containsKey(INPUT) && parameters.get(INPUT) != null){
            model = Reader.readModel(parameters.get(INPUT));
        }
        //import parameters
        this.addParams((HashMap) parameters);


        Model newModel = getNewTripleAsModel();

        model = ModelFactory.createUnion(model, newModel);

//		model.setNsPrefixes(newModel.getNsPrefixMap());

        if( parameters.containsKey(OUTPUT) && parameters.get(OUTPUT) != null){
            String outputFile = parameters.get(OUTPUT);
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
            logger.info("Top ranked Literal Property: " + topRankedProp);
            usedParam.put(LITERAL_PROPERTY, topRankedProp.getURI());
        }


        Property literalProperty = ResourceFactory.createProperty(usedParam.get(LITERAL_PROPERTY));
        logger.info("Used Literal Property: " + literalProperty.toString());
        //find new Relations in this related Literals
        StmtIterator stItr = model.listStatements(null, literalProperty, (RDFNode) null);
        logger.info("--------------- Added triples through MULTIPLE NLP EXTRACTORS ---------------");
        while (stItr.hasNext()) {

            Statement st = stItr.nextStatement();
            RDFNode object = st.getObject();
            RDFNode subject = st.getSubject();
            try{
                if(object.isLiteral()){

                    Literal currentLit = object.asLiteral();

                    Model enrichedModel = ModelFactory.createDefaultModel();
                    HashSet<String> extractorToUse = new HashSet<String>();
                    for(String ext: usedParam.get(USED_EXTRACTOR).split(",")){extractorToUse.add(ext.trim());}
                    logger.info(extractorToUse);
                    for (NLPExtractor ext: extractors) {
                            logger.info(ext.getClass().getSimpleName());
                        if (extractorToUse.contains(ext.getClass().getSimpleName())) {
                            logger.info("grrr");
                            ext.addParams(usedParam);
                            enrichedModel  = ModelFactory.createUnion(
                                    ext.extractFromText((Resource) subject, currentLit),
                                    enrichedModel);
                        }
                    }

                    enrichedModel = annoComb.combineAnnotation(enrichedModel, usedParam);
                    resultModel = ModelFactory.createUnion(enrichedModel, resultModel);

                }
            }catch (Exception e) {
                logger.error(e);
                logger.error(object.toString());
            }

        }
        logger.info("All Text Extracted.");

        return resultModel;
    }


    //@todo update main
    public static void main(String args[]) throws IOException {
        MultipleExtractorNLPModule enricher= new MultipleExtractorNLPModule();

        Map<String, String> parameters = new HashMap<String, String>();

//		// set parameters from command line
        for(int i=0; i<args.length; i+=2){
            HashMap<String, String> wantedParams = new HashMap<String, String>();
            List<String> params = enricher.getParameters();
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
        if(!parameters.containsKey(INPUT)){
            logger.error("No input file/URI, Exit with error!!");
            System.exit(1);
        }

        Model enrichedModel = enricher.process(null, parameters);

        if(!parameters.containsKey(OUTPUT)){
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
