import mill._, scalalib._
import coursier.maven.MavenRepository

// import $ivy.`cn.ac.ios.tis:chisel3-plugin_2.13.8:3.6.0`

trait CommonModule extends ScalaModule {
  override def scalaVersion = "2.13.10"
  override def scalacPluginIvyDeps = Agg(ivy"cn.ac.ios.tis:::chisel3-plugin:3.7-SNAPSHOT")
  override def scalacOptions = Seq("-Xsource:2.13", "-P:chiselplugin:genBundleElements") 
}

trait HasXsource211 extends ScalaModule {
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq(
      "-deprecation",
      "-unchecked",
      "-Xsource:2.13"
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
    ivy"cn.ac.ios.tis:chisel3_2.13:3.7-SNAPSHOT"
 )
}

trait HasChiselTests extends CrossSbtModule  {
  object test extends Tests {
    override def scalacPluginIvyDeps = Agg(ivy"cn.ac.ios.tis:::chisel3-plugin:3.7-SNAPSHOT")
    override def scalacOptions = Seq("-Xsource:2.13", "-P:chiselplugin:genBundleElements") 

    def unmanagedClasspath = T {
      if (!os.exists(millSourcePath / "lib")) {Agg()}
      else {Agg.from(os.list(millSourcePath / "lib" ).map(PathRef(_)))}
    }

    override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.14", ivy"cn.ac.ios.tis:chiseltest_2.13:0.7-SNAPSHOT")
    def testFramework = T {
      "org.scalatest.tools.Framework"
    }
  }
}

object difftest extends SbtModule with CommonModule with HasChisel3 {
  override def millSourcePath = os.pwd / "difftest"
}

object chiselModule extends CrossSbtModule with HasChisel3 with HasChiselTests with HasXsource211 {
  override def millSourcePath = os.pwd
  def crossScalaVersion = "2.13.10"

  
  
  override def scalacPluginIvyDeps = Agg(ivy"cn.ac.ios.tis:::chisel3-plugin:3.7-SNAPSHOT")
  override def scalacOptions = Seq("-Xsource:2.13", "-P:chiselplugin:genBundleElements") 
  override def moduleDeps = super.moduleDeps ++ Seq(
    difftest
  )
}

