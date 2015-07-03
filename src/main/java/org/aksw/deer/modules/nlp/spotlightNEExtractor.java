package org.aksw.deer.modules.nlp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by wolfo on 02.07.15.
 */
public class SpotlightNEExtractor implements NLPExtractor {

    private static final Logger logger = Logger.getLogger(MultipleExtractorNLPModule.class.getName());

    private HashMap<String, String> usedParam = new HashMap<String, String>();


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
            "http://spotlight.dbpedia.org/rest/annotate/";

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

    private void initDefaultParams() {

    }


    @Override
    public HashMap<String, String> getDefaultParam() {
        HashMap<String, String> defaultParam = new HashMap<String, String>();
        defaultParam.put(LITERAL_PROPERTY, LITERAL_PROPERTY_DEFAULT);
        defaultParam.put(ADDED_PROPERTY, ADDED_PROPERTY_DEFAULT);
        defaultParam.put(USED_SPOTLIGHT_IMPLEMENTATION, USE_SPOTLIGHT_IMPLEMENTATION_DEFAULT);
        defaultParam.put(SPOTLIGHT_API_URL, SPOTLIGHT_API_URL_DEFAULT);
        defaultParam.put(SPOTLIGHT_CONFIDENCE, SPOTLIGHT_CONFIDENCE_DEFAULT);
        defaultParam.put(SPOTLIGHT_SUPPORT, SPOTLIGHT_SUPPORT_DEFAULT);
        defaultParam.put(SPOTLIGHT_POLICY, SPOTLIGHT_POLICY_DEFAULT);
        defaultParam.put(SPOTLIGHT_TYPES, SPOTLIGHT_TYPES_DEFAULT);
        defaultParam.put(SPOTLIGHT_SPRQL, SPOTLIGHT_SPRQL_DEFAULT);

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

    public SpotlightNEExtractor() {
        super();
        usedParam = getDefaultParam();
    }
    public SpotlightNEExtractor(HashMap<String, String> params) {
        super();
        addParams(params);
    }

    @Override
    public Model extractFromText(Literal textLiteral) {
        Model enrichedModel = extractFromText(null, textLiteral);

        return enrichedModel;
    }

    @Override
    public Model extractFromText(Resource recConnectedToText, Literal textLiteral) {
        logger.info("spotlight");
        Model enrichedModel = ModelFactory.createDefaultModel();
        HashMap<String,Object> result = null;
        String text = textLiteral.getString();

        try {

            String buffer = "", line;
//			text=refineString(text);
//			Construct data
            String data = "disambiguator=Document";
            data += 	"&text=" 		+ URLEncoder.encode(text, "UTF-8");
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
            URL url = new URL(usedParam.get(SPOTLIGHT_API_URL));
//			logger.info("Spotlight Request:" + url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();


            //add request header
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("content-type","application/x-www-form-urlencoded");
            // Send post request
            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();

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

            enrichedModel = hashMapAnswerToModel(recConnectedToText, result);

        } catch (Exception e) {
            logger.error("SPOTLIGHT rest endpoint Exception: " + e);
            e.printStackTrace();
        }


        return enrichedModel;
    }


    public Model hashMapAnswerToModel(Resource subject, HashMap<String, Object> entities){

        /**
         * 	Fragment of example entities object:
         * 	{	@confidence=0.2,
         *		@text=Queen Elizabeth is the Queen of Great Britain,
         *		@support=20,
         *		@types=,
         *		Resources=
         *		[
         *			{	@URI=http://dbpedia.org/resource/RMS_Queen_Elizabeth,
         *				@support=121,
         *				@types=	DBpedia:Ship,
         *						DBpedia:MeanOfTransportation,
         *						...
         *				@surfaceForm=Queen Elizabeth,
         *				@offset=0,
         *				@similarityScore=0.27637702226638794,
         *				@percentageOfSecondRank=0.7642967609169785
         *			}, ...
         *		],
         *		@sparql=,
         *		@policy=whitelist
         *	}
         */
        HashMap<String, String> prefMap = new HashMap<String, String>();
        prefMap.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefMap.put("ann", "http://www.w3.org/2000/10/annotation-ns#");
        prefMap.put("dbpedia-res", "http://dbpedia.org/resource/");
        prefMap.put("dbpedia-ont", "http://dbpedia.org/ontology/");
        prefMap.put("geo", "http://geoknow.org/ontology/");
        prefMap.put("scms", "http://ns.aksw.org/scms/");
        prefMap.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        // The Model to return
        Model namedEntitymodel = ModelFactory.createDefaultModel();
        namedEntitymodel.setNsPrefixes(prefMap);
        //No Resources Found
        if(!entities.containsKey("Resources")){
            return namedEntitymodel;
        }
        // The Model for annotational Information
        Model annotationModel = ModelFactory.createDefaultModel();
        annotationModel.setNsPrefixes(prefMap);

        ArrayList<HashMap<String, String>> resources = (ArrayList<HashMap<String, String>>) entities.get("Resources");
        String annotatedText = (String) entities.get("@text");
        Property relationProperty = ResourceFactory.createProperty(usedParam.get(ADDED_PROPERTY));

        for (HashMap<String, String> resource: resources) {

            Resource newObject = ResourceFactory.createResource(resource.get("@URI"));
            if (subject != null) {
                namedEntitymodel.add(subject, relationProperty, newObject);
//			logger.info( subject.getURI() + " " + relationProperty.getLocalName()  + " " + newObject.getURI());
                logger.info(subject.getURI() + " " + newObject.getURI());
            }
            numberOfExtractedEntities += 1;
            extractedElements.add(newObject.getURI());
            // add types from resources
            String allTypes = resource.get("@types");
            List<String> typeList = new ArrayList<String>(Arrays.asList(allTypes.split("\\s*,\\s*")));

            Property typeProp = ResourceFactory.createProperty(prefMap.get("rdf") + "type");

            for (String type:typeList) {

                //filter: only dbpedia types

                if (type.startsWith("DBpedia:")){
                    Resource typeRes = ResourceFactory.createResource(prefMap.get("dbpedia-ont") + type.substring(8));
                    namedEntitymodel.add(newObject, typeProp, typeRes);
//					logger.info( newObject.getURI() + " " + typeProp.getLocalName()  + " " + typeRes.getURI());
                }

            }
            String surfaceForm = resource.get("@surfaceForm");
            Integer offset = Integer.parseInt(resource.get("@offset"));
            Integer endIndex = offset + surfaceForm.length();
            Resource annotation = annotationModel.createResource();
            annotationModel.add( annotation,
                    ResourceFactory.createProperty(prefMap.get("rdf") + "type"),
                    ResourceFactory.createResource(prefMap.get("ann") + "Annotation"));
            annotationModel.add(annotation,
                    ResourceFactory.createProperty(prefMap.get("scms"), "body"),
                    ResourceFactory.createTypedLiteral(surfaceForm, XSDDatatype.XSDstring));
            annotationModel.add(annotation,
                    ResourceFactory.createProperty(prefMap.get("scms") + "beginIndex"),
                    ResourceFactory.createTypedLiteral(offset.toString(), XSDDatatype.XSDint));
            annotationModel.add( annotation,
                    ResourceFactory.createProperty(prefMap.get("scms"), "endIndex"),
                    ResourceFactory.createTypedLiteral(endIndex.toString(), XSDDatatype.XSDint));
            annotationModel.add(annotation,
                    ResourceFactory.createProperty(prefMap.get("scms"), "means"),
                    newObject);
            annotationModel.add(annotation,
                    ResourceFactory.createProperty(prefMap.get("scms"), "source"),
                    ResourceFactory.createResource(prefMap.get("scms") + "tools/Spotlight"));
            annotationModel.add( annotation,
                    ResourceFactory.createProperty(prefMap.get("ann"), "annotates"),
                    ResourceFactory.createTypedLiteral(annotatedText, XSDDatatype.XSDstring));

        }

        Model returnModel = ModelFactory.createUnion(namedEntitymodel, annotationModel);

        return returnModel;
    }


    //For Evaluation
    private Integer numberOfExtractedEntities = 0;
    private Set<String> extractedElements = new HashSet<String>();

}
