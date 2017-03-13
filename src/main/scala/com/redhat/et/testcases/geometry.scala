package com.redhat.et.testcases

import scala.language.implicitConversions

/** 
    Object to calculate the real-world distance between two points.  Assumes 
    that the Earth is flat[1] and that you aren't going across the 
    180º meridian or either pole.  In practice, I have few GPS traces
    from any of those locations.

    [1] ok, ok, that the Earth is an ellipsoid projected to a plane
*/
trait RealWorldDistance {
    import math.{cos, sin, asin, atan2}
    import math.sqrt
    import math.pow
    import math.{toRadians, toDegrees}
    
    // earth's radius in km
    val R: Double = 6371.009
    
    /* calculates the distance between two points, given as lat/lon pairs in degrees */
    def distance(pt1:(Double,Double),pt2:(Double,Double)): Double = {
        val (lat1,lon1) = pt1
        val (lat2,lon2) = pt2

        val latDelta = lat2 - lat1
        val lonDelta = lon2 - lon1
        
        val meanLat = toRadians((lat1 + lat2)) / 2

        val K1 = 111.13209 - (0.56605 * cos(2 * meanLat)) + (0.00120 * cos(4 * meanLat))
        val K2 = (111.41513 * cos(meanLat)) - (0.09455 * cos(3 * meanLat)) + (0.00012 * cos(5 * meanLat))
                
        sqrt(pow(K1 * latDelta, 2) + pow(K2 * lonDelta, 2))
    }
    
    def haversine(pt1:(Double,Double),pt2:(Double,Double)): Double = {
        val (lat1, lon1) = (toRadians(pt1._1), toRadians(pt1._2))
        val (lat2, lon2) = (toRadians(pt2._1), toRadians(pt2._2))
         
        val latDelta = lat2 - lat1
        val lonDelta = lon2 - lon1
        
        val a = pow(sin(latDelta/2), 2) + cos(lat1) * cos(lat2) * pow(sin(lonDelta/2), 2)
        val c = 2 * asin(sqrt(a))
        
        R * c
    }
    
    def bearing(pt1:(Double,Double),pt2:(Double,Double)): Double = {
        val (lat1, lon1) = (toRadians(pt1._1), toRadians(pt1._2))
        val (lat2, lon2) = (toRadians(pt2._1), toRadians(pt2._2))
        
        val latDelta = lat2 - lat1
        val lonDelta = lon2 - lon1
        
        val y = sin(lonDelta) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lonDelta)
        
        atan2(y, x).toDegrees
    }
}

object RWDistance extends RealWorldDistance {}

sealed case class Coordinates(lat: Double, lon: Double) extends Ordered[Coordinates] {
  import scala.math.Ordered.orderingToOrdered
  
  import RWDistance.{distance => rw_distance}
  
  /**
    Approximate distance between this and other in meters
  */
  def distance(other:Coordinates) = rw_distance((lat, lon), (other.lat, other.lon))

  /**
    Ordering based on longitude then latitude
  */
  def compare(other: Coordinates) = 
    (this.lon, this.lat) compare (other.lon, other.lat)
  
  /** 
    Ordering based on latitude (then longitude, if necessary) 
  */
  def compare_lat(other: Coordinates) = 
    (this.lat, this.lon) compare (other.lat, other.lon)
}


trait GenericAnnotatable[T,K,V] {
  type AnnotationKey = K
  type AnnotationValue = V
  
  def annotate(k: K, v: V): T
}

trait Annotatable[T] extends GenericAnnotatable[T, String, String] {}

trait GeometryPrimitives {
  def csub(a:Coordinates,b:Coordinates) = 
    Coordinates(a.lat - b.lat, a.lon - b.lon)
  
  def ccross(a:Coordinates,b:Coordinates) = 
    a.lat * b.lon - b.lat * a.lon
  
  def clockwise(o:Coordinates,a:Coordinates,b:Coordinates) =
    ccross(csub(a,o), csub(b,o)) <= 0
  
  def isLeft(p0:Coordinates, p1:Coordinates, p2:Coordinates) =
    (p1.lat - p0.lat) * (p2.lon - p0.lon) - (p2.lat - p0.lat) * (p1.lon - p0.lon)
}

