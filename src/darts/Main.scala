package darts

import java.io.{File, PrintWriter}

import org.bytedeco.javacpp.indexer.DoubleRawIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs.{IMREAD_COLOR, imread, imwrite}
import org.bytedeco.javacpp.opencv_imgproc.{circle, getPerspectiveTransform, _}

import scala.collection.mutable
import scala.collection.mutable.HashSet
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global



/**
  * Created by vassdoki on 2016.12.13..
  *
  */
object Main extends App{
  val Cyan    = new Scalar(255, 255, 0, 0)
  val Purple    = new Scalar(255, 0, 255, 0)
  val Yellow  = new Scalar(0, 255, 255, 0)
  val Red = new Scalar(0, 0, 255, 0)
  val Green = new Scalar(0, 255, 0, 0)
  val Black = new Scalar(0, 0, 0, 0)
  val BlackMat = new Mat(Black)
  val PrevMaskMat = new Mat(new Scalar(100, 100, 100, 0))

  var prevDartsCount1 = 0
  var prevDartsCount2 = 0

//  saveCalibratingImages
//  config
  //  test2

  if (args.length != 1) {
    printHelp
  } else {
    args(0) match {
      case "-config" => config
      case "-calib" => saveCalibratingImages
      case "-run" => run
      case _ => printHelp
    }
  }

  def printHelp = {
    println
      """
        |Usage: java -jar darts.jar [command]
        |Commands:
        | -run       Run the recognition
        | -config    Show the captured images, but do not report results
        | -calib     Save calibration data
        |
        | You may change the content of the config.xml any time, the program
        | will reload it.
      """.stripMargin
  }

  def saveCalibratingImages = {
    val points = List(6, 13, 4, 18, 1, 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10, "bu", "finished")

    val capture1 = new CaptureDevice("1")
    val camConf1 = new Config("1")
    val dartFinder1 = new DartFinder(capture1, camConf1)
    dartFinder1.state = dartFinder1.State.ALWAYS_ON
    val capture2 = new CaptureDevice("2")
    val camConf2 = new Config("2")
    val dartFinder2 = new DartFinder(capture2, camConf2)
    dartFinder2.state = dartFinder2.State.ALWAYS_ON
    var i1 = new Mat
    var i2 = new Mat
    var x1 = 0f
    var x2 = 0f
    i1 = capture1.captureFrame(i1)
    i2 = capture2.captureFrame(i2)
    val maxCounter = 25
    var counter = maxCounter
    var savedImages = 0
    val debug2 = new Mat(i1.rows, i1.cols * 2, 16)
    var result = ""
    while(savedImages < 21) {
      debug2.setTo(BlackMat)

      //i1 = calib1.remap(capture1.captureFrame(i1))
      i1 = capture1.captureFrame(i1)
      i2 = capture2.captureFrame(i2)

      x1 = dartFinder1.proc(i1)
      x2 = dartFinder2.proc(i2)

      if (counter <= 0 && savedImages < points.size - 1) {
        result = result + f"CalibPoint1;${points(savedImages)};${x1/camConf1.int("area/width")}%15f;$x1;${camConf1.int("area/zeroy")}\n"
        result = result + f"CalibPoint2;${points(savedImages)};${x2/camConf2.int("area/width")}%15f;$x2;${camConf2.int("area/zeroy")}\n"
//        println(result)
//        imwrite(f"${Config.str("CALIB_DIR")}/d1-${points(savedImages)}.jpg", i1)
//        imwrite(f"${Config.str("CALIB_DIR")}/d2-${points(savedImages)}.jpg", i2)
        savedImages += 1
        counter = maxCounter
      }
//      drawKivagas(i1, camConf1)
//      drawKivagas(i2, camConf2)
//
//      println(s"x1: $x1 x2: $x2")
//      circle(i1, new Point(x1.toInt, camConf1.int("zeroy")), 3, Red, 4, LINE_AA, 0)
//      circle(i2, new Point(x2.toInt, camConf2.int("zeroy")), 3, Red, 4, LINE_AA, 0)

      val d1 = dartFinder1.debugLastProc
      val d2 = dartFinder2.debugLastProc

      d1.copyTo(debug2(new Rect(0,0,i1.cols,i1.rows)))
      d2.copyTo(debug2(new Rect(i1.cols,0,i1.cols,i1.rows)))
      putText(debug2, s"$counter next target: ${points(savedImages)}", new Point(30, 100),
        FONT_HERSHEY_PLAIN, // font type
        2, // font scale
        Red, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)
      putText(debug2, s"x1: $x1     x2: $x2", new Point(30, 140),
        FONT_HERSHEY_PLAIN, // font type
        2, // font scale
        Red, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)
      Util.show(debug2, s"cam config")
      CvUtil.releaseMat(d1)
      CvUtil.releaseMat(d2)
      counter -= 1
    }
    debug2.release()
    val pw = new PrintWriter(new File("calib_points.csv" ))
    pw.write(result)
    pw.close()
  }

