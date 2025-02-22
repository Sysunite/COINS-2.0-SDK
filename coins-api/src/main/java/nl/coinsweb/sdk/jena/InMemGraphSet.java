/**
 * MIT License
 *
 * Copyright (c) 2016 Bouw Informatie Raad
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 **/
package nl.coinsweb.sdk.jena;


import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateRequest;
import nl.coinsweb.sdk.CoinsGraphSet;
import nl.coinsweb.sdk.Namespace;
import nl.coinsweb.sdk.validator.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 *
 * The Coins GraphSet is the provider of the rdf storage of all the models that are
 * contained in the container. Each model is identified by a namespace (uri).
 *
 * The Jena GraphSet has a setter for a reasoner. Different situations in the
 * Coins Api require a different reasoner.
 *
 * @author Bastian Bijl
 */
public class InMemGraphSet implements CoinsGraphSet {

  private static final Logger log = LoggerFactory.getLogger(InMemGraphSet.class);

  //  OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM_MICRO_RULE_INF;
  public static OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM_RDFS_INF;
  //  OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM;

  protected Namespace instanceNamespace;
  protected Model instanceModel;
  protected Model fullUnionModel;

  public static final String INSTANCE_GRAPH = "http://coinsweb.nl/INSTANCE_GRAPH";
  public static final String WOA_GRAPH = "http://coinsweb.nl/WOA_GRAPH";
  public static final String SCHEMA_GRAPH = "http://coinsweb.nl/SCHEMA_GRAPH";
  public static final String SCHEMA_UNION_GRAPH = "http://coinsweb.nl/SCHEMA_UNION_GRAPH";
  public static final String FULL_UNION_GRAPH = "http://coinsweb.nl/FULL_UNION_GRAPH";



  public Namespace woaNamespace = new Namespace("http://woa.coinsweb.nl/");
  protected Model woaModel;

  protected Map<Namespace, Model> libraryModels;

  private Dataset dataset = null;
  private Dataset validationDataset = null;



  public InMemGraphSet(String namespace) {
    this.instanceNamespace = new Namespace(namespace);
    this.libraryModels = new HashMap<>();
    this.instanceModel = getEmptyModel();
    this.woaModel = getEmptyModel();
  }


  public void setOntModelSpec(OntModelSpec modelSpec) {
    ontModelSpec = modelSpec;
  }

  @Override
  public Map<Namespace, Model> getLibraryModels() {
    return libraryModels;
  }

  @Override
  public void reset() {
    this.libraryModels = new HashMap<>();
  }



  // Namespaces

  @Override
  public Iterator<String> listModelNames() {
    List<String> buffer = new ArrayList<>();
    buffer.add(instanceNamespace.toString());
    buffer.add(woaNamespace.toString());
    Set<Namespace> namespaces = libraryModels.keySet();
    for(Namespace namespace : namespaces) {
      buffer.add(namespace.toString());
    }
    return buffer.iterator();
  }

  @Override
  public void setInstanceNamespace(String namespace) {
    this.instanceNamespace = new Namespace(namespace);
  }

  @Override
  public String getInstanceNamespace() {
    return this.instanceNamespace.toString();
  }

  @Override
  public String getInstanceNamespaceWithoutHash() {
    return this.instanceNamespace.withoutHash();
  }

  @Override
  public void setWoaNamespace(String namespace) {
    this.woaNamespace = new Namespace(namespace);
  }

  @Override
  public String getWoaNamespace() {
    return this.woaNamespace.toString();
  }

  @Override
  public String getFullUnionNamespace() {
    return FULL_UNION_GRAPH;
  }






  // Models

  @Override
  public Model getEmptyModel() {
    return ModelFactory.createDefaultModel();
  }

  @Override
  public Model readModel(String url) {
    Model model = null;
    try {
      model = getEmptyModel().read(url);
    } catch (java.lang.OutOfMemoryError e) {
      log.error("Out of memory while loading "+url+", please assign more memory, the process will be stopped");
      System.exit(1);
    }
    return model;
  }

  @Override
  public void readModel(Model model, String url) {
    try {
      model.read(url);
    } catch (java.lang.OutOfMemoryError e) {
      log.error("Out of memory while loading "+url+", please assign more memory, the process will be stopped");
      System.exit(1);
    }
  }

