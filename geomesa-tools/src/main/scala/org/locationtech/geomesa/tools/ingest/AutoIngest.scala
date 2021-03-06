/***********************************************************************
 * Copyright (c) 2013-2018 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.tools.ingest

import java.io.File
import java.util.concurrent.atomic.AtomicLong

import org.locationtech.geomesa.tools.DistributedRunParam.RunModes.RunMode
import org.locationtech.geomesa.tools.ingest.AbstractIngest.StatusCallback
import org.locationtech.geomesa.tools.utils.DataFormats._

/**
 * Attempts to ingest files based on metadata stored in the file itself. Operates
 * on csv, tsv, and avro files.
 *
 * @param dsParams data store connection parameters
 * @param typeName simple feature type name
 * @param inputs files to ingest
 * @param numLocalThreads for local ingests, how many threads to use
 * @param format format of the file (must be one of TSV, CSV, AVRO)
 */
class AutoIngest(typeName: String,
                 dsParams: Map[String, String],
                 inputs: Seq[String],
                 format: DataFormat,
                 mode: Option[RunMode],
                 libjarsFile: String,
                 libjarsPaths: Iterator[() => Seq[File]],
                 numLocalThreads: Int)
    extends AbstractIngest(dsParams, typeName, inputs, mode, libjarsFile, libjarsPaths, numLocalThreads) {

  import org.locationtech.geomesa.tools.utils.DataFormats._

  require(Seq(Tsv, Csv, Avro).contains(format), "Only Avro or delimited text files are supported for auto ingest")

  override def beforeRunTasks(): Unit = {}

  override def createLocalConverter(path: String, failures: AtomicLong): LocalIngestConverter = {
    format match {
      case Avro      => new AvroIngestConverter(ds, typeName)
      case Tsv | Csv => new DelimitedIngestConverter(ds, typeName, format)
      // in case someone forgets to add a new type here
      case _ => throw new UnsupportedOperationException(s"Invalid input format $format")
    }
  }

  override def runDistributedJob(statusCallback: StatusCallback): (Long, Long) = {
    format match {
      case Avro      => new AvroIngestJob(dsParams, typeName, inputs, libjarsFile, libjarsPaths).run(statusCallback)
      case Tsv | Csv => new DelimitedIngestJob(dsParams, typeName, format, inputs, libjarsFile, libjarsPaths).run(statusCallback)
      // in case someone forgets to add a new type here
      case _ => throw new UnsupportedOperationException(s"Invalid input format $format")
    }
  }

}


