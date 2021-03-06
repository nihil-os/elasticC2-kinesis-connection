package com.nihilos.elasticInstance2kinesis

import java.io.IOException
import java.util.{Optional, Properties}

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration}

/**
  * Created by prayagupd
  * on 3/31/17.
  */

class KinesisEventStreamConfig {

  val config = new Properties() {{
    try {
      load(this.getClass.getClassLoader.getResourceAsStream("application.properties"))
    } catch {
      case x: IOException => Console.err.println(x)
    }
  }}

  def getStreamConnection(): AmazonKinesisClient = {
    println("Initialising stream connection to kinesis.")
    val streamConnection = new AmazonKinesisClient(getAuthProfileCredentials(), getHttpConfiguration())

    if(getStreamRegions().isPresent) {
      streamConnection.withRegion(getStreamRegions().get())
    }

    streamConnection
  }

  def getStreamConsumerConfig(consumerName: String, consumerInstance: String, stream: String) = {

    val kinesisClientConfiguration =
      new KinesisClientLibConfiguration(consumerName, stream, getAuthProfileCredentials(), consumerInstance)
        .withKinesisClientConfig(getHttpConfiguration())
        .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON); //TODO confiure as property

    if(getStreamRegions().isPresent) {
      kinesisClientConfiguration.withRegionName(getStreamRegions().get().getName)
    }

    kinesisClientConfiguration
  }

  def getOffsetConnection() : AmazonDynamoDB = {
    val dynamoDB = new AmazonDynamoDBClient(getAuthProfileCredentials(), getHttpConfiguration())

    Option(getStreamRegions().get()).map(x => dynamoDB.setRegion(Region.getRegion(x)))

    dynamoDB
  }

  protected def getStreamRegions(): Optional[Regions] = {

    val regionOpt = Optional.ofNullable(config.getProperty("stream.region"))

    if (regionOpt.isPresent) {
      return Optional.ofNullable(Regions.fromName(regionOpt.get()))
    }

    Optional.ofNullable(null)
  }

  protected def getHttpConfiguration(): ClientConfiguration = {

    val httpProxyhost = Optional.ofNullable(config.getProperty("stream.http.proxy.host"))

    val httpProxyPort = Optional.ofNullable(config.getProperty("stream.http.proxy.port"))

    val clientConfiguration = new ClientConfiguration()

    if (httpProxyhost.isPresent()) {
      clientConfiguration.setProxyHost(httpProxyhost.get())
    }

    if (httpProxyPort.isPresent()) {
      clientConfiguration.setProxyPort(Integer.valueOf(httpProxyPort.get()))
    }

    clientConfiguration
  }

  private def getAuthProfileCredentials(): DefaultAWSCredentialsProviderChain = {
    Option(config.getProperty("authentication.profile")).map(profile =>
    System.setProperty("aws.profile", config.getProperty("authentication.profile")))

    new DefaultAWSCredentialsProviderChain
  }
}