  @Override
  public Model getInstanceModel() {
    return instanceModel;
  }

  @Override
  public Model getWoaModel() {
    return woaModel;
  }

  @Override
  public Model getSchemaModel() {
    return getModel("http://www.coinsweb.nl/cbim-2.0.rdf");
  }

  @Override
  public Model getSchemaAggregationModel() {

    Model aggregationModel = getEmptyModel();
    for(Namespace key : libraryModels.keySet()) {
      aggregationModel.add(libraryModels.get(key));
    }
    return aggregationModel;
  }

  @Override
  public Model getFullUnionModel() {
    if(fullUnionModel == null) {
      Model nonInstanceUnion = ModelFactory.createUnion(getSchemaAggregationModel(), woaModel);
      fullUnionModel = ModelFactory.createUnion(instanceModel, nonInstanceUnion);
    }
    return fullUnionModel;
  }

  @Override
  public Model getModel(String namespace) {
    Namespace ns = new Namespace(namespace);

    if(this.instanceNamespace.equals(ns)) {
      log.info("InstanceModel requested.");
      return instanceModel;
    }
    if(this.woaNamespace.equals(ns)) {
      log.info("WoaModel requested.");
      return woaModel;
    }
    if(libraryModels.containsKey(ns)) {
      log.info("Some library model requested.");
      return libraryModels.get(ns);
    }

    log.warn("Requested model could not be found: "+ns.toString()+", pick from:");
    log.warn("InstanceModel: "+instanceNamespace.toString());
    log.warn("WoaModel: "+woaNamespace.toString());
    for(Namespace candidate : libraryModels.keySet()) {
      log.warn("Libraries: "+candidate.toString());
    }
    return null;
  }


  // OntModels




  public static OntModel asOntModel(Model model) {

    // Set document manager policy file
    OntDocumentManager dm = new OntDocumentManager();
    dm.setProcessImports(false);

    OntModelSpec modelSpec = ontModelSpec;
    modelSpec.setDocumentManager(dm);

    OntModel result = ModelFactory.createOntologyModel(modelSpec, model);

    return result;
  }


  public static OntModel asOntModel(Model model, Reasoner reasoner) {

    // Set document manager policy file
    OntDocumentManager dm = new OntDocumentManager();
    dm.setProcessImports(false);

    OntModelSpec modelSpec = ontModelSpec;
    modelSpec.setDocumentManager(dm);
    modelSpec.setReasoner(reasoner);

    OntModel result = ModelFactory.createOntologyModel(modelSpec, model);

    return result;
  }

  @Override
  public OntModel getInstanceOntModel() {
    return asOntModel(this.instanceModel);
  }
  public OntModel getInstanceOntModel(Reasoner reasoner) {
    return asOntModel(this.instanceModel, reasoner);
  }

  @Override
  public OntModel getJenaOntModel(String namespace) {
    Model model = getModel(namespace);
    if(model != null) {
      return asOntModel(model);
    }
    return null;
  }
  public OntModel getJenaOntModel(String namespace, Reasoner reasoner) {
    Model model = getModel(namespace);
    if(model != null) {
      return asOntModel(model, reasoner);
    }
    return null;
  }


  @Override
  public OntModel getUnionJenaOntModel() {
    return asOntModel(getFullUnionModel());
  }
  public OntModel getUnionJenaOntModel(Reasoner reasoner) {
    return asOntModel(getFullUnionModel(), reasoner);
  }




  // Datasets

  @Override
  public Dataset getEmptyDataset() {
    return new DatasetImpl(getEmptyModel());
  }

  @Override
  public Dataset getDataset() {
    if(dataset == null) {
      dataset = rebuildDataset();
    }
    return dataset;
  }
  public Dataset rebuildDataset() {


    dataset = getEmptyDataset();

    updateModel(dataset, instanceNamespace.toString(), instanceModel);
    updateModel(dataset, woaNamespace.toString(), woaModel);
    for(Namespace ns : libraryModels.keySet()) {
      updateModel(dataset, ns.toString(), libraryModels.get(ns));
    }
    return dataset;
  }


