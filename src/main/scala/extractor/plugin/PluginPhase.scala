package extractor.plugin
import scala.collection.{SortedSet, mutable}
import scala.tools.nsc.{Global, Phase}
import tools.nsc.plugins.PluginComponent

class PluginPhase(val global: Global)
                  extends PluginComponent
                  { t =>

  import global._

  val runsAfter = List("typer")

  override val runsRightAfter = Some("typer")
  
  val phaseName = "canve-extractor"

  def units = global.currentRun
                    .units
                    .toSeq
                    .sortBy(_.source.content.mkString.hashCode())

  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def run() {
      
      println("\ncanve extraction starting for project " + PluginArgs.projectName + "...")
      
      println(t.global.currentSettings) 
      
      units.foreach { unit =>
        if (unit.source.path.endsWith(".scala")) {
          println("canve examining source file" + unit.source.path + "...")
          TraversalExtractionWriter(t.global)(unit.body)  
        } else
            println("canve skipping non-scala source file: " + unit.source.path)
      }
    }

    def name: String = "canve" 
  }

}