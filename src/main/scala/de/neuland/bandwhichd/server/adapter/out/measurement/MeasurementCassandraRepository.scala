package de.neuland.bandwhichd.server.adapter.out.measurement

import cats.effect.Async
import cats.implicits.*
import com.comcast.ip4s.Hostname
import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.cql.{SimpleStatement, Statement}
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementCassandraCodecs.given
import de.neuland.bandwhichd.server.boot.Configuration
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.domain.{AgentId, Interface, MachineId}
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext
import de.neuland.bandwhichd.server.lib.time.Interval
import fs2.Stream
import io.circe.Encoder

import java.time.ZoneOffset
import java.time.ZoneOffset.UTC

class MeasurementCassandraRepository[F[_]: Async](
    private val cassandraContext: CassandraContext[F],
    private val configuration: Configuration
) extends MeasurementRepository[F] {
  override def record(measurement: Measurement[Timing]): F[Unit] =
    if (configuration.readonly)
      Async[F].raiseError(new Exception("readonly mode enabled"))
    else
      cassandraContext.executeRawExpectNoRow(
        SimpleStatement
          .builder("insert into measurements json ? using ttl ?")
          .addPositionalValues(
            Encoder[Measurement[Timing]]
              .apply(measurement)
              .noSpaces,
            measurement match
              case _: Measurement.NetworkConfiguration =>
                configuration.measurementNetworkConfigurationTTL.toSeconds.toInt
              case _: Measurement.NetworkUtilization =>
                configuration.measurementNetworkUtilizationTTL.toSeconds.toInt
          )
          .setKeyspace(configuration.measurementsKeyspace)
          .setTimeout(configuration.recordMeasurementQueryTimeout)
          .build()
      )

  override def getAll: Stream[F, Measurement[Timing]] =
    cassandraContext
      .executeRaw(
        SimpleStatement
          .builder("select json * from measurements")
          .setKeyspace(configuration.measurementsKeyspace)
          .setTimeout(configuration.getAllMeasurementsQueryTimeout)
          .build()
      )
      .flatMap(reactiveRow =>
        Option(reactiveRow.getString(0)).fold(
          Stream.raiseError(new Exception("could not select json"))
        )(Stream.emit)
      )
      .flatMap(jsonString =>
        io.circe.parser
          .parse(jsonString)
          .fold(
            parsingFailure =>
              Stream.raiseError(
                new Exception(
                  s"could not parse json $jsonString",
                  parsingFailure
                )
              ),
            Stream.emit
          )
      )
      .flatMap(json =>
        io.circe
          .Decoder[Measurement[Timing]]
          .decodeJson(json)
          .fold(
            decodingFailure =>
              Stream.raiseError(
                new Exception(s"could not decode json $json", decodingFailure)
              ),
            Stream.emit
          )
      )
}