  def config = {
    val capture1 = new CaptureDevice("1")
    val camConf1 = new Config("1")
    val dartFinder1 = new DartFinder(capture1, camConf1)
    dartFinder1.state = dartFinder1.State.ALWAYS_ON
    val capture2 = new CaptureDevice("2")
    val camConf2 = new Config("2")
    val dartFinder2 = new DartFinder(capture2, camConf2)
    dartFinder2.state = dartFinder2.State.ALWAYS_ON
    var i1 = new Mat
    var i2 = new Mat
    while(true) {
      dartFinder1.proc(null)
      i1 = dartFinder1.debugLastProc
      Util.show(i1, s"i1")
      dartFinder2.proc(null)
      i2 = dartFinder2.debugLastProc
      Util.show(i2, s"i2")
    }
  }

  def drawCamDebug(m: Mat, conf: Config) = {
    circle(m, new Point(conf.int("camx"), conf.int("camy")), 3, Red, 4, LINE_AA, 0)
    circle(m, new Point(conf.int("leftx"), conf.int("lefty")), 3, Red, 4, LINE_AA, 0)
    circle(m, new Point(conf.int("rightx"), conf.int("righty")), 3, Red, 4, LINE_AA, 0)
    line(m, new Point(conf.int("camx"), conf.int("camy")),
      new Point(conf.int("leftx"), conf.int("lefty")), Cyan, 1, 4, 0)
    line(m, new Point(conf.int("camx"), conf.int("camy")),
      new Point(conf.int("rightx"), conf.int("righty")), Cyan, 1, 4, 0)

  }

