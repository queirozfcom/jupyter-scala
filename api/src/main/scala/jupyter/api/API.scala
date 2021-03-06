package jupyter.api

import ammonite.pprint.{Config, PPrint, TPrint}

import scala.reflect.runtime.universe.WeakTypeTag

trait API {
  /**
   * History of commands that have been entered
   */
  def history: Seq[String]

  /**
   * Tools related to loading external scripts and code
   */
  implicit def load: ammonite.api.Load

  /**
   * Exposes some internals of the current interpreter
   */
  implicit def interpreter: ammonite.api.Interpreter


  /**
   * Controls how things are pretty-printed
   */
  implicit var pprintConfig: ammonite.pprint.Config

  /**
   * Prettyprint the given `value` with no truncation. Optionally takes
   * a number of lines to print.
   */
  def show[T](value: T, lines: Int = 0): ammonite.pprint.Show[T]


  /**
   * Opaque container of the currently processed Jupyter message.
   *
   * Required to send display data or Jupyter comm messages. Opaque
   * not to add extra dependencies.
   */
  implicit def evidence: Evidence

  /**
   * Jupyter publishing helper
   *
   * Allows to push display items to the front-end or to communicate with
   * widgets through Jupyter comms (WIP)
   */
  implicit def publish: jupyter.api.Publish[Evidence]

  val display: Display = new Display {}
}

trait Internal{
  def combinePrints(iters: Iterator[String]*): Iterator[String]
  def print[T: TPrint: PPrint: WeakTypeTag](value: => T, ident: String, custom: Option[String])(implicit cfg: Config): Iterator[String]
  def printDef(definitionLabel: String, ident: String): Iterator[String]
  def printImport(imported: String): Iterator[String]
}

trait FullAPI extends API {
  def Internal: Internal
}

class APIHolder {
  @transient var shell0: FullAPI = null
  @transient lazy val shell = shell0
}

object APIHolder {
  def initReplBridge(holder: Class[APIHolder], api: FullAPI) =
    holder
      .getDeclaredMethods
      .find(_.getName == "shell0_$eq")
      .get
      .invoke(null, api)
}

/**
 * Opaque container of a Jupyter message. Opaque not to add
 * extra dependencies.
 */
final class Evidence private[jupyter] (private[jupyter] val underlying: Any)

trait Display {
  import Base64._

  /*
   * FIXME The publish.display method only accepts data of type String.
   * Support should be added for Seq[String] (base64 encoded data looks better
   * this way) and Json.
   */

  def html(html: String)
          (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    publish.display("", "text/html" -> html)
  }
  def html(node: scala.xml.Node)
          (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    html(node.toString())
  }
  def markdown(md: String)
              (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    publish.display("", "text/markdown" -> md)
  }
  def md(md: String)
        (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    markdown(md)
  }
  def svg(svg: String)
         (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    publish.display("", "image/svg+xml" -> svg)
  }
  def svg(node: scala.xml.Node)
         (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    svg(node.toString())
  }
  def png(data: Array[Byte])
         (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    publish.display("", "image/png" -> data.toBase64)
  }
  def png(data: java.awt.image.BufferedImage)
         (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    import java.io.ByteArrayOutputStream
    import javax.imageio.ImageIO
    val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
    ImageIO.write(data, "png", baos)
    png(baos.toByteArray)
  }
  def jpg(data: Array[Byte])
         (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    publish.display("", "image/jpeg" -> data.toBase64)
  }
  def jpg(data: java.awt.image.BufferedImage)
         (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    import java.io.ByteArrayOutputStream
    import javax.imageio.ImageIO
    val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
    ImageIO.write(data, "jpg", baos)
    jpg(baos.toByteArray)
  }
  def latex(latex: String)
           (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    publish.display("", "text/latex" -> latex)
  }
  def pdf(data: Array[Byte])
         (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    publish.display("", "application/pdf" -> data.toBase64)
  }
  def javascript(code: String)
                (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    publish.display("", "application/javascript" -> code)
  }
  def js(code: String)
        (implicit publish: jupyter.api.Publish[Evidence], ev: Evidence): Unit = {
    javascript(code)
  }
}