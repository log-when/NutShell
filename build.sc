import mill._, scalalib._
import coursier.maven.MavenRepository

trait CommonModule extends ScalaModule {
  override def scalaVersion = "2.12.13"

  override def scalacOptions = Seq("-Xsource:2.11")
}

trait HasXsource211 extends ScalaModule {
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-language:reflectiveCalls",
      "-Xsource:2.11"
    )
  }
}

trait HasChisel3 extends ScalaModule {
  override def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(
      MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
    )
  }
  override def ivyDeps = Agg(
    ivy"edu.berkeley.cs::chisel3:3.5-SNAPSHOT",
    ivy"org.scalatest::scalatest:3.1.4",
    ivy"edu.berkeley.cs::chisel-iotesters:1.2+",
    ivy"com.lihaoyi::utest:0.7.9",
    ivy"edu.berkeley.cs::treadle:1.5-SNAPSHOT",
    ivy"net.java.dev.jna:jna:5.10.0"
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"edu.berkeley.cs:::chisel3-plugin:3.5-SNAPSHOT",
    ivy"org.scalamacros:::paradise:2.1.1"
  )
}

trait HasChiselTests extends CrossSbtModule {
  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.1.4",
      ivy"edu.berkeley.cs::chisel-iotesters:1.2+",
      //ivy"edu.berkeley.cs::chiseltest:0.5-SNAPSHOT"
    )
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

object difftest extends SbtModule with CommonModule with HasChisel3 {
  override def millSourcePath = os.pwd / "difftest"
}

object riscvSpecCore extends SbtModule with CommonModule with HasChisel3 {
  override def millSourcePath = os.pwd / "riscv-spec-core"
}

object chiseltest extends SbtModule with CommonModule with HasChisel3 {
  
  override def millSourcePath = os.pwd / "chisel-testers2"
}

object chiselModule extends CrossSbtModule with HasChisel3 with HasChiselTests with HasXsource211 {
  def crossScalaVersion = "2.12.13"
  override def moduleDeps = super.moduleDeps ++ Seq(
    difftest,    
    chiseltest,
    riscvSpecCore
  )
}