  def run = {
    Profile.start("01 - App")
    println(s"CPU cores: ${Runtime.getRuntime.availableProcessors()}")

    val camConf1 = new Config("1")
    val camConf2 = new Config("2")

    val dartFinder1 = new DartFinder(new CaptureDevice("1"), camConf1)
    val dartFinder2 = new DartFinder(new CaptureDevice("2"), camConf2)

    var i3 = new Mat
    var debug3:Mat = null

    val debug2 = new Mat(800, 800, 16)
    debug2.setTo(BlackMat)
    CvUtil.drawTable(debug2, Cyan, 2)
    CvUtil.drawNumbers(debug2, Yellow)

    val start = System.currentTimeMillis()
    var imgCount = 0
    var prevPoint: Point = null
    var prevTrPoint: Point = null

    val points = new mutable.HashSet[Point]

    var emptyBoardSent = true

    try {
      while (true) {
        List(dartFinder1, dartFinder2).par.foreach(df => df.proc(null))

        imgCount += 1
        if (dartFinder1.state == dartFinder1.State.EMPTY) {
          prevDartsCount1 = 0
        }
        if (dartFinder2.state == dartFinder1.State.EMPTY) {
          prevDartsCount2 = 0
        }
        if (Config.bool("DEBUG_DART_FINDER")) {
          val i1 = dartFinder1.debugLastProc
          Util.show(i1, s"i1")
          val i2 = dartFinder1.debugLastProc
          Util.show(i2, s"i2")

        }
//        if ((dartFinder1.prevNonZeroCount >0 || dartFinder2.prevNonZeroCount > 0) && dartFinder1.prevNonZeroCount < 20000) {
//          println(s"Cam1: State: ${dartFinder1.state} [${dartFinder1.maskState}] zero: ${dartFinder1.prevNonZeroCount} Darts:${dartFinder1.dartsCount}" +
//            s"       Cam2: State: ${dartFinder2.state} [${dartFinder2.maskState}] zero: ${dartFinder2.prevNonZeroCount} Darts:${dartFinder2.dartsCount}")
//        }
        if (dartFinder1.dartsCount == 0 && dartFinder2.dartsCount == 0 && !emptyBoardSent) {
          sendEmtpyBoard
          emptyBoardSent = true
        }
        if (
          (prevDartsCount1 < dartFinder1.dartsCount || prevDartsCount2 < dartFinder2.dartsCount)
            && (dartFinder1.state == dartFinder1.State.STABLE && dartFinder2.state == dartFinder2.State.STABLE)
            && (dartFinder1.dartsCount <= 3 && dartFinder2.dartsCount <= 3)
        ) {
          emptyBoardSent = false
          var xc = (dartFinder2.lineFromCamB - dartFinder1.lineFromCamB) / (dartFinder1.lineFromCamA - dartFinder2.lineFromCamA)
          var yc = xc * dartFinder1.lineFromCamA + dartFinder1.lineFromCamB
          val point = new Point(xc.toInt, yc.toInt)

          val (mod, num) = DartsUtil.identifyNumber(point)
          println(f"${(imgCount.toFloat / (System.currentTimeMillis() - start)) * 1000}%.1f fps num: $num mod: $mod   (${dartFinder1.dartsCount}[${dartFinder1.state}] ${dartFinder2.dartsCount}[${dartFinder2.state}])")
          prevDartsCount1 = dartFinder1.dartsCount
          prevDartsCount2 = dartFinder2.dartsCount

          sendScore(s"num=$num&mod=$mod&x=${point.x}&y=${point.y}&cam1img=&cam2img=&cam1x=&cam2x=&msg=Hello")

          if (Config.bool("DEBUG_MAIN")) {
            debug2.setTo(BlackMat)
            CvUtil.drawTable(debug2, Cyan, 2)
            CvUtil.drawNumbers(debug2, Yellow)

            //            circle(debug2, new Point(x1.toInt, y1.toInt), 3, Red, 4, LINE_AA, 0)
            line(debug2, new Point(camConf1.int("camx"), camConf1.int("camy")), new Point(50, (50 * dartFinder1.lineFromCamA + dartFinder1.lineFromCamB).toInt), Cyan, 1, 4, 0)
            line(debug2, new Point(camConf1.int("leftx"), camConf1.int("lefty")), new Point(camConf1.int("rightx"), camConf1.int("righty")), Yellow, 1, 4, 0)

            //            circle(debug2, new Point(x2.toInt, y2.toInt), 3, Red, 4, LINE_AA, 0)
            line(debug2, new Point(camConf2.int("camx"), camConf2.int("camy")), new Point(750, (750 * dartFinder2.lineFromCamA + dartFinder2.lineFromCamB).toInt), Cyan, 1, 4, 0)
            line(debug2, new Point(camConf2.int("leftx"), camConf2.int("lefty")), new Point(camConf2.int("rightx"), camConf2.int("righty")), Yellow, 1, 4, 0)

            points.add(point)
            if (prevPoint != null) circle(debug2, prevPoint, 2, Green, 2, LINE_AA, 0)
            points.foreach(p => circle(debug2, p, 1, Green, 1, LINE_AA, 0))
            circle(debug2, point, 4, Red, 5, LINE_AA, 0)
            prevPoint = point

            putText(debug2, s"$num x $mod", new Point(20, 90),
              FONT_HERSHEY_PLAIN, // font type
              4, // font scale
              Yellow, // text color (here white)
              4, // text thickness
              8, // Line type.
              false)
            putText(debug2, f"${(imgCount.toFloat / (System.currentTimeMillis() - start)) * 1000}%.1f fps", new Point(400, 30),
              FONT_HERSHEY_PLAIN, // font type
              2, // font scale
              Red, // text color (here white)
              2, // text thickness
              8, // Line type.
              false)
            putText(debug2, s"${xc.toInt} x ${yc.toInt}", new Point(10, 30),
              FONT_HERSHEY_PLAIN, // font type
              3, // font scale
              Red, // text color (here white)
              3, // text thickness
              8, // Line type.
              false)

            Util.show(debug2, s"debug2")
          }
        }
        if (Config.bool("DEBUG_FPS")) {
          println(f"${(imgCount.toFloat / (System.currentTimeMillis() - start)) * 1000}%.1f fps")
        }
      }
    }catch {
      case e: Exception =>
        println(e)
        e.printStackTrace()
    }
    Profile.end("01 - App")
    Profile.print
  }

  /**
    * Send the score to the server
    * @param getParams
    */
  def sendScore(getParams: String): Unit = {
    if (Config.bool("SERVER_USE")) {
      Future({
        val url = s"${Config.str("SERVER_HOST")}/command/newThrow/${Config.str("BOARD_ID")}?$getParams"
        try {
          val res: String = scala.io.Source.fromURL(url).mkString
        } catch {
          case e: Exception =>
            println(e)
            println(s"url: $url")
        }})
    }
  }


  /**
    * Send nextPlayer command to the server if the dart board is empty again
    */
  def sendEmtpyBoard = {
    if (Config.bool("SERVER_USE")) {
      val url = s"${Config.str("SERVER_HOST")}/command/nextPlayer/${Config.str("BOARD_ID")}"
      try {
        val res: String = scala.io.Source.fromURL(url).mkString
      } catch {
        case e: Exception =>
          println(e)
          println(s"url: $url")
      }
    }
  }


}
