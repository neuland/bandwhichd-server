package de.neuland.bandwhichd.server.test

import com.comcast.ip4s.{Arbitraries as Ip4sArbitraries, *}
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.lib.time.Interval
import org.scalacheck.{Arbitrary, Gen}

import java.time.ZoneOffset.UTC
import java.time.{Duration, Instant, LocalDate, ZoneOffset}

object Arbitraries {
  given genArbitrary[A](using Gen[A]): Arbitrary[A] =
    Arbitrary[A].apply(summon[Gen[A]])

  def sample[A](using Gen[A]): () => A = {
    val provider = LazyList.continually(summon[Gen[A]].sample).take(100).flatten
    () => provider.head
  }

  ///////////////////////

  given Gen[Instant] = Gen.calendar.map(_.toInstant)

  ///////////////////////

  given Gen[Cidr[IpAddress]] =
    Ip4sArbitraries.cidrGenerator(Ip4sArbitraries.ipGenerator)
  given Gen[Host] =
    Gen.oneOf(Ip4sArbitraries.ipGenerator, Ip4sArbitraries.hostnameGenerator)
  given Gen[SocketAddress[Host]] =
    for {
      host <- summon[Gen[Host]]
      port <- Ip4sArbitraries.portGenerator
    } yield SocketAddress(host, port)

  ///////////////////////

  def interval(min: Duration, max: Duration): Gen[Interval] =
    for {
      start <- summon[Gen[Timing.Timestamp]]
      durationInMilliseconds <- Gen.choose(min.toMillis, max.toMillis)
    } yield Interval(start.instant, Duration.ofMillis(durationInMilliseconds))

  ///////////////////////

  given Gen[BytesCount] = Gen.long.map(BigInt.apply).map(BytesCount.apply)
  given Gen[InterfaceName] = Gen
    .oneOf("enp0s31f6", "lo", "virbr0", "tun0", "wlp3s0")
    .map(InterfaceName.apply)
  given Gen[MachineId] = Gen.uuid.map(MachineId.apply)
  given Gen[OsRelease.FileContents] =
    Gen
      .oneOf(
        "PRETTY_NAME=\"Debian GNU/Linux 11 (bullseye)\"\nNAME=\"Debian GNU/Linux\"\nVERSION_ID=\"11\"\nVERSION=\"11 (bullseye)\"\nVERSION_CODENAME=bullseye\nID=debian\nHOME_URL=\"https://www.debian.org/\"\nSUPPORT_URL=\"https://www.debian.org/support\"\nBUG_REPORT_URL=\"https://bugs.debian.org/\"",
        "NAME=\"SLES\"\nVERSION=\"15-SP3\"\nVERSION_ID=\"15.3\"\nPRETTY_NAME=\"SUSE Linux Enterprise Server 15 SP3\"\nID=\"sles\"\nID_LIKE=\"suse\"\nANSI_COLOR=\"0;32\"\nCPE_NAME=\"cpe:/o:suse:sles:15:sp3\"\nDOCUMENTATION_URL=\"https://documentation.suse.com/\"",
        "PRETTY_NAME=\"Debian GNU/Linux 8 (jessie)\"\nNAME=\"Debian GNU/Linux\"\nVERSION_ID=\"8\"\nVERSION=\"8 (jessie)\"\nID=debian\nHOME_URL=\"http://www.debian.org/\"\nSUPPORT_URL=\"http://www.debian.org/support\"\nBUG_REPORT_URL=\"https://bugs.debian.org/\""
      )
      .map(OsRelease.FileContents.apply)
  given Gen[ProcessName] =
    Gen
      .oneOf("dhclient", "java", "dnsmasq", "cupsd", "systemd-resolv")
      .map(ProcessName.apply)
  given Gen[Protocol] =
    Gen.prob(.8).map(isTcp => if (isTcp) Protocol.Tcp else Protocol.Udp)

  ///////////////////////

