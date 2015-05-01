package com.nitro.parquet.avro.examples

import java.io.File

import com.nitro.nlp.types._
import me.lyh.parquet.avro.{ Predicate, Projection }
import org.apache.commons.io.FileUtils
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.rdd.{ PairRDDFunctions, RDD }
import parquet.avro.{ AvroParquetInputFormat, AvroReadSupport, AvroParquetOutputFormat, AvroWriteSupport }
import parquet.hadoop.{ ParquetInputFormat, ParquetOutputFormat }
import java.util.UUID
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import scala.collection.JavaConversions._

object ExampleApp extends App {

  // TODO: move this out of Neville Li's project and make it depend only on parquet-avro-extra
  // TODO: via SBT's Github repository reference

  // TODO: replace this with actual CoreNLP POS tagging

  val sentence = "to be or not to be"

  val pos = List(
    new POS(0L, "to", POSTag.TO),
    new POS(1L, "be", POSTag.VB),
    new POS(2L, "or", POSTag.CC),
    new POS(3L, "not", POSTag.RB),
    new POS(4L, "to", POSTag.TO),
    new POS(5L, "be", POSTag.VB)
  )

  //  val pos = new POS(1L, "and", POSTag.CC)
  val pdf = new ParsedPDF(sentence, pos)
  //  val uuid = UUID.randomUUID().toString
  val tuple: Tuple2[Void, ParsedPDF] = (null, pdf)

  println(pdf)

  val conf = new SparkConf().
    setMaster("local").
    setAppName("test").
    set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

  val sc = new SparkContext(conf)
  val job = new Job()

  ParquetOutputFormat.setWriteSupportClass(job, classOf[AvroWriteSupport])
  AvroParquetOutputFormat.setSchema(job, ParsedPDF.SCHEMA$)

  ParquetInputFormat.setReadSupportClass(job, classOf[AvroReadSupport[ParsedPDF]])

  /*
      ParquetInputFormat.setReadSupportClass(job, classOf[AvroReadSupport[AminoAcid]])
    val file = sc.newAPIHadoopFile(outputDir, classOf[ParquetInputFormat[AminoAcid]],
      classOf[Void], classOf[AminoAcid], job.getConfiguration)
    file.foreach(aminoAcidPrinter)
   */

  val rdd = sc.parallelize(List(tuple))

  val dir = new File(s"/Users/mkolodziej/Downloads/${UUID.randomUUID().toString}")
  dir.mkdir()

  val path = s"$dir/output"

  try {

    rdd.saveAsNewAPIHadoopFile(
      path,
      classOf[Void],
      classOf[ParsedPDF],
      classOf[ParquetOutputFormat[ParsedPDF]],
      job.getConfiguration
    )

    // this will be rerun several times (full read, projection, predicate filter)
    def file = sc.newAPIHadoopFile(path, classOf[ParquetInputFormat[ParsedPDF]],
      classOf[Void], classOf[ParsedPDF], job.getConfiguration
    ).
      map(_._2) // drop Void key

    def stringify = file.map(_.toString).collect().mkString("\n")

    println(s"Full record 1:\n$stringify")

    val projection = Projection[ParsedPDF](_.getText)
    AvroParquetInputFormat.setRequestedProjection(job, projection)

    println(s"Projection of text field only:\n$stringify")

    // This won't work - macro expansion for arrays is not supported
    //    val predicate = Predicate[ParsedPDF](x => x.getPos.exists(i => i.getTag == POSTag.CC))

  } finally {

    sc.stop()
    FileUtils.deleteDirectory(dir)
  }

}
