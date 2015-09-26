package Extractor.Plugin

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

class PluginPhase(val global: Global) extends PluginComponent {
  t =>

  val runsAfter = List("typer")

  override val runsRightAfter = Some("typer")

  val phaseName = "canve-extractor"

  def units = global.currentRun
    .units
    .toSeq
    .sortBy(_.source.content.mkString.hashCode())

  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def run() {

      val projectName = PluginArgs.projectName

      Log("extraction starting for project " + projectName + " (" + units.length + " compilation units)")

      Log(t.global.currentSettings.toString) // TODO: remove or move to new compiler plugin dedicated log file

      val graph = units.foldLeft(Graph(Vector.empty, Vector.empty)) {case (graphAcc, unit) =>

        Thread.sleep(5000)

        if (unit.source.path.endsWith(".scala")) {
          Log("examining source file" + unit.source.path + "...")
          val graph = TraversalExtractionWrapper(t.global)(unit)(projectName)
          graphAcc + graph
        } else {
          Log("skipping non-scala source file: " + unit.source.path)
          graphAcc
        }
      }

      Output.write(graph)
    }

    def name: String = "canve"
  }
}