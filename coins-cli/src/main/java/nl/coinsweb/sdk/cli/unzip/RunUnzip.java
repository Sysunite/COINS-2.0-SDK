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
package nl.coinsweb.sdk.cli.unzip;

import nl.coinsweb.sdk.FileManager;
import nl.coinsweb.sdk.cli.Run;
import nl.coinsweb.sdk.cli.validate.ValidateOptions;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * @author Bastiaan Bijl, Sysunite 2016
 */
public class RunUnzip {

  private static final Logger log = LoggerFactory.getLogger(RunUnzip.class);

  public static void go(String[] args) {

    UnzipOptions options;
    try {
      options = new UnzipOptions(args);
    } catch (ParseException e) {
      Run.printHeader();
      System.out.println("(!)" + e.getMessage() + "\n");
      UnzipOptions.usage();
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
    log.info("Started upzipping.");

    if(!options.hasInputOption() || options.getInputOptions().isEmpty()) {
      if(!Run.QUIET) {
        System.out.println("(!) no input file specified");
      }
      return;
    }

    if(!options.hasOutputOption()) {
      if(!Run.QUIET) {
        System.out.println("(!) no output location specified");
      }
      return;
    }

    if(options.hasInputOption() && options.getInputOptions().size() > 1) {
      if(!Run.QUIET) {
        System.out.println("(!) too many input files specified");
      }
      return;
    }

    for(Path zipFile : options.getInputOptions()) {
      FileManager.unzipTo(zipFile.toFile(), options.getOutputOption().resolve(zipFile.getFileName().toString().substring(0, zipFile.getFileName().toString().length() - 4)), false);
    }


  }
}