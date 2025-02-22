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
package nl.coinsweb.sdk.cli.generate;


import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.tdb.TDB;
import nl.coinsweb.sdk.Namespace;
import nl.coinsweb.sdk.cli.Run;
import nl.coinsweb.sdk.cli.validate.ValidateOptions;
import nl.coinsweb.sdk.jena.InMemGraphSet;
import nl.coinsweb.sdk.jena.JenaCoinsContainer;
import nl.coinsweb.sdk.owlgenerator.ClassGenerateEngine;
import org.apache.commons.cli.ParseException;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Bastiaan Bijl, Sysunite 2016
 */
public class RunGenerate {

  private static final Logger log = LoggerFactory.getLogger(RunGenerate.class);




  public void go(String[] args) {

    GenerateOptions options;
    try {
      options = new GenerateOptions(args);
    } catch (ParseException e) {
      Run.printHeader();
      System.out.println("(!)" + e.getMessage() + "\n");
      GenerateOptions.usage();
      System.exit(1);
      return;
    }

    // Print header
    Run.QUIET = options.quietMode();
    Run.printHeader();

    // Asked for help
    if(options.printHelpOption()) {
      ValidateOptions.usage();
      System.exit(1);
      return;
    }

    Run.startLoggingToFile();
    log.info("Started generation.");

    // Check requirements
    if (!options.hasInputOption()) {
      if(!Run.QUIET) {
        System.out.println("(!) no input file specified\n");
      }
      GenerateOptions.usage();
      System.exit(1);
      return;
    }
    if (!options.hasOutputOption()) {
      if(!Run.QUIET) {
        System.out.println("(!) no output file specified\n");
      }
      GenerateOptions.usage();
      System.exit(1);
      return;
    }



    // Check tools javac, jar en ikvmc

    boolean doCompile     = options.hasJarOption() || options.hasJarPathOption() || options.hasDllOption() || options.hasDllPathOption();
    boolean doJar         = options.hasJarOption() || options.hasJarPathOption() || options.hasDllOption() || options.hasDllPathOption();
    boolean doInclSources = doJar && options.hasIncludeSourcesOption();
    boolean doDll         = options.hasDllOption() || options.hasDllPathOption();

    String javaCheck = Run.getCli("javac -version");
    log.info("Result javac -version: "+javaCheck.substring(0, Math.min(javaCheck.length(), 30))+"...");
    if(doCompile && !(javaCheck.startsWith("javac 1.7") || javaCheck.startsWith("javac 1.8"))) {
      if(!Run.QUIET) {
        System.out.println("Please make the javac command from the Java SDK (version 7 or 8) available on the path.");
      }
      System.exit(1);
      return;
    }
    log.info("Java version accepted.");

    String jarCheck = Run.getCli("jar");
    log.info("Result jar: "+jarCheck.substring(0, Math.min(jarCheck.length(), 30))+"...");
    if(doJar && !jarCheck.startsWith("Usage: jar {ctxui}")) {
      if(!Run.QUIET) {
        System.out.println("Please make the jar command available on the path.");
      }
      System.exit(1);
      return;
    }
    log.info("Jar command accepted.");


    if(doDll) {
      if(!options.hasApiPathOption()) {
        if (!Run.QUIET) {
          System.out.println("Please specify a pointer to the coins-api.dll using -a.");
        }
        System.exit(1);
        return;
      } else {
        log.info("Will use supplied coins-api.dll: " + options.getApiPathOption().toString());
      }

      String ikvmCheck = Run.getCli("ikvmc --nonexistantcommand");
      log.info("Result ikvmc --nonexistantcommand: "+ikvmCheck.substring(0, Math.min(ikvmCheck.length(), 30))+"...");
      if(!(ikvmCheck.startsWith("IKVM.NET Compiler version "))) {
        if (!Run.QUIET) {
          System.out.println("Please make the ikvmc command (version 7 or 8) available on the path.");
        }
        System.exit(1);
        return;
      }
      log.info("Ikvmc version accepted.");
    }



    // Pre visit output folders

    String coinsCliJarPath;
    String coinsApiDllPath;
    Path generatedFolder;
    Path javaFolder;
    Path generatedPerJarFolder;
    Path jarFolder;
    Path dllFolder;

    try {

      coinsCliJarPath = (System.getProperty("java.class.path"));
      coinsApiDllPath = doDll?(options.getApiPathOption().toFile().getCanonicalPath()):null;

      generatedFolder = Paths.get(options.getOutputOption().toFile().getCanonicalPath()).resolve("generated");
      generatedFolder.toFile().mkdirs();
      FileUtils.cleanDirectory(generatedFolder.toFile());
      if(!Run.QUIET) {
        System.out.println("Will write java to: " + generatedFolder);
      }

      javaFolder = Paths.get(options.getOutputOption().toFile().getCanonicalPath()).resolve("java");
      if (doCompile) {
        javaFolder.toFile().mkdirs();
        FileUtils.cleanDirectory(javaFolder.toFile());
        if(!Run.QUIET) {
          System.out.println("Will write compiled java to: " + javaFolder);
        }
      }

      generatedPerJarFolder = Paths.get(options.getOutputOption().toFile().getCanonicalPath()).resolve("java-per-jar");
      if (doJar) {
        generatedPerJarFolder.toFile().mkdirs();
        FileUtils.cleanDirectory(generatedPerJarFolder.toFile());
        if(!Run.QUIET) {
          System.out.println("Will split compiled java in: " + generatedPerJarFolder);
        }
      }

      if (doInclSources) {
        if(!Run.QUIET) {
          System.out.println("Will include sources in the jar");
        }
      }

      jarFolder =
          options.hasJarPathOption() ?
              Paths.get(options.getJarPathOption().toFile().getCanonicalPath()) :
              Paths.get(options.getOutputOption().toFile().getCanonicalPath()).resolve("jar");
      if (doJar) {
        jarFolder.toFile().mkdirs();
        if(!Run.QUIET) {
          System.out.println("Will put jars in: " + jarFolder);
        }
      }

      dllFolder =
          options.hasDllPathOption() ?
              Paths.get(options.getDllPathOption().toFile().getCanonicalPath()) :
              Paths.get(options.getOutputOption().toFile().getCanonicalPath()).resolve("win-dist");
      if (doDll) {
        dllFolder.toFile().mkdirs();
        if(!Run.QUIET) {
          System.out.println("Will put dlls in: " + dllFolder);
        }
      }

    } catch (IOException e) {
      if(!Run.QUIET) {
        System.out.println("Something went wrong preparing the folders.");
      }
      log.error("Something went wrong preparing the folders.", e);
      return;
    }




    // Generate java code in one place

    ClassGenerateEngine engine = new ClassGenerateEngine();

    // Load the data file
    OntModelSpec reasoner = null;
    if(options.hasReasonerOption()) {
      reasoner = options.getReasonerOption();
    }
    if(reasoner != null) {
      log.info("Using custom reasoner: "+options.getReasonerString());
    } else {
      log.info("Using default reasoner: OWL_MEM_RDFS_INF");
      reasoner = OntModelSpec.OWL_MEM_RDFS_INF;
    }


    InMemGraphSet graphSet = new InMemGraphSet("http://empty.com/");
    graphSet.setOntModelSpec(reasoner);
    JenaCoinsContainer model = new JenaCoinsContainer(graphSet, false, true);





    ArrayList<String> sourceFileNames = new ArrayList<>();
    for(Path modelPath : options.getInputOptions()) {
      String filePath = null;
      try {
        filePath = modelPath.toFile().getCanonicalPath().toString();
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      }
      model.addImport(filePath, null, true, true, true);
      log.info("Adding "+filePath);
      sourceFileNames.add(modelPath.getFileName().toString());
    }




    engine.setTargetFolder(generatedFolder.toString());
    Map<Namespace, String> mapping = engine.process(model, sourceFileNames);

    if(!doCompile) {
      return;
    }


    // Compile the java code

    String fileList = "";
    for(Namespace namespace : mapping.keySet()) {
      String packageFolder = mapping.get(namespace).replace(".", "//");
      fileList += files(generatedFolder.resolve(packageFolder), ".java", " ");
    }

    TDB.init();

    log.info("javac -target 1.7  -source 1.7 -classpath " + coinsCliJarPath + " -d " + javaFolder.toString() + " " + fileList);
    Run.runCli("javac -target 1.7  -source 1.7 -classpath " + coinsCliJarPath + " -d " + javaFolder.toString() + " " + fileList);

    if(!doJar) {
      return;
    }



    List<String> producedDllFiles = new ArrayList<>();
    List<Namespace> pickList = new ArrayList(mapping.keySet());
    if(options.hasOrderOption()) {
      log.info("Use specified order.");

      // Add all from list
      List<Namespace> reOrderedPickList = new ArrayList();
      for(String fromPriorityList : options.getOrderOptions()) {
        Namespace fromPriorityListNs = new Namespace(fromPriorityList);
        if(mapping.containsKey(fromPriorityListNs)) {
          reOrderedPickList.add(fromPriorityListNs);
        }
      }

      // Add all remaining
      for(Namespace fromOriginalListNs : pickList) {
        if(!reOrderedPickList.contains(fromOriginalListNs)) {
          reOrderedPickList.add(fromOriginalListNs);
        }
      }

      pickList = reOrderedPickList;
    }
    for(Namespace namespace : pickList) {

      log.info("Deal with "+namespace);
      if(!mapping.containsKey(namespace)) {
        log.error("Could not match namespace "+namespace+" derived from input files to the namespaces that where found iterating the union model.");
        continue;
      }

      String packageFolder = mapping.get(namespace).replace(".", "//");
      Path packageFolderPath = Paths.get(packageFolder);
      String libraryName = packageFolderPath.getName(packageFolderPath.getNameCount()-1).toString();

      Path absoluteSubFolder = generatedPerJarFolder.resolve(libraryName);
      absoluteSubFolder.toFile().mkdirs();


      log.info("Start with with "+libraryName);

      // Copy to different folders
      try {

        FileUtils.copyDirectory(
            javaFolder.resolve(packageFolder).toFile(),
            generatedPerJarFolder.resolve(libraryName).resolve(packageFolder).toFile());

      } catch(IOException e) {
        log.error("",e);
        continue;
      }

      // Copy source files
      if(doInclSources) {
        List<String> sourceFiles = Arrays.asList(files(generatedFolder.resolve(packageFolder), ".java", " ").split(" "));
        for (String sourceFile : sourceFiles) {
          File source = new File(sourceFile);
          String fileName = source.getName();
          File destination = generatedPerJarFolder.resolve(libraryName).resolve(packageFolder).resolve(fileName).toFile();
          try {
            FileUtils.copyFile(source, destination);
          } catch (IOException e) {
            log.error(e.getMessage(), e);
          }
        }
      }



      // Copy the rdf file
      for(Path inputFile : options.getInputOptions()) {
        try {
          FileUtils.copyFileToDirectory(inputFile.toFile(), generatedPerJarFolder.resolve(libraryName).toFile());
        } catch (IOException e) {
          log.error("Error while copying source rdf file to namespace folder.", e);
        }
      }




      // Create jar
      String filesToPackageInJar = files(absoluteSubFolder, "", " ", absoluteSubFolder);
      log.info("jar cfM " + jarFolder.resolve(libraryName + ".jar") + " -C " + absoluteSubFolder + "/ " + filesToPackageInJar, absoluteSubFolder);
      Run.runCli("jar cfM " + jarFolder.resolve(libraryName + ".jar") + " -C " + absoluteSubFolder + "/ " + filesToPackageInJar, absoluteSubFolder);


      if(doDll) {

        String dllFile = dllFolder.resolve(libraryName + ".dll").toString();

        String references = "-reference:"+coinsApiDllPath+" " ;
        for(String reference : producedDllFiles) {
          references += "-reference:"+reference+" " ;
        }

        // Create dlls
        String command = "ikvmc -nologo "+references+" -target:library " + jarFolder.resolve(libraryName + ".jar") + " -out:" + dllFile;

        log.info(command);
        Run.runCli(command);

        producedDllFiles.add(dllFile);
      }

      log.info("Done with "+libraryName);
    }
  }


  private static String files(Path path, String extension, String delimiter) {
    return files(path, extension, delimiter, null);
  }
  private static String files(Path path, String extension, String delimiter, Path relativeTo) {

    String result = "";
    File[] directoryListing = path.toFile().listFiles();
    if (directoryListing != null) {
      for (File child : directoryListing) {
        if("".equals(extension) || child.getName().endsWith(extension)) {

          if(relativeTo != null) {
            result += relativeTo.relativize(Paths.get(child.getAbsolutePath())) + delimiter;
          } else {
            result += child.getAbsolutePath() + delimiter;
          }
        }
      }
    }
    return result;
  }
}
