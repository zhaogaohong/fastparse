package fastparse

import fastparse.Implicits.Sequencer
import fastparse.core.Parser
import fastparse.parsers.Combinators.Sequence
import utest._

class ParserApiImpl2[+T](p0: Parser[T]) extends ParserApiImpl(p0){
  override def ~[V, R](p: P[V])
                      (implicit ev: Sequencer[T, V, R])
                      : Parser[R] =
    p0 ~ " ".rep ~ p

  override def ~![V, R](p: P[V])
                       (implicit ev: Sequencer[T, V, R])
                       : Parser[R] =
    p0 ~! " ".rep ~ p
}
object ParserApiImpl2{
  implicit def parserApi[T, V](p0: T)
                              (implicit c: T => Parser[V]): ParserApi[V] =
    new ParserApiImpl2[V](p0)
}
/**
 * Same as MathTests, but demonstrating the use of whitespace
 */
object WhiteSpaceMathTests extends TestSuite{
  import ParserApiImpl2._
  def eval(tree: (Int, Seq[(String, Int)])) = {
    val (base, ops) = tree
    ops.foldLeft(base){ case (left, (op, right)) => op match{
      case "+" => left + right  case "-" => left - right
      case "*" => left * right case "/" => left / right
    }}
  }

  val number: P[Int] = P( CharIn('0'to'9').rep(1).!.map(_.toInt) )
  val parens: P[Int] = P( "(" ~! addSub ~ ")" )
  val factor: P[Int] = P( number | parens )

  val divMul: P[Int] = P( factor ~ (CharIn("*/").! ~! factor).rep ).map(eval)
  val addSub: P[Int] = P( divMul ~ (CharIn("+-").! ~! divMul).rep ).map(eval)
  val expr: P[Int]   = P( addSub ~ End )

  val tests = TestSuite{
    'pass {
      def check(str: String, num: Int) = {
        val Result.Success(value, _) = expr.parse(str)
        assert(value == num)
      }

      check("1   + 1", 2)
      check("1+   1*   2", 3)
      check("(1+   1  *  2)+(   3*4*5)", 63)
      check("15/3", 5)
      check("63  /3", 21)
      check("(1+    1*2)+(3      *4*5)/20", 6)
      check("((1+      1*2)+(3*4*5))/3", 21)
    }
    'fail{
      def check(input: String, trace: String) = {
        val failure = expr.parse(input, trace = false).asInstanceOf[Result.Failure]
        assert(trace == failure.trace)
      }
      check(
        "(+)",
        """(number | parens):1 ..."+)""""
      )
      check(
        "1+-",
        """(number | parens):2 ..."-""""
      )
    }
  }

}