sealed case class Polygon(points: List[Coordinates], properties: Map[String, String] = Map()) extends GeometryPrimitives with Annotatable[Polygon] {
  lazy val closedPoints =
    this.points ++ List(this.points.head)

  lazy val isCW = 
    (closedPoints sliding 3).forall {case List(a:Coordinates, b:Coordinates, c:Coordinates) => clockwise(a,b,c)}

  lazy val isCCW =
    (closedPoints sliding 3).forall {case List(a:Coordinates, b:Coordinates, c:Coordinates) => !clockwise(a,b,c)}
  
  lazy val pointSet =
    this.points.toSet
  
    /**
      calculate whether or not p is in this polygon via the winding number method; adapted from http://geomalgorithms.com/a03-_inclusion.html
    */
  def includesPoint(p: Coordinates) = {
    
    def windingNum(p: Coordinates, poly: List[List[Coordinates]], wn: Int): Int = poly match {
      case List(v1, v2)::ps => 
        if (v1.lon <= p.lon && v2.lon > p.lon && isLeft(v1, v2, p) > 0) {
          // Console.println(s"incrementing wn (was $wn)")
          windingNum(p, ps, wn + 1)
        } else if (!(v1.lon <= p.lon) && v2.lon <= p.lon && isLeft(v1, v2, p) < 0) {
          // Console.println(s"decrementing wn (was $wn)")
          windingNum(p, ps, wn - 1)
        } else {
          // Console.println(s"not changing wn (was $wn)")
          windingNum(p, ps, wn)
        }
      case Nil => wn
    }
    
    pointSet.contains(p) || windingNum(p, (closedPoints sliding 2).toList, 0) != 0
  }
  
  val length = points.length
  
  def annotate(k: AnnotationKey, v: AnnotationValue): Polygon =
    Polygon(this.points, this.properties + Pair(k, v))
  
}

object ConvexHull extends GeometryPrimitives {
  def calculate(points: List[Coordinates]): Polygon = {
    val sortedPoints = points.sorted.distinct
    
    if (sortedPoints.length <= 2) {
      new Polygon(sortedPoints)
    } else {
      val lowerHull = buildHull(sortedPoints, List())
      val upperHull = buildHull(sortedPoints.reverse, List())
      
      new Polygon((lowerHull ++ upperHull).distinct)
    }
  }
    
  private[this] def buildHull(points: List[Coordinates], hull: List[Coordinates]): List[Coordinates] = {
    (points, hull) match {
      case (Nil, _::tl) => tl.reverse
      case (p::ps, first::second::rest) => {
        if(clockwise(second, first, p)) {
          buildHull(points, second::rest)
        } else {
          buildHull(ps, p::hull)
        }
      }
      case (p::ps, ls) if ls.length < 2 => buildHull(ps, p::ls)
    }
  }
}



class Geometry extends Testcase {
  import org.scalacheck.Properties
  import org.scalacheck.Prop.forAll
  
  import org.scalacheck._
  
  import org.scalacheck.Gen
  import org.scalacheck.Gen._
  
  import org.scalacheck.Arbitrary
  import org.scalacheck.Arbitrary._
  
  object HullSpec extends Properties("ConvexHull") with GeometryPrimitives {
    implicit def genCoords(): Gen[Coordinates] = for {
      lat <- Gen.choose(42.827055, 43.196613)
      lon <- Gen.choose(-90.220861, -89.44301)
    } yield Coordinates(lat, lon)
    
    implicit lazy val arbCoordList = Arbitrary { Gen.listOf(genCoords) }
    
    val unique = forAll { 
      (points: List[Coordinates]) =>
	val hull = ConvexHull.calculate(points)
      hull.points.length == hull.points.toSet.size
    }
    
    val subset = forAll { 
      (points: List[Coordinates]) =>
	val hull = ConvexHull.calculate(points)
      hull.points.toSet.diff(points.toSet).size == 0
    }
    
    val decreasing = forAll { 
      (points: List[Coordinates]) =>
	val hull = ConvexHull.calculate(points)
      hull.points.length <= points.length
    }
    
    val isConvex = forAll { 
      (points: List[Coordinates]) =>
	val hull = ConvexHull.calculate(points)
      if (hull.points.length > 3) {
	hull.isCCW
      } else {
	true
      }
    }
    
   val containsAllPoints = forAll { 
      (points: List[Coordinates]) =>
	val hull = ConvexHull.calculate(points)
      if (hull.points.length > 3) {
	points.forall(p => hull.includesPoint(p))
      } else {
	true
      }
    }
  }

  def run {
    HullSpec.unique.check
    HullSpec.subset.check
    HullSpec.decreasing.check
    HullSpec.isConvex.check
    HullSpec.containsAllPoints.check
  }
}