  @Override
  public Dataset getValidationDataset() {
    if(validationDataset == null) {
      validationDataset = rebuildValidationDataset();
    }
    return validationDataset;
  }
  private Dataset rebuildValidationDataset() {

    log.info("Arrange dataset with union graphs.");

    validationDataset = getEmptyDataset();

    updateModel(validationDataset, INSTANCE_GRAPH, instanceModel);
    updateModel(validationDataset, WOA_GRAPH, woaModel);
    updateModel(validationDataset, SCHEMA_UNION_GRAPH, getSchemaAggregationModel());
    updateModel(validationDataset, FULL_UNION_GRAPH, getFullUnionModel());

    log.info("Done arranging.");

    return validationDataset;
  }

  public void updateModel(Dataset dataset, String ns, Model model) {

    log.info("Update model "+ns);
    if(dataset.containsNamedModel(ns)) {
      dataset.replaceNamedModel(ns, model);
    } else {
      dataset.addNamedModel(ns, model);
    }
  }

  @Override
  public Map<String, Long> numTriples() {
    HashMap<String, Long> result = new HashMap<>();
    Iterator<String> graphNameIterator = getValidationDataset().listNames();
    while(graphNameIterator.hasNext()) {
      String graphName = graphNameIterator.next();
      long size = getValidationDataset().getNamedModel(graphName).size();
      result.put(graphName, size);
    }
    return result;
  }

  public static Map<String, Long> diffNumTriples(Map<String, Long> oldValues, Map<String, Long> newValues) {
    HashMap<String, Long> result = new HashMap<>();

    Iterator<String> graphNameIterator = newValues.keySet().iterator();
    while(graphNameIterator.hasNext()) {
      String graphName = graphNameIterator.next();

      Long oldValue;
      if(oldValues.containsKey(graphName)) {
        oldValue = oldValues.get(graphName);
      } else {
        oldValue = 0l;
      }
      Long newValue = newValues.get(graphName);
      result.put(graphName, newValue - oldValue);
    }
    return result;
  }



  // Exporting to a file


  @Override
  public void writeModelToFile(Model model, String baseNamespace, OutputStream output, RDFFormat format) {

    if(format == RDFFormat.RDFXML || format == RDFFormat.RDFXML_ABBREV ||
        format == RDFFormat.RDFXML_PLAIN || format == RDFFormat.RDFXML_PRETTY) {

      RDFWriter writer;
      if(format == RDFFormat.RDFXML_ABBREV) {
        writer = model.getWriter( "RDF/XML-ABBREV" );
      } else {
        writer = model.getWriter( "RDF/XML" );
      }
      writer.setProperty("xmlbase", baseNamespace);
      writer.write(model, output, null);

    } else {
      Dataset dataset = getDataset();
      dataset.getNamedModel(baseNamespace);
      RDFDataMgr.write(output, model, format);
    }
  }

