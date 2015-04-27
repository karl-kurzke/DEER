package org.aksw.deer.modules.nlp;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.rdf.model.*;
import org.aksw.deer.helper.vacabularies.SCMSANN;
import org.aksw.deer.helper.vacabularies.SPECS;
import org.aksw.deer.io.Reader;
import org.aksw.deer.json.ParameterType;
import org.aksw.deer.modules.DeerModule;
import org.aksw.fox.binding.java.FoxApi;
import org.aksw.fox.binding.java.FoxParameter;
import org.aksw.fox.binding.java.FoxResponse;
import org.aksw.fox.binding.java.IFoxApi;
import org.apache.log4j.Logger;


import java.io.*;
import java.net.*;
import java.util.*;


/**
 *
 * @author wo
 * @mail otto@studserv.uni-leipzig.de
 * @author sherif
 */
public class SpotlightModule implements DeerModule{
	private static final Logger logger = Logger.getLogger(SpotlightModule.class.getName());

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

	public static final String USED_SPOTLIGHT_IMPLEMENTATION = "usedSpotlightImplementation";
	public static final String USED_SPOTLIGHT_IMPLEMENTATION_DESC =
			"Select the spotlight Version you want to use. " +
					"Default: restEndPoint";
	public static final String USED_SPOTLIGHT_IMPLEMENTATION_VALUES =
			"restEndPoint, spotlightOnThisMachine";
	public static final String USE_SPOTLIGHT_IMPLEMENTATION_DEFAULT =
			"restEndPoint";

	public static final String SPOTLIGHT_API_URL = "spotlightApiUrl";
	public static final String SPOTLIGHT_API_URL_DESC =
			"Defines the used spotlight rest endpoint." +
					"Default: \"http://spotlight.dbpedia.org/rest/annotate\"";
	public static final String SPOTLIGHT_API_URL_DEFAULT =
			"http://spotlight.dbpedia.org/rest/annotate";

	// SPOTLIGHT CONFIGURATION
	public static final String SPOTLIGHT_CONFIDENCE = "spotlightConfidence";
	public static final String SPOTLIGHT_CONFIDENCE_DESC =
			"Value between 0 and 1 which defines " +
					"minimum confidence for Entities found by Spotlight ";
	public static final String SPOTLIGHT_CONFIDENCE_DEFAULT = "0.5";

	public static final String SPOTLIGHT_SUPPORT = "spotlightSupport";
	public static final String SPOTLIGHT_SUPPORT_DESC =
//			@todo Find out what it is for
			"Integer Value";
	public static final String SPOTLIGHT_SUPPORT_DEFAULT = "20";

	public static final String SPOTLIGHT_POLICY = "spotlightPolicy";
	public static final String SPOTLIGHT_POLICY_DESC =
//			@todo Find out what it is for
			"NIY: ";
	public static final String SPOTLIGHT_POLICY_VALUES =
			"blacklist, whitelist";
	public static final String SPOTLIGHT_POLICY_DEFAULT = null;



	//@todo implement later
	public static final String SPOTLIGHT_TYPES = "spotlightTypes";
	public static final String SPOTLIGHT_TYPES_DESC =
			"Force Spotlight to look for a specific NE’s types only. " +
					"If not defined all possible resources will be extracted. " +
					//@todo implement and look if you realy can use all types
					"All dbpedia Type Ressources are valid.";
	public static final String SPOTLIGHT_TYPES_DEFAULT = null;


	//@todo implement sprql
	public static final String SPOTLIGHT_SPRQL = "spotlightSparql";
	public static final String SPOTLIGHT_SPRQL_DESC =
			"NIY";
	public static final String SPOTLIGHT_SPRQL_DEFAULT = null;

//	//@todo implement n best (How on endpoint?)
//	public static final String SPOTLIGHT_N_Best_DESC =
//
//					"NIY: Select more then one Entity if " +
//					"spotlight finds more then one ";








	private Model model;

	private HashMap<String, String> usedParam;

	private void initDefaultParams() {
		usedParam = new HashMap<String, String>();
		usedParam.put(LITERAL_PROPERTY, LITERAL_PROPERTY_DEFAULT);
		usedParam.put(ADDED_PROPERTY, ADDED_PROPERTY_DEFAULT);
		usedParam.put(USED_SPOTLIGHT_IMPLEMENTATION, USE_SPOTLIGHT_IMPLEMENTATION_DEFAULT);
		usedParam.put(SPOTLIGHT_API_URL, SPOTLIGHT_API_URL_DEFAULT);
		usedParam.put(SPOTLIGHT_CONFIDENCE, SPOTLIGHT_CONFIDENCE_DEFAULT);
		usedParam.put(SPOTLIGHT_SUPPORT, SPOTLIGHT_SUPPORT_DEFAULT);
		usedParam.put(SPOTLIGHT_POLICY, SPOTLIGHT_POLICY_DEFAULT);
		usedParam.put(SPOTLIGHT_TYPES, SPOTLIGHT_TYPES_DEFAULT);
		usedParam.put(SPOTLIGHT_SPRQL, SPOTLIGHT_SPRQL_DEFAULT);
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
	 * @param model
	 * @param literalProperty
	 *@author sherif
	 */
	public SpotlightModule(Model model, Property literalProperty) {
		super();
		this.model = model;
		initDefaultParams();
		this.usedParam.put(LITERAL_PROPERTY, literalProperty.getURI());

	}

	/**
	 * 
	 * @param fileNameOrUri
	 * @param literalPropertyURI
	 */
	public SpotlightModule(String fileNameOrUri, String literalPropertyURI) {
		super();
		this.model = Reader.readModel(fileNameOrUri);
		initDefaultParams();
		this.usedParam.put(LITERAL_PROPERTY, literalPropertyURI);

	}
	/**
	 *
	 *@author sherif
	 */
	public SpotlightModule(Model model) {
		super();
		initDefaultParams();
		this.model = model;

	}
	/**
	 *
	 *
	 */
	public SpotlightModule() {
		super();
		initDefaultParams();
		this.model = null;

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
		logger.info("--------------- SPOTLIGHT NLP Module ---------------");
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
			logger.info("Top ranked Literal Property: " + topRankedProp);
			usedParam.put(LITERAL_PROPERTY, topRankedProp.getURI());
		}


		Property literalProperty = ResourceFactory.createProperty(usedParam.get(LITERAL_PROPERTY));
		//find new Relations in this related Literals
		StmtIterator stItr = model.listStatements(null, literalProperty, (RDFNode) null);
		logger.info("--------------- Added triples through SPOTLIGHT NLP ---------------");
		while (stItr.hasNext()) {

			Statement st = stItr.nextStatement();
			RDFNode object = st.getObject();
			RDFNode subject = st.getSubject();
			try{
				if(object.isLiteral()){
					Model namedEntityModel = getModelFromSpotlightRest((Resource) subject, object.toString());
					resultModel = resultModel.union(namedEntityModel);

				}
			}catch (Exception e) {
				logger.error(e);
				logger.error(object.toString());
			}
		}
		return resultModel;
	}


