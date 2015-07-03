package org.aksw.deer.modules.nlp;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import java.util.HashMap;

/**
 * Created by wolfo on 02.07.15.
 */
public interface NLPExtractor {

    public HashMap<String, String> getDefaultParam ();

    public void addParams(HashMap<String, String> newParams);

    public Model extractFromText(Literal textLiteral);

    public Model extractFromText(Resource recConnectedToText, Literal textLiteral);



}
