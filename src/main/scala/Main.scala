import java.io.File
import java.nio.file.{FileSystems, Paths}

import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, FanInShape2, FlowShape, IOResult, UniformFanInShape, UniformFanOutShape}
import akka.util.{ByteString, Timeout}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.math.Numeric.BigDecimalAsIfIntegral.mkNumericOps

object Main extends App {
  implicit val system = ActorSystem("actor-system")
  val delimiter: Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(ByteString("\n"), 256)

  //source
  val dependencies: Source[LibraryDependencies, Future[IOResult]] = FileIO.fromPath(Paths.get("maven_dependencies.txt"))
    .via(delimiter)
    .map(_.utf8String)
    .map(a => a.split(",").toList)
    .map(a => new LibraryDependencies(a(0), a(2), 0, 0, 0, 0))

  //group by
  val groups = dependencies
    .groupBy(Int.MaxValue, a => a.library)

  val counter: Flow[LibraryDependencies, LibraryDependencies, NotUsed] =
    Flow[LibraryDependencies]
      .reduce((a, b) => (new LibraryDependencies(a.library, a.dependencyType, a.compile + b.compile, a.test + b.test, a.runtime + b.runtime, a.provided + b.provided)))

  def checkDependencyType(libDep: LibraryDependencies, dep: String) = if (libDep.dependencyType.equals(dep)) 1 else 0

  //initialize filtered count to 1
  val initializeFilter: Flow[LibraryDependencies, LibraryDependencies, NotUsed] =
    Flow[LibraryDependencies]
      .map(a => (new LibraryDependencies(a.library, a.dependencyType, checkDependencyType(a, "Compile"), checkDependencyType(a, "Test")
        , checkDependencyType(a, "Runtime"), checkDependencyType(a, "Provided"))))

  val calculateDependencyCounters: Flow[LibraryDependencies, LibraryDependencies, NotUsed] =
    Flow[LibraryDependencies]
      .via(initializeFilter)
      .via(counter)

  val depndencyFile = FileIO.toFile(new File("dependencyResult.txt"))
  // output for file
  val depndencySink = Flow[LibraryDependencies]
    .map(i => ByteString(i.toString + "\n"))
    .toMat(depndencyFile)((_, bytesWritten) => bytesWritten)

  val statisticsFile = FileIO.toFile(new File("statisticsResult.txt"))
  // output for file
  val statisticsSink = Flow[LibraryDependencies]
    .map(i => ByteString(i.toString + "\n"))
    .toMat(statisticsFile)((_, bytesWritten) => bytesWritten)

  val collectedDependencies =
    groups
      .via(calculateDependencyCounters)
      .mergeSubstreams

  //  collectedDependencies.runWith(depndencySink)
  //

  //flow for calculate minimum number of dependencies
  val minimumNumberOfDependencies: Flow[LibraryDependencies, Int, NotUsed] =
    Flow[LibraryDependencies]
      .map(a => a.sumOfDependencies)

  val minimumSink = Sink.reduce((a: Int, b: Int) => (math.min(a, b)))
  val minFuture = collectedDependencies
    .map(a => a.sumOfDependencies)
    .runWith(minimumSink)
  //  minFuture.foreach(a=>println(a))


  val howManyLibsHaveMinimumDependencies: Flow[LibraryDependencies, Int, NotUsed] =
    Flow[LibraryDependencies]
      //      .fold(0)((acc, l) => if (l.sumOfDependencies == minimumNumberOfDependencies) acc + 1 else acc)
      .filter(a => minFuture.asInstanceOf[Int] == a.sumOfDependencies)
      .fold(0)((acc, b) => acc + 1)
  //
  //  //
  collectedDependencies
    .via(howManyLibsHaveMinimumDependencies)
    .map(a => new Statistics(a, 0, 0, 0, 0))
    .to(Sink.foreach(a => println(s"Considered minimum number of dependencies: ${a.minimumDependencies}")))
    .run()
  //
  //  val averageOfDependencies: Flow[LibraryDependencies, Statistics, NotUsed] =
  //    Flow[LibraryDependencies]
  //      .reduce((a, b) => new LibraryDependencies(a.library, "",
  //        (a.compile + b.compile) / 2, (a.test + b.test) / 2, (a.runtime + b.runtime) / 2, (a.provided + b.provided) / 2))
  //      .map(a => new Statistics(0, a.compile, a.test, a.runtime, a.provided))
  //
  //  collectedDependencies
  //    .via(averageOfDependencies)
  //    .to(Sink.foreach(a => println(a)))
  //    .run()

  //    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
  //      import GraphDSL.Implicits._
  //      val in = dependencies
  //      val out = Sink.foreach(a => println(a))
  //
  //      val minimumFanOut: UniformFanOutShape[LibraryDependencies, LibraryDependencies] = builder.add(Broadcast[LibraryDependencies](2))
  //      val minimumFanIn: FanInShape2[Int, LibraryDependencies, Int] =
  //        builder.add(ZipWith((a: Int, b: LibraryDependencies) => a))
  //
  //      val f1 = minimumNumberOfDependencies
  //
  //      in ~> minimumFanOut ~> f1 ~> minimumFanIn ~> out
  //      minimumFanOut ~> minimumFanIn
  //      ClosedShape
  //    })


  //    val flow = Flow.fromGraph(GraphDSL.create() { implicit b =>
  //      import GraphDSL.Implicits._
  //
  //      val workerCount = 2
  //
  //      val broadcast = b.add(Broadcast[LibraryDependencies](workerCount))
  //      val merge = b.add(Merge[LibraryDependencies](workerCount))
  //
  //      for (_ <- 1 to workerCount) {
  //        dependencies ~> Flow[LibraryDependencies].async ~> merge
  //      }
  //
  //      FlowShape(broadcast.in, merge.out)
  //    })


}