  given local[A](using Gen[A]): Gen[Local[A]] =
    summon[Gen[A]].map(Local.apply)
  given remote[A](using Gen[A]): Gen[Remote[A]] =
    summon[Gen[A]].map(Remote.apply)
  given sent[A](using Gen[A]): Gen[Sent[A]] =
    summon[Gen[A]].map(Sent.apply)
  given received[A](using Gen[A]): Gen[Received[A]] =
    summon[Gen[A]].map(Received.apply)

  ///////////////////////

  given Gen[Timing.Timestamp] =
    for {
      epochSecond <- Gen.choose(
        LocalDate
          .of(1950, 1, 1)
          .atStartOfDay(UTC)
          .toEpochSecond,
        LocalDate
          .of(2149, 12, 1)
          .atStartOfDay(UTC)
          .toEpochSecond
      )
    } yield Timing.Timestamp(Instant.ofEpochSecond(epochSecond))
  def timeframe(min: Duration, max: Duration): Gen[Timing.Timeframe] =
    interval(min, max).map(Timing.Timeframe.apply)

  ///////////////////////

  given Gen[Interface] = for {
    name <- summon[Gen[InterfaceName]]
    isUp <- Gen.prob(.8)
    networks <-
      if (isUp) for {
        size <- Gen.chooseNum(0, 3)
        cidrList <- Gen.listOfN(size, summon[Gen[Cidr[IpAddress]]])
      } yield cidrList
      else Gen.const(Seq.empty)
  } yield Interface(
    name = name,
    isUp = isUp,
    networks = networks
  )

  given Gen[OpenSocket] = for {
    socket <- Ip4sArbitraries.socketAddressGenerator(
      Ip4sArbitraries.ipGenerator,
      Ip4sArbitraries.portGenerator
    )
    protocol <- summon[Gen[Protocol]]
    maybeProcessName <- Gen.option(summon[Gen[ProcessName]])
  } yield OpenSocket(
    socket = socket,
    protocol = protocol,
    maybeProcessName = maybeProcessName
  )

  given Gen[Measurement.NetworkConfiguration] = for {
    machineId <- summon[Gen[MachineId]]
    timestamp <- summon[Gen[Timing.Timestamp]]
    maybeOsRelease <- Gen.option(summon[Gen[OsRelease.FileContents]])
    hostname <- Ip4sArbitraries.hostnameGenerator
    numberOfInterfaces <- Gen.chooseNum(1, 4)
    interfaces <- Gen.listOfN(numberOfInterfaces, summon[Gen[Interface]])
    numberOfOpenSockets <- Gen.chooseNum(1, 7)
    openSockets <- Gen.listOfN(numberOfOpenSockets, summon[Gen[OpenSocket]])
  } yield Measurement.NetworkConfiguration(
    machineId = machineId,
    timing = timestamp,
    maybeOsRelease = maybeOsRelease,
    hostname = hostname,
    interfaces = interfaces,
    openSockets = openSockets
  )

  given Gen[Connection] = for {
    interfaceName <- summon[Gen[InterfaceName]]
    localSocket <- summon[Gen[Local[SocketAddress[Host]]]]
    remoteSocket <- summon[Gen[Remote[SocketAddress[Host]]]]
    protocol <- summon[Gen[Protocol]]
    received <- summon[Gen[Received[BytesCount]]]
    sent <- summon[Gen[Sent[BytesCount]]]
  } yield Connection(
    interfaceName = interfaceName,
    localSocket = localSocket,
    remoteSocket = remoteSocket,
    protocol = protocol,
    received = received,
    sent = sent
  )

  given Gen[Measurement.NetworkUtilization] = for {
    machineId <- summon[Gen[MachineId]]
    timeframe <- timeframe(
      Duration.ofSeconds(10),
      Duration.ofSeconds(10).plusMillis(5)
    )
    numberOfConnections <- Gen.chooseNum(0, 20)
    connections <- Gen.listOfN(numberOfConnections, summon[Gen[Connection]])
  } yield Measurement.NetworkUtilization(
    machineId = machineId,
    timing = timeframe,
    connections = connections
  )
}