  @Override
  public String writeModelToString(Model model, String baseNamespace, RDFFormat format) {

    log.trace("Starting to export.");


    ByteArrayOutputStream boas = new ByteArrayOutputStream();


    if(format == RDFFormat.RDFXML || format == RDFFormat.RDFXML_ABBREV ||
        format == RDFFormat.RDFXML_PLAIN || format == RDFFormat.RDFXML_PRETTY) {

      RDFWriter writer;
      if(format == RDFFormat.RDFXML_ABBREV) {
        writer = model.getWriter( "RDF/XML-ABBREV" );
      } else {
        writer = model.getWriter( "RDF/XML" );
      }
      writer.setProperty("xmlbase", baseNamespace);
      writer.write(model, boas, null);

    } else {
      Dataset dataset = getDataset();
      dataset.getNamedModel(baseNamespace);
      RDFDataMgr.write(boas, model, format);
    }


    String result = "";

    try {
      BufferedReader reader = new BufferedReader(new StringReader(boas.toString()));

      String line;
      while ((line = reader.readLine()) != null) {
        result += line+"\n";
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }

    return result;
  }

  @Override
  public void writeFullToFile(OutputStream output, RDFFormat format) {
    Dataset dataset = getDataset();
    RDFDataMgr.write(output, dataset, format);
  }

  @Override
  public void writeFullToFile(Dataset dataset, OutputStream output, RDFFormat format) {
    RDFDataMgr.write(output, dataset, format);
  }

  @Override
  public Iterator<Map<String, String>> query(String sparqlQuery) {

    List<Map<String, String>> result = new ArrayList<>();

    // Execute the query and obtain results
    ResultSet results = getResultSet(sparqlQuery, getDataset());

    // Output query results
    while (results.hasNext()) {

      HashMap<String, String> resultRow = new HashMap();

      QuerySolution row = results.next();

      Iterator columnNames = row.varNames();
      while(columnNames.hasNext()) {
        String columnName = (String) columnNames.next();
        RDFNode item = row.get(columnName);
        if(item.isAnon()) {
          continue;
        }
        if(item.isResource()) {
          resultRow.put(columnName, item.asResource().getURI());
        } else if(item.isLiteral()) {
          resultRow.put(columnName, item.asLiteral().getLexicalForm());
        } else {
          log.warn("Skipping a result from the query.");
        }
      }

      result.add(resultRow);
    }

    // If all the files from the fileNames where in the zip archive, this list is now supposed to be empty
    return result.iterator();
  }



  @Override
  public void insert(InferenceQuery query, InferenceQueryResult result) {

    validationDataset = getValidationDataset();

    long start = new Date().getTime();
    String queryString = query.getSparqlQuery(this);

    try {

      UpdateRequest request = new UpdateRequest();
      request.add(queryString);
      UpdateAction.execute(request, validationDataset);

    } catch (QueryException e) {
      throw new RuntimeException("There is a problem with this query: " + queryString, e);
    }

    long executionTime = new Date().getTime() - start;
    result.addExecutionTime(executionTime);
  }


  public ResultSet getResultSet(String queryString, Dataset dataset) {
    Query query = QueryFactory.create(queryString);

    // Execute the query and obtain results
    QueryExecution qe = QueryExecutionFactory.create(query, dataset);
    ResultSet result = qe.execSelect();
    return result;
  }
  @Override
  public ValidationQueryResult select(ValidationQuery validationQuery) {

    String errorMessage = null;
    boolean passed;
    long start = new Date().getTime();
    String queryString = validationQuery.getSparqlQuery(this);
    Iterator<Map<String, String>> resultSet = null;
    ArrayList<String> formattedResults = new ArrayList<>();

    try {

      List<Map<String, String>> result = new ArrayList<>();

      ResultSet results = getResultSet(queryString, getValidationDataset());

      passed = !results.hasNext();

      // Output query results
      while (results.hasNext()) {

        HashMap<String, String> resultRow = new HashMap();

        QuerySolution row = results.next();

        Iterator columnNames = row.varNames();
        while(columnNames.hasNext()) {
          String columnName = (String) columnNames.next();
          RDFNode item = row.get(columnName);
          if(item.isAnon()) {
            resultRow.put(columnName, "BLANK");
          } else if(item.isResource()) {
            String value = item.asResource().getURI();
            if(value == null) {
              value = "NON INTERPRETABLE URI";
            }
            resultRow.put(columnName, value);
          } else if(item.isLiteral()) {
            String value = item.asLiteral().getLexicalForm();
            if(value == null) {
              value = "NON INTERPRETABLE LITERAL";
            }
            resultRow.put(columnName, value);
          } else {
            resultRow.put(columnName, "NOT INTERPRETED");
            log.warn("Skipping a result from the query "+validationQuery.getReference()+".");
          }
        }

        formattedResults.add(validationQuery.formatResult(resultRow));

        result.add(resultRow);
      }

      resultSet = result.iterator();


      if(passed) {
        log.trace("Query "+validationQuery.getReference()+" found no results, passed.");
      } else {
        log.trace("For query "+validationQuery.getReference()+" results where found, not passing validation.");

      }

    } catch (QueryParseException e) {

      errorMessage = "Problem executing query "+validationQuery.getReference()+": ";
      errorMessage += escapeHtml4("\n" + queryString + "\n" + e.getMessage());
      log.error(errorMessage);
      passed = false;

    } catch (OutOfMemoryError e) {

      errorMessage = "Problem executing query "+validationQuery.getReference()+", not enough memory.";
      log.error(errorMessage);
      passed = false;

      // Free up variables
      resultSet = null;
      formattedResults = null;
    }

    long executionTime = new Date().getTime() - start;
    return new ValidationQueryResult(validationQuery.getReference(), validationQuery.getDescription(), queryString, resultSet, formattedResults, passed, errorMessage, executionTime);
  }

  @Override
  public long numTriples(String graph) {
    return getValidationDataset().getNamedModel(graph).size();
  }

}