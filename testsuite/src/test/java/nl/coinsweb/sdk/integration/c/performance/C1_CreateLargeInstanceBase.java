package nl.coinsweb.sdk.integration.c.performance;

import com.hp.hpl.jena.rdf.model.RDFNode;
import nl.coinsweb.sdk.CoinsModel;

import nl.coinsweb.sdk.FileManager;
import nl.coinsweb.sdk.integration.MemoryAsserts;
import nl.coinsweb.sdk.integration.TimeAsserts;
import nl.coinsweb.sdk.integration.ZipAsserts;
import nl.coinsweb.sdk.jena.JenaCoinsContainer;
import org.apache.jena.riot.RDFFormat;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Usecase C1:
 *
 *    Measure performance without use of a generated .dll library.
 *
 *
 *
 *
 * @author Bastiaan Bijl, Sysunite 2016
 */

public class C1_CreateLargeInstanceBase {


  private Set<String> rdfZipContent;


  @Before
  public void initAssertLists() {

    rdfZipContent = new HashSet<>();
    rdfZipContent.add("bim/content.rdf");
    rdfZipContent.add("bim/repository/cbim-2.0.rdf");
    rdfZipContent.add("doc/");
    rdfZipContent.add("woa/woa.rdf");
  }


  @AfterClass
  public static void cleanup() {
    FileManager.destroyAll();
  }

  private int SOMANY_INSTANCES = 10000;
  private double SAMPLE_PROBABILITY = .01;

  private Set<RDFNode> sampleList = new HashSet<>();


  @Test
  public void createCcrWithManyTriplesRdfInMem() {
    createCcrWithManyTriplesRdf(new JenaCoinsContainer());
  }


  public void createCcrWithManyTriplesRdf(CoinsModel workspace) {

    MemoryAsserts.sample();
    TimeAsserts.sample();

    // Create the container
    JenaCoinsContainer ccr = new JenaCoinsContainer();





    MemoryAsserts.sample();


    // Export all the rdf data to the container
    ccr.exportModel(ccr.getCoinsGraphSet().getInstanceModel(), ccr.getCoinsGraphSet().getInstanceNamespace(), "/tmp/coinstest/content.rdf", RDFFormat.RDFXML);
    MemoryAsserts.sample();
    TimeAsserts.sample();
    ccr.exportModel(ccr.getCoinsGraphSet().getInstanceModel(), ccr.getCoinsGraphSet().getInstanceNamespace(), "/tmp/coinstest/content.ttl", RDFFormat.TTL);
    MemoryAsserts.sample();
    TimeAsserts.sample();
    ccr.exportModel(ccr.getCoinsGraphSet().getInstanceModel(), ccr.getCoinsGraphSet().getInstanceNamespace(), "/tmp/coinstest/content.jsonld", RDFFormat.JSONLD);



    MemoryAsserts.sample();
    TimeAsserts.sample();

    assertEquals(ccr.getAttachments().size(), 0);

    MemoryAsserts.sample();
    TimeAsserts.sample();


    // Create the container file
    ccr.export(Paths.get("/tmp/coinstest/bigrdf.ccr").toFile().toString());

    MemoryAsserts.sample();
    TimeAsserts.sample();



    assertTrue(ZipAsserts.containsFiles(new File("/tmp/coinstest/bigrdf.ccr"), rdfZipContent, false));
    assertTrue(MemoryAsserts.neverUnderMbFree(10));
//    assertTrue(MemoryAsserts.neverOverMbUsed(1024));
    assertTrue(TimeAsserts.neverLongerSec(120));

    ccr.close();
    workspace.asExpertCoinsModel().close();
  }


}