	// Spotlight Search
	// ________________




	/**
	 * Get new to subject related Properties in given text.
	 *
	 * @param subject The Subject to whom the found Entities are related
	 * @param text Text in which related Resources should be found
	 * @return Model with all new Entities and Relations
	 */
	public Model getModelFromSpotlightRest(Resource subject, String text) {
		return HashMapAnswerToModel(subject, getEntityHashMapFromSpotlightRest(text));

	}

	/**
	 * Get Request from SoptlightRest
	 * @param text text with probably new entities
	 * @return HashMap with new Entities
	 */
	public HashMap<String, Object> getEntityHashMapFromSpotlightRest(String text){
		HashMap<String,Object> result = null;
		try {
			String buffer = "", line;
//			text=refineString(text);

			// Construct data
			String data = 	"?text=" 		+ URLEncoder.encode(text, "UTF-8");
			if (usedParam.get(SPOTLIGHT_CONFIDENCE) != null) {
				data += "&confidence=" + URLEncoder.encode(usedParam.get(SPOTLIGHT_CONFIDENCE), "UTF-8");
			}
			if (usedParam.get(SPOTLIGHT_SUPPORT) != null) {
				data += "&support=" + URLEncoder.encode(usedParam.get(SPOTLIGHT_SUPPORT), "UTF-8");
			}
			//@todo implement these parameter
//			data += 		"&policy=" 	+ URLEncoder.encode(support, "UTF-8");
//			data += 		"&types=" 	+ URLEncoder.encode(support, "UTF-8");
//			data += 		"&sparql=" 	+ URLEncoder.encode(support, "UTF-8");

			// Send data
			URL url = new URL(usedParam.get(SPOTLIGHT_API_URL) + data);
//			logger.info("Spotlight Request:" + url.toString());
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("Accept", "application/json");

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				buffer = buffer + line + "\n";
			}
			rd.close();
			//		Get HashMap out of json
			result = new ObjectMapper().readValue(buffer, HashMap.class);
			//todo use jena riot to get modelfrom json
//			org.openjena.riot.RIOT.init(); //wires RIOT readers/writers into Jena
//			java.io.OutputStream os = null;
//			// Serialize over an outputStream
//			os = new java.io.ByteArrayOutputStream();
//			model.write(os, "RDF/JSON", relativeUriBase);
		} catch (Exception e) {
			logger.error("SPOTLIGHT rest endpoint Exception: " + e);
			e.printStackTrace();
		}

		return result;
	}


	public Model HashMapAnswerToModel(Resource subject, HashMap<String, Object> entities){
		Model namedEntitymodel = ModelFactory.createDefaultModel();
		ArrayList<HashMap<String, String>> resources = (ArrayList<HashMap<String, String>>) entities.get("Resources");
		Property relationProperty = ResourceFactory.createProperty(usedParam.get(ADDED_PROPERTY));
		for (HashMap<String, String> resource: resources) {
//			logger.info( subject.getURI() + " " + relationProperty.getLocalName()  + " " + resource.get("@URI"));

			Resource newObject = ResourceFactory.createResource(resource.get("@URI"));
			namedEntitymodel.add(subject, relationProperty, newObject);
			// add types from resources
			String allTypes = resource.get("@types");
			List<String> typeList = new ArrayList<String>(Arrays.asList(allTypes.split("\\s*,\\s*")));
			Property typeProp = ResourceFactory.createProperty("owl:a");
			for (String type:typeList) {
				//filter: only dbpedia types
//				logger.info(type);
				if (type.startsWith("DBpedia")){
					Resource typeRes = ResourceFactory.createResource(type);
					namedEntitymodel.add(newObject, typeProp, typeRes);
				}

			}



		}
		return namedEntitymodel;
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
		SpotlightModule spotlightEnricher= new SpotlightModule();

		Map<String, String> parameters = new HashMap<String, String>();

//		// set parameters from command line
		for(int i=0; i<args.length; i+=2){
			HashMap<String, String> wantedParams = new HashMap<String, String>();
			List<String> params = spotlightEnricher.getParameters();
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

		Model enrichedModel = spotlightEnricher.process(null, parameters);

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

		return SPECS.NLPModule;
	}

}